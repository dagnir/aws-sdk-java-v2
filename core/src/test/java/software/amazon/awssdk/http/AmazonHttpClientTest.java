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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.handlers.HandlerContextKey;
import software.amazon.awssdk.internal.http.apache.client.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.internal.http.apache.request.impl.ApacheHttpRequestFactory;
import software.amazon.awssdk.internal.http.request.HttpRequestFactory;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;

public class AmazonHttpClientTest {

    private final String serverName = "testsvc";
    private final String uriName = "http://testsvc.region.amazonaws.com";

    private ConnectionManagerAwareHttpClient httpClient;
    private AmazonHttpClient client;

    @Before
    public void setUp() {
        LegacyClientConfiguration config = new LegacyClientConfiguration();

        httpClient = EasyMock.createMock(ConnectionManagerAwareHttpClient.class);
        EasyMock.replay(httpClient);

        client = new AmazonHttpClient(config, httpClient, null);
    }

    @Test
    public void testRetryIoExceptionFromExecute() throws IOException {
        IOException exception = new IOException("BOOM");

        EasyMock.reset(httpClient);

        EasyMock
                .expect(httpClient.getConnectionManager())
                .andReturn(null)
                .anyTimes();

        EasyMock
                .expect(httpClient.execute(EasyMock.<HttpUriRequest>anyObject(),
                                           EasyMock.<HttpContext>anyObject()))
                .andThrow(exception)
                .times(4);

        EasyMock.replay(httpClient);

        ExecutionContext context = new ExecutionContext();

        Request<?> request = new DefaultRequest<Object>("testsvc");
        request.setEndpoint(java.net.URI.create(
                "http://testsvc.region.amazonaws.com"));
        request.setContent(new ByteArrayInputStream(new byte[0]));

        try {

            client.requestExecutionBuilder().request(request).executionContext(context).execute();
            Assert.fail("No exception when request repeatedly fails!");

        } catch (AmazonClientException e) {
            Assert.assertSame(exception, e.getCause());
        }

        // Verify that we called execute 4 times.
        EasyMock.verify(httpClient);
    }

    @Test
    public void testRetryIoExceptionFromHandler() throws Exception {
        final IOException exception = new IOException("BOOM");

        HttpResponseHandler<AmazonWebServiceResponse<Object>> handler =
                EasyMock.createMock(HttpResponseHandler.class);

        EasyMock
                .expect(handler.needsConnectionLeftOpen())
                .andReturn(false)
                .anyTimes();

        EasyMock
                .expect(handler.handle(EasyMock.<HttpResponse>anyObject()))
                .andThrow(exception)
                .times(4);

        EasyMock.replay(handler);

        BasicHttpResponse response = createBasicHttpResponse();

        EasyMock.reset(httpClient);

        EasyMock
                .expect(httpClient.getConnectionManager())
                .andReturn(null)
                .anyTimes();

        EasyMock
                .expect(httpClient.execute(EasyMock.<HttpUriRequest>anyObject(),
                                           EasyMock.<HttpContext>anyObject()))
                .andReturn(response)
                .times(4);

        EasyMock.replay(httpClient);

        ExecutionContext context = new ExecutionContext();

        Request<?> request = new DefaultRequest<Object>(null, "testsvc");
        request.setEndpoint(java.net.URI.create(
                "http://testsvc.region.amazonaws.com"));
        request.setContent(new java.io.ByteArrayInputStream(new byte[0]));

        try {

            client.requestExecutionBuilder().request(request).executionContext(context).execute(handler);
            Assert.fail("No exception when request repeatedly fails!");

        } catch (AmazonClientException e) {
            Assert.assertSame(exception, e.getCause());
        }

        // Verify that we called execute 4 times.
        EasyMock.verify(httpClient);
    }

    @Test
    public void testUseExpectContinueTrue() throws IOException {
        Request<?> request = mockRequest(serverName, HttpMethodName.PUT, uriName, true);
        LegacyClientConfiguration clientConfiguration = new LegacyClientConfiguration().withUseExpectContinue(true);

        HttpRequestFactory<HttpRequestBase> httpRequestFactory = new ApacheHttpRequestFactory();
        HttpRequestBase httpRequest = httpRequestFactory.create(request, HttpClientSettings.adapt(clientConfiguration));

        Assert.assertNotNull(httpRequest);
        Assert.assertTrue(httpRequest.getConfig().isExpectContinueEnabled());

    }

    @Test
    public void testUseExpectContinueFalse() throws IOException {
        Request<?> request = mockRequest(serverName, HttpMethodName.PUT, uriName, true);
        LegacyClientConfiguration clientConfiguration = new LegacyClientConfiguration().withUseExpectContinue(false);

        HttpRequestFactory<HttpRequestBase> httpRequestFactory = new ApacheHttpRequestFactory();
        HttpRequestBase httpRequest = httpRequestFactory.create(request, HttpClientSettings.adapt(clientConfiguration));

        Assert.assertNotNull(httpRequest);
        Assert.assertFalse(httpRequest.getConfig().isExpectContinueEnabled());
    }

    @Test
    public void testPutRetryNoCl() throws Exception {
        Request<?> request = mockRequest(serverName, HttpMethodName.PUT, uriName, false);
        testRetries(request, 100);
    }

    @Test
    public void testPostRetryNoCl() throws Exception {
        Request<?> request = mockRequest(serverName, HttpMethodName.POST, uriName, false);
        testRetries(request, 100);
    }

    @Test
    public void testPutRetryCl() throws Exception {
        Request<?> request = mockRequest(serverName, HttpMethodName.PUT, uriName, true);
        testRetries(request, 100);
    }

