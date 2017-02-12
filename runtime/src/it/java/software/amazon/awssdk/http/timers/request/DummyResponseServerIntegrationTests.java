/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.timers.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.assertNumberOfRetries;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.assertNumberOfTasksTriggered;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.TEST_TIMEOUT;

import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.TestPreConditions;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.MockServerTestBase;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.internal.http.apache.client.impl.ApacheHttpClientFactory;
import software.amazon.awssdk.internal.http.apache.client.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.internal.http.client.HttpClientFactory;
import software.amazon.awssdk.internal.http.response.ErrorDuringUnmarshallingResponseHandler;
import software.amazon.awssdk.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;

/**
 * Tests that use a server that returns a predetermined response within the timeout limit
 */
public class DummyResponseServerIntegrationTests extends MockServerTestBase {

    private static final int STATUS_CODE = 500;
    private AmazonHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Override
    protected MockServer buildMockServer() {
        return new MockServer(
                MockServer.DummyResponseServerBehavior.build(STATUS_CODE, "Internal Server Failure", "Dummy response"));
    }

    @Test(timeout = TEST_TIMEOUT)
    public void requestTimeoutEnabled_ServerRespondsWithRetryableError_RetriesUpToLimitThenThrowsServerException()
            throws IOException {
        int maxRetries = 2;
        ClientConfiguration config = new ClientConfiguration().withRequestTimeout(25 * 1000)
                .withClientExecutionTimeout(25 * 1000).withMaxErrorRetry(maxRetries);
        HttpClientFactory<ConnectionManagerAwareHttpClient> httpClientFactory = new ApacheHttpClientFactory();
        ConnectionManagerAwareHttpClient rawHttpClient = spy(httpClientFactory.create(HttpClientSettings.adapt(config)));

        httpClient = new AmazonHttpClient(config, rawHttpClient, null);

        try {
            httpClient.execute(newGetRequest(),
                               new ErrorDuringUnmarshallingResponseHandler(),
                               new NullErrorResponseHandler(),
                               new ExecutionContext());
            fail("Exception expected");
        } catch (AmazonServiceException e) {
            assertEquals(e.getStatusCode(), STATUS_CODE);
            int expectedNumberOfRequests = 1 + maxRetries;
            assertNumberOfRetries(rawHttpClient, expectedNumberOfRequests);
            assertNumberOfTasksTriggered(httpClient.getHttpRequestTimer(), 0);
            assertNumberOfTasksTriggered(httpClient.getClientExecutionTimer(), 0);
        }
    }


}