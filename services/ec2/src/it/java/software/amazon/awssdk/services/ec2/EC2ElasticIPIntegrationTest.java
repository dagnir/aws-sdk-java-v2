package software.amazon.awssdk.services.ec2;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;

import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResult;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;

/**
 * Integration tests for the Elastic IP operations in the Amazon EC2 Java client
 * library.
 *
 * @author fulghum@amazon.com
 */
public class EC2ElasticIPIntegrationTest extends EC2IntegrationTestBase {

    private static String allocationId;

    private static String elasticIp;

    /**
     * Ensures that all testing resources are correctly released.
     */
    @AfterClass
    public static void tearDown() {
        if (allocationId != null) {
            ec2.releaseAddress(new ReleaseAddressRequest().withAllocationId(allocationId));
            assertFalse(doAddressesContainIp(ec2.describeAddresses().getAddresses(), elasticIp));
        }
    }

    /**
     * Tests that the various EC2 Elastic IP operations work correctly by going
     * through a series of calls that hit all of the Elastic IP operations.
     */
    @Test
    public void testElasticIpOperations() {

        // Allocate an address
        AllocateAddressResult allocateAddressResult = ec2.allocateAddress();

        elasticIp = allocateAddressResult.getPublicIp();
        allocationId = allocateAddressResult.getAllocationId();

        assertNotNull(elasticIp);
        assertThat(elasticIp.length(), greaterThan(5));

        // Describe addresses
        DescribeAddressesResult describeAddressesResult = ec2.describeAddresses();
        assertNotNull(describeAddressesResult);
        List<Address> addresses = describeAddressesResult.getAddresses();
        assertTrue(doAddressesContainIp(addresses, elasticIp));

        // Describe with a filter
        addresses = ec2.describeAddresses(new DescribeAddressesRequest()
                        .withFilters(new Filter()
                                .withName("public-ip")
                                .withValues(elasticIp))
                ).getAddresses();
        assertEquals(1, addresses.size());
        assertTrue(doAddressesContainIp(addresses, elasticIp));
    }


    /*
     * Test Helper Methods
     */

    /**
     * Tests if the specified expected IP address is contained in the list of
     * address objects. If the expected IP address is not found in any of the
     * address objects, this method will return false.
     *
     * @param addresses
     *            The list of Address objects to check.
     * @param expectedIp
     *            The IP expected to be in the list of addresses.
     *
     * @return True if the list of addresses contains an address with the
     *         specified Elastic IP, otherwise false.
     */
    private static boolean doAddressesContainIp(List<Address> addresses, String expectedIp) {
        assertNotNull(addresses);

        for (Address address : addresses) {
            String ip = address.getPublicIp();

            // Bail out early if we see the IP we're expecting.
            if (ip.equals(expectedIp)) return true;
        }

        return false;
    }

}
