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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResult;
import software.amazon.awssdk.services.ec2.model.DetachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;

/**
 * Tests APIs related to internet gateways.
 */
public class EC2InternetGatewayIntegrationTests extends EC2IntegrationTestBase {

    private static final String VPC_CIDR_BLOCK = "10.0.0.0/23";

    /**
     * The id of the VPC created by the test
     */
    private static String vpcId;

    /**
     * The id of the internet gateway created by the test
     */
    private static String internetGatewayId;

    @BeforeClass
    public static void setUp() {
        vpcId = createVpc();
        internetGatewayId = createInternetGateway();
    }

    /**
     * Deletes all groups used by this test (this run or past runs), and any
     * VPCs that we can safely delete.
     */
    @AfterClass
    public static void cleanUp() {
        if (internetGatewayId != null) {
            deleteInternetGateway(internetGatewayId);
        }
        if (vpcId != null) {
            ec2.deleteVpc(new DeleteVpcRequest(vpcId));
        }
    }

    /**
     * Also tests that tags work as expected
     */
    @Test
    public void testDescribeGateways() {
        tagResource(internetGatewayId, TAGS);

        DescribeInternetGatewaysResult result = ec2.describeInternetGateways(
                new DescribeInternetGatewaysRequest()
                        .withInternetGatewayIds(internetGatewayId));
        assertEquals(1, result.getInternetGateways().size());

        InternetGateway ig = result.getInternetGateways().get(0);
        assertEquals(internetGatewayId, ig.getInternetGatewayId());
        assertEqualUnorderedTagLists(TAGS, ig.getTags());
        assertNotNull(ig.getAttachments());
    }

    /**
     * Tests attaching and then detaching a gateway.
     */
    @Test
    public void testAttachGateway() {

        // Attach
        final DescribeInternetGatewaysRequest describeInternetGatewaysRequest = new DescribeInternetGatewaysRequest().withInternetGatewayIds
                (internetGatewayId);

        DescribeInternetGatewaysResult result = ec2.describeInternetGateways
                (describeInternetGatewaysRequest);
        Assert.assertEquals(1, result.getInternetGateways().size());
        InternetGateway gateway = result.getInternetGateways().get(0);
        assertEquals(0, gateway.getAttachments().size());

        ec2.attachInternetGateway(new AttachInternetGatewayRequest()
                .withInternetGatewayId(internetGatewayId)
                .withVpcId(vpcId));

        result = ec2.describeInternetGateways(describeInternetGatewaysRequest);
        assertEquals(1, result.getInternetGateways().size());
        gateway = result.getInternetGateways().get(0);
        assertEquals(1, gateway.getAttachments().size());
        assertEquals(vpcId, gateway.getAttachments().get(0).getVpcId());

        // Detach
        ec2.detachInternetGateway(new DetachInternetGatewayRequest()
                .withInternetGatewayId(internetGatewayId)
                .withVpcId(vpcId));
        result = ec2.describeInternetGateways(describeInternetGatewaysRequest);
        assertEquals(1, result.getInternetGateways().size());
        gateway = result.getInternetGateways().get(0);
        assertEquals(0, gateway.getAttachments().size());
    }


    private static String createInternetGateway() {
        return ec2.createInternetGateway()
                .getInternetGateway().getInternetGatewayId();
    }

    private static String createVpc() {
        return ec2.createVpc(new CreateVpcRequest()
                .withCidrBlock(VPC_CIDR_BLOCK)
                ).getVpc().getVpcId();
    }

    private static void deleteInternetGateway(String internetGatewayId) {

        InternetGateway gateway = ec2.describeInternetGateways(
                new DescribeInternetGatewaysRequest()
                        .withInternetGatewayIds(internetGatewayId)
                ).getInternetGateways().get(0);

        // We need to detach any gateways before deleting them
        for (InternetGatewayAttachment att : gateway.getAttachments()) {
            ec2.detachInternetGateway(new DetachInternetGatewayRequest()
                    .withInternetGatewayId(gateway.getInternetGatewayId())
                    .withVpcId(att.getVpcId()));
        }

        ec2.deleteInternetGateway(new DeleteInternetGatewayRequest()
                .withInternetGatewayId(gateway.getInternetGatewayId()));
    }
}
