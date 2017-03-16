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

package software.amazon.awssdk.services.opsworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.CreateInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.opsworks.model.CreateInstanceRequest;
import software.amazon.awssdk.services.opsworks.model.CreateLayerRequest;
import software.amazon.awssdk.services.opsworks.model.CreateStackRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteInstanceRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteLayerRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteStackRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeStackSummaryRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeStackSummaryResult;
import software.amazon.awssdk.services.opsworks.model.DescribeStacksRequest;
import software.amazon.awssdk.services.opsworks.model.Instance;
import software.amazon.awssdk.services.opsworks.model.Stack;
import software.amazon.awssdk.services.opsworks.model.StackConfigurationManager;

public class VPCIntegrationTest extends IntegrationTestBase {

    private static String TRUST_POLICY = "{"
                                         + "\"Version\": \"2008-10-17\","
                                         + "\"Statement\": ["
                                         + "{"
                                         + "\"Sid\": \"dotnettestProd\","
                                         + "\"Effect\": \"Allow\","
                                         + "\"Principal\": {"
                                         + "\"Service\": \"opsworks.amazonaws.com\""
                                         + "},"
                                         + "\"Action\": \"sts:AssumeRole\""
                                         + "},"
                                         + "{"
                                         + "\"Sid\": \"dotnettestAlpha\","
                                         + "\"Effect\": \"Allow\","
                                         + "\"Principal\": {"
                                         + "\"AWS\": \"arn:aws:iam::225123898355:root\""
                                         + "},"
                                         + "\"Action\": \"sts:AssumeRole\""
                                         + "}"
                                         + "]"
                                         + "}";


    private static String PERMISSIONS = "{\"Statement\": [{\"Action\": [\"ec2:*\", \"iam:PassRole\","
                                        + "\"cloudwatch:GetMetricStatistics\"],"
                                        + "\"Effect\": \"Allow\","
                                        + "\"Resource\": [\"*\"] }]}";

    private static String stackName = "java-sdk--test-stack-name" + System.currentTimeMillis();

    private String vpcId;

    private String subId;

    private Role role;

    private EC2Client ec2;

    private IAMClient iam;

    private String profileName;

    private String roleName;

    private InstanceProfile instanceProfile;

    @Test
    public void testVpc() throws InterruptedException {
        initialize();

        Thread.sleep(1000 * 30);

        try {
            String stackId = opsWorks.createStack(new CreateStackRequest()
                                                          .withName(stackName)
                                                          .withRegion("us-east-1")
                                                          .withVpcId(vpcId)
                                                          .withDefaultSubnetId(subId)
                                                          .withServiceRoleArn(role.getArn())
                                                          .withDefaultInstanceProfileArn(instanceProfile.getArn())
                                                          .withConfigurationManager(new StackConfigurationManager().withName("Chef").withVersion("0.9"))
                                                 ).getStackId();


            Stack stack = opsWorks.describeStacks(new DescribeStacksRequest().withStackIds(stackId)).getStacks().get(0);

            assertEquals(vpcId, stack.getVpcId());
            assertEquals(subId, stack.getDefaultSubnetId());


            String layerId = opsWorks.createLayer(new CreateLayerRequest()
                                                          .withName("foo")
                                                          .withShortname("fo")
                                                          .withStackId(stackId)
                                                          .withType("custom")).getLayerId();


            String instanceId = opsWorks.createInstance(new CreateInstanceRequest()
                                                                .withStackId(stackId)
                                                                .withLayerIds(layerId)
                                                                .withSubnetId(subId)
                                                                .withInstanceType("m1.small")
                                                       ).getInstanceId();


            Instance instance = opsWorks.describeInstances(new DescribeInstancesRequest()
                                                                   .withInstanceIds(instanceId)).getInstances().get(0);

            assertEquals(subId, instance.getSubnetId());

            DescribeStackSummaryResult describeStackSummaryResult =
                    opsWorks.describeStackSummary(new DescribeStackSummaryRequest().withStackId(stackId));

            assertEquals(stackId, describeStackSummaryResult.getStackSummary().getStackId());
            assertEquals(stackName, describeStackSummaryResult.getStackSummary().getName());
            assertEquals(new Integer(1), describeStackSummaryResult.getStackSummary().getLayersCount());
            assertNotNull(describeStackSummaryResult.getStackSummary().getInstancesCount());
            assertEquals(new Integer(0), describeStackSummaryResult.getStackSummary().getAppsCount());

            opsWorks.deleteInstance(new DeleteInstanceRequest().withInstanceId(instanceId));

            opsWorks.deleteLayer(new DeleteLayerRequest().withLayerId(layerId));
            opsWorks.deleteStack(new DeleteStackRequest().withStackId(stackId));
        } finally {
            try {
                ec2.deleteSubnet(new DeleteSubnetRequest().withSubnetId(subId));

                ec2.deleteVpc(new DeleteVpcRequest().withVpcId(vpcId));

                iam.deleteInstanceProfile(new DeleteInstanceProfileRequest().withInstanceProfileName(profileName));
                iam.deleteRolePolicy(new DeleteRolePolicyRequest().withRoleName(roleName).withPolicyName("TestPolicy"));
                iam.deleteRole(new DeleteRoleRequest().withRoleName(roleName));
            } catch (Exception e) {
                // Ignored or expected.
            }

        }
    }

    private void initialize() throws InterruptedException {
        iam = IAMClient.builder().withCredentials(CREDENTIALS_PROVIDER_CHAIN).build();
        ec2 = EC2Client.builder().withCredentials(CREDENTIALS_PROVIDER_CHAIN).build();
        roleName = "java-test-role" + System.currentTimeMillis();
        profileName = "java-profile" + System.currentTimeMillis();
        role = iam.createRole(new CreateRoleRequest().withRoleName(roleName).withAssumeRolePolicyDocument(TRUST_POLICY)).getRole();

        iam.putRolePolicy(new PutRolePolicyRequest().withPolicyName("TestPolicy").withRoleName(roleName).withPolicyDocument(PERMISSIONS));

        instanceProfile = iam.createInstanceProfile(new CreateInstanceProfileRequest().withInstanceProfileName(profileName)).getInstanceProfile();
        vpcId = ec2.createVpc(new CreateVpcRequest().withCidrBlock("10.2.0.0/16")).getVpc().getVpcId();

        do {
            Thread.sleep(1000 * 2);
        } while (!ec2.describeVpcs(new DescribeVpcsRequest().withVpcIds(vpcId)).getVpcs().get(0).getState().equals("available"));

        subId = ec2.createSubnet(new CreateSubnetRequest().withVpcId(vpcId).withCidrBlock("10.2.0.0/24")).getSubnet().getSubnetId();

        do {
            Thread.sleep(1000 * 2);
        }
        while (!ec2.describeSubnets(new DescribeSubnetsRequest().withSubnetIds(subId)).getSubnets().get(0).getState().equals("available"));
    }

}
