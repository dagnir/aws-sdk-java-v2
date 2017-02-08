/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.protocol.restjson.AmazonProtocolRestJsonClient;
import software.amazon.awssdk.services.protocol.restjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocol.restjson.model.AmazonProtocolRestJsonException;
import software.amazon.awssdk.services.protocol.restjson.model.EmptyModeledException;
import software.amazon.awssdk.services.protocol.restjson.model.HeadOperationRequest;
import software.amazon.awssdk.services.protocol.restjson.model.MultiLocationOperationRequest;

/**
 * Exception related tests for AWS REST JSON.
 */
public class RestJsonExceptionTests {

    private static final String ALL_TYPES_PATH = "/2016-03-11/allTypes";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private final AmazonProtocolRestJsonClient client = new AmazonProtocolRestJsonClient(
            new BasicAWSCredentials("akid", "skid"));

    @Before
    public void setup() {
        BasicConfigurator.configure();
        client.setEndpoint("http://localhost:" + wireMock.port());
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "{\"__type\": \"SomeUnknownType\"}");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(ALL_TYPES_PATH, "{\"__type\": \"EmptyModeledException\"}");
        assertThrowsException(this::callAllTypes, EmptyModeledException.class);
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "THIS ISN'T JSON");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledExceptionInHeadRequest_UnmarshalledIntoModeledException() {
        stubFor(head(urlEqualTo("/2016-03-11/headOperation"))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withHeader("x-amzn-ErrorType", "EmptyModeledException")));
        assertThrowsException(() -> client.headOperation(new HeadOperationRequest()), EmptyModeledException.class);
    }

    @Test
    public void unmodeledExceptionInHeadRequest_UnmarshalledIntoModeledException() {
        stubFor(head(urlEqualTo("/2016-03-11/headOperation"))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withHeader("x-amzn-ErrorType", "SomeUnknownType")));
        assertThrowsServiceBaseException(() -> client.headOperation(new HeadOperationRequest()));
    }

    @Test
    public void nullPathParam_ThrowsIllegalArgumentException() {
        assertThrowsIllegalArgumentException(() -> client.multiLocationOperation(new MultiLocationOperationRequest()));
    }

    @Test
    public void emptyPathParam_ThrowsIllegalArgumentException() {
        assertThrowsIllegalArgumentException(() -> client.multiLocationOperation(
                new MultiLocationOperationRequest().withPathParam("")));
    }


    private void callAllTypes() {
        client.allTypes(new AllTypesRequest());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        assertThrowsException(runnable, AmazonProtocolRestJsonException.class);
    }

    private void assertThrowsIllegalArgumentException(Runnable runnable) {
        assertThrowsException(runnable, IllegalArgumentException.class);
    }

    private void assertThrowsException(Runnable runnable, Class<? extends Exception> expectedException) {
        try {
            runnable.run();
        } catch (Exception e) {
            assertEquals(expectedException, e.getClass());
        }
    }
}
