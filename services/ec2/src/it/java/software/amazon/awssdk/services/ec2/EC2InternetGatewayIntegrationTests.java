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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayRequest;
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
            ec2.deleteVpc(DeleteVpcRequest.builder().vpcId(vpcId).build());
        }
    }

    private static String createInternetGateway() {
        return ec2.createInternetGateway(CreateInternetGatewayRequest.builder().build())
                  .internetGateway().internetGatewayId();
    }

    private static String createVpc() {
        return ec2.createVpc(CreateVpcRequest.builder()
                                             .cidrBlock(VPC_CIDR_BLOCK).build()
                            ).vpc().vpcId();
    }

    private static void deleteInternetGateway(String internetGatewayId) {

        InternetGateway gateway = ec2.describeInternetGateways(
                DescribeInternetGatewaysRequest.builder()
                                               .internetGatewayIds(internetGatewayId).build()).internetGateways().get(0);

        // We need to detach any gateways before deleting them
        for (InternetGatewayAttachment att : gateway.attachments()) {
            ec2.detachInternetGateway(DetachInternetGatewayRequest.builder()
                                                                  .internetGatewayId(gateway.internetGatewayId())
                                                                  .vpcId(att.vpcId()).build());
        }

        ec2.deleteInternetGateway(DeleteInternetGatewayRequest.builder()
                                                              .internetGatewayId(gateway.internetGatewayId()).build());
    }

    /**
     * Also tests that tags work as expected
     */
    @Test
    public void testDescribeGateways() {
        tagResource(internetGatewayId, TAGS);

        DescribeInternetGatewaysResult result = ec2.describeInternetGateways(
                DescribeInternetGatewaysRequest.builder()
                                               .internetGatewayIds(internetGatewayId).build());
        assertEquals(1, result.internetGateways().size());

        InternetGateway ig = result.internetGateways().get(0);
        assertEquals(internetGatewayId, ig.internetGatewayId());
        assertEqualUnorderedTagLists(TAGS, ig.tags());
        assertNotNull(ig.attachments());
    }

    /**
     * Tests attaching and then detaching a gateway.
     */
    @Test
    public void testAttachGateway() {

        // Attach
        final DescribeInternetGatewaysRequest describeInternetGatewaysRequest =
                DescribeInternetGatewaysRequest.builder().internetGatewayIds(internetGatewayId).build();

        DescribeInternetGatewaysResult result = ec2.describeInternetGateways(describeInternetGatewaysRequest);
        Assert.assertEquals(1, result.internetGateways().size());
        InternetGateway gateway = result.internetGateways().get(0);
        assertEquals(0, gateway.attachments().size());

        ec2.attachInternetGateway(AttachInternetGatewayRequest.builder()
                                                              .internetGatewayId(internetGatewayId)
                                                              .vpcId(vpcId).build());

        result = ec2.describeInternetGateways(describeInternetGatewaysRequest);
        assertEquals(1, result.internetGateways().size());
        gateway = result.internetGateways().get(0);
        assertEquals(1, gateway.attachments().size());
        assertEquals(vpcId, gateway.attachments().get(0).vpcId());

        // Detach
        ec2.detachInternetGateway(DetachInternetGatewayRequest.builder()
                                                              .internetGatewayId(internetGatewayId)
                                                              .vpcId(vpcId).build());
        result = ec2.describeInternetGateways(describeInternetGatewaysRequest);
        assertEquals(1, result.internetGateways().size());
        gateway = result.internetGateways().get(0);
        assertEquals(0, gateway.attachments().size());
    }
}
