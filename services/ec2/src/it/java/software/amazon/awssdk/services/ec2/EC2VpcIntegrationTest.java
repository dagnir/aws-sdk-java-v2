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

    /** Release resources used in testing */
    @After
    public void tearDown() {
        if (vpc != null) {
            EC2TestHelper.deleteVpc(vpc.getVpcId());
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
            vpc = createVpcResult.getVpc();
            assertNotNull(vpc);
            assertTrue(vpc.getVpcId().startsWith("vpc-"));
            tagResource(vpc.getVpcId(), TAGS);
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
                EC2TestHelper.describeVpc(vpc.getVpcId());

        assertNotNull(describeResult.getVpcs());
        assertTrue(describeResult.getVpcs().size() == 1);
        assertTrue(describeResult.getVpcs().get(0).getVpcId().equals(vpc.getVpcId()));
        assertEqualUnorderedTagLists(TAGS, describeResult.getVpcs().get(0).getTags());

        // Describe attributes
        DescribeVpcAttributeResult describeVpcAttributesResult = EC2TestHelper.describeVpcAttribute(vpc.getVpcId(), true, false);
        assertEquals(describeVpcAttributesResult.getVpcId(), vpc.getVpcId());
        assertNotNull(describeVpcAttributesResult.getEnableDnsHostnames());
        assertNull(describeVpcAttributesResult.getEnableDnsSupport());

        // Modify the attributes
        EC2TestHelper.modifyVpcAttribute(vpc.getVpcId());
        describeVpcAttributesResult = EC2TestHelper.describeVpcAttribute(vpc.getVpcId(), false, true);
        assertEquals(describeVpcAttributesResult.getVpcId(), vpc.getVpcId());
        assertEquals(true, describeVpcAttributesResult.getEnableDnsSupport());

        // Delete
        EC2TestHelper.deleteVpc(vpc.getVpcId());
        vpc = null;
    }

}
