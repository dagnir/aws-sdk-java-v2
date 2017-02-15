package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpnGatewaysResult;
import software.amazon.awssdk.services.ec2.model.VpnGateway;

public class EC2VpnGatewaysIntegrationTest extends EC2IntegrationTestBase {

    private VpnGateway vpnGateway;

    /** Release resources used by tests */
    @After
    public void tearDown() {
        if (vpnGateway != null) {
            EC2TestHelper.deleteVpnGateway(vpnGateway.getVpnGatewayId());
        }
    }

    /**
     * Tests that we can create, describe and delete a VpnGateway.
     */
    @Test
    public void testVpnGatewayOperations() {
        // Create VpnGateway
        try {
            CreateVpnGatewayResult createResult =
                    EC2TestHelper.createVpnGateway("ipsec.1");
            vpnGateway = createResult.getVpnGateway();
            tagResource(vpnGateway.getVpnGatewayId(), TAGS);
        } catch (AmazonServiceException ase) {
            if (!ase.getErrorCode().equals("VpnGatewayLimitExceeded")) {
                throw ase;
            }

            System.err.println("Unable to run " + getClass().getName() + ": "
                               + ase.getMessage());
            return;
        }

        assertNotNull(vpnGateway);
        assertTrue(vpnGateway.getType().equals("ipsec.1"));

        // Describe VpnGateway
        DescribeVpnGatewaysResult describeResult =
                EC2TestHelper.describeVpnGateway(vpnGateway.getVpnGatewayId());

        assertEquals(1, describeResult.getVpnGateways().size());
        assertEquals(vpnGateway.getVpnGatewayId(),
                     describeResult.getVpnGateways().get(0).getVpnGatewayId());
        assertEqualUnorderedTagLists(TAGS, describeResult.getVpnGateways().get(0).getTags());

        // Delete VpnGateway
        EC2TestHelper.deleteVpnGateway(vpnGateway.getVpnGatewayId());
        vpnGateway = null;
    }

}
