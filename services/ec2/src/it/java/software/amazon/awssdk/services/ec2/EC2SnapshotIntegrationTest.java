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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.ec2.model.CopySnapshotRequest;
import software.amazon.awssdk.services.ec2.model.CopySnapshotResult;
import software.amazon.awssdk.services.ec2.model.CreateSnapshotRequest;
import software.amazon.awssdk.services.ec2.model.CreateSnapshotResult;
import software.amazon.awssdk.services.ec2.model.CreateVolumePermission;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResult;
import software.amazon.awssdk.services.ec2.model.DeleteSnapshotRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotAttributeResult;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsResult;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ModifySnapshotAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ResetSnapshotAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.transform.CopySnapshotRequestMarshaller;
import software.amazon.awssdk.services.ec2.model.transform.GeneratePreSignUrlRequestHandler;
import software.amazon.awssdk.services.securitytoken.auth.STSSessionCredentialsProvider;

/**
 * Integration tests for the EC2 snapshot operations.
 *
 * @author fulghum@amazon.com
 */
public class EC2SnapshotIntegrationTest extends EC2IntegrationTestBase {

    /**
     * Test user account to grant createVolumePermission
     */
    private static final String USER_ID = "960747153117";
    /**
     * Source regions where the volume and source snapshot resides.
     */
    private static final Regions sourceRegion = Regions.AP_SOUTHEAST_2;
    /**
     * Availability zone for the encrypted volumes.
     */
    private static final String availabilityZone = "ap-southeast-2a";
    /**
     * Destination regions where the encrypted snapshot is to be copied.
     */
    private static final Regions destinationRegion = Regions.SA_EAST_1;
    /**
     * Size of the encrypted volume
     */
    private static final int volumeSizeInGB = 1;
    /**
     * Description for the encrypted snapshot
     */
    private static final String snapShotDescription = "test-copy-snapshot-" + System.currentTimeMillis();

    private static final int WAIT_TIME_MILLIS = 30000;
    /**
     * Volume created by tests
     */
    private static Volume volume;
    /**
     * Snapshot created by tests
     */
    private static Snapshot sourceSnapshot;
    /**
     * Snapshot copied from the source snapshot
     */
    private static Snapshot destSnapshot;
    /**
     * Encrypted volume created for tests.
     */
    private static Volume encryptedVolume;
    /**
     * Encrypted snapshot to be copied.
     */
    private static Snapshot encryptedSourceSnapshot;
    /**
     * Encrypted snapshot in the destination copied from source.
     */
    private static Snapshot encryptedDestinationSnapshot;
    /**
     * Encrypted snapshot in the destination copied from source using session credentials.
     */
    private static Snapshot encryptedDestinationSnapshotSession;
    private static AmazonEC2Client source;
    private static AmazonEC2Client destination;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        source = new AmazonEC2Client(getCredentials());
        source.configureRegion(sourceRegion);
        destination = new AmazonEC2Client(getCredentials());
        destination.configureRegion(destinationRegion);

        volume = createVolume(source, false);
        sourceSnapshot = createSnapshot(volume.getVolumeId(), source);
        assertEquals(volume.getSize(), sourceSnapshot.getVolumeSize());

