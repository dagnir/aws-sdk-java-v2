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

        Assert.assertNotNull(result.getCodeSize());
        Assert.assertNotNull(result.getDescription());
        Assert.assertNotNull(result.getFunctionArn());
        Assert.assertNotNull(result.getFunctionName());
        Assert.assertNotNull(result.getHandler());
        Assert.assertNotNull(result.getLastModified());
        Assert.assertNotNull(result.getMemorySize());
        Assert.assertNotNull(result.getRole());
        Assert.assertNotNull(result.getRuntime());
        Assert.assertNotNull(result.getTimeout());
    }

    private static void checkValid_GetFunctionConfigurationResult(GetFunctionConfigurationResult result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getCodeSize());
        Assert.assertNotNull(result.getDescription());
        Assert.assertNotNull(result.getFunctionArn());
        Assert.assertNotNull(result.getFunctionName());
        Assert.assertNotNull(result.getHandler());
        Assert.assertNotNull(result.getLastModified());
        Assert.assertNotNull(result.getMemorySize());
        Assert.assertNotNull(result.getRole());
        Assert.assertNotNull(result.getRuntime());
        Assert.assertNotNull(result.getTimeout());
    }

    private static void checkValid_GetFunctionResult(GetFunctionResult result) {
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getCode());
        Assert.assertNotNull(result.getCode().getLocation());
        Assert.assertNotNull(result.getCode().getRepositoryType());

        FunctionConfiguration config = result.getConfiguration();
        checkValid_FunctionConfiguration(config);
    }

    private static void checkValid_FunctionConfiguration(FunctionConfiguration config) {

        Assert.assertNotNull(config);

        Assert.assertNotNull(config.getCodeSize());
        Assert.assertNotNull(config.getDescription());
        Assert.assertNotNull(config.getFunctionArn());
        Assert.assertNotNull(config.getFunctionName());
        Assert.assertNotNull(config.getHandler());
        Assert.assertNotNull(config.getLastModified());
        Assert.assertNotNull(config.getMemorySize());
        Assert.assertNotNull(config.getRole());
        Assert.assertNotNull(config.getRuntime());
        Assert.assertNotNull(config.getTimeout());
    }

    private static void checkValid_CreateEventSourceMappingResult(CreateEventSourceMappingResult result) {

        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getBatchSize());
        Assert.assertNotNull(result.getEventSourceArn());
        Assert.assertNotNull(result.getFunctionArn());
        Assert.assertNotNull(result.getLastModified());
        Assert.assertNotNull(result.getLastProcessingResult());
        Assert.assertNotNull(result.getState());
        Assert.assertNotNull(result.getStateTransitionReason());
        Assert.assertNotNull(result.getUUID());
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

        CreateFunctionResult result = lambda.createFunction(new CreateFunctionRequest()
                .withDescription("My cloud function").withFunctionName(FUNCTION_NAME)
                .withCode(new FunctionCode().withZipFile(ByteBuffer.wrap(functionBits)))
                .withHandler("helloworld.handler").withMemorySize(128).withRuntime(Runtime.Nodejs43).withTimeout(10)
                .withRole(lambdaServiceRoleArn)).join();

        checkValid_CreateFunctionResult(result);
    }

    @After
    public void deleteFunction() {
        lambda.deleteFunction(new DeleteFunctionRequest().withFunctionName(FUNCTION_NAME));
    }

    @Test
    public void testFunctionOperations() throws IOException {

        // Get function
        GetFunctionResult getFunc = lambda.getFunction(new GetFunctionRequest().withFunctionName(FUNCTION_NAME)).join();
        checkValid_GetFunctionResult(getFunc);

        // Get function configuration
        GetFunctionConfigurationResult getConfig = lambda
                .getFunctionConfiguration(new GetFunctionConfigurationRequest().withFunctionName(FUNCTION_NAME)).join();
        checkValid_GetFunctionConfigurationResult(getConfig);

        // List functions
        ListFunctionsResult listFunc = lambda.listFunctions(new ListFunctionsRequest()).join();
        Assert.assertFalse(listFunc.getFunctions().isEmpty());
        for (FunctionConfiguration funcConfig : listFunc.getFunctions()) {
            checkValid_FunctionConfiguration(funcConfig);
        }

        // Invoke the function
        InvokeAsyncResult invokeAsyncResult = lambda.invokeAsync(new InvokeAsyncRequest().withFunctionName(
                FUNCTION_NAME).withInvokeArgs(new ByteArrayInputStream("{}".getBytes()))).join();

        Assert.assertEquals(202, invokeAsyncResult.getStatus().intValue());

        InvokeResult invokeResult = lambda.invoke(new InvokeRequest().withFunctionName(FUNCTION_NAME)
                .withInvocationType(InvocationType.Event).withPayload(ByteBuffer.wrap("{}".getBytes()))).join();

        Assert.assertEquals(202, invokeResult.getStatusCode().intValue());
        Assert.assertNull(invokeResult.getLogResult());
        Assert.assertEquals(0, invokeResult.getPayload().remaining());

        invokeResult = lambda.invoke(new InvokeRequest().withFunctionName(FUNCTION_NAME)
                .withInvocationType(InvocationType.RequestResponse).withLogType(LogType.Tail)
                .withPayload(ByteBuffer.wrap("{}".getBytes()))).join();

        Assert.assertEquals(200, invokeResult.getStatusCode().intValue());

        System.out.println(new String(Base64.decode(invokeResult.getLogResult()), StringUtils.UTF8));

        Assert.assertEquals("\"Hello World\"", StringUtils.UTF8.decode(invokeResult.getPayload()).toString());
    }

    @Test
    public void testEventSourceOperations() {

        // AddEventSourceResult
        CreateEventSourceMappingResult addResult = lambda
                .createEventSourceMapping(new CreateEventSourceMappingRequest().withFunctionName(FUNCTION_NAME)
                        .withEventSourceArn(streamArn).withStartingPosition("TRIM_HORIZON").withBatchSize(100)).join();
        checkValid_CreateEventSourceMappingResult(addResult);

        String eventSourceUUID = addResult.getUUID();

        // GetEventSource
        GetEventSourceMappingResult getResult = lambda.getEventSourceMapping(new GetEventSourceMappingRequest()
                .withUUID(eventSourceUUID)).join();

        // RemoveEventSource
        lambda.deleteEventSourceMapping(new DeleteEventSourceMappingRequest().withUUID(eventSourceUUID));
    }

}
