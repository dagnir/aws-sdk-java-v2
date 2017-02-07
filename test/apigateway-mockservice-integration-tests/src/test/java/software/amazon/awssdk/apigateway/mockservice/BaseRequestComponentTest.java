package software.amazon.awssdk.apigateway.mockservice;

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import software.amazon.awssdk.http.exception.HttpRequestTimeoutException;
import software.amazon.awssdk.http.timers.client.ClientExecutionTimeoutException;
import software.amazon.awssdk.opensdk.SdkRequestConfig;
import software.amazon.awssdk.services.apigateway.mockservice.MyService;
import software.amazon.awssdk.services.apigateway.mockservice.model.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.apigateway.mockservice.WireMockExtensions.anyRequestedFor;

/**
 * For this test GETs are stubbed with a dummy successful response and PUTS are stubbed with a
 * successful response with a fixed delay of {@value #DELAY_ON_PUTS}.
 */
public class BaseRequestComponentTest {

    private static final int DELAY_ON_PUTS = 10_000;

    private static final UrlMatchingStrategy URI = urlMatching(".*");

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort());

    private MyService client;

    @Before
    public void setUp() {
        stubFor(get(URI).willReturn(aResponse()));
        stubFor(put(URI).willReturn(aResponse().withFixedDelay(DELAY_ON_PUTS)));
        client = MyService.builder().endpoint("http://localhost:" + mockServer.port()).build();
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
     * to observe that request timeout is honored is to trigger the timeout.
     */
    @Test
    public void customRequestTimeoutSetInBaseRequest_IsHonoredByRuntime() {
        final int expectedTimeout = 100;
        assertRequestTimeoutTriggered(expectedTimeout, () ->
                client.putNoauthScalars(new PutNoauthScalarsRequest().sdkRequestConfig(
                        SdkRequestConfig.builder()
                                .httpRequestTimeout(expectedTimeout)
                                .build()
                ))
        );
    }

    /**
     * Note that this is a time based test and may be subject to occasional flakiness. The only way
     * to observe that client execution timeout is honored is to trigger the timeout.
     */
    @Test
    public void customClientExecutionSetInBaseRequest_IsHonoredByRuntime() {
        final int expectedTimeout = 100;
        assertClientExecutionTimeoutTriggered(expectedTimeout, () ->
                client.putNoauthScalars(new PutNoauthScalarsRequest().sdkRequestConfig(
                        SdkRequestConfig.builder()
                                .totalExecutionTimeout(expectedTimeout)
                                .build()
                ))
        );
    }

    @Test
    public void userAgentContainsApigAgent() {
        client.getNoauthScalars(new GetNoauthScalarsRequest());
        verify(anyRequestedFor(URI).withHeader("User-Agent", containing("apig-java")));
    }

    private void assertRequestTimeoutTriggered(int expectedTimeout, Runnable runnable) {
        assertTimeoutExceptionTriggered(expectedTimeout, runnable,
                                        e -> assertThat(e.getCause(), instanceOf(
                                                HttpRequestTimeoutException.class)));
    }

    private void assertClientExecutionTimeoutTriggered(int expectedTimeout, Runnable runnable) {
        assertTimeoutExceptionTriggered(expectedTimeout, runnable,
                                        e -> assertThat(e, instanceOf(
                                                ClientExecutionTimeoutException.class)));
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