        encryptedVolume = createVolume(source, true);
        encryptedSourceSnapshot = createSnapshot(encryptedVolume.getVolumeId(), source);
    }

    /**
     * Ensures any EC2 resources are released after the tests run.
     */
    @AfterClass
    public static void tearDown() throws InterruptedException {
        // Sleep for a few seconds to ensure we can cleanly delete the snapshot we started
        Thread.sleep(WAIT_TIME_MILLIS);
        if (volume != null) {
            deleteVolume(volume.getVolumeId(), source);
        }
        if (sourceSnapshot != null) {
            deleteSnapshot(sourceSnapshot.getSnapshotId(), source);
        }
        if (destSnapshot != null) {
            deleteSnapshot(destSnapshot.getSnapshotId(), destination);
        }

        if (encryptedSourceSnapshot != null) {
            deleteSnapshot(encryptedSourceSnapshot.getSnapshotId(), source);
        }

        if (encryptedDestinationSnapshot != null) {
            deleteSnapshot(encryptedDestinationSnapshot.getSnapshotId(), destination);
        }

        if (encryptedDestinationSnapshotSession != null) {
            deleteSnapshot(encryptedDestinationSnapshotSession.getSnapshotId(), destination);
        }

        if (encryptedVolume != null) {
            deleteVolume(encryptedVolume.getVolumeId(), source);
        }

    }

    private static Snapshot createSnapshot(String volumeId, AmazonEC2 ec2) throws InterruptedException {
        CreateSnapshotResult result = ec2.createSnapshot(new
                                                                 CreateSnapshotRequest().withVolumeId(volumeId)
                                                                                        .withDescription(snapShotDescription));
        assertEquals(snapShotDescription, result.getSnapshot().getDescription());
        waitUntilSnapshotIsReady(result.getSnapshot().getSnapshotId(), ec2);
        return result.getSnapshot();
    }

    private static Volume createVolume(AmazonEC2 ec2, boolean encrypted) throws InterruptedException {
        CreateVolumeResult result = ec2.createVolume(new CreateVolumeRequest()
                                                             .withEncrypted(encrypted)
                                                             .withSize(volumeSizeInGB)
                                                             .withAvailabilityZone(availabilityZone));
        assertEquals(volumeSizeInGB, result.getVolume().getSize(), volumeSizeInGB);
        waitUntilVolumeIsCreated(result.getVolume(), ec2);
        return result.getVolume();
    }

    private static DescribeSnapshotsResult waitUntilSnapshotIsReady(String snapshotId,
                                                                    AmazonEC2 client) throws InterruptedException {
        DescribeSnapshotsResult result = null;
        Snapshot snapshot;
        while (true) {
            result = client.describeSnapshots(new DescribeSnapshotsRequest()
                                                      .withSnapshotIds(snapshotId));

            snapshot = result.getSnapshots().get(0);
            if (snapshot.getState().equals("completed")) {
                return result;
            }
            if (snapshot.getState().equals("error")) {
                throw new AmazonClientException(
                        "Snapshot is in error state. Snapshot ID : "
                        + snapshotId);
            }
            Thread.sleep(WAIT_TIME_MILLIS);
        }
    }

    private static void waitUntilVolumeIsCreated(Volume volume, AmazonEC2 ec2) throws InterruptedException {
        DescribeVolumesResult result = null;
        while (true) {
            result = ec2.describeVolumes(new DescribeVolumesRequest()
                                                 .withVolumeIds(volume.getVolumeId()));

            if (result.getVolumes().get(0).getState().equals("available")) {
                break;
            }
            Thread.sleep(WAIT_TIME_MILLIS);
        }
    }

    private static boolean userHasCreateVolumePermission(AmazonEC2 ec2,
                                                         String snapshotId,
                                                         String userId) {
        DescribeSnapshotAttributeRequest request = new DescribeSnapshotAttributeRequest();
        request.setSnapshotId(snapshotId);
        request.setAttribute("createVolumePermission");
        DescribeSnapshotAttributeResult result = ec2.describeSnapshotAttribute(request);

        List<CreateVolumePermission> createVolumePermissions = result.getCreateVolumePermissions();
        for (CreateVolumePermission permission : createVolumePermissions) {
            if (permission.getUserId().equals(userId)) {
                return true;
            }
        }

        return false;
    }

    private static void deleteVolume(String volumeId, AmazonEC2 ec2) {
        ec2.deleteVolume(new DeleteVolumeRequest().withVolumeId(volumeId));
    }

    /*
     * Individual Tests
     */

    private static void deleteSnapshot(String snapshotId, AmazonEC2 ec2) {
        ec2.deleteSnapshot(new DeleteSnapshotRequest().withSnapshotId(snapshotId));
    }

    @Test
    public void copy_unencrypted_snapshot_returns_success() throws
                                                            InterruptedException {
        CopySnapshotResult result = destination.copySnapshot(new
                                                                     CopySnapshotRequest().withSourceSnapshotId(sourceSnapshot.getSnapshotId())
                                                                                          .withSourceRegion(sourceRegion.getName()));
        Assert.assertThat(result.getSnapshotId(), Matchers.not(Matchers.isEmptyOrNullString()));
        Thread.sleep(WAIT_TIME_MILLIS);
        destSnapshot = destination.describeSnapshots(new
                                                             DescribeSnapshotsRequest().withSnapshotIds(result.getSnapshotId())).getSnapshots().get(0);
        assertEquals(destSnapshot.getVolumeSize(), sourceSnapshot.getVolumeSize());
        assertEquals(destSnapshot.getOwnerId(), sourceSnapshot.getOwnerId());

    }

    @Test
    public void describe_snapshot_returns_snapshot_created() {
        DescribeSnapshotsRequest request;

        // Test all (no filters)
        request = new DescribeSnapshotsRequest();
        List<Snapshot> snapshots = source.describeSnapshots(request)
                                         .getSnapshots();
        assertTrue(snapshotListContainsSnapshot(snapshots, sourceSnapshot.getSnapshotId()));

        // Test by specific snapshot ID
        request = new DescribeSnapshotsRequest();
        request.withSnapshotIds(sourceSnapshot.getSnapshotId());
        snapshots = source.describeSnapshots(request).getSnapshots();
        assertEquals((Integer) 1, (Integer) snapshots.size());
        assertEquals(sourceSnapshot.getSnapshotId(), snapshots.get(0).getSnapshotId());
        assertEquals(volume.getSize(), snapshots.get(0).getVolumeSize());

        // Test by owner ID
        request = new DescribeSnapshotsRequest();
        request.withOwnerIds(sourceSnapshot.getOwnerId());
        snapshots = source.describeSnapshots(request).getSnapshots();
        assertTrue(snapshotListContainsSnapshot(snapshots, sourceSnapshot.getSnapshotId()));

        // Test by filters
        request = new DescribeSnapshotsRequest();
        request.withFilters(new Filter("snapshot-id", null).withValues(sourceSnapshot.getSnapshotId()));
        snapshots = source.describeSnapshots(request).getSnapshots();
        assertEquals((Integer) 1, (Integer) snapshots.size());
        assertEquals(sourceSnapshot.getSnapshotId(), snapshots.get(0).getSnapshotId());
        assertEquals(volume.getSize(), snapshots.get(0).getVolumeSize());
    }

    @Test
    public void copy_encrypted_snapshot_returns_success() throws InterruptedException {

        assertTrue(encryptedSourceSnapshot.isEncrypted());

        CopySnapshotResult copyResult = destination
                .copySnapshot(new CopySnapshotRequest().withSourceRegion(
                        sourceRegion.getName()).withSourceSnapshotId(
                        encryptedSourceSnapshot.getSnapshotId()));

        DescribeSnapshotsResult describeSnapshotResult = waitUntilSnapshotIsReady(
                copyResult.getSnapshotId(), destination);

        List<Snapshot> snapshots = describeSnapshotResult.getSnapshots();
        encryptedDestinationSnapshot = snapshots.get(0);
        assertNotNull(snapshots);
        assertTrue(snapshots.size() == 1);
        assertTrue(encryptedDestinationSnapshot.isEncrypted());
    }

    @Test
    public void copy_encrypted_snapshot_with_session_credentials() throws
                                                                   InterruptedException {
        AmazonEC2Client ec2WithSTS = new AmazonEC2Client(new
                                                                 STSSessionCredentialsProvider(getCredentials()));
        ec2WithSTS.configureRegion(destinationRegion);

        CopySnapshotResult copyResult = ec2WithSTS
                .copySnapshot(new CopySnapshotRequest().withSourceRegion(
                        sourceRegion.getName()).withSourceSnapshotId(
                        encryptedSourceSnapshot.getSnapshotId()));

        DescribeSnapshotsResult describeSnapshotResult =
                waitUntilSnapshotIsReady(copyResult.getSnapshotId(), ec2WithSTS);

        List<Snapshot> snapshots = describeSnapshotResult.getSnapshots();
        encryptedDestinationSnapshotSession = snapshots.get(0);
        assertNotNull(snapshots);
        assertTrue(snapshots.size() == 1);
        assertTrue(encryptedDestinationSnapshotSession.isEncrypted());

    }

    /**
     * Tests to see if a presign url generation fails for a copy snapshot even when
     * the source region is not present in region xml.
     */
    @Test(expected = AmazonClientException.class)
    public void copy_snapshot_with_fake_region_fails_during_presigned_url_creation() {

        final Request<CopySnapshotRequest> request = new CopySnapshotRequestMarshaller()
                .marshall(new CopySnapshotRequest()
                                  .withSourceRegion("fake-region")
                                  .withSourceSnapshotId("fake-snapshot-id")
                                  .withDestinationRegion("us-east-1"));
        final GeneratePreSignUrlRequestHandler handler = new GeneratePreSignUrlRequestHandler();
        handler.setCredentials(getCredentials());
        handler.beforeRequest(request);
    }

    @Test
    public void modify_reset_snapshot_attributes() {
        testModifySnapshotAttribute();
        testResetSnapshotAttribute();
    }

    /**
     * Tests that we can modify the createVolumePermission snapshot attribute
     * for the snapshot we created earlier in these tests.
     */
    private void testModifySnapshotAttribute() {
        assertFalse(userHasCreateVolumePermission(source, sourceSnapshot
                .getSnapshotId(), USER_ID));

        ModifySnapshotAttributeRequest request = new ModifySnapshotAttributeRequest();
        request.setAttribute("createVolumePermission");
        request.setSnapshotId(sourceSnapshot.getSnapshotId());
        request.setOperationType("add");
        request.withUserIds(USER_ID);
        source.modifySnapshotAttribute(request);

        assertTrue(userHasCreateVolumePermission(source, sourceSnapshot
                .getSnapshotId(), USER_ID));
    }

    /**
     * Tests that we can reset the createVolumePermission snapshot attribute we
     * modified earlier in these tests.
     */
    private void testResetSnapshotAttribute() {
        assertTrue(userHasCreateVolumePermission(source, sourceSnapshot
                .getSnapshotId(), USER_ID));

        ResetSnapshotAttributeRequest request = new ResetSnapshotAttributeRequest();
        request.setSnapshotId(sourceSnapshot.getSnapshotId());
        request.setAttribute("createVolumePermission");
        source.resetSnapshotAttribute(request);

        assertFalse(userHasCreateVolumePermission(source, sourceSnapshot
                .getSnapshotId(), USER_ID));
    }

    private boolean snapshotListContainsSnapshot(List<Snapshot> snapshots, String expectedId) {
        for (Snapshot snapshot : snapshots) {
            if (snapshot.getSnapshotId().equals(expectedId)) {
                return true;
            }
        }

        return false;
    }
}
