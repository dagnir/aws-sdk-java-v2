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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResult;
import software.amazon.awssdk.services.ec2.model.CreateVpcResult;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;

public class EC2SubnetsIntegrationTest extends EC2IntegrationTestBase {

    private static final String CIDR_BLOCK = "10.0.0.0/23";
    private static final String CIDR_BLOCK_SUBNET = "10.0.0.0/24";

    private Subnet subnet;
    private Vpc vpc;

    /** Release resources used in testing. */
    @After
    public void tearDown() {
        if (subnet != null) {
            EC2TestHelper.deleteSubnet(subnet.subnetId());
        }

        if (vpc != null) {
            EC2TestHelper.deleteVpc(vpc.vpcId());
        }
    }

    /**
     * Tests that we can create, describe and delete a subnet.
     */
    @Test
    public void testSubnetOperations() {
        // Create VPC
        try {
            CreateVpcResult createVpcResult = EC2TestHelper.createVpc(CIDR_BLOCK);
            vpc = createVpcResult.vpc();
        } catch (AmazonServiceException ase) {
            if (!ase.getErrorCode().equals("VpcLimitExceeded")) {
                throw ase;
            }
            System.err.println("Unable to run " + getClass().getName() + ": "
                               + ase.getMessage());
            return;
        }

        // Create Subnet
        CreateSubnetResult createResult =
                EC2TestHelper.createSubnet(vpc.vpcId(), CIDR_BLOCK_SUBNET);
        subnet = createResult.subnet();
        assertNotNull(subnet);
        assertTrue(subnet.subnetId().startsWith("subnet-"));
        tagResource(subnet.subnetId(), TAGS);

        // Describe Subnet
        DescribeSubnetsResult describeResult =
                EC2TestHelper.describeSubnet(subnet.subnetId());

        assertNotNull(describeResult.subnets());
        assertEquals(1, describeResult.subnets().size());
        assertEquals(subnet.subnetId(),
                     describeResult.subnets().get(0).subnetId());
        assertEqualUnorderedTagLists(TAGS, describeResult.subnets().get(0).tags());

        // Delete Subnet
        EC2TestHelper.deleteSubnet(subnet.subnetId());
        String subnetId = subnet.subnetId();
        subnet = null;

        // We can't use the subnet ID field of the request, because it generates a 404
        describeResult = ec2.describeSubnets(DescribeSubnetsRequest.builder()
                                                                   .filters(Filter.builder().name("subnet-id").values(
                                                                           subnetId).build()).build());
        assertEquals(0, describeResult.subnets().size());
    }

}
