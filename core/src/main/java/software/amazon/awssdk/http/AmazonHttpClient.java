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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.SdkGlobalConfiguration.PROFILING_SYSTEM_PROPERTY;
import static software.amazon.awssdk.event.SdkProgressPublisher.publishProgress;
import static software.amazon.awssdk.event.SdkProgressPublisher.publishRequestContentLength;
import static software.amazon.awssdk.event.SdkProgressPublisher.publishResponseContentLength;
import static software.amazon.awssdk.util.IoUtils.closeQuietly;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.pool.ConnPoolControl;
import org.apache.http.protocol.HttpContext;
import software.amazon.awssdk.AbortedException;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestClientOptions.Marker;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.CanHandleNullCredentials;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressInputStream;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.handlers.CredentialsRequestHandler;
import software.amazon.awssdk.handlers.HandlerContextKey;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.http.exception.HttpRequestTimeoutException;
import software.amazon.awssdk.http.exception.SdkInterruptedException;
import software.amazon.awssdk.internal.AmazonWebServiceRequestAdapter;
import software.amazon.awssdk.internal.http.apache.client.impl.ApacheHttpClientFactory;
import software.amazon.awssdk.internal.http.apache.client.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.internal.http.apache.request.impl.ApacheHttpRequestFactory;
import software.amazon.awssdk.internal.http.apache.utils.ApacheUtils;
import software.amazon.awssdk.internal.http.client.HttpClientFactory;
import software.amazon.awssdk.internal.http.conn.IdleConnectionReaper;
import software.amazon.awssdk.internal.http.request.HttpRequestFactory;
import software.amazon.awssdk.internal.http.response.AwsErrorResponseHandler;
import software.amazon.awssdk.internal.http.response.AwsResponseHandlerAdapter;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.internal.http.timers.request.HttpRequestAbortTaskTracker;
import software.amazon.awssdk.internal.http.timers.request.HttpRequestTimer;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicyAdapter;
import software.amazon.awssdk.retry.RetryUtils;
import software.amazon.awssdk.retry.internal.AuthErrorRetryStrategy;
import software.amazon.awssdk.retry.internal.AuthRetryParameters;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.retry.v2.RetryPolicyContext;
import software.amazon.awssdk.runtime.auth.SignerProviderContext;
import software.amazon.awssdk.runtime.io.ReleasableInputStream;
import software.amazon.awssdk.runtime.io.ResettableInputStream;
import software.amazon.awssdk.runtime.io.SdkBufferedInputStream;
import software.amazon.awssdk.util.AwsRequestMetrics;
import software.amazon.awssdk.util.CapacityManager;
import software.amazon.awssdk.util.CollectionUtils;
import software.amazon.awssdk.util.CountingInputStream;
import software.amazon.awssdk.util.DateUtils;
import software.amazon.awssdk.util.FakeIoException;
import software.amazon.awssdk.util.ImmutableMapParameter;
import software.amazon.awssdk.util.MetadataCache;
import software.amazon.awssdk.util.NullResponseMetadataCache;
import software.amazon.awssdk.util.ResponseMetadataCache;
import software.amazon.awssdk.util.RuntimeHttpUtils;
import software.amazon.awssdk.util.SdkHttpUtils;
import software.amazon.awssdk.util.UnreliableFilterInputStream;

@ThreadSafe
@SdkProtectedApi
public class AmazonHttpClient {
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_SDK_TRANSACTION_ID = "amz-sdk-invocation-id";
    public static final String HEADER_SDK_RETRY_INFO = "amz-sdk-retry";
    /**
     * Logger providing detailed information on requests/responses. Users can enable this logger to
     * get access to AWS request IDs for responses, individual requests and parameters sent to AWS,
     * etc.
     */
    @SdkInternalApi
    public static final Log REQUEST_LOG = LogFactory.getLog("software.amazon.awssdk.request");
    /**
     * Logger for more detailed debugging information, that might not be as useful for end users
     * (ex: HTTP client configuration, etc).
     */
    static final Log LOG = LogFactory.getLog(AmazonHttpClient.class);
    private static final HttpClientFactory<ConnectionManagerAwareHttpClient> HTTP_CLIENT_FACTORY = new
            ApacheHttpClientFactory();
    /**
     * When throttled retries are enabled, each retry attempt will consume this much capacity.
     * Successful retry attempts will release this capacity back to the pool while failed retries
     * will not.  Successful initial (non-retry) requests will always release 1 capacity unit to the
     * pool.
     */
    private static final int THROTTLED_RETRY_COST = 5;
    /**
     * When throttled retries are enabled, this is the total number of subsequent failed retries
     * that may be attempted before retry capacity is fully drained.
     */
    private static final int THROTTLED_RETRIES = 100;
    /**
     * Used for testing via failure injection.
     */
    private static UnreliableTestConfig unreliableTestConfig;

    static {
        // Customers have reported XML parsing issues with the following
        // JVM versions, which don't occur with more recent versions, so
        // if we detect any of these, give customers a heads up.
        // https://bugs.openjdk.java.net/browse/JDK-8028111
        List<String> problematicJvmVersions = Arrays
                .asList("1.6.0_06", "1.6.0_13", "1.6.0_17", "1.6.0_65", "1.7.0_45");
        String jvmVersion = System.getProperty("java.version");
        if (problematicJvmVersions.contains(jvmVersion)) {
            LOG.warn("Detected a possible problem with the current JVM version (" + jvmVersion +
                     ").  " +
                     "If you experience XML parsing problems using the SDK, try upgrading to a more recent JVM update.");
        }
    }

    private final HttpRequestFactory<HttpRequestBase> httpRequestFactory =
            new ApacheHttpRequestFactory();
    /**
     * Client configuration options, such as proxy httpClientSettings, max retries, etc.
     */
    private final ClientConfiguration config;
    private final RetryPolicy retryPolicy;
    /**
     * Client configuration options, such as proxy httpClientSettings, max retries, etc.
     */
    private final HttpClientSettings httpClientSettings;
    /**
     * Cache of metadata for recently executed requests for diagnostic purposes.
     */
    private final MetadataCache responseMetadataCache;
    /**
     * Timer to enforce HTTP request timeouts.
     */
    private final HttpRequestTimer httpRequestTimer;
    /**
     * Retry capacity manager, used to manage throttled retry resource.
     */
    private final CapacityManager retryCapacity;
    /**
     * Timer to enforce timeouts on the whole execution of the request (request handlers, retries,
     * backoff strategy, unmarshalling, etc).
     */
    private final ClientExecutionTimer clientExecutionTimer;
    /**
     * A request metric collector used specifically for this httpClientSettings client; or null if
     * there is none. This collector, if specified, always takes precedence over the one specified
     * at the AWS SDK level.
     *
     * @see AwsSdkMetrics
     */
    private final RequestMetricCollector requestMetricCollector;
    /**
     * Used to generate UUID's for client transaction id. This gives a higher probability of id
     * clashes but is more performant then using {@link UUID#randomUUID()} which uses SecureRandom
     * internally.
     **/
    private final Random random = new Random();
    /**
     * Internal client for sending HTTP requests.
     */
    private ConnectionManagerAwareHttpClient httpClient;
    /**
     * The time difference in seconds between this client and AWS.
     */
    private volatile int timeOffset = SdkGlobalTime.getGlobalTimeOffset();

    /**
     * Constructs a new AWS client using the specified client configuration options (ex: max retry
     * attempts, proxy httpClientSettings, etc).
     *
     * @param config Configuration options specifying how this client will communicate with AWS (ex:
     *               proxy httpClientSettings, retry count, etc.).
     */
    public AmazonHttpClient(ClientConfiguration config) {
        this(config, null);
    }

    /**
     * Constructs a new AWS client using the specified client configuration options (ex: max retry
     * attempts, proxy httpClientSettings, etc), and request metric collector.
     *
     * @param config                 Configuration options specifying how this client will
     *                               communicate with AWS (ex: proxy httpClientSettings, retry
     *                               count, etc.).
     * @param requestMetricCollector client specific request metric collector, which takes
     *                               precedence over the one at the AWS SDK level; or null if there
     *                               is none.
     */
    public AmazonHttpClient(ClientConfiguration config,
                            RequestMetricCollector requestMetricCollector) {
        this(config, requestMetricCollector, false);
    }

