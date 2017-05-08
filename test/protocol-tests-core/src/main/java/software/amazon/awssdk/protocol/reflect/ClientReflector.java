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

package software.amazon.awssdk.protocol.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.client.builder.ClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.wiremock.WireMockUtils;

/**
 * Reflection utils to create the client class and invoke operation methods.
 */
public class ClientReflector {

    private final IntermediateModel model;
    private final Metadata metadata;
    private final Object client;
    private final Class<?> interfaceClass;

    public ClientReflector(IntermediateModel model) {
        this.model = model;
        this.metadata = model.getMetadata();
        this.interfaceClass = getInterfaceClass();
        this.client = createClient();
    }

    private Class<?> getInterfaceClass() {
        try {
            return Class.forName(getClientFqcn(metadata.getSyncInterface()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call the operation method on the client with the given request.
     *
     * @param requestObject Request object to call operation with.
     * @return Unmarshalled result
     */
    public Object invokeMethod(TestCase testCase, Object requestObject) throws Exception {
        final String operationName = testCase.getWhen().getOperationName();
        Method operationMethod = getOperationMethod(operationName);
        return operationMethod.invoke(client, requestObject);
    }

    /**
     * Create the sync client to use in the tests.
     */
    private Object createClient() {
        try {
            // Reflectively create a builder, configure it, and then create the client.
            Object untypedBuilder = interfaceClass.getMethod("builder").invoke(null);
            ClientBuilder<?, ?> builder = (ClientBuilder<?, ?>) untypedBuilder;
            return builder.credentialsProvider(getMockCredentials())
                          .region("us-east-1")
                          .endpointOverride(URI.create(getEndpoint()))
                          .build();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private String getEndpoint() {
        return "http://localhost:" + WireMockUtils.port();
    }

    /**
     * @return Dummy credentials to create client with.
     */
    private AwsStaticCredentialsProvider getMockCredentials() {
        return new AwsStaticCredentialsProvider(new BasicAwsCredentials("akid", "skid"));
    }

    /**
     * @param simpleClassName Class name to fully qualify.
     * @return Fully qualified name of class in the client's base package.
     */
    private String getClientFqcn(String simpleClassName) {
        return String.format("%s.%s", metadata.getFullClientPackageName(), simpleClassName);
    }

    /**
     * @param simpleClassName Class name to fully qualify.
     * @return Fully qualified name of class in the client's model package.
     */
    private String getModelFqcn(String simpleClassName) {
        return String.format("%s.%s", metadata.getFullModelPackageName(), simpleClassName);
    }

    /**
     * @return Method object to invoke operation.
     */
    private Method getOperationMethod(String operationName) throws Exception {
        return interfaceClass.getMethod(getOperationMethodName(operationName), Class.forName(
                getModelFqcn(getOperationRequestClassName(operationName))));
    }

    /**
     * @return Name of the client method for the given operation.
     */
    private String getOperationMethodName(String operationName) {
        return model.getOperations().get(operationName).getMethodName();
    }

    /**
     * @return Name of the request class that corresponds to the given operation.
     */
    private String getOperationRequestClassName(String operationName) {
        return model.getOperations().get(operationName).getInput().getVariableType();
    }
}
