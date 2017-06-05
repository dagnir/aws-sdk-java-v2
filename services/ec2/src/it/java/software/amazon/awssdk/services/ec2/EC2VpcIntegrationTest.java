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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.CreateVpcResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResult;
import software.amazon.awssdk.services.ec2.model.Vpc;

public class EC2VpcIntegrationTest extends EC2IntegrationTestBase {

    private static final String CIDR_BLOCK = "10.2.0.0/23";
    private Vpc vpc;

    /** Release resources used in testing. */
    @After
    public void tearDown() {
        if (vpc != null) {
            EC2TestHelper.deleteVpc(vpc.vpcId());
        }
    }

    /**
     * Tests that we can create, describe and delete VPCs.
     */
    @Test
    public void testVpcOperations() {
        // Create VPC
        try {
            CreateVpcResult createVpcResult = EC2TestHelper.createVpc(CIDR_BLOCK);
            vpc = createVpcResult.vpc();
            assertNotNull(vpc);
            assertTrue(vpc.vpcId().startsWith("vpc-"));
            tagResource(vpc.vpcId(), TAGS);
        } catch (AmazonServiceException ase) {
            if (!ase.getErrorCode().equals("VpcLimitExceeded")) {
                throw ase;
            }
            System.err.println("Unable to run " + getClass().getName() + ": "
                               + ase.getMessage());
            return;
        }

        // Describe
        DescribeVpcsResult describeResult =
                EC2TestHelper.describeVpc(vpc.vpcId());

        assertNotNull(describeResult.vpcs());
        assertTrue(describeResult.vpcs().size() == 1);
        assertTrue(describeResult.vpcs().get(0).vpcId().equals(vpc.vpcId()));
        assertEqualUnorderedTagLists(TAGS, describeResult.vpcs().get(0).tags());

        // Describe attributes
        DescribeVpcAttributeResult describeVpcAttributesResult = EC2TestHelper.describeVpcAttribute(vpc.vpcId(), true, false);
        assertEquals(describeVpcAttributesResult.vpcId(), vpc.vpcId());
        assertNotNull(describeVpcAttributesResult.enableDnsHostnames());
        assertNull(describeVpcAttributesResult.enableDnsSupport());

        // Modify the attributes
        EC2TestHelper.modifyVpcAttribute(vpc.vpcId());
        describeVpcAttributesResult = EC2TestHelper.describeVpcAttribute(vpc.vpcId(), false, true);
        assertEquals(describeVpcAttributesResult.vpcId(), vpc.vpcId());
        assertEquals(true, describeVpcAttributesResult.enableDnsSupport());

        // Delete
        EC2TestHelper.deleteVpc(vpc.vpcId());
        vpc = null;
    }

}
