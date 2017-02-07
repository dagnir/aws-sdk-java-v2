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

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.reflect.ClientReflector;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.protocol.wiremock.WireMockUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;

/**
 * Test runner for test cases exercising the client marshallers.
 */
public class MarshallingTestRunner {

    private final IntermediateModel model;
    private final ClientReflector clientReflector;

    public MarshallingTestRunner(IntermediateModel model, ClientReflector clientReflector) {
        this.model = model;
        this.clientReflector = clientReflector;
    }

    public void runTest(TestCase testCase) throws Exception {
        resetWireMock();
        clientReflector.invokeMethod(testCase, createRequestObject(testCase));
        LoggedRequest actualRequest = getLoggedRequest();
        testCase.getThen().getMarshallingAssertion().assertMatches(actualRequest);
    }

    /**
     * @return LoggedRequest that wire mock captured.
     */
    public static LoggedRequest getLoggedRequest() {
        List<LoggedRequest> requests = WireMockUtils.findAllLoggedRequests();
        assertEquals(1, requests.size());
        return requests.get(0);
    }

    /**
     * Reset wire mock and re-configure stubbing.
     */
    private void resetWireMock() {
        WireMock.reset();
        // Stub to return 200 for all requests
        final ResponseDefinitionBuilder responseDefBuilder = aResponse().withStatus(200);
        // XML Unmarshallers expect at least one level in the XML document.
        if (model.getMetadata().isXmlProtocol()) {
            responseDefBuilder.withBody("<foo></foo>");
        }
        stubFor(any(urlMatching(".*")).willReturn(responseDefBuilder));
    }

    /**
     * Create a request object initialized with data from the Given declaration of the test case.
     */
    private Object createRequestObject(TestCase testCase) {
        final String operationName = testCase.getWhen().getOperationName();
        final String requestClassName = getOperationRequestClassName(operationName);
        final JsonNode input = testCase.getGiven().getInput();
        return new ShapeModelReflector(model, requestClassName, input).createShapeObject();
    }

    /**
     * @return Name of the request class that corresponds to the given operation.
     */
    private String getOperationRequestClassName(String operationName) {
        return model.getOperations().get(operationName).getInput().getVariableType();
    }

}
