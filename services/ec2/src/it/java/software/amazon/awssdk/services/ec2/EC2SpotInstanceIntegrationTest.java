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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CancelSpotInstanceRequestsRequest;
import software.amazon.awssdk.services.ec2.model.CancelledSpotInstanceRequest;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.CreateSpotDatafeedSubscriptionRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSpotDatafeedSubscriptionRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotDatafeedSubscriptionRequest;
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
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;

/**
 * Integration tests for Spot Instance APIs in EC2.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class EC2SpotInstanceIntegrationTest extends EC2IntegrationTestBase {

    /** The AMI ID to use in test data - needs to be a real AMI in order for EC2 to accept it. */
    private static final String TEST_AMI_ID = "ami-84db39ed";

    private static final String DEFAULT_GROUP_NAME = "default";
    private static final String UNAVAILABLE_ZONE = "us-east-1a";
    private AvailabilityZone zone;

    /**
     * Tests that we can call the DescribeSpotPriceHistory operation and get
     * back sane results.
     */
    @Test
    public void testDescribeSpotPriceHistory() throws Exception {
        DescribeSpotPriceHistoryResult priceHistoryResult =
                ec2.describeSpotPriceHistory(DescribeSpotPriceHistoryRequest.builder()
                                                                            .productDescriptions("Linux/UNIX", "Windows")
                                                                            .build());

        List<SpotPrice> spotPrices = priceHistoryResult.spotPriceHistory();
        assertTrue(spotPrices.size() > 2);

        assertNotNull(priceHistoryResult.nextToken());

        for (SpotPrice spotPrice : spotPrices) {
            assertNotNull(spotPrice.instanceType());
            Assert.assertThat(spotPrice.productDescription(), Matchers.not(Matchers.isEmptyOrNullString()));
            Assert.assertThat(spotPrice.spotPrice(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertNotNull(spotPrice.availabilityZone());
            assertNotNull(spotPrice.timestamp());
        }
    }

    /** Tests that we can use the pagination features of DescribeSpotPriceHistory. */
    @Test
    public void testDescribeSpotPriceHistory_withPagination() throws Exception {
        DescribeSpotPriceHistoryResult priceHistoryResult =
                ec2.describeSpotPriceHistory(DescribeSpotPriceHistoryRequest.builder()
                                                                            .productDescriptions("Linux/UNIX", "Windows")
                                                                            .maxResults(10).build());

        String nextToken = priceHistoryResult.nextToken();
        String availabilityZone = priceHistoryResult.spotPriceHistory().get(0).availabilityZone();

        assertNotNull(nextToken);
        assertNotNull(availabilityZone);
        Assert.assertFalse(priceHistoryResult.spotPriceHistory().size() > 10);
        ec2.describeSpotPriceHistory(DescribeSpotPriceHistoryRequest.builder()
                                                                    .productDescriptions("Linux/UNIX", "Windows")
                                                                    .nextToken(nextToken)
                                                                    .availabilityZone(availabilityZone).build());
    }

    /**
     * Tests that we can create/describe/cancel requests for spot instances.
     */
    @Test
    public void testSpotInstanceRequests() {
        // Pick any valid, existing key pair to test with
        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs(DescribeKeyPairsRequest.builder().build()).keyPairs();
        String keyName = null;
        if (keyPairs.isEmpty() == false) {
            keyName = keyPairs.get(0).keyName();
        } else {
            keyName = ec2.createKeyPair(CreateKeyPairRequest.builder()
                                                            .keyName("java-integ-" + System.currentTimeMillis())
                                                            .build()).keyPair().keyName();
        }

        BlockDeviceMapping blockDeviceMapping = BlockDeviceMapping.builder()
                                                                  .deviceName("/dev/d2")
                                                                  .ebs(EbsBlockDevice.builder().deleteOnTermination(true)
                                                                                     .volumeSize(1).build()).build();

        zone = getAvailableZone();
        assertNotNull(zone);

        LaunchSpecification launchSpecification = LaunchSpecification.builder()
                                                                     .monitoringEnabled(true)
                                                                     .securityGroups("default")
                                                                     .blockDeviceMappings(blockDeviceMapping)
                                                                     .instanceType(InstanceType.M1Small.toString())
                                                                     .imageId(TEST_AMI_ID)
                                                                     .placement(SpotPlacement.builder()
                                                                                             .availabilityZone(zone.zoneName())
                                                                                             .build())
                                                                     .userData("foobaruserdata")
                                                                     .keyName(keyName).build();

        RequestSpotInstancesResult result = ec2.requestSpotInstances(
                RequestSpotInstancesRequest.builder()
                                           .availabilityZoneGroup("zone-group")
                                           .instanceCount(1)
                                           .launchGroup("launch-group")
                                           .launchSpecification(launchSpecification)
                                           .spotPrice("0.01")
                                           .type(SpotInstanceType.OneTime.toString())
                                           .validFrom(new Date(new Date().getTime() + 1000 * 60 * 60))
                                           .validUntil(new Date(new Date().getTime() + 1000 * 60 * 60 * 24)).build());

        // RequestSpotInstances
        List<SpotInstanceRequest> spotInstanceRequests = result.spotInstanceRequests();
        assertEquals(1, spotInstanceRequests.size());
        SpotInstanceRequest spotInstanceRequest = spotInstanceRequests.get(0);
        String spotInstanceRequestId = spotInstanceRequest.spotInstanceRequestId();
        assertValidSpotInstanceRequest(spotInstanceRequest);
        tagResource(spotInstanceRequestId, TAGS);

        // DescribeSpotInstanceRequests
        List<SpotInstanceRequest> describedSpotInstanceRequests =
                ec2.describeSpotInstanceRequests(DescribeSpotInstanceRequestsRequest.builder()
                                                                                    .spotInstanceRequestIds(spotInstanceRequestId)
                                                                                    .build())
                   .spotInstanceRequests();
        assertEquals(1, describedSpotInstanceRequests.size());
        assertValidSpotInstanceRequest(describedSpotInstanceRequests.get(0));
        assertEqualUnorderedTagLists(TAGS, describedSpotInstanceRequests.get(0).tags());

        // DescribeSpotInstanceRequests with filters
        describedSpotInstanceRequests = ec2.describeSpotInstanceRequests(
                DescribeSpotInstanceRequestsRequest.builder()
                                                   .filters(Filter.builder().name("spot-instance-request-id")
                                                                  .values(spotInstanceRequestId).build()).build())
                                           .spotInstanceRequests();
        assertEquals(1, describedSpotInstanceRequests.size());
        assertValidSpotInstanceRequest(describedSpotInstanceRequests.get(0));

        // CancelSpotInstanceRequests
        List<CancelledSpotInstanceRequest> canceledSpotInstanceRequests =
                ec2.cancelSpotInstanceRequests(CancelSpotInstanceRequestsRequest.builder()
                                                                                .spotInstanceRequestIds(spotInstanceRequestId)
                                                                                .build())
                   .cancelledSpotInstanceRequests();
        assertEquals(1, canceledSpotInstanceRequests.size());
        Assert.assertThat(canceledSpotInstanceRequests.get(0)
                                                      .state(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertEquals(spotInstanceRequestId, canceledSpotInstanceRequests.get(0).spotInstanceRequestId());
    }


    /**
     * Tests that we can create, describe and delete Spot datafeed
     * subscriptions.
     */
    @Test
    public void testSpotDatafeedSubscriptions() throws Exception {
        // Grab the name of a random bucket in our test account to use
        AmazonS3 s3 = AmazonS3Client.builder().withCredentials(new StaticCredentialsProvider(getCredentials())).build();
        String bucketName = s3.listBuckets().get(0).getName();
        String prefix = "my-test-prefix";

        // Create a datafeed subscription
        SpotDatafeedSubscription datafeedSubscription =
                ec2.createSpotDatafeedSubscription(
                        CreateSpotDatafeedSubscriptionRequest.builder()
                                                             .bucket(bucketName)
                                                             .prefix(prefix).build()).spotDatafeedSubscription();
        assertValidSpotDatafeedSubscription(datafeedSubscription, bucketName, prefix);

        // Wait a few seconds for eventual consistency
        Thread.sleep(1000 * 5);

        // Describe Spot Datafeed Subscriptions
        assertValidSpotDatafeedSubscription(
                ec2.describeSpotDatafeedSubscription(DescribeSpotDatafeedSubscriptionRequest.builder().build())
                   .spotDatafeedSubscription(),
                bucketName, prefix);

        // Delete a Spot Datafeed Subscription
        ec2.deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscriptionRequest.builder().build());
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
        assertEquals(bucketName, datafeedSubscription.bucket());
        assertNull(datafeedSubscription.fault());
        Assert.assertThat(datafeedSubscription.ownerId(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertEquals(prefix, datafeedSubscription.prefix());
        Assert.assertThat(datafeedSubscription.state(), Matchers.not(Matchers.isEmptyOrNullString()));
    }

    /**
     * Asserts that the specified SpotInstanceRequest is valid, and that the
     * data matches up to what we requested earlier in this test.
     *
     * @param spotInstanceRequest
     *            The spot instance request to test.
     */
    private void assertValidSpotInstanceRequest(SpotInstanceRequest spotInstanceRequest) {
        assertEquals("zone-group", spotInstanceRequest.availabilityZoneGroup());
        assertRecent(spotInstanceRequest.createTime());
        assertNull(spotInstanceRequest.fault());
        assertNull(spotInstanceRequest.instanceId());

        assertEquals("launch-group", spotInstanceRequest.launchGroup());
        Assert.assertThat(spotInstanceRequest.productDescription(), Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(spotInstanceRequest.spotInstanceRequestId(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertEquals(Double.parseDouble("0.01"), Double.parseDouble(spotInstanceRequest.spotPrice()), .001);
        Assert.assertThat(spotInstanceRequest.state(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertEquals(SpotInstanceType.OneTime.toString(), spotInstanceRequest.type());
        assertRecent(spotInstanceRequest.validFrom());
        assertRecent(spotInstanceRequest.validUntil());
        assertNotNull(spotInstanceRequest.status());
        assertNotNull(spotInstanceRequest.status().code());
        assertNotNull(spotInstanceRequest.status().message());
        assertNotNull(spotInstanceRequest.status().updateTime());

        LaunchSpecification launchSpecification = spotInstanceRequest.launchSpecification();
        assertNull(launchSpecification.addressingType());
        assertEquals(1, launchSpecification.securityGroups().size());
        // TODO: this needs to change when we sort out the security group question
        assertEquals(DEFAULT_GROUP_NAME, launchSpecification.securityGroups().get(0));
        assertEquals(TEST_AMI_ID, launchSpecification.imageId());
        assertEquals(InstanceType.M1Small.toString(), launchSpecification.instanceType());
        Assert.assertThat(launchSpecification.keyName(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertNull(launchSpecification.kernelId());
        assertEquals(zone.zoneName(), launchSpecification.placement().availabilityZone());
        assertNull(launchSpecification.ramdiskId());
        assertTrue(launchSpecification.monitoringEnabled());

        List<BlockDeviceMapping> blockDeviceMappings = launchSpecification.blockDeviceMappings();
        assertEquals(1, blockDeviceMappings.size());
        BlockDeviceMapping blockDeviceMapping = blockDeviceMappings.get(0);
        assertEquals("/dev/d2", blockDeviceMapping.deviceName());
        assertTrue(blockDeviceMapping.ebs().deleteOnTermination());
        assertNull(blockDeviceMapping.ebs().snapshotId());
        assertEquals(1, blockDeviceMapping.ebs().volumeSize(), 0.0);
    }

    private AvailabilityZone getAvailableZone() {
        for (AvailabilityZone zone : ec2.describeAvailabilityZones(DescribeAvailabilityZonesRequest.builder().build())
                                        .availabilityZones()) {
            if (!zone.zoneName().equals(UNAVAILABLE_ZONE)) {
                return zone;
            }
        }
        return null;
    }

}
