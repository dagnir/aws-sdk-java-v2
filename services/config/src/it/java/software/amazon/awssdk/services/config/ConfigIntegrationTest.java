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

package software.amazon.awssdk.services.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.config.model.ConfigurationRecorder;
import software.amazon.awssdk.services.config.model.DescribeConfigurationRecordersRequest;
import software.amazon.awssdk.services.config.model.DescribeConfigurationRecordersResult;
import software.amazon.awssdk.services.config.model.PutConfigurationRecorderRequest;
import software.amazon.awssdk.services.config.model.StartConfigurationRecorderRequest;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResult;
import software.amazon.awssdk.test.AwsTestBase;

public class ConfigIntegrationTest extends AwsTestBase {

    /** Policy to describe the AWS resources. */
    private static final String POLICY_DESCRIBE_RESOURCES = "{"
                                                            + "\"Version\": \"2012-10-17\"," + "\"Statement\":" + "[" + "{"
                                                            + "\"Action\":" + "[" + "\"cloudtrail:DescribeTrails\","
                                                            + "\"ec2:Describe*\"" + "]," + "\"Effect\": \"Allow\","
                                                            + "\"Resource\": \"*\"" + "}" + "]" + "}";
    /** Name of the role being created. */
    private static final String DESCRIBE_ROLE_NAME = "java-sdk-config-describe-role-"
                                                     + System.currentTimeMillis();
    /** Reference to the config service client. */
    protected static ConfigClient configServiceClient;
    /** Name of the configuration recorded. */
    private static String recorderName = null;
    /**
     * ARN of the IAM role associated with the recorder to describe the AWS
     * resources
     */
    private static String configRecorderRoleArn = null;
    /** Reference to the IAM client. */
    private static IAMClient iam = null;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        configServiceClient =
                ConfigClient.builder().region(Region.US_EAST_1).credentialsProvider(new StaticCredentialsProvider(credentials))
                            .build();
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
        iam = IAMClient.builder().credentialsProvider(new StaticCredentialsProvider(credentials)).build();

    }

    /**
     * Tries to put a new configuration recorder if it not exists.
     */
    private void testPutConfigurationRecorderIfNotExists() {
        DescribeConfigurationRecordersResult describeConfigRecorderResult = configServiceClient
                .describeConfigurationRecorders(DescribeConfigurationRecordersRequest.builder().build());

        List<ConfigurationRecorder> configurationRecorders = describeConfigRecorderResult
                .configurationRecorders();

        if (configurationRecorders == null || configurationRecorders.isEmpty()) {
            createRoleForConfigurationRecorder();
            configServiceClient.putConfigurationRecorder(
                    PutConfigurationRecorderRequest.builder()
                                                   .configurationRecorder(ConfigurationRecorder.builder()
                                                                                               .roleARN(configRecorderRoleArn)
                                                                                               .build())
                                                   .build());
            describeConfigRecorderResult = configServiceClient
                    .describeConfigurationRecorders(DescribeConfigurationRecordersRequest.builder().build());
            configurationRecorders = describeConfigRecorderResult
                    .configurationRecorders();
        }
        recorderName = configurationRecorders.get(0).name();
    }

    /**
     * creates a new IAM role to be used for describing purposes.
     */
    private void createRoleForConfigurationRecorder() {
        CreateRoleResult createRoleResult = iam
                .createRole(CreateRoleRequest.builder().roleName(
                        DESCRIBE_ROLE_NAME).assumeRolePolicyDocument(
                        POLICY_DESCRIBE_RESOURCES).build());
        configRecorderRoleArn = createRoleResult.role().arn();
    }

    /**
     * Tries to start the recording. Currently the request fails as there is no
     * delivery channel associated with the configuration recorder. Could
     * succeed if some other SDK uses the test account to create a delivery
     * channel associated with the default recorder.
     */
    @Test
    public void testStartRecodingConfiguration() {
        testPutConfigurationRecorderIfNotExists();
        try {
            configServiceClient
                    .startConfigurationRecorder(StartConfigurationRecorderRequest.builder()
                                                                                 .configurationRecorderName(recorderName)
                                                                                 .build());
        } catch (AmazonServiceException e) {
            // Expected.
            // Delivery channel is not associated with the configuration
            // recorder. Hence it is fails to start
        }

    }

}
