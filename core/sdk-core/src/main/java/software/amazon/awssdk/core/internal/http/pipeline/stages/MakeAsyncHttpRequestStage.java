/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.timeCompletableFuture;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.OptionalUtils;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
@SdkInternalApi
public final class MakeAsyncHttpRequestStage<OutputT>
    implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> {

    private static final Logger log = Logger.loggerFor(MakeAsyncHttpRequestStage.class);

    private final SdkAsyncHttpClient sdkAsyncHttpClient;
    private final TransformingAsyncResponseHandler<OutputT> responseHandler;
    private final TransformingAsyncResponseHandler<? extends SdkException> errorResponseHandler;
    private final Executor futureCompletionExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    private final Duration apiCallAttemptTimeout;

    public MakeAsyncHttpRequestStage(TransformingAsyncResponseHandler<OutputT> responseHandler,
                                     TransformingAsyncResponseHandler<? extends SdkException> errorResponseHandler,
                                     HttpClientDependencies dependencies) {
        this.responseHandler = responseHandler;
        this.errorResponseHandler = errorResponseHandler;
        this.futureCompletionExecutor =
                dependencies.clientConfiguration().option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
        this.sdkAsyncHttpClient = dependencies.clientConfiguration().option(SdkClientOption.ASYNC_HTTP_CLIENT);
        this.apiCallAttemptTimeout = dependencies.clientConfiguration().option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT);
        this.timeoutExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request,
                                                        RequestExecutionContext context) throws Exception {
        InterruptMonitor.checkInterrupted();
        return executeHttpRequest(request, context);
    }

    private CompletableFuture<Response<OutputT>> executeHttpRequest(SdkHttpFullRequest request,
                                                                    RequestExecutionContext context) throws Exception {

        long timeout = apiCallAttemptTimeoutInMillis(context.requestConfig());
//        Completable completable = new Completable(timeout);

        ResponseHandler handler = new ResponseHandler();

        SdkHttpContentPublisher requestProvider = context.requestProvider() == null
                ? new SimpleHttpContentPublisher(request, context.executionAttributes())
                : context.requestProvider();
        // Set content length if it hasn't been set already.
        SdkHttpFullRequest requestWithContentLength = getRequestWithContentLength(request, requestProvider);

        AsyncExecuteRequest executeRequest = AsyncExecuteRequest.builder()
                .request(requestWithContentLength)
                .requestContentPublisher(requestProvider)
                .responseHandler(handler)
                .build();

        CompletableFuture<Void> executeFuture = sdkAsyncHttpClient.execute(executeRequest);

        return executeFuture.thenCompose(ignored -> handler.transformResult());


//        // Set the abortable so that the abortable request can be aborted after timeout if timeout is enabled
//        completable.abortable(abortableRunnable);
//
//        if (context.apiCallTimeoutTracker() != null && context.apiCallTimeoutTracker().isEnabled()) {
//            context.apiCallTimeoutTracker().abortable(abortableRunnable);
//        }
//
//        abortableRunnable.run();
    }

    private SdkHttpFullRequest getRequestWithContentLength(SdkHttpFullRequest request, SdkHttpContentPublisher requestProvider) {
        if (shouldSetContentLength(request, requestProvider)) {
            return request.toBuilder()
                          .putHeader("Content-Length", String.valueOf(requestProvider.contentLength()))
                          .build();
        }
        return request;
    }

    private boolean shouldSetContentLength(SdkHttpFullRequest request, SdkHttpContentPublisher requestProvider) {
        return requestProvider != null
               && !request.firstMatchingHeader("Content-Length").isPresent()
               // Can cause issues with signing if content length is present for these method
               && request.method() != SdkHttpMethod.GET
               && request.method() != SdkHttpMethod.HEAD;

    }

    /**
     * Detects whether the response succeeded or failed and delegates to appropriate response handler.
     */
    private class ResponseHandler implements TransformingAsyncResponseHandler<Response<OutputT>> {
        private volatile boolean isSuccess = false;
        private SdkHttpFullResponse response;

        @Override
        public void onHeaders(SdkHttpResponse response) {
            if (response.isSuccessful()) {
                SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received successful response: " + response.statusCode());
                isSuccess = true;
                responseHandler.onHeaders(response);
            } else {
                SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received error response: " + response.statusCode());
                errorResponseHandler.onHeaders(response);
            }
            this.response = (SdkHttpFullResponse) response;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            if (isSuccess) {
                // TODO handle exception as non retryable
                responseHandler.onStream(publisher);
            } else {
                errorResponseHandler.onStream(publisher);
            }
        }

        @Override
        public CompletableFuture<Response<OutputT>> transformResult() {
            if (isSuccess) {
                return responseHandler.transformResult()
                                      .thenApply(r -> Response.fromSuccess(r, response));
            }
            return errorResponseHandler.transformResult()
                                       .thenApply(e -> Response.fromFailure(e, response));
        }
    }

    /**
     * An interface similar to {@link CompletableFuture} that may or may not dispatch completion of the future to an executor
     * service, depending on the client's configuration.
     */
    private class Completable {
        private final CompletableFuture<Response<OutputT>> completableFuture = new CompletableFuture<>();
        private TimeoutTracker timeoutTracker;

        Completable(long timeoutInMills) {
            timeoutTracker = timeCompletableFuture(completableFuture, timeoutExecutor,
                                                   ApiCallAttemptTimeoutException.create(timeoutInMills),
                                                   timeoutInMills);
        }

        void abortable(Abortable abortable) {
            if (timeoutTracker != null) {
                timeoutTracker.abortable(abortable);
            }
        }

        void complete(Response<OutputT> result) {
            try {
                futureCompletionExecutor.execute(() -> completableFuture.complete(result));
            } catch (RejectedExecutionException e) {
                completableFuture.completeExceptionally(explainRejection(e));
            }
        }

        void completeExceptionally(Throwable exception) {
            try {
                futureCompletionExecutor.execute(() -> completableFuture.completeExceptionally(exception));
            } catch (RejectedExecutionException e) {
                completableFuture.completeExceptionally(explainRejection(e));
            }
        }

        private RejectedExecutionException explainRejection(RejectedExecutionException e) {
            return new RejectedExecutionException("The SDK was unable to complete the async future to provide you with a " +
                                                  "response. This may be caused by too-few threads in the response executor, " +
                                                  "too-short of a queue or too long of an operation in your future completion " +
                                                  "chain. You can provide a larger executor service to the SDK via the " +
                                                  "client's async configuration setting or you can reduce the amount of work " +
                                                  "performed on the async execution thread.", e);
        }
    }

    private long apiCallAttemptTimeoutInMillis(RequestOverrideConfiguration requestConfig) {
        return OptionalUtils.firstPresent(
            requestConfig.apiCallAttemptTimeout(), () -> apiCallAttemptTimeout)
                            .map(Duration::toMillis)
                            .orElse(0L);
    }
}