    /**
     * Constructs a new AWS client using the specified client configuration options (ex: max retry
     * attempts, proxy httpClientSettings, etc), and request metric collector.
     *
     * @param config                 Configuration options specifying how this client will
     *                               communicate with AWS (ex: proxy httpClientSettings, retry
     *                               count, etc.).
     * @param requestMetricCollector client specific request metric collector, which takes
     *                               precedence over the one at the AWS SDK level; or null if there
     *                               is none.
     */
    public AmazonHttpClient(ClientConfiguration config,
                            RequestMetricCollector requestMetricCollector,
                            boolean useBrowserCompatibleHostNameVerifier) {
        this(config, requestMetricCollector, useBrowserCompatibleHostNameVerifier, false);
    }

    /**
     * Constructs a new AWS client using the specified client configuration options (ex: max retry
     * attempts, proxy httpClientSettings, etc), and request metric collector.
     *
     * @param config                           Configuration options specifying how this client will
     *                                         communicate with AWS (ex: proxy httpClientSettings,
     *                                         retry count, etc.).
     * @param requestMetricCollector           client specific request metric collector, which takes
     *                                         precedence over the one at the AWS SDK level; or null
     *                                         if there is none.
     * @param calculateCrc32FromCompressedData The flag indicating whether the CRC32 checksum is
     *                                         calculated from compressed data or not. It is only
     *                                         applicable when the header "x-amz-crc32" is set in
     *                                         the response.
     */
    public AmazonHttpClient(ClientConfiguration config,
                            RequestMetricCollector requestMetricCollector,
                            boolean useBrowserCompatibleHostNameVerifier,
                            boolean calculateCrc32FromCompressedData) {
        this(config,
             null,
             requestMetricCollector,
             useBrowserCompatibleHostNameVerifier,
             calculateCrc32FromCompressedData);
    }

    private AmazonHttpClient(ClientConfiguration config,
                             RetryPolicy retryPolicy,
                             RequestMetricCollector requestMetricCollector,
                             boolean useBrowserCompatibleHostNameVerifier,
                             boolean calculateCrc32FromCompressedData) {
        this(config,
             retryPolicy,
             requestMetricCollector,
             HttpClientSettings.adapt(config, useBrowserCompatibleHostNameVerifier, calculateCrc32FromCompressedData));
        this.httpClient = HTTP_CLIENT_FACTORY.create(this.httpClientSettings);
    }

    /**
     * Package-protected constructor for unit test purposes.
     */
    @SdkTestInternalApi
    public AmazonHttpClient(ClientConfiguration clientConfig,
                            ConnectionManagerAwareHttpClient httpClient,
                            RequestMetricCollector requestMetricCollector) {
        this(clientConfig,
             null,
             requestMetricCollector,
             HttpClientSettings.adapt(clientConfig, false));
        this.httpClient = httpClient;
    }

