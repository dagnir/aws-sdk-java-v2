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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.AcceptVpcPeeringConnectionRequest;
import software.amazon.awssdk.services.ec2.model.AcceptVpcPeeringConnectionResult;
import software.amazon.awssdk.services.ec2.model.CreateVpcPeeringConnectionRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcPeeringConnectionResult;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResult;
import software.amazon.awssdk.services.ec2.model.DeleteVpcPeeringConnectionRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcPeeringConnectionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcPeeringConnectionsResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.RejectVpcPeeringConnectionRequest;
import software.amazon.awssdk.services.ec2.model.RejectVpcPeeringConnectionResult;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcPeeringConnection;

public class EC2VpcPeeringIntegrationTest extends EC2VPCIntegrationTestBase {

    private static final String REQUESTER_CIDR_BLOCK = "10.0.0.0/24";
    private static final String ACCEPTER_CIDR_BLOCK = "10.0.1.0/24";

    private static Vpc requesterVpc;
    private static Vpc accepterVpc;
    private static String peeringConnectionIdToBeRejected;
    private static String peeringConnectionIdToBeAccepted;

    @BeforeClass
    public static void createTestVpcs() {
        deleteAllPeeringConnections();

        CreateVpcResult createRequesterVpcResult =
                ec2.createVpc(CreateVpcRequest.builder().cidrBlock(REQUESTER_CIDR_BLOCK).build());
        requesterVpc = createRequesterVpcResult.vpc();

        CreateVpcResult createPeerVpcResult =
                ec2.createVpc(CreateVpcRequest.builder().cidrBlock(ACCEPTER_CIDR_BLOCK).build());
        accepterVpc = createPeerVpcResult.vpc();
    }

    @AfterClass
    public static void tearDown() {
        ec2.deleteVpcPeeringConnection(
                DeleteVpcPeeringConnectionRequest.builder()
                                                 .vpcPeeringConnectionId(peeringConnectionIdToBeAccepted).build());

        ec2.deleteVpc(DeleteVpcRequest.builder().vpcId(requesterVpc.vpcId()).build());
        ec2.deleteVpc(DeleteVpcRequest.builder().vpcId(accepterVpc.vpcId()).build());
    }

