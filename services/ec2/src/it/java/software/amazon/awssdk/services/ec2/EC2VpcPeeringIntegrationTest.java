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
                ec2.createVpc(new CreateVpcRequest().withCidrBlock(REQUESTER_CIDR_BLOCK));
        requesterVpc = createRequesterVpcResult.getVpc();
        
        CreateVpcResult createPeerVpcResult =
                ec2.createVpc(new CreateVpcRequest().withCidrBlock(ACCEPTER_CIDR_BLOCK));
        accepterVpc = createPeerVpcResult.getVpc();
    }

    @AfterClass
    public static void tearDown() {
        ec2.deleteVpcPeeringConnection(
                new DeleteVpcPeeringConnectionRequest()
                    .withVpcPeeringConnectionId(peeringConnectionIdToBeAccepted));

        ec2.deleteVpc(new DeleteVpcRequest(requesterVpc.getVpcId()));
        ec2.deleteVpc(new DeleteVpcRequest(accepterVpc.getVpcId()));
    }

    @Test
    public void testVpcPeeringOperations() {
        // Filter the connections by requester vpc id
        DescribeVpcPeeringConnectionsRequest describeConnectionsByRequesterId = new DescribeVpcPeeringConnectionsRequest()
                .withFilters(new Filter("requester-vpc-info.vpc-id")
                        .withValues(requesterVpc.getVpcId()));
        DescribeVpcPeeringConnectionsResult describeVpcPeeringResult = ec2
                .describeVpcPeeringConnections(describeConnectionsByRequesterId);
        assertTrue(describeVpcPeeringResult.getVpcPeeringConnections().isEmpty());

        // Create a peering connection
        CreateVpcPeeringConnectionResult createVpcPeeringResult = ec2.createVpcPeeringConnection(
                new CreateVpcPeeringConnectionRequest()
                    .withVpcId(requesterVpc.getVpcId())
                    .withPeerVpcId(accepterVpc.getVpcId()));
        peeringConnectionIdToBeRejected = createVpcPeeringResult.getVpcPeeringConnection().getVpcPeeringConnectionId();

        // The newly created connection should be in the "pending-acceptance" status
        describeVpcPeeringResult = ec2.describeVpcPeeringConnections(describeConnectionsByRequesterId);
        assertEquals(1, describeVpcPeeringResult.getVpcPeeringConnections().size());
        VpcPeeringConnection createdConnection = describeVpcPeeringResult.getVpcPeeringConnections().get(0);
        assertEquals("pending-acceptance", createdConnection.getStatus().getCode());
        assertEquals(peeringConnectionIdToBeRejected, createdConnection.getVpcPeeringConnectionId());
        assertEquals(requesterVpc.getVpcId(), createdConnection.getRequesterVpcInfo().getVpcId());
        assertEquals(requesterVpc.getCidrBlock(), createdConnection.getRequesterVpcInfo().getCidrBlock());
        assertEquals(accepterVpc.getVpcId(), createdConnection.getAccepterVpcInfo().getVpcId());

        // Reject this connection
        RejectVpcPeeringConnectionResult rejectConnectionResult = ec2.rejectVpcPeeringConnection(
                new RejectVpcPeeringConnectionRequest()
                    .withVpcPeeringConnectionId(peeringConnectionIdToBeRejected));
        assertTrue(rejectConnectionResult.getReturn());

        // Now the connection should be in "rejected" status
        describeVpcPeeringResult = ec2.describeVpcPeeringConnections(describeConnectionsByRequesterId);
        assertEquals(1, describeVpcPeeringResult.getVpcPeeringConnections().size());
        VpcPeeringConnection rejectedConnection = describeVpcPeeringResult.getVpcPeeringConnections().get(0);
        assertEquals("rejected", rejectedConnection.getStatus().getCode());
        assertEquals(peeringConnectionIdToBeRejected, rejectedConnection.getVpcPeeringConnectionId());
        assertEquals(requesterVpc.getVpcId(), rejectedConnection.getRequesterVpcInfo().getVpcId());
        assertEquals(requesterVpc.getCidrBlock(), rejectedConnection.getRequesterVpcInfo().getCidrBlock());
        assertEquals(accepterVpc.getVpcId(), rejectedConnection.getAccepterVpcInfo().getVpcId());

        // Create another peering connection
        createVpcPeeringResult = ec2.createVpcPeeringConnection(
                new CreateVpcPeeringConnectionRequest()
                    .withVpcId(requesterVpc.getVpcId())
                    .withPeerVpcId(accepterVpc.getVpcId()));
        peeringConnectionIdToBeAccepted = createVpcPeeringResult.getVpcPeeringConnection().getVpcPeeringConnectionId();
        
        // This time, we accept this connection
        AcceptVpcPeeringConnectionResult acceptConnectionResult = ec2.acceptVpcPeeringConnection(
                new AcceptVpcPeeringConnectionRequest()
                    .withVpcPeeringConnectionId(peeringConnectionIdToBeAccepted));
        assertEquals(peeringConnectionIdToBeAccepted, acceptConnectionResult.getVpcPeeringConnection().getVpcPeeringConnectionId());
        
        // Now there should be two connections visible, one "rejected" and one "accepted"
        // Let's filter out the "accepted" connection
        describeVpcPeeringResult = ec2
                .describeVpcPeeringConnections(describeConnectionsByRequesterId
                        .withFilters(new Filter("status-code")
                                .withValues("active")));
        assertEquals(1, describeVpcPeeringResult.getVpcPeeringConnections().size());
        VpcPeeringConnection acceptedConnection = describeVpcPeeringResult.getVpcPeeringConnections().get(0);
        assertEquals("active", acceptedConnection.getStatus().getCode());
        assertEquals(peeringConnectionIdToBeAccepted, acceptedConnection.getVpcPeeringConnectionId());
        assertEquals(requesterVpc.getVpcId(), acceptedConnection.getRequesterVpcInfo().getVpcId());
        assertEquals(requesterVpc.getCidrBlock(), acceptedConnection.getRequesterVpcInfo().getCidrBlock());
        assertEquals(accepterVpc.getVpcId(), acceptedConnection.getAccepterVpcInfo().getVpcId());
        // The accepter VPC's CIDR block should be returned once the connection is accpeted
        assertEquals(accepterVpc.getCidrBlock(), acceptedConnection.getAccepterVpcInfo().getCidrBlock());
        
        // We should not be able to reject an already-accepted connection
        try {
            ec2.rejectVpcPeeringConnection(new RejectVpcPeeringConnectionRequest().withVpcPeeringConnectionId(peeringConnectionIdToBeAccepted));
            fail("We shouldn't be able to reject an already-acceptec connection.");
        } catch (AmazonServiceException expected) {}
    }
}
