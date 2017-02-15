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

package software.amazon.awssdk.services.s3.notif;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagement;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.services.identitymanagement.model.CreateRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateRoleResult;
import software.amazon.awssdk.services.identitymanagement.model.DeleteRoleRequest;
import software.amazon.awssdk.services.lambda.AWSLambda;
import software.amazon.awssdk.services.lambda.AWSLambdaClient;
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.LambdaConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.S3Event;
import software.amazon.awssdk.test.retry.RetryableAction;
import software.amazon.awssdk.test.retry.RetryableParams;
import software.amazon.awssdk.util.IOUtils;
import software.amazon.awssdk.util.StringUtils;

/**
 * Integration tests for the Amazon S3 bucket notification with lambda configuration.
 */
public class LambdaBucketNotificationConfigurationIntegrationTest extends S3IntegrationTestBase {

    private static final String NOTIFICATION_NAME = "myLambdaConfiguration";

    private static final Log LOGGER = LogFactory.getLog(LambdaBucketNotificationConfigurationIntegrationTest.class);

    private static final String BUCKET_NAME = "bucket-notification-integ-test-" + System.currentTimeMillis();

    private static final String CLOUD_FUNCTION_NAME = "bucket-notification-cloudfunction-" + System.currentTimeMillis();

    /**
     * Simple hello world Lambda function to upload
     */
    private static final String FUNCTION_CONTENTS = "console.log(\'Loading http\')" + "\n"
                                                    + "exports.handler = function (request, response) {" + "\n" + "response.write(\"Hello, world!\");" + "\n"
                                                    + "response.end();" + "\n" + "console.log(\"Request completed\");" + "\n" + "}";

    /**
     * Assume role policy for the lambda function. Used by lambda when talking to other AWS services
     */
    private static final String LAMBDA_ASSUME_ROLE_POLICY = "{" + "\"Version\": \"2012-10-17\"," + "\"Statement\": ["
                                                            + "{" + "\"Sid\": \"\"," + "\"Effect\": \"Allow\"," + "\"Principal\": {"
                                                            + "\"Service\": [\"lambda.amazonaws.com\"]" + "}," + "\"Action\": \"sts:AssumeRole\"" + "}" + "]" + "}";

    private static final String LAMBDA_EXECUTION_ROLE_NAME = "lambda-java-sdk-test-role-" + System.currentTimeMillis();
    ;

    private static String lambdaExecutionRoleArn;

    private static String lambdaFunctionArn;

    private static AWSLambda lambda;

    private static AmazonIdentityManagement iam;

    private static File lambdaFunctionZipFile;

    /**
     * creates the s3 bucket, lambda function and adds permission to the function so S3 can invoke
     * it.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        iam = new AmazonIdentityManagementClient(credentials);
        lambda = new AWSLambdaClient(credentials);
        s3.createBucket(BUCKET_NAME);

        lambdaFunctionZipFile = createLambdaFunctionZip();
        lambdaExecutionRoleArn = createLambdaExecutionRole();
        lambdaFunctionArn = RetryableAction.doRetryableAction(
                new Callable<String>() {
                    public String call() throws Exception {
                        return createLambdaFunction();
                    }
                },
                new RetryableParams()
                        .withMaxAttempts(15)
                        .withDelayInMs(1000));

        authorizeS3ToInvokeLambdaFunction();
    }

    /**
     * Deletes the buckets, lambda function, role and the zip file.
     */
    @AfterClass
    public static void tearDown() {
        try {
            CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
            if (CLOUD_FUNCTION_NAME != null) {
                lambda.deleteFunction(new DeleteFunctionRequest().withFunctionName(CLOUD_FUNCTION_NAME));
            }
            if (lambdaExecutionRoleArn != null) {
                iam.deleteRole(new DeleteRoleRequest().withRoleName(LAMBDA_EXECUTION_ROLE_NAME));
            }
            if (lambdaFunctionZipFile != null) {
                lambdaFunctionZipFile.delete();
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error in cleaning up resources for the Bucket Notification Lambda Configuration test.", e);
            }
        }
    }