    @Test
    public void testVpcPeeringOperations() {
        // Filter the connections by requester vpc id
        DescribeVpcPeeringConnectionsRequest.Builder describeConnectionsByRequesterId =
                DescribeVpcPeeringConnectionsRequest.builder()
                                                    .filters(Filter.builder().name("requester-vpc-info.vpc-id")
                                                                   .values(requesterVpc.vpcId()).build());
        DescribeVpcPeeringConnectionsResult describeVpcPeeringResult = ec2
                .describeVpcPeeringConnections(describeConnectionsByRequesterId.build());
        assertTrue(describeVpcPeeringResult.vpcPeeringConnections().isEmpty());

        // Create a peering connection
        CreateVpcPeeringConnectionResult createVpcPeeringResult = ec2.createVpcPeeringConnection(
                CreateVpcPeeringConnectionRequest.builder()
                                                 .vpcId(requesterVpc.vpcId())
                                                 .peerVpcId(accepterVpc.vpcId()).build());
        peeringConnectionIdToBeRejected = createVpcPeeringResult.vpcPeeringConnection().vpcPeeringConnectionId();

        // The newly created connection should be in the "pending-acceptance" status
        describeVpcPeeringResult = ec2.describeVpcPeeringConnections(describeConnectionsByRequesterId.build());
        assertEquals(1, describeVpcPeeringResult.vpcPeeringConnections().size());
        VpcPeeringConnection createdConnection = describeVpcPeeringResult.vpcPeeringConnections().get(0);
        assertEquals("pending-acceptance", createdConnection.status().code());
        assertEquals(peeringConnectionIdToBeRejected, createdConnection.vpcPeeringConnectionId());
        assertEquals(requesterVpc.vpcId(), createdConnection.requesterVpcInfo().vpcId());
        assertEquals(requesterVpc.cidrBlock(), createdConnection.requesterVpcInfo().cidrBlock());
        assertEquals(accepterVpc.vpcId(), createdConnection.accepterVpcInfo().vpcId());

        // Reject this connection
        RejectVpcPeeringConnectionResult rejectConnectionResult = ec2.rejectVpcPeeringConnection(
                RejectVpcPeeringConnectionRequest.builder()
                                                 .vpcPeeringConnectionId(peeringConnectionIdToBeRejected).build());
        assertTrue(rejectConnectionResult.returnValue());

        // Now the connection should be in "rejected" status
        describeVpcPeeringResult = ec2.describeVpcPeeringConnections(describeConnectionsByRequesterId.build());
        assertEquals(1, describeVpcPeeringResult.vpcPeeringConnections().size());
        VpcPeeringConnection rejectedConnection = describeVpcPeeringResult.vpcPeeringConnections().get(0);
        assertEquals("rejected", rejectedConnection.status().code());
        assertEquals(peeringConnectionIdToBeRejected, rejectedConnection.vpcPeeringConnectionId());
        assertEquals(requesterVpc.vpcId(), rejectedConnection.requesterVpcInfo().vpcId());
        assertEquals(requesterVpc.cidrBlock(), rejectedConnection.requesterVpcInfo().cidrBlock());
        assertEquals(accepterVpc.vpcId(), rejectedConnection.accepterVpcInfo().vpcId());

        // Create another peering connection
        createVpcPeeringResult = ec2.createVpcPeeringConnection(
                CreateVpcPeeringConnectionRequest.builder()
                                                 .vpcId(requesterVpc.vpcId())
                                                 .peerVpcId(accepterVpc.vpcId()).build());
        peeringConnectionIdToBeAccepted = createVpcPeeringResult.vpcPeeringConnection().vpcPeeringConnectionId();

        // This time, we accept this connection
        AcceptVpcPeeringConnectionResult acceptConnectionResult = ec2.acceptVpcPeeringConnection(
                AcceptVpcPeeringConnectionRequest.builder()
                                                 .vpcPeeringConnectionId(peeringConnectionIdToBeAccepted).build());
        assertEquals(peeringConnectionIdToBeAccepted,
                     acceptConnectionResult.vpcPeeringConnection().vpcPeeringConnectionId());

        // Now there should be two connections visible, one "rejected" and one "accepted"
        // Let's filter out the "accepted" connection
        describeVpcPeeringResult = ec2.describeVpcPeeringConnections(
                describeConnectionsByRequesterId.filters(Filter.builder().name("status-code").values("active").build())
                                                .build());
        assertEquals(1, describeVpcPeeringResult.vpcPeeringConnections().size());
        VpcPeeringConnection acceptedConnection = describeVpcPeeringResult.vpcPeeringConnections().get(0);
        assertEquals("active", acceptedConnection.status().code());
        assertEquals(peeringConnectionIdToBeAccepted, acceptedConnection.vpcPeeringConnectionId());
        assertEquals(requesterVpc.vpcId(), acceptedConnection.requesterVpcInfo().vpcId());
        assertEquals(requesterVpc.cidrBlock(), acceptedConnection.requesterVpcInfo().cidrBlock());
        assertEquals(accepterVpc.vpcId(), acceptedConnection.accepterVpcInfo().vpcId());
        // The accepter VPC's CIDR block should be returned once the connection is accpeted
        assertEquals(accepterVpc.cidrBlock(), acceptedConnection.accepterVpcInfo().cidrBlock());

        // We should not be able to reject an already-accepted connection
        try {
            ec2.rejectVpcPeeringConnection(
                    RejectVpcPeeringConnectionRequest.builder().vpcPeeringConnectionId(peeringConnectionIdToBeAccepted).build());
            fail("We shouldn't be able to reject an already-acceptec connection.");
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }
    }
}
