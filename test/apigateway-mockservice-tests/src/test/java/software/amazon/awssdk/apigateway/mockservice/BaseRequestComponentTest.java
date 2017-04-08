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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.apigateway.mockservice.WireMockExtensions.anyRequestedFor;

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.apigateway.mockservice.model.GetNoauthScalarsRequest;
import software.amazon.awssdk.apigateway.mockservice.model.PutNoauthScalarsRequest;
import software.amazon.awssdk.http.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.opensdk.SdkRequestConfig;

/**
 * For this test GETs are stubbed with a dummy successful response and PUTS are stubbed with a
 * successful response with a fixed delay of {@value #DELAY_ON_PUTS}.
 */
public class BaseRequestComponentTest {

    private static final int DELAY_ON_PUTS = 10_000;

    private static final UrlMatchingStrategy URI = urlMatching(".*");

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort());

    private MyServiceClient client;

    @Before
    public void setUp() {
        stubFor(get(URI).willReturn(aResponse()));
        stubFor(put(URI).willReturn(aResponse().withFixedDelay(DELAY_ON_PUTS)));
        client = MyServiceClient.builder().endpoint("http://localhost:" + mockServer.port()).build();
    }

    @Test
    public void customHeadersSetInBaseRequest_AreSentInActualRequest() {
        client.getNoauthScalars(new GetNoauthScalarsRequest().sdkRequestConfig(
                SdkRequestConfig.builder()
                        .customHeader("FooHeader", "FooValue")
                        .customHeader("BarHeader", "BarValue")
                        .build()
        ));
        verify(anyRequestedFor(URI).withHeader("FooHeader", equalTo("FooValue")));
        verify(anyRequestedFor(URI).withHeader("BarHeader", equalTo("BarValue")));
    }

    @Test
    public void customQueryParamsSetInBaseRequest_AreSentInActualRequest() {
        client.getNoauthScalars(new GetNoauthScalarsRequest().sdkRequestConfig(
                SdkRequestConfig.builder()
                        .customQueryParam("FooParam", "valOne")
                        .customQueryParam("FooParam", "valTwo")
                        .customQueryParam("BarParam", "BarValue")
                        .build()
        ));
        verify(anyRequestedFor(URI).withQueryParam("FooParam", equalTo("valOne")));
        verify(anyRequestedFor(URI).withQueryParam("FooParam", equalTo("valTwo")));
        verify(anyRequestedFor(URI).withQueryParam("BarParam", equalTo("BarValue")));
    }

    /**
     * Note that this is a time based test and may be subject to occasional flakiness. The only way
     * to observe that client execution timeout is honored is to trigger the timeout.
     */
    @Test
    public void customClientExecutionSetInBaseRequest_IsHonoredByRuntime() {
        final int expectedTimeout = 100;
        Runnable runnable = () -> {
            SdkRequestConfig requestConfig = SdkRequestConfig.builder().totalExecutionTimeout(expectedTimeout).build();
            client.putNoauthScalars(new PutNoauthScalarsRequest().sdkRequestConfig(requestConfig));
        };
        assertClientExecutionTimeoutTriggered(expectedTimeout, runnable);
    }

    @Test
    public void userAgentContainsApigAgent() {
        client.getNoauthScalars(new GetNoauthScalarsRequest());
        verify(anyRequestedFor(URI).withHeader("User-Agent", containing("apig-java")));
    }

    private void assertClientExecutionTimeoutTriggered(int expectedTimeout, Runnable runnable) {
        assertTimeoutExceptionTriggered(
                expectedTimeout, runnable,
                e -> assertThat(e, instanceOf(ClientExecutionTimeoutException.class))
        );
    }

    /**
     * Asserts that the timeout is triggered within the expected timeout value.
     *
     * @param expectedTimeout    Expected timeout value (a generous delta is applied to the
     *                           assertion as there is no hard guarantee on when the timeout is
     *                           triggered).
     * @param runnable           Client operation to invoke that should timeout
     * @param exceptionAssertion Consumer that should throw an {@link AssertionError} if the
     *                           expection is not what was expected.
     */
    private void assertTimeoutExceptionTriggered(int expectedTimeout, Runnable runnable,
                                                 Consumer<Exception> exceptionAssertion) {
        // Delta of 1 second on either side
        final int delta = 1000;

        long startTime = System.nanoTime();
        try {
            runnable.run();
            fail("Expected an exception");
        } catch (Exception e) {
            exceptionAssertion.accept(e);
        }
        long endTime = System.nanoTime();

        assertTimeout(expectedTimeout, TimeUnit.MILLISECONDS
                .convert(endTime - startTime, TimeUnit.NANOSECONDS), delta);
    }

    /**
     * Assert that the actual time taken matches the expected time within some acceptable delta.
     *
     * @param expectedTime Expected time in milliseconds
     * @param actualTime   Actual time taken in milliseconds
     * @param delta        Acceptable delta for comparison
     */
    private void assertTimeout(int expectedTime, long actualTime, long delta) {
        System.out.printf("Expected %d Actual %d", expectedTime, actualTime);
        assertThat(Math.abs(expectedTime - actualTime), lessThan(delta));
    }
}
