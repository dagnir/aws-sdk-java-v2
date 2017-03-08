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

package software.amazon.awssdk.internal.http.timers.request;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.SocketTimeoutException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.TestPreConditions;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.UnresponsiveMockServerTestBase;
import software.amazon.awssdk.http.exception.HttpRequestTimeoutException;
import software.amazon.awssdk.internal.http.request.EmptyHttpRequest;
import software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.internal.http.timers.TimeoutTestConstants;
import utils.model.EmptyAmazonWebServiceRequest;

/**
 * Tests requiring an unresponsive server, that is a server that accepts a connection but doesn't
 * write any data to the response
 */
public class UnresponsiveServerTests extends UnresponsiveMockServerTestBase {

    private static final int REQUEST_TIMEOUT = 5 * 1000;
    private static final int LONGER_SOCKET_TIMEOUT = REQUEST_TIMEOUT * TimeoutTestConstants.PRECISION_MULTIPLIER;
    private static final int SHORTER_SOCKET_TIMEOUT = REQUEST_TIMEOUT / TimeoutTestConstants.PRECISION_MULTIPLIER;

    private AmazonHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Test(timeout = TimeoutTestConstants.TEST_TIMEOUT)
    public void requestTimeoutDisabled_ConnectionClosedBySocketTimeout_NoThreadsCreated() {
        final int socketTimeout = 1000;
        httpClient = new AmazonHttpClient(
                new LegacyClientConfiguration().withSocketTimeout(socketTimeout).withRequestTimeout(0).withMaxErrorRetry(0));

        try {
            ClientExecutionAndRequestTimerTestUtils.execute(httpClient, newGetRequest());
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertThat(e.getCause(), instanceOf(SocketTimeoutException.class));
            ClientExecutionAndRequestTimerTestUtils.assertRequestTimerExecutorNotCreated(httpClient.getHttpRequestTimer());
        }
    }

    @Test(timeout = TimeoutTestConstants.TEST_TIMEOUT)
    public void requestTimeoutSetInRequestObject_WithLongerSocketTimeout_ThrowsRequestTimeoutException() {
        httpClient = new AmazonHttpClient(
                new LegacyClientConfiguration().withSocketTimeout(LONGER_SOCKET_TIMEOUT).withMaxErrorRetry(0));

        try {
            EmptyHttpRequest request = newGetRequest();
            request.setOriginalRequest(new EmptyAmazonWebServiceRequest().withSdkRequestTimeout(REQUEST_TIMEOUT));
            ClientExecutionAndRequestTimerTestUtils.execute(httpClient, request);
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertThat(e.getCause(), instanceOf(HttpRequestTimeoutException.class));
        }
    }

    @Test(timeout = TimeoutTestConstants.TEST_TIMEOUT)
    public void requestTimeoutSetInRequestObject_WithShorterSocketTimeout_ThrowsRequestTimeoutException() {
        httpClient = new AmazonHttpClient(
                new LegacyClientConfiguration().withSocketTimeout(SHORTER_SOCKET_TIMEOUT).withMaxErrorRetry(0));

        try {
            EmptyHttpRequest request = newGetRequest();
            request.setOriginalRequest(new EmptyAmazonWebServiceRequest().withSdkRequestTimeout(REQUEST_TIMEOUT));
            ClientExecutionAndRequestTimerTestUtils.execute(httpClient, request);
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertThat(e.getCause(), instanceOf(SocketTimeoutException.class));
        }
    }

    @Test(timeout = TimeoutTestConstants.TEST_TIMEOUT)
    public void requestTimeoutSetInRequestObject_TakesPrecedenceOverClientConfiguration() {
        // Client configuration is set arbitrarily high so that the test will timeout if the
        // client configuration is incorrectly honored over the request config
        httpClient = new AmazonHttpClient(new LegacyClientConfiguration().withSocketTimeout(LONGER_SOCKET_TIMEOUT)
                                                                         .withRequestTimeout(REQUEST_TIMEOUT * 1000).withMaxErrorRetry(0));

        try {
            EmptyHttpRequest request = newGetRequest();
            request.setOriginalRequest(new EmptyAmazonWebServiceRequest().withSdkRequestTimeout(REQUEST_TIMEOUT));
            ClientExecutionAndRequestTimerTestUtils.execute(httpClient, request);
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertThat(e.getCause(), instanceOf(HttpRequestTimeoutException.class));
        }
    }

    @Test(timeout = TimeoutTestConstants.TEST_TIMEOUT)
    public void requestTimeoutDisabledInRequestObject_TakesPrecedenceOverClientConfiguration() {
        final int socketTimeout = REQUEST_TIMEOUT;
        // Client configuration is set arbitrarily low so that the request will be aborted if
        // the client configuration is incorrectly honored over the request config
        httpClient = new AmazonHttpClient(
                new LegacyClientConfiguration().withSocketTimeout(socketTimeout).withRequestTimeout(1).withMaxErrorRetry(0));

        try {
            EmptyHttpRequest request = newGetRequest();
            request.setOriginalRequest(new EmptyAmazonWebServiceRequest().withSdkRequestTimeout(0));
            ClientExecutionAndRequestTimerTestUtils.execute(httpClient, request);
            fail("Exception expected");
        } catch (AmazonClientException e) {
            assertThat(e.getCause(), instanceOf(SocketTimeoutException.class));
        }
    }

}
