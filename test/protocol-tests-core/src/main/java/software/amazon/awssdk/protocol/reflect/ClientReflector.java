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

package software.amazon.awssdk.protocol.reflect;

import java.lang.reflect.Method;
import software.amazon.awssdk.AmazonWebServiceClient;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.opensdk.protect.client.SdkSyncClientBuilder;
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
            return Class.forName(getFqcn(metadata.getSyncInterface()));
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
            if (metadata.getProtocol().equals(Protocol.API_GATEWAY)) {
                SdkSyncClientBuilder<?, ?> builder =
                        (SdkSyncClientBuilder<?, ?>) interfaceClass.getMethod("builder").invoke(null);
                builder.getClass()
                        .getMethod("iamCredentials", AWSCredentialsProvider.class)
                        .invoke(builder, new AWSStaticCredentialsProvider(getMockCredentials()));
                return builder
                        .endpoint("http://localhost:" + WireMockUtils.port())
                        .build();
            } else {
                return createAmazonServiceClient();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AmazonWebServiceClient createAmazonServiceClient() throws Exception {
        final Class<?> syncClientClass = Class.forName(getFqcn(metadata.getSyncClient()));
        AmazonWebServiceClient amazonClient = (AmazonWebServiceClient) syncClientClass
                .getConstructor(AWSCredentials.class)
                .newInstance(getMockCredentials());
        amazonClient.setEndpoint("http://localhost:" + WireMockUtils.port());
        return amazonClient;
    }

    /**
     * @return Dummy credentials to create client with.
     */
    private BasicAWSCredentials getMockCredentials() {
        return new BasicAWSCredentials("akid", "skid");
    }

    /**
     * @param simpleClassName Class name to fully qualify.
     * @return Fully qualified name of class in the client's base package.
     */
    private String getFqcn(String simpleClassName) {
        return String.format("%s.%s", metadata.getPackageName(), simpleClassName);
    }

    /**
     * @param simpleClassName Class name to fully qualify.
     * @return Fully qualified name of class in the client's model package.
     */
    private String getModelFqcn(String simpleClassName) {
        return String.format("%s.model.%s", metadata.getPackageName(), simpleClassName);
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
