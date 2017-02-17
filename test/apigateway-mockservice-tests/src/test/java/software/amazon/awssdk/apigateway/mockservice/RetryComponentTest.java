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

package software.amazon.awssdk.apigateway.mockservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static software.amazon.awssdk.apigateway.mockservice.WireMockExtensions.anyRequestedFor;

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.opensdk.retry.RetryPolicyBuilder;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.services.apigateway.mockservice.MyService;
import software.amazon.awssdk.services.apigateway.mockservice.MyServiceClientBuilder;
import software.amazon.awssdk.services.apigateway.mockservice.model.GetNoauthErrorsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.InternalServerErrorException;

public class RetryComponentTest {

    private static final UrlMatchingStrategy URI = urlMatching(".*");

    private static final RetryPolicy CUSTOM_RETRY_POLICY = RetryPolicyBuilder.standard()
                                                                             .retryOnStatusCodes(404, 429)
                                                                             .retryOnExceptions(InternalServerErrorException.class)
                                                                             .fixedBackoff(10)
                                                                             .maxNumberOfRetries(3)
                                                                             .build();
    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort());

    @Test
    public void defaultRetryPolicyUsed_ThrottlingErrorsRetriedUpToMax() {
        stubFor(any(URI).willReturn(aResponse().withStatus(429)));
        MyService service = createServiceBuilder().build();

        callApi(service);

        verify(4, anyRequestedFor(URI));
    }

    @Test
    public void customRetryPolicyUsed_NonRetryableStatusCodeNotRetried() {
        stubFor(any(URI).willReturn(aResponse().withStatus(413)));
        MyService service = createServiceBuilder()
                .retryPolicy(CUSTOM_RETRY_POLICY)
                .build();

        callApi(service);

        verify(1, anyRequestedFor(URI));
    }

    @Test
    public void customRetryPolicyUsed_RetryableStatusCodeRetriedUpToMax() {
        stubFor(any(URI).willReturn(aResponse().withStatus(404)));
        MyService service = createServiceBuilder()
                .retryPolicy(CUSTOM_RETRY_POLICY)
                .build();

        callApi(service);

        verify(4, anyRequestedFor(URI));
    }

    @Test
    public void customRetryPolicyUsed_RetryableExceptionRetriedUpToMax() {
        // The 500 status code is bound to the InternalErrorException class and it's marked
        // as retryable per the custom policy
        stubFor(any(URI).willReturn(aResponse().withStatus(500)));
        MyService service = createServiceBuilder()
                .retryPolicy(CUSTOM_RETRY_POLICY)
                .build();

        callApi(service);

        verify(4, anyRequestedFor(URI));
    }

    private void callApi(MyService service) {
        try {
            service.getNoauthErrors(new GetNoauthErrorsRequest()
                                            .errorType("InternalError"));
        } catch (SdkBaseException expected) {
            // Ignored or expected.
        }

    }

    private MyServiceClientBuilder createServiceBuilder() {
        return MyService.builder().endpoint("http://localhost:" + mockServer.port());
    }

}
