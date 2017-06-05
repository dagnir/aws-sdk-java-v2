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

package software.amazon.awssdk.services.ec2;

import java.util.Date;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.model.CancelSpotFleetRequestsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestHistoryRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestHistoryResult;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestsResult;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RequestSpotFleetRequest;
import software.amazon.awssdk.services.ec2.model.SpotFleetLaunchSpecification;
import software.amazon.awssdk.services.ec2.model.SpotFleetRequestConfigData;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyRequest;

public class EC2SpotFleetIntegrationTest extends EC2IntegrationTestBase {

    private static IAMClient iam;

    private static String roleName = "ec2-spot-fleet-java-"
                                     + System.currentTimeMillis();

    private static String roleArn;
    private static String requestId;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        iam = IAMClient.builder().credentialsProvider(new StaticCredentialsProvider(getCredentials())).build();

        roleArn = iam.createRole(CreateRoleRequest.builder()
                                                  .assumeRolePolicyDocument(
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
                                                  .roleName(roleName).build()).role().arn();

        iam.attachRolePolicy(AttachRolePolicyRequest.builder()
                                                    .roleName(roleName)
                                                    .policyArn("arn:aws:iam::aws:policy/AmazonEC2FullAccess").build());

        System.out.println("Sleeping for 60 seconds for eventual consistency");
        Thread.sleep(60000);
    }

    @AfterClass
    public static void cleanUp() {
        try {
            ec2.cancelSpotFleetRequests(CancelSpotFleetRequestsRequest.builder()
                                                                      .spotFleetRequestIds(requestId)
                                                                      .terminateInstances(true).build());
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        }

        try {
            iam.detachRolePolicy(DetachRolePolicyRequest.builder()
                                                        .roleName(roleName)
                                                        .policyArn("arn:aws:iam::aws:policy/AmazonEC2FullAccess").build());
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        }

        try {
            iam.deleteRole(DeleteRoleRequest.builder().roleName(roleName).build());
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRequestSpotFleet() {
        requestId = ec2.requestSpotFleet(RequestSpotFleetRequest.builder()
                                             .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                                                         .spotPrice("0.01")
                                                                         .iamFleetRole(roleArn)
                                                                         .targetCapacity(1)
                                                                         .terminateInstancesWithExpiration(true)
                                                                         .validFrom(new Date())
                                                                         .validUntil(new Date(System.currentTimeMillis() + 60000))
                                                                         .launchSpecifications(
                                                                             SpotFleetLaunchSpecification.builder()
                                                                                 .instanceType(InstanceType.T1Micro)
                                                                                 .imageId("ami-0022c769")
                                                                                 .build())
                                                                         .build())
                                             .build())
            .spotFleetRequestId();

        DescribeSpotFleetRequestsResult result =
                ec2.describeSpotFleetRequests(DescribeSpotFleetRequestsRequest.builder()
                                                                              .spotFleetRequestIds(requestId)
                                                                              .build());

        Assert.assertNotNull(result.spotFleetRequestConfigs()
                                   .get(0)
                                   .spotFleetRequestConfig()
                                   .validFrom());

        Assert.assertNotNull(result.spotFleetRequestConfigs()
                                   .get(0)
                                   .spotFleetRequestConfig()
                                   .validUntil());

        DescribeSpotFleetRequestHistoryResult result2 =
                ec2.describeSpotFleetRequestHistory(
                        DescribeSpotFleetRequestHistoryRequest.builder()
                                                              .spotFleetRequestId(requestId)
                                                              .startTime(new Date(System.currentTimeMillis() - 60000)).build());

        Assert.assertNotNull(result2.startTime());
        Assert.assertNotNull(result2.lastEvaluatedTime());
    }
}
