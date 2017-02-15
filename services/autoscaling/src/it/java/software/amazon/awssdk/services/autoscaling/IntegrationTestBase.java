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

package software.amazon.awssdk.services.autoscaling;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.autoscaling.model.CreateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.sns.AmazonSNS;
import software.amazon.awssdk.services.sns.AmazonSNSClient;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for AutoScaling integration tests. Provides several convenience methods for creating
 * test data, test data values, and automatically loads the AWS credentials from a properties file
 * on disk and instantiates clients for the test subclasses to use.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public abstract class IntegrationTestBase extends AWSTestBase {

    /*
     * Test data values
     */
    protected static final String AVAILABILITY_ZONE = "us-east-1a";
    protected static final String AMI_ID = "ami-1ecae776";
    protected static final String INSTANCE_TYPE = "m1.small";
    /** Shared Autoscaling client for all tests to use */
    protected static AmazonAutoScalingClient autoscaling;
    /** Shared Autoscaling async client for tests to use */
    protected static AmazonAutoScalingAsyncClient autoscalingAsync;
    /** Shared SNS client for tests to use */
    protected static AmazonSNS sns;

    /**
     * Loads the AWS account info for the integration tests and creates an AutoScaling client for
     * tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        autoscaling = new AmazonAutoScalingClient(credentials);
        autoscalingAsync = new AmazonAutoScalingAsyncClient(credentials);
        sns = new AmazonSNSClient(credentials);
    }

    /*
     * Test Helper Methods
     */

    /**
     * Creates a launch configuration with the specified name.
     *
     * @param name
     *            The name for the new launch configuration.
     */
    protected void createLaunchConfiguration(String name) {
        CreateLaunchConfigurationRequest createRequest = new CreateLaunchConfigurationRequest()
                .withLaunchConfigurationName(name).withImageId(AMI_ID).withInstanceType(INSTANCE_TYPE);
        autoscaling.createLaunchConfiguration(createRequest);
    }

    /**
     * Creates an autoscaling group with the specified name and specified launch configuration.
     *
     * @param name
     *            The name of the autoscaling group to create.
     * @param launchConfigurationName
     *            The name of an existing launch configuration to use in the new autoscaling group.
     */
    protected void createAutoscalingGroup(String name, String launchConfigurationName) {
        CreateAutoScalingGroupRequest createRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(name).withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(AVAILABILITY_ZONE).withMaxSize(2).withMinSize(1);
        autoscaling.createAutoScalingGroup(createRequest);
    }

}
