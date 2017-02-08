/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.ec2;

import java.util.Date;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.CancelSpotFleetRequestsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestHistoryRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestHistoryResult;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestsResult;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RequestSpotFleetRequest;
import software.amazon.awssdk.services.ec2.model.SpotFleetLaunchSpecification;
import software.amazon.awssdk.services.ec2.model.SpotFleetRequestConfigData;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.services.identitymanagement.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.DetachRolePolicyRequest;

public class EC2SpotFleetIntegrationTest extends EC2IntegrationTestBase {

    private static AmazonIdentityManagementClient iam;

    private static String roleName = "ec2-spot-fleet-java-"
            + System.currentTimeMillis();

    private static String roleArn;
    private static String requestId;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        iam = new AmazonIdentityManagementClient(getCredentials());

        roleArn = iam.createRole(new CreateRoleRequest()
                .withAssumeRolePolicyDocument(
                        "{"
                        + "\"Version\": \"2012-10-17\","
                        + "\"Statement\": ["
                          + "{"
                            + "\"Sid\": \"\","
                            + "\"Effect\": \"Allow\","
                            + "\"Principal\": {"
                              + "\"Service\": \"spotfleet.amazonaws.com\""
                              + "},"
                            + "\"Action\": \"sts:AssumeRole\""
                          + "}"
                        + "]"
                      + "}")
                .withRoleName(roleName)).getRole().getArn();

        iam.attachRolePolicy(new AttachRolePolicyRequest()
               .withRoleName(roleName)
               .withPolicyArn("arn:aws:iam::aws:policy/AmazonEC2FullAccess"));

        System.out.println("Sleeping for 60 seconds for eventual consistency");
        Thread.sleep(60000);
    }

    @AfterClass
    public static void cleanUp() {
        try {
            ec2.cancelSpotFleetRequests(new CancelSpotFleetRequestsRequest()
                    .withSpotFleetRequestIds(requestId)
                    .withTerminateInstances(true));
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        }

        try {
            iam.detachRolePolicy(new DetachRolePolicyRequest()
                    .withRoleName(roleName)
                    .withPolicyArn("arn:aws:iam::aws:policy/AmazonEC2FullAccess"));
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        }

        try {
            iam.deleteRole(new DeleteRoleRequest().withRoleName(roleName));
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRequestSpotFleet() {
        requestId = ec2.requestSpotFleet(new RequestSpotFleetRequest()
            .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                .withSpotPrice("0.01")
                .withIamFleetRole(roleArn)
                .withTargetCapacity(1)
                .withTerminateInstancesWithExpiration(true)
                .withValidFrom(new Date())
                .withValidUntil(new Date(System.currentTimeMillis() + 60000))
                .withLaunchSpecifications(new SpotFleetLaunchSpecification()
                    .withInstanceType(InstanceType.T1Micro)
                    .withImageId("ami-0022c769"))))
                            .getSpotFleetRequestId();

        DescribeSpotFleetRequestsResult result = ec2.describeSpotFleetRequests(
            new DescribeSpotFleetRequestsRequest()
                .withSpotFleetRequestIds(requestId));

        Assert.assertNotNull(result
                .getSpotFleetRequestConfigs()
                .get(0)
                .getSpotFleetRequestConfig()
                .getValidFrom());

        Assert.assertNotNull(result
                .getSpotFleetRequestConfigs()
                .get(0)
                .getSpotFleetRequestConfig()
                .getValidUntil());

        DescribeSpotFleetRequestHistoryResult result2 =
            ec2.describeSpotFleetRequestHistory(
                new DescribeSpotFleetRequestHistoryRequest()
                    .withSpotFleetRequestId(requestId)
                    .withStartTime(new Date(System.currentTimeMillis() - 60000)));

        Assert.assertNotNull(result2.getStartTime());
        Assert.assertNotNull(result2.getLastEvaluatedTime());
    }
}
