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

package software.amazon.awssdk.services.lambda;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingResult;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResult;
import software.amazon.awssdk.services.lambda.model.DeleteEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.GetEventSourceMappingResult;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationResult;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResult;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeAsyncRequest;
import software.amazon.awssdk.services.lambda.model.InvokeAsyncResult;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResult;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResult;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.test.retry.RetryRule;
import software.amazon.awssdk.util.Base64;
import software.amazon.awssdk.util.StringUtils;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String FUNCTION_NAME = "java-sdk-helloworld-" + System.currentTimeMillis();

    @Rule
    public RetryRule retryRule = new RetryRule(10, 2000, TimeUnit.MILLISECONDS);

    @BeforeClass
    public static void setUpKinesis() {
        IntegrationTestBase.createKinesisStream();
    }

    private static void checkValid_CreateFunctionResult(CreateFunctionResult result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.codeSize());
        Assert.assertNotNull(result.description());
        Assert.assertNotNull(result.functionArn());
        Assert.assertNotNull(result.functionName());
        Assert.assertNotNull(result.handler());
        Assert.assertNotNull(result.lastModified());
        Assert.assertNotNull(result.memorySize());
        Assert.assertNotNull(result.role());
        Assert.assertNotNull(result.runtime());
        Assert.assertNotNull(result.timeout());
    }

    private static void checkValid_GetFunctionConfigurationResult(GetFunctionConfigurationResult result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.codeSize());
        Assert.assertNotNull(result.description());
        Assert.assertNotNull(result.functionArn());
        Assert.assertNotNull(result.functionName());
        Assert.assertNotNull(result.handler());
        Assert.assertNotNull(result.lastModified());
        Assert.assertNotNull(result.memorySize());
        Assert.assertNotNull(result.role());
        Assert.assertNotNull(result.runtime());
        Assert.assertNotNull(result.timeout());
    }

    private static void checkValid_GetFunctionResult(GetFunctionResult result) {
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.code());
        Assert.assertNotNull(result.code().location());
        Assert.assertNotNull(result.code().repositoryType());

        FunctionConfiguration config = result.configuration();
        checkValid_FunctionConfiguration(config);
    }

    private static void checkValid_FunctionConfiguration(FunctionConfiguration config) {

        Assert.assertNotNull(config);

        Assert.assertNotNull(config.codeSize());
        Assert.assertNotNull(config.description());
        Assert.assertNotNull(config.functionArn());
        Assert.assertNotNull(config.functionName());
        Assert.assertNotNull(config.handler());
        Assert.assertNotNull(config.lastModified());
        Assert.assertNotNull(config.memorySize());
        Assert.assertNotNull(config.role());
        Assert.assertNotNull(config.runtime());
        Assert.assertNotNull(config.timeout());
    }

    private static void checkValid_CreateEventSourceMappingResult(CreateEventSourceMappingResult result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.batchSize());
        Assert.assertNotNull(result.eventSourceArn());
        Assert.assertNotNull(result.functionArn());
        Assert.assertNotNull(result.lastModified());
        Assert.assertNotNull(result.lastProcessingResult());
        Assert.assertNotNull(result.state());
        Assert.assertNotNull(result.stateTransitionReason());
        Assert.assertNotNull(result.uUID());
    }

    @Before
    public void uploadFunction() throws IOException {
        // Upload function
        byte[] functionBits;
        InputStream functionZip = new FileInputStream(cloudFuncZip);
        try {
            functionBits = read(functionZip);
        } finally {
            functionZip.close();
        }

        CreateFunctionResult result = lambda.createFunction(CreateFunctionRequest.builder_()
                .description("My cloud function")
                .functionName(FUNCTION_NAME)
                .code(FunctionCode.builder_().zipFile(ByteBuffer.wrap(functionBits)).build_())
                .handler("helloworld.handler")
                .memorySize(128)
                .runtime(Runtime.Nodejs43)
                .timeout(10)
                .role(lambdaServiceRoleArn).build_()).join();

        checkValid_CreateFunctionResult(result);
    }

    @After
    public void deleteFunction() {
        lambda.deleteFunction(DeleteFunctionRequest.builder_().functionName(FUNCTION_NAME).build_());
    }

    @Test
    public void testFunctionOperations() throws IOException {

        // Get function
        GetFunctionResult getFunc = lambda.getFunction(GetFunctionRequest.builder_().functionName(FUNCTION_NAME).build_()).join();
        checkValid_GetFunctionResult(getFunc);

        // Get function configuration
        GetFunctionConfigurationResult getConfig = lambda
                .getFunctionConfiguration(GetFunctionConfigurationRequest.builder_().functionName(FUNCTION_NAME).build_()).join();
        checkValid_GetFunctionConfigurationResult(getConfig);

        // List functions
        ListFunctionsResult listFunc = lambda.listFunctions(ListFunctionsRequest.builder_().build_()).join();
        Assert.assertFalse(listFunc.functions().isEmpty());
        for (FunctionConfiguration funcConfig : listFunc.functions()) {
            checkValid_FunctionConfiguration(funcConfig);
        }

        // Invoke the function
        InvokeAsyncResult invokeAsyncResult = lambda.invokeAsync(InvokeAsyncRequest.builder_()
                .functionName(FUNCTION_NAME)
                .invokeArgs(new ByteArrayInputStream("{}".getBytes())).build_()).join();

        Assert.assertEquals(202, invokeAsyncResult.status().intValue());

        InvokeResult invokeResult = lambda.invoke(InvokeRequest.builder_()
                .functionName(FUNCTION_NAME)
                .invocationType(InvocationType.Event)
                .payload(ByteBuffer.wrap("{}".getBytes())).build_()).join();

        Assert.assertEquals(202, invokeResult.statusCode().intValue());
        Assert.assertNull(invokeResult.logResult());
        Assert.assertEquals(0, invokeResult.payload().remaining());

        invokeResult = lambda.invoke(InvokeRequest.builder_()
                .functionName(FUNCTION_NAME)
                .invocationType(InvocationType.RequestResponse)
                .logType(LogType.Tail)
                .payload(ByteBuffer.wrap("{}".getBytes())).build_()).join();

        Assert.assertEquals(200, invokeResult.statusCode().intValue());

        System.out.println(new String(Base64.decode(invokeResult.logResult()), StringUtils.UTF8));

        Assert.assertEquals("\"Hello World\"", StringUtils.UTF8.decode(invokeResult.payload()).toString());
    }

    @Test
    public void testEventSourceOperations() {

        // AddEventSourceResult
        CreateEventSourceMappingResult addResult = lambda
                .createEventSourceMapping(CreateEventSourceMappingRequest.builder_().functionName(FUNCTION_NAME)
                        .eventSourceArn(streamArn).startingPosition("TRIM_HORIZON").batchSize(100).build_()).join();
        checkValid_CreateEventSourceMappingResult(addResult);

        String eventSourceUUID = addResult.uUID();

        // GetEventSource
        GetEventSourceMappingResult getResult = lambda.getEventSourceMapping(GetEventSourceMappingRequest
                .builder_()
                .uUID(eventSourceUUID).build_()).join();

        // RemoveEventSource
        lambda.deleteEventSourceMapping(DeleteEventSourceMappingRequest.builder_().uUID(eventSourceUUID).build_());
    }

}