    /**
     * Generates a zip file in the temporary folder for the cloud function.
     */
    private static File createLambdaFunctionZip() throws IOException {
        File zipFile = File.createTempFile("lambda-cloud-function", ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
        out.putNextEntry(new ZipEntry("helloworld.js"));
        out.write(FUNCTION_CONTENTS.getBytes(StringUtils.UTF8));
        out.close();
        return zipFile;
    }

    private static String createLambdaFunction() throws FileNotFoundException, IOException {
        InputStream functionZip = new FileInputStream(lambdaFunctionZipFile);
        byte[] codeZip = IOUtils.toByteArray(functionZip);
        CreateFunctionRequest request = new CreateFunctionRequest().withFunctionName(CLOUD_FUNCTION_NAME)
                                                                   .withHandler("helloworld.handler").withMemorySize(128).withRuntime("nodejs")
                                                                   .withRole(lambdaExecutionRoleArn).withCode(new FunctionCode().withZipFile(ByteBuffer.wrap(codeZip)));
        return lambda.createFunction(request).getFunctionArn();
    }

    /**
     * Creates a iam role to be associated with lambda function.
     */
    private static String createLambdaExecutionRole() {
        CreateRoleResult result = iam.createRole(new CreateRoleRequest().withRoleName(LAMBDA_EXECUTION_ROLE_NAME)
                                                                        .withAssumeRolePolicyDocument(LAMBDA_ASSUME_ROLE_POLICY));
        return result.getRole().getArn();
    }

    private static void authorizeS3ToInvokeLambdaFunction() {
        lambda.addPermission(new AddPermissionRequest().withAction("lambda:InvokeFunction")
                                                       .withFunctionName(CLOUD_FUNCTION_NAME).withPrincipal("s3.amazonaws.com").withStatementId("1"));
    }

    /**
     * Sets a lambda configuration to an Amazon S3 bucket. Retrieves it and check if they are same.
     */
    @Test
    public void putLambdaBucketConfiguration_ReturnsSameConfigurationOnGet() throws Exception {
        setBucketNotificationConfiguration();
        BucketNotificationConfiguration notificationConfig = s3.getBucketNotificationConfiguration(BUCKET_NAME);
        assertEquals(1, notificationConfig.getConfigurations().size());
        assertContainsCorrectLambdaConfiguration(notificationConfig.getConfigurations());
    }

    private void setBucketNotificationConfiguration() {
        BucketNotificationConfiguration bucketNotificationConfiguration = new BucketNotificationConfiguration();
        bucketNotificationConfiguration.addConfiguration(
                NOTIFICATION_NAME,
                new LambdaConfiguration(lambdaFunctionArn, EnumSet.of(S3Event.ObjectCreatedByPut,
                                                                      S3Event.ObjectCreatedByCompleteMultipartUpload, S3Event.ObjectCreatedByCopy)));
        s3.setBucketNotificationConfiguration(BUCKET_NAME, bucketNotificationConfiguration);
    }

    private void assertContainsCorrectLambdaConfiguration(Map<String, NotificationConfiguration> notificationConfigs) {
        NotificationConfiguration notificationConfiguration = notificationConfigs.get(NOTIFICATION_NAME);
        assertThat(notificationConfiguration, instanceOf(LambdaConfiguration.class));

        LambdaConfiguration lambdaConfig = (LambdaConfiguration) notificationConfiguration;
        assertEquals(lambdaFunctionArn, lambdaConfig.getFunctionARN());

        Set<String> events = lambdaConfig.getEvents();
        assertThat(events, hasSize(3));
        assertThat(events, hasItem(S3Event.ObjectCreatedByCompleteMultipartUpload.toString()));
        assertThat(events, hasItem(S3Event.ObjectCreatedByPut.toString()));
        assertThat(events, hasItem(S3Event.ObjectCreatedByCopy.toString()));
    }
}
