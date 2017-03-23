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

package software.amazon.awssdk.protocol.tests.exception;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.apigateway.protocol.ApiGatewayProtocolClient;
import software.amazon.awssdk.apigateway.protocol.model.ApiGatewayProtocolClientException;
import software.amazon.awssdk.apigateway.protocol.model.NoModeledExceptionsRequest;
import software.amazon.awssdk.apigateway.protocol.model.SameShapeDifferentStatusCodesRequest;
import software.amazon.awssdk.apigateway.protocol.model.SharedExceptionsAcrossOperationsWithDifferentStatusCodesRequest;
import software.amazon.awssdk.apigateway.protocol.model.SomeModeledException;

@RunWith(Enclosed.class)
public class ApiGatewayExceptionTests {

    private static ApiGatewayProtocolClient buildClient(WireMockRule wireMock) {
        return ApiGatewayProtocolClient.builder()
                                 .endpoint("http://localhost:" + wireMock.port())
                                 .iamCredentials(
                                         new AwsStaticCredentialsProvider(new BasicAwsCredentials("akid", "skid")))
                                 .build();
    }

    private static void stub500Response(String path, String body) {
        stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(500).withBody(body)));
    }

    private static void stub404Response(String path, String body) {
        stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(404).withBody(body)));
    }

    private static void stub413Response(String path, String body) {
        stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(413).withBody(body)));
    }

    private static void assertIsBaseException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ApiGatewayProtocolClientException e) {
            assertEquals(ApiGatewayProtocolClientException.class, e.getClass());
        }
    }

    /**
     * When an operation defines no modeled exceptions, all exceptional responses from the service
     * should be unmarshalled into the service specific base exception.
     */
    public static class NoModeledExceptionsTests {

        private static final String PATH = "/errors/noModeledExceptions";

        @Rule
        public WireMockRule wireMock = new WireMockRule(0);

        private ApiGatewayProtocolClient client;

        @Before
        public void setup() {
            client = buildClient(wireMock);
        }

        @Test
        public void unmodeled500Exception_UnmarshalledIntoBaseServiceException() {
            stub500Response(PATH, "{\"resourceName\": \"someResource\"}");
            assertIsBaseException(this::callApi);
        }

        @Test
        public void unmodeled404Exception_UnmarshalledIntoBaseServiceException() {
            stub404Response(PATH, "{\"resourceName\": \"someResource\"}");
            assertIsBaseException(this::callApi);
        }

        @Test
        public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
            stub404Response(PATH, "");
            assertIsBaseException(this::callApi);
        }

        @Test
        public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
            stub404Response(PATH, "THIS ISN'T JSON");
            assertIsBaseException(this::callApi);
        }

        private void callApi() {
            client.noModeledExceptions(new NoModeledExceptionsRequest());
        }
    }

    /**
     * API Gateway allows attaching the same model to different status codes for the same operation.
     * These tests assert that we handle that appropriately and unmarshall both into the shared
     * model class.
     */
    public static class SameShapeDifferentStatusCodesTests {

        private static final String PATH = "/errors/sameShapeDifferentStatusCodes";

        @Rule
        public WireMockRule wireMock = new WireMockRule(0);

        private ApiGatewayProtocolClient client;

        @Before
        public void setup() {
            client = buildClient(wireMock);
        }

        @Test(expected = SomeModeledException.class)
        public void modeled404Exception_UnmarshalledIntoModeledException() {
            stub404Response(PATH, "{\"resourceName\": \"someResource\"}");
            callApi();
        }

        @Test(expected = SomeModeledException.class)
        public void modeled500Exception_UnmarshalledIntoModeledException() {
            stub500Response(PATH, "{\"resourceName\": \"someResource\"}");
            callApi();
        }

        @Test
        public void unmodeled413Exception_UnmarshalledIntoBaseServiceException() {
            stub413Response(PATH, "{\"resourceName\": \"someResource\"}");
            assertIsBaseException(this::callApi);
        }

        private void callApi() {
            client.sameShapeDifferentStatusCodes(new SameShapeDifferentStatusCodesRequest());
        }


    }

    /**
     * The same exception shape might have different http status codes associated with it for
     * different operations. This test asserts that not only does the client respect http status
     * codes declared at the exception reference, it ignores any exception references to the same
     * shape not belonging to this operation.
     */
    public static class SharedExceptionsAcrossOperationsWithDifferentStatusCodesTests {

        private static final String PATH = "/errors/sharedExceptionsAcrossOperationsWithDifferentStatusCodes";

        @Rule
        public WireMockRule wireMock = new WireMockRule(0);

        private ApiGatewayProtocolClient client;

        @Before
        public void setup() {
            client = buildClient(wireMock);
        }

        @Test(expected = SomeModeledException.class)
        public void modeledException_UnmarshalledIntoBaseServiceException() {
            stub413Response(PATH, "{\"resourceName\": \"someResource\"}");
            callApi();
        }

        /**
         * Even though there is another operation that associates {@link SomeModeledException} with
         * a 404, this should not be considered here and the error response should instead be
         * unmarshalled into a base service exception.
         */
        @Test
        public void unmodeledException_UnmarshalledIntoBaseServiceException() {
            stub404Response(PATH, "{\"resourceName\": \"someResource\"}");
            assertIsBaseException(this::callApi);
        }

        private void callApi() {
            client.sharedExceptionsAcrossOperationsWithDifferentStatusCodes(
                    new SharedExceptionsAcrossOperationsWithDifferentStatusCodesRequest());
        }


    }

    /**
     * Since API Gateway errors are identified by HTTP status code we can still unmarshall into the
     * correct modeled exception regardless if the JSON body is well formed (although the data won't
     * be present in the exception fields).
     */
    public static class MalformedErrorResponseTests {

        private static final String PATH = "/errors/sameShapeDifferentStatusCodes";

        @Rule
        public WireMockRule wireMock = new WireMockRule(0);

        private ApiGatewayProtocolClient client;

        @Before
        public void setup() {
            client = buildClient(wireMock);
        }

        @Test(expected = SomeModeledException.class)
        public void emptyErrorResponse_UnmarshalledIntoModeledException() {
            stub500Response(PATH, "");
            callApi();
        }

        @Test(expected = SomeModeledException.class)
        public void malformedErrorResponse_UnmarshalledIntoModeledException() {
            stub500Response(PATH, "THIS ISN'T JSON");
            callApi();
        }

        private void callApi() {
            client.sameShapeDifferentStatusCodes(new SameShapeDifferentStatusCodesRequest());
        }
    }
}
