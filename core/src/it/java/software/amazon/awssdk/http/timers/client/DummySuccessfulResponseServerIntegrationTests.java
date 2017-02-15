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

package software.amazon.awssdk.http.timers.client;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.interruptCurrentThreadAfterDelay;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.CLIENT_EXECUTION_TIMEOUT;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.SLOW_REQUEST_HANDLER_TIMEOUT;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.TEST_TIMEOUT;

import java.io.IOException;
import java.util.List;
import org.apache.http.pool.ConnPoolControl;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.TestPreConditions;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.MockServerTestBase;
import software.amazon.awssdk.http.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.internal.http.apache.client.impl.ApacheHttpClientFactory;
import software.amazon.awssdk.internal.http.apache.client.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.internal.http.apache.client.impl.SdkHttpClient;
import software.amazon.awssdk.internal.http.request.RequestHandlerTestUtils;
import software.amazon.awssdk.internal.http.request.SlowRequestHandler;
import software.amazon.awssdk.internal.http.response.DummyResponseHandler;
import software.amazon.awssdk.internal.http.response.UnresponsiveResponseHandler;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;

public class DummySuccessfulResponseServerIntegrationTests extends MockServerTestBase {

    private static final int STATUS_CODE = 200;

    private AmazonHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Override
    protected MockServer buildMockServer() {
        return new MockServer(MockServer.DummyResponseServerBehavior.build(STATUS_CODE, "OK", "Hi"));
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowResponseHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = new AmazonHttpClient(
                new ClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT));

        requestBuilder().execute(new UnresponsiveResponseHandler());
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowAfterResponseRequestHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = new AmazonHttpClient(
                new ClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT));

        List<RequestHandler2> requestHandlers = RequestHandlerTestUtils.buildRequestHandlerList(
                new SlowRequestHandler().withAfterResponseWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT));

        requestBuilder().executionContext(withHandlers(requestHandlers)).execute(new DummyResponseHandler());
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowBeforeRequestRequestHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = new AmazonHttpClient(
                new ClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT));

        List<RequestHandler2> requestHandlers = RequestHandlerTestUtils.buildRequestHandlerList(
                new SlowRequestHandler().withBeforeRequestWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT));

        requestBuilder().executionContext(withHandlers(requestHandlers)).execute(new DummyResponseHandler());
    }

    /**
     * Tests that a streaming operation has it's request properly cleaned up if the client is interrupted after the
     * response is received.
     *
     * @see TT0070103230
     */
    @Test
    public void clientInterruptedDuringResponseHandlers_DoesNotLeakConnection() throws IOException {
        ClientConfiguration config = new ClientConfiguration();
        ConnectionManagerAwareHttpClient rawHttpClient = new ApacheHttpClientFactory().create(HttpClientSettings.adapt(config));

        httpClient = new AmazonHttpClient(config, rawHttpClient, null);

        interruptCurrentThreadAfterDelay(1000);
        List<RequestHandler2> requestHandlers = RequestHandlerTestUtils
                .buildRequestHandlerList(new SlowRequestHandler().withAfterResponseWaitInSeconds(10));
        try {
            requestBuilder().executionContext(withHandlers(requestHandlers))
                            .execute(new DummyResponseHandler().leaveConnectionOpen());
            fail("Expected exception");
        } catch (AmazonClientException e) {
            assertThat(e.getCause(), instanceOf(InterruptedException.class));
        }

        @SuppressWarnings("deprecation")
        int leasedConnections = ((ConnPoolControl<?>) ((SdkHttpClient) rawHttpClient).getHttpClientConnectionManager())
                .getTotalStats().getLeased();
        assertEquals(0, leasedConnections);
    }

    private AmazonHttpClient.RequestExecutionBuilder requestBuilder() {
        return httpClient.requestExecutionBuilder().request(newGetRequest());
    }

    private ExecutionContext withHandlers(List<RequestHandler2> requestHandlers) {
        return ExecutionContext.builder().withRequestHandler2s(requestHandlers).build();
    }

}
