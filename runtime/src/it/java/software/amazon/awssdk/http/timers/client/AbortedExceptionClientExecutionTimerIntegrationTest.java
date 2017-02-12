/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.timers.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.createMockGetRequest;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.createRawHttpClientSpy;
import static software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.execute;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.CLIENT_EXECUTION_TIMEOUT;

import java.io.IOException;
import java.io.InputStream;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AbortedException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.TestPreConditions;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.HttpMethodName;
import software.amazon.awssdk.http.MockServerTestBase;
import software.amazon.awssdk.http.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.internal.http.apache.client.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.internal.http.request.EmptyHttpRequest;
import software.amazon.awssdk.runtime.io.SdkBufferedInputStream;

/**
 *
 */
public class AbortedExceptionClientExecutionTimerIntegrationTest extends
                                                                 MockServerTestBase {

    private AmazonHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Override
    protected MockServer buildMockServer() {
        return new MockServer(
                MockServer.DummyResponseServerBehavior.build(200, "Hi",
                                                             "Dummy response"));
    }

    @Test(expected = AbortedException.class)
    public void
    clientExecutionTimeoutEnabled_aborted_exception_occurs_timeout_not_expired()
            throws Exception {
        ClientConfiguration config = new ClientConfiguration()
                .withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
                .withMaxErrorRetry(0);
        ConnectionManagerAwareHttpClient rawHttpClient =
                createRawHttpClientSpy(config);

        doThrow(new AbortedException()).when(rawHttpClient).execute(any
                                                                            (HttpRequestBase.class), any(HttpContext.class));

        httpClient = new AmazonHttpClient(config, rawHttpClient, null);

        execute(httpClient, createMockGetRequest());
    }

    @Test(expected = ClientExecutionTimeoutException.class)
    public void
    clientExecutionTimeoutEnabled_aborted_exception_occurs_timeout_expired()
            throws Exception {
        ClientConfiguration config = new ClientConfiguration()
                .withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
                .withMaxErrorRetry(0);
        ConnectionManagerAwareHttpClient rawHttpClient =
                createRawHttpClientSpy(config);

        httpClient = new AmazonHttpClient(config, rawHttpClient, null);

        execute(httpClient, new EmptyHttpRequest(server.getEndpoint(),
                                                 HttpMethodName.PUT, new SdkBufferedInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                // Sleeping here to avoid OOM issues from a limitless InputStream
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 1;
            }
        })));
    }
}
