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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static software.amazon.awssdk.http.timers.ClientExecutionAndRequestTimerTestUtils.assertNumberOfRetries;
import static software.amazon.awssdk.http.timers.ClientExecutionAndRequestTimerTestUtils.assertNumberOfTasksTriggered;
import static software.amazon.awssdk.http.timers.ClientExecutionAndRequestTimerTestUtils.execute;
import static software.amazon.awssdk.http.timers.TimeoutTestConstants.TEST_TIMEOUT;

import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.TestPreConditions;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.OverloadedMockServerTestBase;
import software.amazon.awssdk.http.apache.client.impl.ApacheHttpClientFactory;
import software.amazon.awssdk.http.apache.client.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.client.HttpClientFactory;
import software.amazon.awssdk.http.exception.HttpRequestTimeoutException;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.http.server.MockServer.ServerBehavior;
import software.amazon.awssdk.http.settings.HttpClientSettings;

/**
 * Tests requiring an Overloaded server, that is a server that responds but can't close the connection in a timely
 * fashion
 */
public class OverloadedServerIntegrationTests extends OverloadedMockServerTestBase {

    private AmazonHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Override
    protected MockServer buildMockServer() {
        return MockServer.createMockServer(ServerBehavior.OVERLOADED);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void requestTimeoutEnabled_HonorsRetryPolicy() throws IOException {
        int maxRetries = 2;
        ClientConfiguration config = new ClientConfiguration().withRequestTimeout(1 * 1000)
                .withMaxErrorRetry(maxRetries);
        HttpClientFactory<ConnectionManagerAwareHttpClient> httpClientFactory = new ApacheHttpClientFactory();
        ConnectionManagerAwareHttpClient rawHttpClient = spy(httpClientFactory.create(HttpClientSettings.adapt(config)));

        httpClient = new AmazonHttpClient(config, rawHttpClient, null);

        try {
            execute(httpClient, newGetRequest());
            fail("Exception expected");
        } catch (AmazonClientException e) {
            /* the expected exception and number of requests. */
            assertThat(e.getCause(), instanceOf(HttpRequestTimeoutException.class));
            int expectedNumberOfRequests = 1 + maxRetries;
            assertNumberOfRetries(rawHttpClient, expectedNumberOfRequests);
            assertNumberOfTasksTriggered(httpClient.getHttpRequestTimer(), expectedNumberOfRequests);
        }
    }

}