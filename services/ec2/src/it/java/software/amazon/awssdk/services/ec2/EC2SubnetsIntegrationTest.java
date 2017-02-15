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

    /** Release resources used in testing */
    @After
    public void tearDown() {
        if (subnet != null) {
            EC2TestHelper.deleteSubnet(subnet.getSubnetId());
        }

        if (vpc != null) {
            EC2TestHelper.deleteVpc(vpc.getVpcId());
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
            vpc = createVpcResult.getVpc();
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
                EC2TestHelper.createSubnet(vpc.getVpcId(), CIDR_BLOCK_SUBNET);
        subnet = createResult.getSubnet();
        assertNotNull(subnet);
        assertTrue(subnet.getSubnetId().startsWith("subnet-"));
        tagResource(subnet.getSubnetId(), TAGS);

        // Describe Subnet
        DescribeSubnetsResult describeResult =
                EC2TestHelper.describeSubnet(subnet.getSubnetId());

        assertNotNull(describeResult.getSubnets());
        assertEquals(1, describeResult.getSubnets().size());
        assertEquals(subnet.getSubnetId(),
                     describeResult.getSubnets().get(0).getSubnetId());
        assertEqualUnorderedTagLists(TAGS, describeResult.getSubnets().get(0).getTags());

        // Delete Subnet
        EC2TestHelper.deleteSubnet(subnet.getSubnetId());
        String subnetId = subnet.getSubnetId();
        subnet = null;

        // We can't use the subnet ID field of the request, because it generates a 404
        describeResult = ec2.describeSubnets(new DescribeSubnetsRequest()
                                                     .withFilters(new Filter().withName("subnet-id").withValues(
                                                             subnetId)));
        assertEquals(0, describeResult.getSubnets().size());
    }

}