    private AmazonHttpClient(ClientConfiguration clientConfig,
                             RetryPolicy retryPolicy,
                             RequestMetricCollector requestMetricCollector,
                             HttpClientSettings httpClientSettings) {
        this.config = clientConfig;
        this.retryPolicy =
                retryPolicy == null ? new RetryPolicyAdapter(clientConfig.getRetryPolicy(), clientConfig) : retryPolicy;
        this.httpClientSettings = httpClientSettings;
        this.requestMetricCollector = requestMetricCollector;
        this.responseMetadataCache =
                clientConfig.getCacheResponseMetadata() ?
                new ResponseMetadataCache(clientConfig.getResponseMetadataCacheSize()) :
                new NullResponseMetadataCache();
        this.httpRequestTimer = new HttpRequestTimer();
        this.clientExecutionTimer = new ClientExecutionTimer();

        // When enabled, total retry capacity is computed based on retry cost
        // and desired number of retries.
        int throttledRetryMaxCapacity = clientConfig.useThrottledRetries()
                                        ? THROTTLED_RETRY_COST * THROTTLED_RETRIES : -1;
        this.retryCapacity = new CapacityManager(throttledRetryMaxCapacity);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static boolean isTemporaryRedirect(org.apache.http.HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        return status == HttpStatus.SC_TEMPORARY_REDIRECT && response.getHeaders("Location") != null
               && response.getHeaders("Location").length > 0;
    }

    /**
     * Used to configure the test conditions for injecting intermittent failures to the content
     * input stream.
     *
     * @param config unreliable test configuration for failure injection; or null to disable such
     *               test.
     */
    static void configUnreliableTestConditions(UnreliableTestConfig config) {
        unreliableTestConfig = config;
    }

    /**
     * Shuts down this HTTP client object, releasing any resources that might be held open. This is
     * an optional method, and callers are not expected to call it, but can if they want to
     * explicitly release any open resources. Once a client has been shutdown, it cannot be used to
     * make more requests.
     */
    public void shutdown() {
        clientExecutionTimer.shutdown();
        httpRequestTimer.shutdown();
        IdleConnectionReaper.removeConnectionManager(httpClient.getHttpClientConnectionManager());
        httpClient.getHttpClientConnectionManager().shutdown();
    }

    /**
     * Package protected for unit-testing.
     */
    @SdkTestInternalApi
    public HttpRequestTimer getHttpRequestTimer() {
        return this.httpRequestTimer;
    }

    /**
     * Package protected for unit-testing.
     */
    @SdkTestInternalApi
    public ClientExecutionTimer getClientExecutionTimer() {
        return this.clientExecutionTimer;
    }

    /**
     * Returns additional response metadata for an executed request. Response metadata isn't
     * considered part of the standard results returned by an operation, so it's accessed instead
     * through this diagnostic interface. Response metadata is typically used for troubleshooting
     * issues with AWS support staff when services aren't acting as expected.
     *
     * @param request A previously executed AmazonWebServiceRequest object, whose response metadata
     *                is desired.
     * @return The response metadata for the specified request, otherwise null if there is no
     *         response metadata available for the request
     */
    public ResponseMetadata getResponseMetadataForRequest(AmazonWebServiceRequest request) {
        return responseMetadataCache.get(request);
    }

    /**
     * Returns the httpClientSettings client specific request metric collector; or null if there is
     * none.
     */
    public RequestMetricCollector getRequestMetricCollector() {
        return requestMetricCollector;
    }

    /**
     * Returns the time difference in seconds between this client and AWS.
     */
    public int getTimeOffset() {
        return timeOffset;
    }

    /**
     * Executes the request and returns the result.
     *
     * @param request              The AmazonWebServices request to send to the remote server
     * @param responseHandler      A response handler to accept a successful response from the
     *                             remote server
     * @param errorResponseHandler A response handler to accept an unsuccessful response from the
     *                             remote server
     * @param executionContext     Additional information about the context of this web service
     *                             call
     * @deprecated Use {@link #requestExecutionBuilder()} to configure and execute a HTTP request.
     */
    @Deprecated
    public <T> Response<T> execute(Request<?> request,
                                   HttpResponseHandler<AmazonWebServiceResponse<T>> responseHandler,
                                   HttpResponseHandler<AmazonServiceException> errorResponseHandler,
                                   ExecutionContext executionContext) {
        HttpResponseHandler<T> adaptedRespHandler = new AwsResponseHandlerAdapter<T>(
                getNonNullResponseHandler(responseHandler),
                request,
                executionContext.getAwsRequestMetrics(),
                responseMetadataCache);
        return requestExecutionBuilder()
                .request(request)
                .requestConfig(new AmazonWebServiceRequestAdapter(request.getOriginalRequest()))
                .errorResponseHandler(new AwsErrorResponseHandler(errorResponseHandler, executionContext.getAwsRequestMetrics()))
                .executionContext(executionContext)
                .execute(adaptedRespHandler);
    }

    /**
     * Ensures the response handler is not null. If it is this method returns a dummy response
     * handler.
     *
     * @return Either original response handler or dummy response handler.
     */
    private <T> HttpResponseHandler<T> getNonNullResponseHandler(
            HttpResponseHandler<T> responseHandler) {
        if (responseHandler != null) {
            return responseHandler;
        } else {
            // Return a Dummy, No-Op handler
            return new HttpResponseHandler<T>() {

                @Override
                public T handle(HttpResponse response) throws Exception {
                    return null;
                }

                @Override
                public boolean needsConnectionLeftOpen() {
                    return false;
                }
            };
        }
    }

    /**
     * @return A builder used to configure and execute a HTTP request.
     */
    public RequestExecutionBuilder requestExecutionBuilder() {
        return new RequestExecutionBuilderImpl();
    }

    /**
     * Interface to configure a request execution and execute the request.
     */
    public interface RequestExecutionBuilder {

        /**
         * Fluent setter for {@link Request}
         *
         * @param request Request object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder request(Request<?> request);

        /**
         * Fluent setter for the error response handler
         *
         * @param errorResponseHandler Error response handler
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder errorResponseHandler(
                HttpResponseHandler<? extends SdkBaseException> errorResponseHandler);

        /**
         * Fluent setter for the execution context
         *
         * @param executionContext Execution context
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder executionContext(ExecutionContext executionContext);

        /**
         * Fluent setter for {@link RequestConfig}
         *
         * @param requestConfig Request config object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder requestConfig(RequestConfig requestConfig);

        /**
         * Executes the request with the given configuration.
         *
         * @param responseHandler Response handler that outputs the actual result type which is
         *                        preferred going forward.
         * @param <OutputT>        Result type
         * @return Unmarshalled result type.
         */
        <OutputT> Response<OutputT> execute(HttpResponseHandler<OutputT> responseHandler);

        /**
         * Executes the request with the given configuration; not handling response.
         *
         * @return Void response
         */
        Response<Void> execute();

    }

    public static class Builder {

        private ClientConfiguration clientConfig;
        private RetryPolicy retryPolicy;
        private RequestMetricCollector requestMetricCollector;
        private boolean useBrowserCompatibleHostNameVerifier;
        private boolean calculateCrc32FromCompressedData;

        private Builder() {
        }

        public Builder clientConfiguration(ClientConfiguration clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder requestMetricCollector(RequestMetricCollector requestMetricCollector) {
            this.requestMetricCollector = requestMetricCollector;
            return this;
        }

        public Builder useBrowserCompatibleHostNameVerifier(boolean useBrowserCompatibleHostNameVerifier) {
            this.useBrowserCompatibleHostNameVerifier = useBrowserCompatibleHostNameVerifier;
            return this;
        }

        public Builder calculateCrc32FromCompressedData(boolean calculateCrc32FromCompressedData) {
            this.calculateCrc32FromCompressedData = calculateCrc32FromCompressedData;
            return this;
        }

        public AmazonHttpClient build() {
            return new AmazonHttpClient(clientConfig,
                                        retryPolicy,
                                        requestMetricCollector,
                                        useBrowserCompatibleHostNameVerifier,
                                        calculateCrc32FromCompressedData);
        }
    }

    private class RequestExecutionBuilderImpl implements RequestExecutionBuilder {

        private Request<?> request;
        private RequestConfig requestConfig;
        private HttpResponseHandler<? extends SdkBaseException> errorResponseHandler;
        private ExecutionContext executionContext = new ExecutionContext();

        @Override
        public RequestExecutionBuilder request(Request<?> request) {
            this.request = request;
            return this;
        }

        @Override
        public RequestExecutionBuilder errorResponseHandler(
                HttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
            this.errorResponseHandler = errorResponseHandler;
            return this;
        }

        @Override
        public RequestExecutionBuilder executionContext(
                ExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        @Override
        public RequestExecutionBuilder requestConfig(RequestConfig requestConfig) {
            this.requestConfig = requestConfig;
            return this;
        }

        @Override
        public <OutputT> Response<OutputT> execute(HttpResponseHandler<OutputT> responseHandler) {
            RequestConfig config = requestConfig != null ? requestConfig
                                                         : new AmazonWebServiceRequestAdapter(request.getOriginalRequest());
            return new RequestExecutor<OutputT>(request,
                                                config,
                                                getNonNullResponseHandler(errorResponseHandler),
                                                getNonNullResponseHandler(responseHandler),
                                                executionContext,
                                                getRequestHandlers()
            ).execute();
        }

        @Override
        public Response<Void> execute() {
            return execute(null);
        }

        private List<RequestHandler2> getRequestHandlers() {
            List<RequestHandler2> requestHandler2s = executionContext.getRequestHandler2s();
            if (requestHandler2s == null) {
                return Collections.emptyList();
            }
            return requestHandler2s;
        }

    }

    private class RequestExecutor<OutputT> {
        private final Request<?> request;
        private final RequestConfig requestConfig;
        private final HttpResponseHandler<? extends SdkBaseException> errorResponseHandler;
        private final HttpResponseHandler<OutputT> responseHandler;
        private final ExecutionContext executionContext;
        private final List<RequestHandler2> requestHandler2s;
        private final AwsRequestMetrics awsRequestMetrics;

        private RequestExecutor(Request<?> request, RequestConfig requestConfig,
                                HttpResponseHandler<? extends SdkBaseException> errorResponseHandler,
                                HttpResponseHandler<OutputT> responseHandler,
                                ExecutionContext executionContext,
                                List<RequestHandler2> requestHandler2s) {
            this.request = request;
            this.requestConfig = requestConfig;
            this.errorResponseHandler = errorResponseHandler;
            this.responseHandler = responseHandler;
            this.executionContext = executionContext;
            this.requestHandler2s = requestHandler2s;
            this.awsRequestMetrics = executionContext.getAwsRequestMetrics();
        }

        /**
         * Executes the request and returns the result.
         */
        private Response<OutputT> execute() {
            if (executionContext == null) {
                throw new SdkClientException(
                        "Internal SDK Error: No execution context parameter specified.");
            }
            try {
                return executeWithTimer();
            } catch (InterruptedException ie) {
                throw handleInterruptedException(ie);
            } catch (AbortedException ae) {
                throw handleAbortedException(ae);
            }
        }

        /**
         * Start and end client execution timer around the execution of the request. It's important
         * that the client execution task is canceled before the InterruptedExecption is handled by
         * {@link #execute()} so * the interrupt status doesn't leak out to the callers code
         */
        private Response<OutputT> executeWithTimer() throws InterruptedException {
            try {
                executionContext.setClientExecutionTrackerTask(
                        clientExecutionTimer.startTimer(getClientExecutionTimeout(requestConfig)));
                return doExecute();
            } finally {
                executionContext.getClientExecutionTrackerTask().cancelTask();
            }
        }

        private Response<OutputT> doExecute() throws InterruptedException {
            runBeforeRequestHandlers();
            setSdkTransactionId(request);
            setUserAgent(request);

            ProgressListener listener = requestConfig.getProgressListener();
            // add custom headers
            request.getHeaders().putAll(config.getHeaders());
            request.getHeaders().putAll(requestConfig.getCustomRequestHeaders());
            // add custom query parameters
            mergeQueryParameters(requestConfig.getCustomQueryParameters());
            Response<OutputT> response = null;
            final InputStream origContent = request.getContent();
            final InputStream toBeClosed = beforeRequest(); // for progress tracking
            // make "notCloseable", so reset would work with retries
            final InputStream notCloseable = (toBeClosed == null) ? null
                                                                  : ReleasableInputStream.wrap(toBeClosed).disableClose();
            request.setContent(notCloseable);
            try {
                publishProgress(listener, ProgressEventType.CLIENT_REQUEST_STARTED_EVENT);
                response = executeHelper();
                publishProgress(listener, ProgressEventType.CLIENT_REQUEST_SUCCESS_EVENT);
                awsRequestMetrics.getTimingInfo().endTiming();
                afterResponse(response);
                return response;
            } catch (AmazonClientException e) {
                publishProgress(listener, ProgressEventType.CLIENT_REQUEST_FAILED_EVENT);
                afterError(response, e);
                throw e;
            } finally {
                // Always close so any progress tracking would get the final events propagated.
                closeQuietly(toBeClosed, LOG);
                request.setContent(origContent); // restore the original content
            }
        }

        private void runBeforeRequestHandlers() {
            AwsCredentials credentials = getCredentialsFromContext();
            request.addHandlerContext(HandlerContextKey.AWS_CREDENTIALS, credentials);
            // Apply any additional service specific request handlers that need to be run
            for (RequestHandler2 requestHandler2 : requestHandler2s) {
                // If the request handler is a type of CredentialsRequestHandler, then set the credentials in the request handler.
                if (requestHandler2 instanceof CredentialsRequestHandler) {
                    ((CredentialsRequestHandler) requestHandler2).setCredentials(credentials);
                }
                requestHandler2.beforeRequest(request);
            }
        }

        /**
         * Determine if an interrupted exception is caused by the client execution timer
         * interrupting the current thread or some other task interrupting the thread for another
         * purpose.
         *
         * @return {@link ClientExecutionTimeoutException} if the {@link InterruptedException} was
         *     caused by the {@link ClientExecutionTimer}. Otherwise re-interrupts the current thread
         *     and returns a {@link SdkClientException} wrapping an {@link InterruptedException}
         */
        private RuntimeException handleInterruptedException(InterruptedException e) {
            if (e instanceof SdkInterruptedException) {
                if (((SdkInterruptedException) e).getResponse() != null) {
                    ((SdkInterruptedException) e).getResponse().getHttpResponse().getHttpRequest().abort();
                }
            }
            if (executionContext.getClientExecutionTrackerTask().hasTimeoutExpired()) {
                // Clear the interrupt status
                Thread.interrupted();
                return new ClientExecutionTimeoutException();
            } else {
                Thread.currentThread().interrupt();
                return new AbortedException(e);
            }
        }

        /**
         * Determine if an aborted exception is caused by the client execution timer interrupting
         * the current thread. If so throws {@link ClientExecutionTimeoutException} else throws the
         * original {@link AbortedException}
         *
         * @param ae aborted exception that occurred
         * @return {@link ClientExecutionTimeoutException} if the {@link AbortedException} was
         *     caused by the {@link ClientExecutionTimer}. Otherwise throws the original {@link AbortedException}
         */
        private RuntimeException handleAbortedException(final AbortedException ae) {
            if (executionContext.getClientExecutionTrackerTask().hasTimeoutExpired()) {
                return new ClientExecutionTimeoutException();
            } else {
                return ae;
            }
        }

        /**
         * Check if the thread has been interrupted. If so throw an {@link InterruptedException}.
         * Long running tasks should be periodically checked if the current thread has been
         * interrupted and handle it appropriately
         *
         * @throws InterruptedException If thread has been interrupted
         */
        private void checkInterrupted() throws InterruptedException {
            checkInterrupted(null);
        }

        /**
         * Check if the thread has been interrupted. If so throw an {@link InterruptedException}.
         * Long running tasks should be periodically checked if the current thread has been
         * interrupted and handle it appropriately
         *
         * @param response Response to be closed before returning control to the caller to avoid
         *                 leaking the connection.
         * @throws InterruptedException If thread has been interrupted
         */
        private void checkInterrupted(Response<?> response) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new SdkInterruptedException(response);
            }
        }

        /**
         * Merge query parameters into the given request.
         */
        private void mergeQueryParameters(Map<String, List<String>> params) {
            Map<String, List<String>> existingParams = request.getParameters();
            for (Entry<String, List<String>> param : params.entrySet()) {
                String pName = param.getKey();
                List<String> pValues = param.getValue();
                existingParams.put(pName, CollectionUtils.mergeLists(existingParams.get(pName), pValues));
            }
        }

        /**
         * Publishes the "request content length" event, and returns an input stream, which will be
         * made mark-and-resettable if possible, for progress tracking purposes.
         *
         * @return an input stream, which will be made mark-and-resettable if possible, for progress
         *     tracking purposes; or null if the request doesn't have an input stream
         */
        private InputStream beforeRequest() {
            ProgressListener listener = requestConfig.getProgressListener();
            reportContentLength(listener);
            if (request.getContent() == null) {
                return null;
            }
            final InputStream content = monitorStreamProgress(listener,
                                                              buffer(
                                                                      makeResettable(
                                                                              request.getContent())));
            if (AmazonHttpClient.unreliableTestConfig == null) {
                return content;
            }
            return wrapWithUnreliableStream(content);
        }

        /**
         * If content length is present on the request, report it to the progress listener.
         *
         * @param listener Listener to notify.
         */
        private void reportContentLength(ProgressListener listener) {
            Map<String, String> headers = request.getHeaders();
            String contentLengthStr = headers.get("Content-Length");
            if (contentLengthStr != null) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    publishRequestContentLength(listener, contentLength);
                } catch (NumberFormatException e) {
                    LOG.warn("Cannot parse the Content-Length header of the request.");
                }
            }
        }

