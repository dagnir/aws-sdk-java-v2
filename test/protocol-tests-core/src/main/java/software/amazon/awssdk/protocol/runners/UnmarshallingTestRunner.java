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

package software.amazon.awssdk.protocol.runners;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.protocol.asserts.unmarshalling.UnmarshallingTestContext;
import software.amazon.awssdk.protocol.model.GivenResponse;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.reflect.ClientReflector;

/**
 * Test runner for test cases exercising the client unmarshallers.
 */
public class UnmarshallingTestRunner {

    private final IntermediateModel model;
    private final Metadata metadata;
    private final ClientReflector clientReflector;

    public UnmarshallingTestRunner(IntermediateModel model, ClientReflector clientReflector) {
        this.model = model;
        this.metadata = model.getMetadata();
        this.clientReflector = clientReflector;
    }

    public void runTest(TestCase testCase) throws Exception {
        resetWireMock(testCase.getGiven().getResponse());
        final String operationName = testCase.getWhen().getOperationName();
        Object actualResult = clientReflector
                .invokeMethod(testCase, createRequestObject(operationName));
        testCase.getThen().getUnmarshallingAssertion()
                .assertMatches(createContext(operationName), actualResult);
    }

    /**
     * Reset wire mock and re-configure stubbing.
     */
    private void resetWireMock(GivenResponse givenResponse) {
        WireMock.reset();
        // Stub to return given response in test definition.
        stubFor(any(urlMatching(".*")).willReturn(toResponseBuilder(givenResponse)));
    }

    private ResponseDefinitionBuilder toResponseBuilder(GivenResponse givenResponse) {

        final ResponseDefinitionBuilder responseBuilder = aResponse().withStatus(200);
        if (givenResponse.getHeaders() != null) {
            givenResponse.getHeaders().forEach((k, v) -> responseBuilder.withHeader(k, v));
        }
        if (givenResponse.getStatusCode() != null) {
            responseBuilder.withStatus(givenResponse.getStatusCode());
        }
        if (givenResponse.getBody() != null) {
            responseBuilder.withBody(givenResponse.getBody());
        } else if (metadata.isXmlProtocol()) {
            // XML Unmarshallers expect at least one level in the XML document. If no body is explicitly
            // set by the test add a fake one here.
            responseBuilder.withBody("<foo></foo>");
        }
        return responseBuilder;
    }

    /**
     * @return An empty request object to call the operation method with.
     */
    private Object createRequestObject(String operationName) throws Exception {
        final String requestClassName = getModelFqcn(getOperationRequestClassName(operationName));
        return Class.forName(requestClassName).newInstance();
    }

    private UnmarshallingTestContext createContext(String operationName) {
        return new UnmarshallingTestContext().withModel(model).withOperationName(operationName);
    }

    /**
     * @param simpleClassName Class name to fully qualify.
     * @return Fully qualified name of class in the client's model package.
     */
    public String getModelFqcn(String simpleClassName) {
        return String.format("%s.model.%s", metadata.getPackageName(), simpleClassName);
    }

    /**
     * @return Name of the request class that corresponds to the given operation.
     */
    private String getOperationRequestClassName(String operationName) {
        return model.getOperations().get(operationName).getInput().getVariableType();
    }
}
