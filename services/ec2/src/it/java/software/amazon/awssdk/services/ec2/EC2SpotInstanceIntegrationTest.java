package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CancelSpotInstanceRequestsRequest;
import software.amazon.awssdk.services.ec2.model.CancelledSpotInstanceRequest;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.CreateSpotDatafeedSubscriptionRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotPriceHistoryRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotPriceHistoryResult;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.LaunchSpecification;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesResult;
import software.amazon.awssdk.services.ec2.model.SpotDatafeedSubscription;
import software.amazon.awssdk.services.ec2.model.SpotInstanceRequest;
import software.amazon.awssdk.services.ec2.model.SpotInstanceType;
import software.amazon.awssdk.services.ec2.model.SpotPlacement;
import software.amazon.awssdk.services.ec2.model.SpotPrice;
import software.amazon.awssdk.services.s3.AmazonS3Client;

/**
 * Integration tests for Spot Instance APIs in EC2.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class EC2SpotInstanceIntegrationTest extends EC2IntegrationTestBase {

    /** The AMI ID to use in test data - needs to be a real AMI in order for EC2 to accept it. */
    private static final String TEST_AMI_ID = "ami-84db39ed";

    private static final String DEFAULT_GROUP_NAME = "default";
    private final String UNAVAILABLE_ZONE = "us-east-1a";
    private AvailabilityZone zone;

    /**
     * Tests that we can call the DescribeSpotPriceHistory operation and get
     * back sane results.
     */
    @Test
    public void testDescribeSpotPriceHistory() throws Exception {
        DescribeSpotPriceHistoryResult priceHistoryResult =
                ec2.describeSpotPriceHistory(new DescribeSpotPriceHistoryRequest()
                                                     .withProductDescriptions("Linux/UNIX", "Windows"));

        List<SpotPrice> spotPrices = priceHistoryResult.getSpotPriceHistory();
        assertTrue(spotPrices.size() > 2);

        assertNotNull(priceHistoryResult.getNextToken());

        for (SpotPrice spotPrice : spotPrices) {
            assertNotNull(spotPrice.getInstanceType());
            Assert.assertThat(spotPrice.getProductDescription(), Matchers.not
                    (Matchers.isEmptyOrNullString()));
            Assert.assertThat(spotPrice.getSpotPrice(), Matchers.not
                    (Matchers.isEmptyOrNullString()));
            assertNotNull(spotPrice.getAvailabilityZone());
            assertNotNull(spotPrice.getTimestamp());
        }
    }

    /** Tests that we can use the pagination features of DescribeSpotPriceHistory. */
    @Test
    public void testDescribeSpotPriceHistory_withPagination() throws Exception {
        DescribeSpotPriceHistoryResult priceHistoryResult =
                ec2.describeSpotPriceHistory(new DescribeSpotPriceHistoryRequest()
                                                     .withProductDescriptions("Linux/UNIX", "Windows")
                                                     .withMaxResults(10));

        String nextToken = priceHistoryResult.getNextToken();
        String availabilityZone = priceHistoryResult.getSpotPriceHistory().get(0).getAvailabilityZone();

        assertNotNull(nextToken);
        assertNotNull(availabilityZone);
        Assert.assertFalse(priceHistoryResult.getSpotPriceHistory().size() > 10);
        ec2.describeSpotPriceHistory(new DescribeSpotPriceHistoryRequest()
                                             .withProductDescriptions("Linux/UNIX", "Windows")
                                             .withNextToken(nextToken)
                                             .withAvailabilityZone(availabilityZone));
    }

    /**
     * Tests that we can create/describe/cancel requests for spot instances.
     */
    @Test
    public void testSpotInstanceRequests() {
        // Pick any valid, existing key pair to test with
        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs().getKeyPairs();
        String keyName = null;
        if (keyPairs.isEmpty() == false) {
            keyName = keyPairs.get(0).getKeyName();
        } else {
            keyName = ec2.createKeyPair(new CreateKeyPairRequest(
                    "java-integ-" + System.currentTimeMillis())).getKeyPair().getKeyName();
        }

        BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping()
                .withDeviceName("/dev/d2")
                .withEbs(new EbsBlockDevice()
                                 .withDeleteOnTermination(true)
                                 .withVolumeSize(1));

        zone = getAvailableZone();
        assertNotNull(zone);

        LaunchSpecification launchSpecification = new LaunchSpecification()
                .withMonitoringEnabled(true)
                .withSecurityGroups("default")
                .withBlockDeviceMappings(blockDeviceMapping)
                .withInstanceType(InstanceType.M1Small.toString())
                .withImageId(TEST_AMI_ID)
                .withPlacement(new SpotPlacement(zone.getZoneName()))
                .withUserData("foobaruserdata")
                .withKeyName(keyName);

        RequestSpotInstancesResult result = ec2.requestSpotInstances(new RequestSpotInstancesRequest()
                                                                             .withAvailabilityZoneGroup("zone-group")
                                                                             .withInstanceCount(1)
                                                                             .withLaunchGroup("launch-group")
                                                                             .withLaunchSpecification(launchSpecification)
                                                                             .withSpotPrice("0.01")
                                                                             .withType(SpotInstanceType.OneTime.toString())
                                                                             .withValidFrom(new Date(new Date().getTime() + 1000 * 60 * 60))
                                                                             .withValidUntil(new Date(new Date().getTime() + 1000 * 60 * 60 * 24)));

        // RequestSpotInstances
        List<SpotInstanceRequest> spotInstanceRequests = result.getSpotInstanceRequests();
        assertEquals(1, spotInstanceRequests.size());
        SpotInstanceRequest spotInstanceRequest = spotInstanceRequests.get(0);
        String spotInstanceRequestId = spotInstanceRequest.getSpotInstanceRequestId();
        assertValidSpotInstanceRequest(spotInstanceRequest);
        tagResource(spotInstanceRequestId, TAGS);

        // DescribeSpotInstanceRequests
        List<SpotInstanceRequest> describedSpotInstanceRequests =
                ec2.describeSpotInstanceRequests(new DescribeSpotInstanceRequestsRequest()
                                                         .withSpotInstanceRequestIds(spotInstanceRequestId)).getSpotInstanceRequests();
        assertEquals(1, describedSpotInstanceRequests.size());
        assertValidSpotInstanceRequest(describedSpotInstanceRequests.get(0));
        assertEqualUnorderedTagLists(TAGS, describedSpotInstanceRequests.get(0).getTags());

        // DescribeSpotInstanceRequests with filters
        describedSpotInstanceRequests = ec2.describeSpotInstanceRequests(new DescribeSpotInstanceRequestsRequest()
                                                                                 .withFilters(new Filter("spot-instance-request-id", null).withValues(spotInstanceRequestId))).getSpotInstanceRequests();
        assertEquals(1, describedSpotInstanceRequests.size());
        assertValidSpotInstanceRequest(describedSpotInstanceRequests.get(0));

        // CancelSpotInstanceRequests
        List<CancelledSpotInstanceRequest> canceledSpotInstanceRequests =
                ec2.cancelSpotInstanceRequests(new CancelSpotInstanceRequestsRequest()
                                                       .withSpotInstanceRequestIds(spotInstanceRequestId)).getCancelledSpotInstanceRequests();
        assertEquals(1, canceledSpotInstanceRequests.size());
        Assert.assertThat(canceledSpotInstanceRequests.get(0)
                                                      .getState(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertEquals(spotInstanceRequestId, canceledSpotInstanceRequests.get(0).getSpotInstanceRequestId());
    }


    /**
     * Tests that we can create, describe and delete Spot datafeed
     * subscriptions.
     */
    @Test
    public void testSpotDatafeedSubscriptions() throws Exception {
        // Grab the name of a random bucket in our test account to use
        AmazonS3Client s3 = new AmazonS3Client(getCredentials());
        String bucketName = s3.listBuckets().get(0).getName();
        String prefix = "my-test-prefix";

        // Create a datafeed subscription
        SpotDatafeedSubscription datafeedSubscription =
                ec2.createSpotDatafeedSubscription(
                        new CreateSpotDatafeedSubscriptionRequest()
                                .withBucket(bucketName)
                                .withPrefix(prefix)).getSpotDatafeedSubscription();
        assertValidSpotDatafeedSubscription(datafeedSubscription, bucketName, prefix);

        // Wait a few seconds for eventual consistency
        Thread.sleep(1000 * 5);

        // Describe Spot Datafeed Subscriptions
        assertValidSpotDatafeedSubscription(
                ec2.describeSpotDatafeedSubscription().getSpotDatafeedSubscription(),
                bucketName, prefix);

        // Delete a Spot Datafeed Subscription
        ec2.deleteSpotDatafeedSubscription();
    }


    /*
     * Private Interface
     */

    /**
     * Asserts that the specified spot datafeed subscription is valid and that
     * bucket name and prefix match the specified values.
     *
     * @param datafeedSubscription
     *            The spot datafeed subscription to test.
     * @param bucketName
     *            The expected bucket name.
     * @param prefix
     *            The expected prefix.
     */
    private void assertValidSpotDatafeedSubscription(SpotDatafeedSubscription datafeedSubscription, String bucketName,
                                                     String prefix) {
        assertEquals(bucketName, datafeedSubscription.getBucket());
        assertNull(datafeedSubscription.getFault());
        Assert.assertThat(datafeedSubscription.getOwnerId(), Matchers.not
                (Matchers.isEmptyOrNullString()));
        assertEquals(prefix, datafeedSubscription.getPrefix());
        Assert.assertThat(datafeedSubscription.getState(), Matchers.not
                (Matchers.isEmptyOrNullString()));
    }

    /**
     * Asserts that the specified SpotInstanceRequest is valid, and that the
     * data matches up to what we requested earlier in this test.
     *
     * @param spotInstanceRequest
     *            The spot instance request to test.
     */
    private void assertValidSpotInstanceRequest(SpotInstanceRequest spotInstanceRequest) {
        assertEquals("zone-group", spotInstanceRequest.getAvailabilityZoneGroup());
        assertRecent(spotInstanceRequest.getCreateTime());
        assertNull(spotInstanceRequest.getFault());
        assertNull(spotInstanceRequest.getInstanceId());

        assertEquals("launch-group", spotInstanceRequest.getLaunchGroup());
        Assert.assertThat(spotInstanceRequest.getProductDescription(), Matchers.not
                (Matchers.isEmptyOrNullString()));
        Assert.assertThat(spotInstanceRequest.getSpotInstanceRequestId(), Matchers.not
                (Matchers.isEmptyOrNullString()));
        assertEquals(Double.parseDouble("0.01"), Double.parseDouble(spotInstanceRequest.getSpotPrice()), .001);
        Assert.assertThat(spotInstanceRequest.getState(), Matchers.not
                (Matchers.isEmptyOrNullString()));
        assertEquals(SpotInstanceType.OneTime.toString(), spotInstanceRequest.getType());
        assertRecent(spotInstanceRequest.getValidFrom());
        assertRecent(spotInstanceRequest.getValidUntil());
        assertNotNull(spotInstanceRequest.getStatus());
        assertNotNull(spotInstanceRequest.getStatus().getCode());
        assertNotNull(spotInstanceRequest.getStatus().getMessage());
        assertNotNull(spotInstanceRequest.getStatus().getUpdateTime());

        LaunchSpecification launchSpecification = spotInstanceRequest.getLaunchSpecification();
        assertNull(launchSpecification.getAddressingType());
        assertEquals(1, launchSpecification.getSecurityGroups().size());
        // TODO: this needs to change when we sort out the security group question
        assertEquals(DEFAULT_GROUP_NAME, launchSpecification.getSecurityGroups().get(0));
        assertEquals(TEST_AMI_ID, launchSpecification.getImageId());
        assertEquals(InstanceType.M1Small.toString(), launchSpecification.getInstanceType());
        Assert.assertThat(launchSpecification.getKeyName(), Matchers.not
                (Matchers.isEmptyOrNullString()));
        assertNull(launchSpecification.getKernelId());
        assertEquals(zone.getZoneName(), launchSpecification.getPlacement().getAvailabilityZone());
        assertNull(launchSpecification.getRamdiskId());
        assertTrue(launchSpecification.getMonitoringEnabled());

        List<BlockDeviceMapping> blockDeviceMappings = launchSpecification.getBlockDeviceMappings();
        assertEquals(1, blockDeviceMappings.size());
        BlockDeviceMapping blockDeviceMapping = blockDeviceMappings.get(0);
        assertEquals("/dev/d2", blockDeviceMapping.getDeviceName());
        assertTrue(blockDeviceMapping.getEbs().getDeleteOnTermination());
        assertNull(blockDeviceMapping.getEbs().getSnapshotId());
        assertEquals(1, blockDeviceMapping.getEbs().getVolumeSize(), 0.0);
    }

    private AvailabilityZone getAvailableZone() {
        for (AvailabilityZone zone : ec2.describeAvailabilityZones().getAvailabilityZones()) {
            if (!zone.getZoneName().equals(UNAVAILABLE_ZONE)) {
                return zone;
            }
        }
        return null;
    }

}