        /**
         * Make input stream resettable if possible.
         *
         * @param content Input stream to make resettable
         * @return ResettableInputStream if possible otherwise original input stream.
         */
        private InputStream makeResettable(InputStream content) {
            if (!content.markSupported()) {
                // try to wrap the content input stream to become
                // mark-and-resettable for signing and retry purposes.
                if (content instanceof FileInputStream) {
                    try {
                        // ResettableInputStream supports mark-and-reset without
                        // memory buffering
                        return new ResettableInputStream((FileInputStream) content);
                    } catch (IOException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("For the record; ignore otherwise", e);
                        }
                    }
                }
            }
            return content;
        }

        /**
         * Buffer input stream if possible.
         *
         * @param content Input stream to buffer
         * @return SdkBufferedInputStream if possible, otherwise original input stream.
         */
        private InputStream buffer(InputStream content) {
            if (!content.markSupported()) {
                content = new SdkBufferedInputStream(content);
            }
            return content;
        }

        /**
         * Wrap with a {@link ProgressInputStream} to report request progress to listener.
         *
         * @param listener Listener to report to
         * @param content  Input stream to monitor progress for
         * @return Wrapped input stream with progress monitoring capabilities.
         */
        private InputStream monitorStreamProgress(ProgressListener listener,
                                                  InputStream content) {
            return ProgressInputStream.inputStreamForRequest(content, listener);
        }

        /**
         * Used only for internal testing purposes. Makes a stream unreliable in certain ways for
         * fault testing.
         *
         * @param content Input stream to make unreliable.
         * @return UnreliableFilterInputStream
         */
        private InputStream wrapWithUnreliableStream(InputStream content) {
            return new UnreliableFilterInputStream(content,
                                                   unreliableTestConfig.isFakeIoException())
                    .withBytesReadBeforeException(
                            unreliableTestConfig.getBytesReadBeforeException())
                    .withMaxNumErrors(unreliableTestConfig.getMaxNumErrors())
                    .withResetIntervalBeforeException(
                            unreliableTestConfig.getResetIntervalBeforeException());
        }


        private void afterError(Response<?> response,
                                AmazonClientException e) throws InterruptedException {
            for (RequestHandler2 handler2 : requestHandler2s) {
                handler2.afterError(request, response, e);
                checkInterrupted(response);
            }
        }

        private <T> void afterResponse(Response<T> response) throws InterruptedException {
            for (RequestHandler2 handler2 : requestHandler2s) {
                handler2.afterResponse(request, response);
                checkInterrupted(response);
            }
        }

        /**
         * Internal method to execute the HTTP method given.
         */
        private Response<OutputT> executeHelper() throws InterruptedException {
            /*
             * add the service endpoint to the logs. You can infer service name from service endpoint
             */
            awsRequestMetrics
                    .addPropertyWith(AwsRequestMetrics.Field.RequestType, requestConfig.getRequestType())
                    .addPropertyWith(AwsRequestMetrics.Field.ServiceName, request.getServiceName())
                    .addPropertyWith(AwsRequestMetrics.Field.ServiceEndpoint, request.getEndpoint());
            // Make a copy of the original request params and headers so that we can
            // permute it in this loop and start over with the original every time.
            final Map<String, List<String>> originalParameters = new LinkedHashMap<String, List<String>>(request.getParameters());
            final Map<String, String> originalHeaders = new HashMap<String, String>(request.getHeaders());
            // Always mark the input stream before execution.
            final ExecOneRequestParams execOneParams = new ExecOneRequestParams();
            final InputStream originalContent = request.getContent();
            if (originalContent != null && originalContent.markSupported()
                && !(originalContent instanceof BufferedInputStream)) {
                // Mark only once for non-BufferedInputStream
                final int readLimit = requestConfig.getRequestClientOptions().getReadLimit();
                originalContent.mark(readLimit);
            }
            while (true) {
                checkInterrupted();
                if (originalContent instanceof BufferedInputStream && originalContent.markSupported()) {
                    // Mark everytime for BufferedInputStream, since the marker could have been invalidated
                    final int readLimit = requestConfig.getRequestClientOptions().getReadLimit();
                    originalContent.mark(readLimit);
                }
                execOneParams.initPerRetry();
                if (execOneParams.redirectedUri != null) {
                    /*
                     * [scheme:][//authority][path][?query][#fragment]
                     */
                    String scheme = execOneParams.redirectedUri.getScheme();
                    String beforeAuthority = scheme == null ? "" : scheme + "://";
                    String authority = execOneParams.redirectedUri.getAuthority();
                    String path = execOneParams.redirectedUri.getPath();

                    request.setEndpoint(URI.create(beforeAuthority + authority));
                    request.setResourcePath(SdkHttpUtils.urlEncode(path, true));
                }
                if (execOneParams.authRetryParam != null) {
                    request.setEndpoint(execOneParams.authRetryParam.getEndpointForRetry());
                }
                awsRequestMetrics.setCounter(AwsRequestMetrics.Field.RequestCount, execOneParams.requestCount);
                if (execOneParams.isRetry()) {
                    request.setParameters(originalParameters);
                    request.setHeaders(originalHeaders);
                    request.setContent(originalContent);
                }
                try {
                    Response<OutputT> response = executeOneRequest(execOneParams);
                    if (response != null) {
                        return response;
                    }
                } catch (IOException ioe) {
                    captureExceptionMetrics(ioe);
                    awsRequestMetrics.addProperty(AwsRequestMetrics.Field.AWSRequestID, null);
                    SdkClientException sdkClientException = new SdkClientException(
                            "Unable to execute HTTP request: " + ioe.getMessage(), ioe);
                    boolean willRetry = shouldRetry(execOneParams, sdkClientException);
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(sdkClientException.getMessage() + (willRetry ? " Request will be retried." : ""), ioe);
                    } else if (LOG.isDebugEnabled()) {
                        LOG.trace(sdkClientException.getMessage() + (willRetry ? " Request will be retried." : ""));
                    }
                    if (!willRetry) {
                        throw lastReset(sdkClientException);
                    }
                    // Cache the retryable exception
                    execOneParams.retriedException = sdkClientException;
                } catch (RuntimeException e) {
                    throw lastReset(captureExceptionMetrics(e));
                } catch (Error e) {
                    throw lastReset(captureExceptionMetrics(e));
                } finally {
                    /*
                     * Some response handlers need to manually manage the HTTP connection and will take
                     * care of releasing the connection on their own, but if this response handler
                     * doesn't need the connection left open, we go ahead and release the it to free up
                     * resources.
                     */
                    if (!execOneParams.leaveHttpConnectionOpen) {
                        if (execOneParams.apacheResponse != null) {
                            HttpEntity entity = execOneParams.apacheResponse.getEntity();
                            if (entity != null) {
                                try {
                                    closeQuietly(entity.getContent(), LOG);
                                } catch (IOException e) {
                                    LOG.warn("Cannot close the response content.", e);
                                }
                            }
                        }
                    }
                }
            } /* end while (true) */
        }

        /**
         * Used to perform a last reset on the content input stream (if mark-supported); this is so
         * that, for backward compatibility reason, any "blind" retry (ie without calling reset) by
         * user of this library with the same input stream (such as ByteArrayInputStream) could
         * still succeed.
         *
         * @param t the failure
         * @return the failure as given
         */
        private <T extends Throwable> T lastReset(final T t) {
            try {
                InputStream content = request.getContent();
                if (content != null) {
                    if (content.markSupported()) {
                        content.reset();
                    }
                }
            } catch (Exception ex) {
                LOG.debug("FYI: failed to reset content inputstream before throwing up", ex);
            }
            return t;
        }

        /**
         * Returns the credentials from the execution if exists. Else returns null.
         */
        private AwsCredentials getCredentialsFromContext() {
            final AwsCredentialsProvider credentialsProvider = executionContext.getCredentialsProvider();

            AwsCredentials credentials = null;
            if (credentialsProvider != null) {
                awsRequestMetrics.startEvent(AwsRequestMetrics.Field.CredentialsRequestTime);
                try {
                    credentials = credentialsProvider.getCredentials();
                } finally {
                    awsRequestMetrics.endEvent(AwsRequestMetrics.Field.CredentialsRequestTime);
                }
            }
            return credentials;
        }

        /**
         * Returns the response from executing one httpClientSettings request; or null for retry.
         */
        private Response<OutputT> executeOneRequest(ExecOneRequestParams execOneParams)
                throws IOException, InterruptedException {
            if (execOneParams.isRetry()) {
                resetRequestInputStream(request);
            }
            checkInterrupted();
            if (REQUEST_LOG.isDebugEnabled()) {
                REQUEST_LOG.debug((execOneParams.isRetry() ? "Retrying " : "Sending ") + "Request: " + request);
            }
            final AwsCredentials credentials = getCredentialsFromContext();
            final ProgressListener listener = requestConfig.getProgressListener();

            if (execOneParams.isRetry()) {
                pauseBeforeRetry(execOneParams, listener);
            }
            updateRetryHeaderInfo(request, execOneParams);

            // Sign the request if a signer was provided
            execOneParams.newSigner(request, executionContext);
            if (execOneParams.signer != null &&
                (credentials != null || execOneParams.signer instanceof CanHandleNullCredentials)) {
                awsRequestMetrics.startEvent(AwsRequestMetrics.Field.RequestSigningTime);
                try {
                    if (timeOffset != 0) {
                        // Always use the client level timeOffset if it was
                        // non-zero; Otherwise, we respect the timeOffset in the
                        // request, which could have been externally configured (at
                        // least for the 1st non-retry request).
                        //
                        // For retry due to clock skew, the timeOffset in the
                        // request used for the retry is assumed to have been
                        // adjusted when execution reaches here.
                        request.setTimeOffset(timeOffset);
                    }
                    execOneParams.signer.sign(request, credentials);
                } finally {
                    awsRequestMetrics.endEvent(AwsRequestMetrics.Field.RequestSigningTime);
                }
            }

            checkInterrupted();
            execOneParams.newApacheRequest(httpRequestFactory, request, httpClientSettings);

            captureConnectionPoolMetrics();

            Map<String, AwsRequestMetrics> attributes =
                    ImmutableMapParameter.of(AwsRequestMetrics.class.getSimpleName(), awsRequestMetrics);
            final HttpClientContext localRequestContext = ApacheUtils.newClientContext(httpClientSettings, attributes);

            execOneParams.resetBeforeHttpRequest();
            publishProgress(listener, ProgressEventType.HTTP_REQUEST_STARTED_EVENT);
            awsRequestMetrics.startEvent(AwsRequestMetrics.Field.HttpRequestTime);
            awsRequestMetrics.setCounter(AwsRequestMetrics.Field.RetryCapacityConsumed, retryCapacity.consumedCapacity());

            /////////// Send HTTP request ////////////
            executionContext.getClientExecutionTrackerTask().setCurrentHttpRequest(execOneParams.apacheRequest);
            final HttpRequestAbortTaskTracker requestAbortTaskTracker = httpRequestTimer
                    .startTimer(execOneParams.apacheRequest, getRequestTimeout(requestConfig));

            try {
                execOneParams.apacheResponse = httpClient.execute(execOneParams.apacheRequest, localRequestContext);
                if (shouldBufferHttpEntity(responseHandler.needsConnectionLeftOpen(),
                                           executionContext,
                                           execOneParams,
                                           requestAbortTaskTracker)) {
                    execOneParams.apacheResponse
                            .setEntity(new BufferedHttpEntity(
                                    execOneParams.apacheResponse.getEntity()));
                }
            } catch (IOException ioe) {
                // Client execution timeouts take precedence as it's not retryable
                if (executionContext.getClientExecutionTrackerTask().hasTimeoutExpired()) {
                    throw new InterruptedException();
                } else if (requestAbortTaskTracker.httpRequestAborted()) {
                    throw new HttpRequestTimeoutException(ioe);
                } else {
                    throw ioe;
                }
            } finally {
                requestAbortTaskTracker.cancelTask();
                awsRequestMetrics.endEvent(AwsRequestMetrics.Field.HttpRequestTime);
            }

            publishProgress(listener, ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT);
            final StatusLine statusLine = execOneParams.apacheResponse.getStatusLine();
            final int statusCode = statusLine == null ? -1 : statusLine.getStatusCode();
            if (isRequestSuccessful(execOneParams.apacheResponse)) {
                awsRequestMetrics.addProperty(AwsRequestMetrics.Field.StatusCode, statusCode);
                /*
                 * If we get back any 2xx status code, then we know we should treat the service call as
                 * successful.
                 */
                execOneParams.leaveHttpConnectionOpen = responseHandler.needsConnectionLeftOpen();
                HttpResponse httpResponse = createResponse(execOneParams.apacheRequest,
                                                           execOneParams.apacheResponse,
                                                           localRequestContext);
                OutputT response = handleResponse(httpResponse);

                /*
                 * If this was a successful retry attempt we'll release the full retry capacity that
                 * the attempt originally consumed.  If this was a successful initial request
                 * we return a lesser amount.
                 */
                if (execOneParams.isRetry() && executionContext.retryCapacityConsumed()) {
                    retryCapacity.release(THROTTLED_RETRY_COST);
                } else {
                    retryCapacity.release();
                }
                return new Response<OutputT>(response, httpResponse);
            }
            if (isTemporaryRedirect(execOneParams.apacheResponse)) {
                /*
                 * S3 sends 307 Temporary Redirects if you try to delete an EU bucket from the US
                 * endpoint. If we get a 307, we'll point the HTTP method to the redirected location,
                 * and let the next retry deliver the request to the right location.
                 */
                Header[] locationHeaders = execOneParams.apacheResponse.getHeaders("location");
                String redirectedLocation = locationHeaders[0].getValue();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Redirecting to: " + redirectedLocation);
                }
                execOneParams.redirectedUri = URI.create(redirectedLocation);
                awsRequestMetrics.addPropertyWith(AwsRequestMetrics.Field.StatusCode, statusCode)
                                 .addPropertyWith(AwsRequestMetrics.Field.RedirectLocation, redirectedLocation)
                                 .addPropertyWith(AwsRequestMetrics.Field.AWSRequestID, null);
                return null; // => retry
            }
            execOneParams.leaveHttpConnectionOpen = errorResponseHandler.needsConnectionLeftOpen();
            final SdkBaseException exception = handleErrorResponse(execOneParams.apacheRequest,
                                                                   execOneParams.apacheResponse,
                                                                   localRequestContext);
            // Check whether we should internally retry the auth error
            execOneParams.authRetryParam = null;
            AuthErrorRetryStrategy authRetry = executionContext.getAuthErrorRetryStrategy();
            if (authRetry != null && exception instanceof AmazonServiceException) {
                HttpResponse httpResponse = createResponse(execOneParams.apacheRequest,
                                                           execOneParams.apacheResponse,
                                                           localRequestContext);
                execOneParams.authRetryParam = authRetry
                        .shouldRetryWithAuthParam(request, httpResponse, (AmazonServiceException) exception);
            }
            if (execOneParams.authRetryParam == null && !shouldRetry(execOneParams, exception)) {
                throw exception;
            }
            // Comment out for now. Ref: CR2662349
            // Preserve the cause of retry before retrying
            // awsRequestMetrics.addProperty(RetryCause, ase);
            if (RetryUtils.isThrottlingException(exception)) {
                awsRequestMetrics.incrementCounterWith(AwsRequestMetrics.Field.ThrottleException)
                                 .addProperty(AwsRequestMetrics.Field.ThrottleException, exception);
            }
            // Cache the retryable exception
            execOneParams.retriedException = exception;
            /*
             * Checking for clock skew error again because we don't want to set the global time offset
             * for every service exception.
             */
            if (RetryUtils.isClockSkewError(exception)) {
                int clockSkew = parseClockSkewOffset(execOneParams.apacheResponse, exception);
                SdkGlobalTime.setGlobalTimeOffset(timeOffset = clockSkew);
                request.setTimeOffset(timeOffset); // adjust time offset for the retry
            }
            return null; // => retry
        }

        /**
         * Reset the input stream of the request before a retry.
         *
         * @param request Request containing input stream to reset
         * @throws ResetException If Input Stream can't be reset which means the request can't be
         *                        retried
         */
        private void resetRequestInputStream(final Request<?> request) throws ResetException {
            InputStream requestInputStream = request.getContent();
            if (requestInputStream != null) {
                if (requestInputStream.markSupported()) {
                    try {
                        requestInputStream.reset();
                    } catch (IOException ex) {
                        throw new ResetException("Failed to reset the request input stream", ex);
                    }
                }
            }
        }

        /**
         * @return True if the {@link HttpEntity} should be wrapped in a {@link BufferedHttpEntity}.
         */
        private boolean shouldBufferHttpEntity(final boolean needsConnectionLeftOpen,
                                               final ExecutionContext execContext,
                                               ExecOneRequestParams execParams,
                                               final HttpRequestAbortTaskTracker requestAbortTaskTracker) {
            return (execContext.getClientExecutionTrackerTask().isEnabled() ||
                    requestAbortTaskTracker.isEnabled())
                   && !needsConnectionLeftOpen && execParams.apacheResponse.getEntity() != null;
        }


        /**
         * Captures the connection pool metrics.
         */
        private void captureConnectionPoolMetrics() {
            if (awsRequestMetrics.isEnabled() &&
                httpClient.getHttpClientConnectionManager() instanceof
                        ConnPoolControl<?>) {
                ConnPoolControl<?> control = (ConnPoolControl<?>) httpClient
                        .getHttpClientConnectionManager();

                awsRequestMetrics
                        .withCounter(AwsRequestMetrics.Field.HttpClientPoolAvailableCount,
                                     control.getTotalStats().getAvailable())
                        .withCounter(AwsRequestMetrics.Field.HttpClientPoolLeasedCount, control.getTotalStats().getLeased())
                        .withCounter(AwsRequestMetrics.Field.HttpClientPoolPendingCount,
                                     control.getTotalStats().getPending());
            }

        }

        /**
         * Capture the metrics for the given throwable.
         */
        private <T extends Throwable> T captureExceptionMetrics(T t) {
            awsRequestMetrics.incrementCounterWith(AwsRequestMetrics.Field.Exception)
                             .addProperty(AwsRequestMetrics.Field.Exception, t);
            if (t instanceof AmazonServiceException) {
                AmazonServiceException ase = (AmazonServiceException) t;
                if (RetryUtils.isThrottlingException(ase)) {
                    awsRequestMetrics.incrementCounterWith(AwsRequestMetrics.Field.ThrottleException)
                                     .addProperty(AwsRequestMetrics.Field.ThrottleException, ase);
                }
            }
            return t;
        }

        /**
         * Create a client side identifier that will be sent with the initial request and each
         * retry.
         */
        private void setSdkTransactionId(Request<?> request) {
            request.addHeader(HEADER_SDK_TRANSACTION_ID,
                              new UUID(random.nextLong(), random.nextLong()).toString());
        }

        /**
         * Sets a User-Agent for the specified request, taking into account any custom data.
         */
        private void setUserAgent(Request<?> request) {
            RequestClientOptions opts = requestConfig.getRequestClientOptions();
            if (opts != null) {
                request.addHeader(HEADER_USER_AGENT, RuntimeHttpUtils
                        .getUserAgent(config, opts.getClientMarker(Marker.USER_AGENT)));
            } else {
                request.addHeader(HEADER_USER_AGENT, RuntimeHttpUtils.getUserAgent(config, null));
            }
        }

        /**
         * Adds Retry information to the {@link #HEADER_SDK_RETRY_INFO} header. Used for analysis of
         * retry policy.
         *
         * @param request              Request to add header to
         * @param execOneRequestParams Request context containing retry information
         */
        private void updateRetryHeaderInfo(Request<?> request,
                                           ExecOneRequestParams execOneRequestParams) {
            int availableRetryCapacity = retryCapacity.availableCapacity();

            String headerValue = String.format("%s/%s/%s",
                                               execOneRequestParams.requestCount - 1,
                                               execOneRequestParams.lastBackoffDelay,
                                               availableRetryCapacity >= 0 ?
                                               availableRetryCapacity : "");

            request.addHeader(HEADER_SDK_RETRY_INFO, headerValue);
        }

        /**
         * Returns true if a failed request should be retried.
         *
         * @param params    Params for the individual request being executed.
         * @param exception The client/service exception from the failed request.
         * @return True if the failed request should be retried.
         */
        private boolean shouldRetry(ExecOneRequestParams params, SdkBaseException exception) {
            final int retriesAttempted = params.requestCount - 1;
            final HttpRequestBase method = params.apacheRequest;

            // Never retry on requests containing non-repeatable entity
            if (method instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) method).getEntity();
                if (entity != null && !entity.isRepeatable()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Entity not repeatable");
                    }
                    return false;
                }
            }

            // Do not use retry capacity for throttling exceptions
            if (!RetryUtils.isThrottlingException(exception)) {
                // See if we have enough available retry capacity to be able to execute
                // this retry attempt.
                if (!retryCapacity.acquire(THROTTLED_RETRY_COST)) {
                    awsRequestMetrics.incrementCounter(AwsRequestMetrics.Field.ThrottledRetryCount);
                    return false;
                }
                executionContext.markRetryCapacityConsumed();
            }

            RetryPolicyContext context = RetryPolicyContext.builder()
                                                           .request(request)
                                                           .originalRequest(requestConfig.getOriginalRequest())
                                                           .exception(exception)
                                                           .retriesAttempted(retriesAttempted)
                                                           .httpStatusCode(params.getStatusCode())
                                                           .build();
            // Finally, pass all the context information to the RetryCondition and let it
            // decide whether it should be retried.
            if (!retryPolicy.shouldRetry(context)) {
                // If the retry policy fails we immediately return consumed capacity to the pool.
                if (executionContext.retryCapacityConsumed()) {
                    retryCapacity.release(THROTTLED_RETRY_COST);
                }
                return false;
            }

            return true;
        }

        private boolean isRequestSuccessful(org.apache.http.HttpResponse response) {
            int status = response.getStatusLine().getStatusCode();
            return status / 100 == HttpStatus.SC_OK / 100;
        }

        /**
         * Handles a successful response from a service call by unmarshalling the results using the
         * specified response handler.
         *
         * @return The contents of the response, unmarshalled using the specified response handler.
         * @throws IOException If any problems were encountered reading the response contents from
         *                     the HTTP method object.
         */
        @SuppressWarnings("deprecation")
        private OutputT handleResponse(HttpResponse httpResponse) throws IOException,
                                                                         InterruptedException {
            ProgressListener listener = requestConfig.getProgressListener();
            try {
                /*
                 * Apply the byte counting stream wrapper if the legacy runtime profiling is enabled.
                 */
                CountingInputStream countingInputStream = null;
                InputStream is = httpResponse.getContent();
                if (is != null) {
                    if (System.getProperty(PROFILING_SYSTEM_PROPERTY) != null) {
                        is = countingInputStream = new CountingInputStream(is);
                        httpResponse.setContent(is);
                    }
                    httpResponse.setContent(ProgressInputStream.inputStreamForResponse(is, listener));
                }
                Map<String, String> headers = httpResponse.getHeaders();
                String s = headers.get("Content-Length");
                if (s != null) {
                    try {
                        long contentLength = Long.parseLong(s);
                        publishResponseContentLength(listener, contentLength);
                    } catch (NumberFormatException e) {
                        LOG.warn("Cannot parse the Content-Length header of the response.");
                    }
                }

                OutputT awsResponse;
                awsRequestMetrics.startEvent(AwsRequestMetrics.Field.ResponseProcessingTime);
                publishProgress(listener, ProgressEventType.HTTP_RESPONSE_STARTED_EVENT);
                try {
                    awsResponse = responseHandler
                            .handle(beforeUnmarshalling(httpResponse));
                } finally {
                    awsRequestMetrics.endEvent(AwsRequestMetrics.Field.ResponseProcessingTime);
                }
                publishProgress(listener, ProgressEventType.HTTP_RESPONSE_COMPLETED_EVENT);

                if (countingInputStream != null) {
                    awsRequestMetrics
                            .setCounter(AwsRequestMetrics.Field.BytesProcessed, countingInputStream.getByteCount());
                }
                return awsResponse;
            } catch (IOException | InterruptedException e) {
                throw e;
            } catch (Exception e) {
                String errorMessage =
                        "Unable to unmarshall response (" + e.getMessage() + "). Response Code: "
                        + httpResponse.getStatusCode() + ", Response Text: " +
                        httpResponse.getStatusText();
                throw new SdkClientException(errorMessage, e);
            }
        }

        /**
         * Run {@link RequestHandler2#beforeUnmarshalling(Request, HttpResponse)} callback
         *
         * @param origHttpResponse Original {@link HttpResponse}
         * @return {@link HttpResponse} object to pass to unmarshaller. May have been modified or
         *     replaced by the request handlers
         */
        private HttpResponse beforeUnmarshalling(HttpResponse origHttpResponse) {
            HttpResponse toReturn = origHttpResponse;
            for (RequestHandler2 requestHandler : requestHandler2s) {
                toReturn = requestHandler.beforeUnmarshalling(request, toReturn);
            }
            return toReturn;
        }

        /**
         * Responsible for handling an error response, including unmarshalling the error response
         * into the most specific exception type possible, and throwing the exception.
         *
         * @param method The HTTP method containing the actual response content.
         * @throws IOException If any problems are encountering reading the error response.
         */
        private SdkBaseException handleErrorResponse(HttpRequestBase method,
                                                     final org.apache.http.HttpResponse apacheHttpResponse,
                                                     final HttpContext context)
                throws IOException, InterruptedException {
            final StatusLine statusLine = apacheHttpResponse.getStatusLine();
            final int statusCode;
            final String reasonPhrase;
            if (statusLine == null) {
                statusCode = -1;
                reasonPhrase = null;
            } else {
                statusCode = statusLine.getStatusCode();
                reasonPhrase = statusLine.getReasonPhrase();
            }
            HttpResponse response = createResponse(method, apacheHttpResponse, context);
            SdkBaseException exception;
            try {
                exception = errorResponseHandler.handle(response);
                if (REQUEST_LOG.isDebugEnabled()) {
                    REQUEST_LOG.debug("Received error response: " + exception);
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    String errorMessage = "Unable to unmarshall error response (" + e.getMessage() +
                                          "). Response Code: "
                                          + (statusLine == null ? "None" : statusCode) +
                                          ", Response Text: " + reasonPhrase;
                    throw new SdkClientException(errorMessage, e);
                }
            }

            exception.fillInStackTrace();
            return exception;
        }

        /**
         * Creates and initializes an HttpResponse object suitable to be passed to an HTTP response
         * handler object.
         *
         * @param method  The HTTP method that was invoked to get the response.
         * @param context The HTTP context associated with the request and response.
         * @return The new, initialized HttpResponse object ready to be passed to an HTTP response
         *     handler object.
         * @throws IOException If there were any problems getting any response information from the
         *                     HttpClient method object.
         */
        private HttpResponse createResponse(HttpRequestBase method,
                                            org.apache.http.HttpResponse apacheHttpResponse,
                                            HttpContext context) throws IOException {
            HttpResponse httpResponse = new HttpResponse(request, method, context);

            if (apacheHttpResponse.getEntity() != null) {
                httpResponse.setContent(apacheHttpResponse.getEntity().getContent());
            }

            httpResponse.setStatusCode(apacheHttpResponse.getStatusLine().getStatusCode());
            httpResponse.setStatusText(apacheHttpResponse.getStatusLine().getReasonPhrase());
            for (Header header : apacheHttpResponse.getAllHeaders()) {
                httpResponse.addHeader(header.getName(), header.getValue());
            }

            return httpResponse;
        }

        /**
         * Pause before the next retry and record metrics around retry behavior.
         */
        private void pauseBeforeRetry(ExecOneRequestParams execOneParams,
                                      final ProgressListener listener) throws InterruptedException {
            publishProgress(listener, ProgressEventType.CLIENT_REQUEST_RETRY_EVENT);
            // Notify the progress listener of the retry
            awsRequestMetrics.startEvent(AwsRequestMetrics.Field.RetryPauseTime);
            try {
                doPauseBeforeRetry(execOneParams);
            } finally {
                awsRequestMetrics.endEvent(AwsRequestMetrics.Field.RetryPauseTime);
            }
        }

        /**
         * Sleep for a period of time on failed request to avoid flooding a service with retries.
         */
        private void doPauseBeforeRetry(ExecOneRequestParams execOneParams) throws InterruptedException {
            final int retriesAttempted = execOneParams.requestCount - 2;
            RetryPolicyContext context = RetryPolicyContext.builder()
                                                           .request(request)
                                                           .originalRequest(requestConfig.getOriginalRequest())
                                                           .retriesAttempted(retriesAttempted)
                                                           .exception(execOneParams.retriedException)
                                                           .build();
            // don't pause if the retry was not due to a redirection (I.E. when retried exception is null)
            if (context.exception() != null) {
                long delay = retryPolicy.computeDelayBeforeNextRetry(context);
                execOneParams.lastBackoffDelay = delay;

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Retriable error detected, " + "will retry in " + delay +
                              "ms, attempt number: " + retriesAttempted);
                }
                Thread.sleep(delay);
            }
        }

        // SWF: Signature not yet current: 20140819T173921Z is still later than 20140819T173829Z
        // (20140819T173329Z + 5 min.)

        /**
         * Returns date string from the exception message body in form of yyyyMMdd'T'HHmmss'Z' We
         * needed to extract date from the message body because SQS is the only service that does
         * not provide date header in the response. Example, when device time is behind than the
         * server time than we get a string that looks something like this: "Signature expired:
         * 20130401T030113Z is now earlier than 20130401T034613Z (20130401T040113Z - 15 min.)"
         *
         * @param body The message from where the server time is being extracted
         * @return Return datetime in string format (yyyyMMdd'T'HHmmss'Z')
         */
        private String getServerDateFromException(String body) {
            final int startPos = body.indexOf("(");
            int endPos = body.indexOf(" + ");
            if (endPos == -1) {
                endPos = body.indexOf(" - ");
            }
            return endPos == -1 ? null : body.substring(startPos + 1, endPos);
        }

        /**
         * Returns the difference between the client's clock time and the service clock time in unit
         * of seconds.
         */
        private int parseClockSkewOffset(org.apache.http.HttpResponse response,
                                         SdkBaseException exception) {
            final long currentTimeMilli = System.currentTimeMillis();
            Date serverDate;
            String serverDateStr = null;
            Header[] responseDateHeader = response.getHeaders("Date");

            try {
                if (responseDateHeader.length == 0) {
                    // SQS doesn't return Date header
                    final String errmsg = exception.getMessage();
                    serverDateStr = getServerDateFromException(errmsg);
                    if (serverDateStr == null) {
                        LOG.warn("Unable to parse clock skew offset from errmsg: " + errmsg);
                        return 0;
                    }
                    serverDate = DateUtils.parseCompressedIso8601Date(serverDateStr);
                } else {
                    serverDateStr = responseDateHeader[0].getValue();
                    serverDate = DateUtils.parseRfc822Date(serverDateStr);
                }
            } catch (RuntimeException e) {
                LOG.warn("Unable to parse clock skew offset from response: " + serverDateStr, e);
                return 0;
            }

            long diff = currentTimeMilli - serverDate.getTime();
            return (int) (diff / 1000);
        }

        /**
         * Gets the correct request timeout taking into account precedence of the configuration in
         * {@link AmazonWebServiceRequest} versus {@link ClientConfiguration}.
         *
         * @param requestConfig Current request configuration
         * @return Request timeout value or 0 if none is set
         */
        private int getRequestTimeout(RequestConfig requestConfig) {
            if (requestConfig.getRequestTimeout() != null) {
                return requestConfig.getRequestTimeout();
            } else {
                return config.getRequestTimeout();
            }
        }

        /**
         * Gets the correct client execution timeout taking into account precedence of the
         * configuration in {@link AmazonWebServiceRequest} versus {@link ClientConfiguration}.
         *
         * @param requestConfig Current request configuration
         * @return Client Execution timeout value or 0 if none is set
         */
        private int getClientExecutionTimeout(RequestConfig requestConfig) {
            if (requestConfig.getClientExecutionTimeout() != null) {
                return requestConfig.getClientExecutionTimeout();
            } else {
                return config.getClientExecutionTimeout();
            }
        }

        /**
         * Stateful parameters that are used for executing a single httpClientSettings request.
         */
        private class ExecOneRequestParams {
            int requestCount; // monotonic increasing
            /**
             * Last delay between retries.
             */
            long lastBackoffDelay = 0;
            SdkBaseException retriedException; // last retryable exception
            HttpRequestBase apacheRequest;
            org.apache.http.HttpResponse apacheResponse;
            URI redirectedUri;
            AuthRetryParameters authRetryParam;
            /*
             * Depending on which response handler we end up choosing to handle the HTTP response, it
             * might require us to leave the underlying HTTP connection open, depending on whether or
             * not it reads the complete HTTP response stream from the HTTP connection, or if delays
             * reading any of the content until after a response is returned to the caller.
             */
            boolean leaveHttpConnectionOpen;
            private Signer signer; // cached
            private URI signerUri;

            boolean isRetry() {
                return requestCount > 1 || redirectedUri != null || authRetryParam != null;
            }

            void initPerRetry() {
                requestCount++;
                apacheRequest = null;
                apacheResponse = null;
                leaveHttpConnectionOpen = false;
            }

            void newSigner(final Request<?> request, final ExecutionContext execContext) {
                final SignerProviderContext.Builder signerProviderContext = SignerProviderContext
                        .builder()
                        .withRequest(request)
                        .withRequestConfig(requestConfig);
                if (authRetryParam != null) {
                    signerUri = authRetryParam.getEndpointForRetry();
                    signer = authRetryParam.getSignerForRetry();
                    // Push the local signer override back to the execution context
                    execContext.setSigner(signer);
                } else if (redirectedUri != null && !redirectedUri.equals(signerUri)) {
                    signerUri = redirectedUri;
                    signer = execContext.getSigner(signerProviderContext
                                                           .withUri(signerUri)
                                                           .withIsRedirect(true)
                                                           .build());
                } else if (signer == null) {
                    signerUri = request.getEndpoint();
                    signer = execContext
                            .getSigner(signerProviderContext.withUri(signerUri).build());
                }
            }

            /**
             * @throws FakeIoException thrown only during test simulation.
             */
            HttpRequestBase newApacheRequest(
                    final HttpRequestFactory<HttpRequestBase> httpRequestFactory,
                    final Request<?> request,
                    final HttpClientSettings options) throws IOException {

                apacheRequest = httpRequestFactory.create(request, options);
                if (redirectedUri != null) {
                    apacheRequest.setURI(redirectedUri);
                }
                return apacheRequest;
            }

            void resetBeforeHttpRequest() {
                retriedException = null;
                authRetryParam = null;
                redirectedUri = null;
            }

            private Integer getStatusCode() {
                if (apacheResponse == null || apacheResponse.getStatusLine() == null) {
                    return null;
                }
                return apacheResponse.getStatusLine().getStatusCode();
            }
        }
    }
}
