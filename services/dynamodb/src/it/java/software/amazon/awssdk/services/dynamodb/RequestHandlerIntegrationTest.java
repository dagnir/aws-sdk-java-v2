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

package software.amazon.awssdk.services.dynamodb;

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.config.ClientListenerConfiguration;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResult;
import software.amazon.awssdk.test.AwsIntegrationTestBase;
import software.amazon.awssdk.util.StringInputStream;
import software.amazon.awssdk.utils.IoUtils;

public class RequestHandlerIntegrationTest extends AwsIntegrationTestBase {

    private static DynamoDBClient ddb;

    private RequestHandler2 mockRequestHandler;

    @Before
    public void setupFixture() {
        mockRequestHandler = spy(new RequestHandler2() {
        });
        ddb = DynamoDBClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .listenerConfiguration(ClientListenerConfiguration.builder().addRequestListener(mockRequestHandler).build())
                .build();
    }

    @After
    public void tearDown() throws Exception {
        ddb.close();
    }

    @Test
    public void successfulRequest_InvokesAllSuccessCallbacks() {
        ddb.listTables(ListTablesRequest.builder_().build_());

        verify(mockRequestHandler).beforeMarshalling(any(AmazonWebServiceRequest.class));
        verify(mockRequestHandler).beforeRequest(any(Request.class));
        verify(mockRequestHandler).beforeUnmarshalling(any(Request.class), any(HttpResponse.class));
        verify(mockRequestHandler).afterResponse(any(Request.class), any(Response.class));
    }

    @Test
    public void successfulRequest_BeforeMarshalling_ReplacesOriginalRequest() {
        ListTablesRequest originalRequest = ListTablesRequest.builder_().build_();
        ListTablesRequest spiedRequest = spy(originalRequest);
        when(mockRequestHandler.beforeMarshalling(eq(originalRequest))).thenReturn(spiedRequest);

        ddb.listTables(originalRequest);

        verify(mockRequestHandler).beforeMarshalling(any(AmazonWebServiceRequest.class));
        // Asserts that the request is actually replaced with what's returned by beforeMarshalling
        verify(spiedRequest).exclusiveStartTableName();
    }

    @Test
    public void failedRequest_InvokesAllErrorCallbacks() {
        try {
            ddb.describeTable(DescribeTableRequest.builder_().tableName("some-nonexistent-table-name").build_());
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }

        // Before callbacks should always be called
        verify(mockRequestHandler).beforeMarshalling(any(AmazonWebServiceRequest.class));
        verify(mockRequestHandler).beforeRequest(any(Request.class));
        verify(mockRequestHandler).afterError(any(Request.class), any(Response.class), any(Exception.class));
    }

    /**
     * Asserts that changing the {@link HttpResponse} during the beforeUnmarshalling callback has an
     * affect on the final unmarshalled response
     */
    @Test
    public void beforeUnmarshalling_ModificationsToHttpResponse_AreReflectedInUnmarshalling() {
        final String injectedTableName = "SomeInjectedTableName";
        DynamoDBClient ddb = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        RequestHandler2 requestHandler = new RequestHandler2() {
            @Override
            public HttpResponse beforeUnmarshalling(Request<?> request, HttpResponse origHttpResponse) {
                final HttpResponse newHttpResponse = new HttpResponse(origHttpResponse.getRequest());
                // TODO we should be careful about letting customers replace the content in V2. We either need
                // to hang on to the original to ensure it's properly closed or just not let them modify it.
                IoUtils.drainInputStream(origHttpResponse.getContent());
                newHttpResponse.setStatusCode(origHttpResponse.getStatusCode());
                newHttpResponse.setStatusText(origHttpResponse.getStatusText());

                final String newContent = "{\"TableNames\":[\"" + injectedTableName + "\"]}";
                try {
                    newHttpResponse.setContent(new StringInputStream(newContent));
                } catch (UnsupportedEncodingException e) {
                    throw new UncheckedIOException(e);
                }
                // Replacing the content requires updating the checksum and content length
                newHttpResponse.addHeader("Content-Length", String.valueOf(newContent.length()));
                return newHttpResponse;
            }

            private long getCrc32Checksum(String newContent) {
                final CRC32 crc32 = new CRC32();
                crc32.update(newContent.getBytes());
                return crc32.getValue();
            }
        };
        ddb = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .listenerConfiguration(ClientListenerConfiguration.builder().addRequestListener(requestHandler).build()).build();
        ListTablesResult result = ddb.listTables(ListTablesRequest.builder_().build_());
        // Assert that the unmarshalled response contains our injected table name and not the actual
        // list of tables
        assertThat(result.tableNames().toArray(new String[0]), arrayContaining(injectedTableName));
    }

}
