package software.amazon.awssdk.services.dynamodbv2;

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.services.dynamodbv2.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ListTablesResult;
import software.amazon.awssdk.test.AWSIntegrationTestBase;
import software.amazon.awssdk.util.StringInputStream;

public class RequestHandlerIntegrationTest extends AWSIntegrationTestBase {

    private static AmazonDynamoDBClient ddb;

    private RequestHandler2 mockRequestHandler;

    @BeforeClass
    public static void setupFixture() {
        ddb = new AmazonDynamoDBClient(getCredentials());
    }

    @Before
    public void setup() {
        mockRequestHandler = spy(new RequestHandler2() {
        });
        ddb.addRequestHandler(mockRequestHandler);
    }

    @After
    public void tearDown() {
        ddb.removeRequestHandler(mockRequestHandler);
    }

    @Test
    public void successfulRequest_InvokesAllSuccessCallbacks() {
        ddb.listTables();

        verify(mockRequestHandler).beforeMarshalling(any(AmazonWebServiceRequest.class));
        verify(mockRequestHandler).beforeRequest(any(Request.class));
        verify(mockRequestHandler).beforeUnmarshalling(any(Request.class), any(HttpResponse.class));
        verify(mockRequestHandler).afterResponse(any(Request.class), any(Response.class));
    }

    @Test
    public void successfulRequest_BeforeMarshalling_ReplacesOriginalRequest() {
        ListTablesRequest originalRequest = new ListTablesRequest();
        ListTablesRequest spiedRequest = spy(originalRequest);
        when(mockRequestHandler.beforeMarshalling(eq(originalRequest))).thenReturn(spiedRequest);

        ddb.listTables(originalRequest);

        verify(mockRequestHandler).beforeMarshalling(any(AmazonWebServiceRequest.class));
        // Asserts that the request is actually replaced with what's returned by beforeMarshalling
        verify(spiedRequest).getRequestCredentialsProvider();
    }

    @Test
    public void failedRequest_InvokesAllErrorCallbacks() {
        try {
            ddb.describeTable("some-nonexistent-table-name");
        } catch (AmazonServiceException expected) {
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
        AmazonDynamoDBClient ddb = new AmazonDynamoDBClient(getCredentials());
        RequestHandler2 requestHandler = new RequestHandler2() {
            @Override
            public HttpResponse beforeUnmarshalling(Request<?> request, HttpResponse origHttpResponse) {
                final HttpResponse newHttpResponse = new HttpResponse(origHttpResponse.getRequest(),
                        origHttpResponse.getHttpRequest());
                newHttpResponse.setStatusCode(origHttpResponse.getStatusCode());
                newHttpResponse.setStatusText(origHttpResponse.getStatusText());

                final String newContent = "{\"TableNames\":[\"" + injectedTableName + "\"]}";
                try {
                    newHttpResponse.setContent(new StringInputStream(newContent));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
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
        ddb.addRequestHandler(requestHandler);
        ListTablesResult result = ddb.listTables();
        // Assert that the unmarshalled response contains our injected table name and not the actual
        // list of tables
        assertThat(result.getTableNames().toArray(new String[0]), arrayContaining(injectedTableName));
    }

}
