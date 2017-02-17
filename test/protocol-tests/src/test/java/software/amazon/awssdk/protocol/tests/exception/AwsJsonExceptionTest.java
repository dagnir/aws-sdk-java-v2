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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.services.protocol.jsonrpc.AmazonProtocolJsonRpcClient;
import software.amazon.awssdk.services.protocol.jsonrpc.model.AllTypesRequest;
import software.amazon.awssdk.services.protocol.jsonrpc.model.AmazonProtocolJsonRpcException;
import software.amazon.awssdk.services.protocol.jsonrpc.model.EmptyModeledException;

/**
 * Exception related tests for AWS/JSON RPC.
 */
public class AwsJsonExceptionTest {
    private static final String PATH = "/";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private final AmazonProtocolJsonRpcClient client = new AmazonProtocolJsonRpcClient(
            new BasicAwsCredentials("akid", "skid"));

    @Before
    public void setup() {
        client.setEndpoint("http://localhost:" + wireMock.port());
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "{\"__type\": \"SomeUnknownType\"}");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(PATH, "{\"__type\": \"EmptyModeledException\"}");
        try {
            callAllTypes();
        } catch (EmptyModeledException e) {
            assertThat(e, instanceOf(AmazonProtocolJsonRpcException.class));
        }
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "THIS ISN'T JSON");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    private void callAllTypes() {
        client.allTypes(new AllTypesRequest());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        try {
            runnable.run();
        } catch (AmazonProtocolJsonRpcException e) {
            assertEquals(AmazonProtocolJsonRpcException.class, e.getClass());
        }
    }
}