    @Test
    public void testPostRetryCl() throws Exception {
        Request<?> request = mockRequest(serverName, HttpMethodName.POST, uriName, true);
        testRetries(request, 100);
    }

    @Test
    public void testUserAgentPrefixAndSuffixAreAdded() throws Exception {
        String prefix = "somePrefix";
        String suffix = "someSuffix";
        Request<?> request = mockRequest(serverName, HttpMethodName.PUT, uriName, true);

        HttpResponseHandler<AmazonWebServiceResponse<Object>> handler = createStubResponseHandler();
        EasyMock.replay(handler);
        LegacyClientConfiguration config =
                new LegacyClientConfiguration().withUserAgentPrefix(prefix).withUserAgentSuffix(suffix);

        Capture<HttpRequestBase> capturedRequest = new Capture<HttpRequestBase>();

        EasyMock.reset(httpClient);
        EasyMock
                .expect(httpClient.execute(
                        EasyMock.capture(capturedRequest), EasyMock.<HttpContext>anyObject()))
                .andReturn(createBasicHttpResponse())
                .once();
        EasyMock.replay(httpClient);

        AmazonHttpClient client = new AmazonHttpClient(config, httpClient, null);

        client.requestExecutionBuilder().request(request).execute(handler);

        String userAgent = capturedRequest.getValue().getFirstHeader("User-Agent").getValue();
        Assert.assertTrue(userAgent.startsWith(prefix));
        Assert.assertTrue(userAgent.endsWith(suffix));
    }

    @Test
    public void testCredentialsSetInRequestContext() throws Exception {
        EasyMock.reset(httpClient);
        EasyMock
                .expect(httpClient.execute(EasyMock.<HttpRequestBase>anyObject(), EasyMock.<HttpContext>anyObject()))
                .andReturn(createBasicHttpResponse())
                .once();
        EasyMock.replay(httpClient);

        AmazonHttpClient client = new AmazonHttpClient(new LegacyClientConfiguration(), httpClient, null);

        final BasicAwsCredentials credentials = new BasicAwsCredentials("foo", "bar");

        AwsCredentialsProvider credentialsProvider = EasyMock.createMock(AwsCredentialsProvider.class);
        EasyMock.expect(credentialsProvider.getCredentials())
                .andReturn(credentials)
                .anyTimes();
        EasyMock.replay(credentialsProvider);

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setCredentialsProvider(credentialsProvider);

        Request<?> request = mockRequest(serverName, HttpMethodName.PUT, uriName, true);

        HttpResponseHandler<AmazonWebServiceResponse<Object>> handler = createStubResponseHandler();
        EasyMock.replay(handler);

        client.execute(request, handler, null, executionContext);

        assertEquals(credentials, request.getHandlerContext(HandlerContextKey.AWS_CREDENTIALS));
    }

    private BasicHttpResponse createBasicHttpResponse() {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[0]));

        BasicHttpResponse response = new BasicHttpResponse(
                new ProtocolVersion("http", 1, 1),
                200,
                "OK");
        response.setEntity(entity);
        return response;
    }

    private HttpResponseHandler<AmazonWebServiceResponse<Object>> createStubResponseHandler() throws Exception {
        HttpResponseHandler<AmazonWebServiceResponse<Object>> handler =
                EasyMock.createMock(HttpResponseHandler.class);
        AmazonWebServiceResponse<Object> response = new AmazonWebServiceResponse<Object>();
        EasyMock
                .expect(handler.needsConnectionLeftOpen())
                .andReturn(false)
                .anyTimes();

        EasyMock
                .expect(handler.handle(EasyMock.<HttpResponse>anyObject()))
                .andReturn(response)
                .anyTimes();
        return handler;
    }

    private void testRetries(Request<?> request, int contentLength)
            throws IOException {

        ExecutionContext context = new ExecutionContext();

        mockFailure(contentLength);

        try {
            client.requestExecutionBuilder().request(request).executionContext(context).execute();
            Assert.fail("Expected AmazonClientException");
        } catch (AmazonClientException e) {
            // Expected.
        }
    }

    private void mockFailure(final int contentLength) throws IOException {

        EasyMock.reset(httpClient);

        EasyMock
                .expect(httpClient.getConnectionManager())
                .andReturn(null)
                .anyTimes();

        for (int i = 0; i < 4; ++i) {
            EasyMock
                    .expect(httpClient.execute(
                            EasyMock.<HttpUriRequest>anyObject(),
                            EasyMock.<HttpContext>anyObject()))
                    .andAnswer(new IAnswer<org.apache.http.HttpResponse>() {

                        @Override
                        public org.apache.http.HttpResponse answer()
                                throws Throwable {

                            HttpEntityEnclosingRequestBase request =
                                    (HttpEntityEnclosingRequestBase)
                                            EasyMock.getCurrentArguments()[0];

                            InputStream stream = request.getEntity().getContent();
                            int len = 0;
                            while (true) {
                                int b = stream.read(new byte[1024]);
                                if (b == -1) {
                                    break;
                                }
                                len += b;
                            }

                            assertEquals(contentLength, len);

                            throw new IOException("BOOM");
                        }
                    });
        }

        EasyMock.replay(httpClient);
    }

    private Request<?> mockRequest(String serverName, HttpMethodName methodName, String uri, boolean hasCL) {
        Request<?> request = new DefaultRequest<Object>(null, serverName);
        request.setHttpMethod(methodName);
        request.setContent(new ByteArrayInputStream(new byte[100]));
        request.setEndpoint(URI.create(uri));
        if (hasCL) {
            request.addHeader("Content-Length", "100");
        }

        return request;
    }
}
