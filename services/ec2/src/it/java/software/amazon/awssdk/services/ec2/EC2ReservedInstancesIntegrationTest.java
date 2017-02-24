package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.services.ec2.model.DescribeReservedInstancesOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeReservedInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.PurchaseReservedInstancesOfferingResult;
import software.amazon.awssdk.services.ec2.model.ReservedInstances;
import software.amazon.awssdk.services.ec2.model.ReservedInstancesOffering;

/**
 * Note: These tests use a test offering ID set up for the aws-dr-tools-test@
 * account.
 */
public class EC2ReservedInstancesIntegrationTest extends EC2IntegrationTestBase {

    private static final String TEST_OFFERING_ID = "438012d3-352b-4361-9d2e-a3166db861b1";

    /** Uses our test offering to purchase the reserved instances. */
    @Test
    public void testPurchaseReservedInstancesOffering() {
        PurchaseReservedInstancesOfferingResult purchaseResult = EC2TestHelper.purchaseReservedInstancesOffering(TEST_OFFERING_ID, 1);
        String reservedInstancesId = purchaseResult.getReservedInstancesId();
        assertNotNull(reservedInstancesId);
        tagResource(reservedInstancesId, TAGS);

        DescribeReservedInstancesRequest describeRequest = new DescribeReservedInstancesRequest();
        describeRequest.withFilters(new Filter("reserved-instances-id", null).withValues(reservedInstancesId));
        List<ReservedInstances> instances = ec2.describeReservedInstances(describeRequest).getReservedInstances();
        assertEquals(1, instances.size());
        assertEqualUnorderedTagLists(TAGS, instances.get(0).getTags());
        assertNotNull(instances.get(0).getInstanceTenancy());
        assertNotNull(instances.get(0).getCurrencyCode());
    }

    @Test
    public void testDescribeReservedInstancesOfferings() {
        DescribeReservedInstancesOfferingsRequest request = new DescribeReservedInstancesOfferingsRequest();
        request.withInstanceTenancy("default");
        request.withFilters(new Filter("reserved-instances-offering-id", null).withValues(TEST_OFFERING_ID));
        List<ReservedInstancesOffering> offerings = ec2.describeReservedInstancesOfferings(request).getReservedInstancesOfferings();
        assertEquals(1, offerings.size());
        assertNotNull(offerings.get(0).getInstanceTenancy());
        assertNotNull(offerings.get(0).getCurrencyCode());
    }
}
