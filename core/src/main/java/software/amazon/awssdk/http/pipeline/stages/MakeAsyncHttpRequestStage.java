/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.pipeline.stages;

import static software.amazon.awssdk.event.SdkProgressPublisher.publishProgress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkRequestChannel;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
public class MakeAsyncHttpRequestStage implements RequestPipeline<Request<?>, CompletableFuture<SdkHttpFullResponse>> {

    private final SdkAsyncHttpClient sdkAsyncHttpClient;

    public MakeAsyncHttpRequestStage(HttpClientDependencies dependencies) {
        this.sdkAsyncHttpClient = dependencies.sdkAsyncHttpClient();
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public CompletableFuture<SdkHttpFullResponse> execute(Request<?> request, RequestExecutionContext context) throws Exception {
        AmazonHttpClient.checkInterrupted();
        final ProgressListener listener = context.requestConfig().getProgressListener();

        publishProgress(listener, ProgressEventType.HTTP_REQUEST_STARTED_EVENT);
        return executeHttpRequest(request, context, listener);
    }

    private CompletableFuture<SdkHttpFullResponse> executeHttpRequest(Request<?> request,
                                                                      RequestExecutionContext context,
                                                                      ProgressListener listener) throws Exception {
        CompletableFuture<SdkHttpFullResponse> future = new CompletableFuture<>();
        sdkAsyncHttpClient.prepareRequest(new SimpleRequestProvider(request, context.awsRequestMetrics(), future),
                                          new SimpleResponseHandler(future, listener))
                          .run();

        // TODO client execution timer
        //        context.getClientExecutionTrackerTask().setCurrentHttpRequest(requestCallable);
        return future;
    }

    private static class SimpleRequestProvider implements SdkHttpRequestProvider {

        private final Request<?> request;
        private final AwsRequestMetrics metrics;
        private final CompletableFuture<SdkHttpFullResponse> future;

        private SimpleRequestProvider(Request<?> request,
                                      AwsRequestMetrics metrics,
                                      CompletableFuture<SdkHttpFullResponse> future) {
            this.request = request;
            this.metrics = metrics;
            this.future = future;
        }

        @Override
        public SdkHttpRequest request() {
            return new SdkHttpFullRequestAdapter(request);

        }

        @Override
        public SdkRequestContext context() {
            return SdkRequestContext.builder()
                                    .metrics(metrics)
                                    .build();
        }

        @Override
        public void readyForData(SdkRequestChannel channel) {
            try {
                channel.writeAndComplete(ByteBuffer.wrap(IoUtils.toByteArray(request.getContent())));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void exceptionOccurred(Throwable exception) {
            future.completeExceptionally(exception);
        }
    }

    private static class SimpleResponseHandler implements SdkHttpResponseHandler {

        private final SdkHttpFullResponse.Builder fullResponse = SdkHttpFullResponse.builder();
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private final WritableByteChannel channel = Channels.newChannel(baos);
        private final CompletableFuture<SdkHttpFullResponse> future;
        private final ProgressListener listener;

        private SimpleResponseHandler(CompletableFuture<SdkHttpFullResponse> future, ProgressListener listener) {
            this.future = future;
            this.listener = listener;
        }

        @Override
        public void headersReceived(SdkHttpResponse response) {
            fullResponse.headers(response.getHeaders());
            fullResponse.statusCode(response.getStatusCode());
            fullResponse.statusText(response.getStatusText());
        }

        @Override
        public void bodyPartReceived(ByteBuffer part) {
            try {
                channel.write(part);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        @Override
        public void complete() {
            try {
                channel.close();
                fullResponse.content(new ByteArrayInputStream(baos.toByteArray()));
                future.complete(fullResponse.build());
                publishProgress(listener, ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT);
            } catch (IOException e) {
                future.completeExceptionally(e);
                throw new UncheckedIOException(e);
            }
        }
    }
}
