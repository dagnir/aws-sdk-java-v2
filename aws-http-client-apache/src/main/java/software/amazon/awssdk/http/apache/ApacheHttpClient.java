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

package software.amazon.awssdk.http.apache;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.pool.ConnPoolControl;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.apache.internal.impl.ApacheHttpRequestFactory;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
class ApacheHttpClient implements SdkHttpClient {

    private final ApacheHttpRequestFactory apacheHttpRequestFactory = new ApacheHttpRequestFactory();
    private final ConnectionManagerAwareHttpClient httpClient;
    private final ApacheHttpRequestConfig requestConfig;

    ApacheHttpClient(ConnectionManagerAwareHttpClient httpClient, ApacheHttpRequestConfig requestConfig) {
        this.httpClient = Validate.notNull(httpClient, "httpClient must not be null.");
        this.requestConfig = Validate.notNull(requestConfig, "requestConfig must not be null.");
    }

    @Override
    public AbortableCallable<SdkHttpResponse> prepareRequest(SdkHttpRequest request, SdkRequestContext context) {
        final HttpRequestBase apacheRequest = toApacheRequest(request);
        return new AbortableCallable<SdkHttpResponse>() {
            @Override
            public SdkHttpResponse call() throws Exception {
                return execute(context, apacheRequest);
            }

            @Override
            public void abort() {
                apacheRequest.abort();
            }
        };
    }

    private SdkHttpResponse execute(SdkRequestContext context, HttpRequestBase apacheRequest) throws IOException {
        final AwsRequestMetrics awsRequestMetrics = context.metrics();
        captureConnectionPoolMetrics(awsRequestMetrics);

        Map<String, Object> metricsContext = new HashMap<>();
        metricsContext.put(AwsRequestMetrics.class.getSimpleName(), awsRequestMetrics);

        final HttpClientContext localRequestContext = ApacheUtils
                .newClientContext(requestConfig.proxyConfiguration(), metricsContext);
        try {
            awsRequestMetrics.startEvent(AwsRequestMetrics.Field.HttpRequestTime);
            final HttpResponse httpResponse = httpClient.execute(apacheRequest, localRequestContext);
            return createResponse(httpResponse);
        } finally {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.HttpRequestTime);
        }
    }

    private void captureConnectionPoolMetrics(AwsRequestMetrics awsRequestMetrics) {
        if (awsRequestMetrics.isEnabled() &&
            httpClient.getHttpClientConnectionManager() instanceof ConnPoolControl<?>) {
            ConnPoolControl<?> control = (ConnPoolControl<?>) httpClient.getHttpClientConnectionManager();

            awsRequestMetrics
                    .withCounter(AwsRequestMetrics.Field.HttpClientPoolAvailableCount, control.getTotalStats().getAvailable())
                    .withCounter(AwsRequestMetrics.Field.HttpClientPoolLeasedCount, control.getTotalStats().getLeased())
                    .withCounter(AwsRequestMetrics.Field.HttpClientPoolPendingCount, control.getTotalStats().getPending());
        }

    }

    private HttpRequestBase toApacheRequest(SdkHttpRequest request) {
        return apacheHttpRequestFactory.create(request, requestConfig);
    }

    @Override
    public void close() {
        httpClient.getHttpClientConnectionManager().shutdown();
    }

    /**
     * Creates and initializes an HttpResponse object suitable to be passed to an HTTP response
     * handler object.
     *
     * @return The new, initialized HttpResponse object ready to be passed to an HTTP response handler object.
     * @throws IOException If there were any problems getting any response information from the
     *                     HttpClient method object.
     */
    private SdkHttpResponse createResponse(org.apache.http.HttpResponse apacheHttpResponse) throws IOException {
        return SdkHttpResponse.builder()
                .statusCode(apacheHttpResponse.getStatusLine().getStatusCode())
                .statusText(apacheHttpResponse.getStatusLine().getReasonPhrase())
                .content(apacheHttpResponse.getEntity() != null ? apacheHttpResponse.getEntity().getContent() : null)
                .headers(transformHeaders(apacheHttpResponse))
                .build();

    }

    private Map<String, List<String>> transformHeaders(HttpResponse apacheHttpResponse) {
        return Stream.of(apacheHttpResponse.getAllHeaders())
                .collect(groupingBy(Header::getName, mapping(Header::getValue, toList())));
    }
}
