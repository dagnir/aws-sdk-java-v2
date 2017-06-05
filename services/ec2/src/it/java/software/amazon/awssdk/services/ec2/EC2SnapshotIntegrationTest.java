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
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
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
import software.amazon.awssdk.services.ec2.transform.CopySnapshotRequestMarshaller;
import software.amazon.awssdk.services.ec2.transform.GeneratePreSignUrlRequestHandler;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider;

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
    private static final Region sourceRegion = Region.AP_SOUTHEAST_2;
    /**
     * Availability zone for the encrypted volumes.
     */
    private static final String availabilityZone = "ap-southeast-2a";
    /**
     * Destination regions where the encrypted snapshot is to be copied.
     */
    private static final Region destinationRegion = Region.SA_EAST_1;
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
    private static EC2Client source;
    private static EC2Client destination;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        source = EC2Client.builder()
                          .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                          .region(sourceRegion)
                          .build();

        destination = EC2Client.builder()
                               .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                               .region(destinationRegion)
                               .build();

        volume = createVolume(source, false);
        sourceSnapshot = createSnapshot(volume.volumeId(), source);
        assertEquals(volume.size(), sourceSnapshot.volumeSize());

        encryptedVolume = createVolume(source, true);
        encryptedSourceSnapshot = createSnapshot(encryptedVolume.volumeId(), source);
    }

    /**
     * Ensures any EC2 resources are released after the tests run.
     */
    @AfterClass
    public static void tearDown() throws InterruptedException {
        // Sleep for a few seconds to ensure we can cleanly delete the snapshot we started
        Thread.sleep(WAIT_TIME_MILLIS);
        if (volume != null) {
            deleteVolume(volume.volumeId(), source);
        }
        if (sourceSnapshot != null) {
            deleteSnapshot(sourceSnapshot.snapshotId(), source);
        }
        if (destSnapshot != null) {
            deleteSnapshot(destSnapshot.snapshotId(), destination);
        }

        if (encryptedSourceSnapshot != null) {
            deleteSnapshot(encryptedSourceSnapshot.snapshotId(), source);
        }

        if (encryptedDestinationSnapshot != null) {
            deleteSnapshot(encryptedDestinationSnapshot.snapshotId(), destination);
        }

        if (encryptedDestinationSnapshotSession != null) {
            deleteSnapshot(encryptedDestinationSnapshotSession.snapshotId(), destination);
        }

        if (encryptedVolume != null) {
            deleteVolume(encryptedVolume.volumeId(), source);
        }

    }

    private static Snapshot createSnapshot(String volumeId, EC2Client ec2) throws InterruptedException {
        CreateSnapshotResult result = ec2.createSnapshot(CreateSnapshotRequest.builder().volumeId(volumeId)
                                                                              .description(snapShotDescription).build());
        assertEquals(snapShotDescription, result.snapshot().description());
        waitUntilSnapshotIsReady(result.snapshot().snapshotId(), ec2);
        return result.snapshot();
    }

    private static Volume createVolume(EC2Client ec2, boolean encrypted) throws InterruptedException {
        CreateVolumeResult result = ec2.createVolume(CreateVolumeRequest.builder()
                                                                        .encrypted(encrypted)
                                                                        .size(volumeSizeInGB)
                                                                        .availabilityZone(availabilityZone).build());
        assertEquals(volumeSizeInGB, result.volume().size(), volumeSizeInGB);
        waitUntilVolumeIsCreated(result.volume(), ec2);
        return result.volume();
    }

    private static DescribeSnapshotsResult waitUntilSnapshotIsReady(String snapshotId,
                                                                    EC2Client client) throws InterruptedException {
        DescribeSnapshotsResult result;
        Snapshot snapshot;
        while (true) {
            result = client.describeSnapshots(DescribeSnapshotsRequest.builder()
                                                                      .snapshotIds(snapshotId).build());

            snapshot = result.snapshots().get(0);
            if (snapshot.state().equals("completed")) {
                return result;
            }
            if (snapshot.state().equals("error")) {
                throw new AmazonClientException(
                        "Snapshot is in error state. Snapshot ID : "
                        + snapshotId);
            }
            Thread.sleep(WAIT_TIME_MILLIS);
        }
    }

    private static void waitUntilVolumeIsCreated(Volume volume, EC2Client ec2) throws InterruptedException {
        DescribeVolumesResult result;
        while (true) {
            result = ec2.describeVolumes(DescribeVolumesRequest.builder()
                                                               .volumeIds(volume.volumeId()).build());

            if (result.volumes().get(0).state().equals("available")) {
                break;
            }
            Thread.sleep(WAIT_TIME_MILLIS);
        }
    }

    private static boolean userHasCreateVolumePermission(EC2Client ec2,
                                                         String snapshotId,
                                                         String userId) {
        DescribeSnapshotAttributeRequest.Builder request = DescribeSnapshotAttributeRequest.builder();
        request.snapshotId(snapshotId);
        request.attribute("createVolumePermission");
        DescribeSnapshotAttributeResult result = ec2.describeSnapshotAttribute(request.build());

        List<CreateVolumePermission> createVolumePermissions = result.createVolumePermissions();
        for (CreateVolumePermission permission : createVolumePermissions) {
            if (permission.userId().equals(userId)) {
                return true;
            }
        }

        return false;
    }

    private static void deleteVolume(String volumeId, EC2Client ec2) {
        ec2.deleteVolume(DeleteVolumeRequest.builder().volumeId(volumeId).build());
    }

    /*
     * Individual Tests
     */

    private static void deleteSnapshot(String snapshotId, EC2Client ec2) {
        ec2.deleteSnapshot(DeleteSnapshotRequest.builder().snapshotId(snapshotId).build());
    }

    @Test
    public void copy_unencrypted_snapshot_returns_success() throws
                                                            InterruptedException {
        CopySnapshotResult result = destination.copySnapshot(CopySnapshotRequest.builder()
                                                                                .sourceSnapshotId(sourceSnapshot.snapshotId())
                                                                                .sourceRegion(sourceRegion.value()).build());
        Assert.assertThat(result.snapshotId(), Matchers.not(Matchers.isEmptyOrNullString()));
        Thread.sleep(WAIT_TIME_MILLIS);
        destSnapshot = destination.describeSnapshots(DescribeSnapshotsRequest.builder().snapshotIds(result.snapshotId()).build())
                                  .snapshots().get(0);
        assertEquals(destSnapshot.volumeSize(), sourceSnapshot.volumeSize());
        assertEquals(destSnapshot.ownerId(), sourceSnapshot.ownerId());

    }

    @Test
    public void describe_snapshot_returns_snapshot_created() {
        DescribeSnapshotsRequest.Builder request;

        // Test all (no filters)
        request = DescribeSnapshotsRequest.builder();
        List<Snapshot> snapshots = source.describeSnapshots(request.build())
                                         .snapshots();
        assertTrue(snapshotListContainsSnapshot(snapshots, sourceSnapshot.snapshotId()));

        // Test by specific snapshot ID
        request = DescribeSnapshotsRequest.builder();
        request.snapshotIds(sourceSnapshot.snapshotId());
        snapshots = source.describeSnapshots(request.build()).snapshots();
        assertEquals((Integer) 1, (Integer) snapshots.size());
        assertEquals(sourceSnapshot.snapshotId(), snapshots.get(0).snapshotId());
        assertEquals(volume.size(), snapshots.get(0).volumeSize());

        // Test by owner ID
        request = DescribeSnapshotsRequest.builder();
        request.ownerIds(sourceSnapshot.ownerId());
        snapshots = source.describeSnapshots(request.build()).snapshots();
        assertTrue(snapshotListContainsSnapshot(snapshots, sourceSnapshot.snapshotId()));

        // Test by filters
        request = DescribeSnapshotsRequest.builder();
        request.filters(Filter.builder().name("snapshot-id").values(sourceSnapshot.snapshotId()).build());
        snapshots = source.describeSnapshots(request.build()).snapshots();
        assertEquals((Integer) 1, (Integer) snapshots.size());
        assertEquals(sourceSnapshot.snapshotId(), snapshots.get(0).snapshotId());
        assertEquals(volume.size(), snapshots.get(0).volumeSize());
    }

    @Test
    public void copy_encrypted_snapshot_returns_success() throws InterruptedException {

        assertTrue(encryptedSourceSnapshot.encrypted());

        CopySnapshotResult copyResult = destination
                .copySnapshot(CopySnapshotRequest.builder().sourceRegion(sourceRegion.value())
                                                 .sourceSnapshotId(encryptedSourceSnapshot.snapshotId()).build());

        DescribeSnapshotsResult describeSnapshotResult = waitUntilSnapshotIsReady(
                copyResult.snapshotId(), destination);

        List<Snapshot> snapshots = describeSnapshotResult.snapshots();
        encryptedDestinationSnapshot = snapshots.get(0);
        assertNotNull(snapshots);
        assertTrue(snapshots.size() == 1);
        assertTrue(encryptedDestinationSnapshot.encrypted());
    }

    @Test
    public void copy_encrypted_snapshot_with_session_credentials() throws
                                                                   InterruptedException {
        STSClient stsClient = STSClient.builder()
                                       .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                                       .region(destinationRegion)
                                       .build();
        AwsCredentialsProvider credentialsProvider = StsGetSessionTokenCredentialsProvider.builder()
                                                                                          .stsClient(stsClient)
                                                                                          .build();
        EC2Client ec2WithSTS = EC2Client.builder()
                                        .credentialsProvider(credentialsProvider)
                                        .region(destinationRegion)
                                        .build();

        CopySnapshotResult copyResult = ec2WithSTS.copySnapshot(
                CopySnapshotRequest.builder()
                                   .sourceRegion(sourceRegion.value())
                                   .sourceSnapshotId(encryptedSourceSnapshot.snapshotId())
                                   .build());

        DescribeSnapshotsResult describeSnapshotResult =
                waitUntilSnapshotIsReady(copyResult.snapshotId(), ec2WithSTS);

        List<Snapshot> snapshots = describeSnapshotResult.snapshots();
        encryptedDestinationSnapshotSession = snapshots.get(0);
        assertNotNull(snapshots);
        assertTrue(snapshots.size() == 1);
        assertTrue(encryptedDestinationSnapshotSession.encrypted());

    }

    /**
     * Tests to see if a presign url generation fails for a copy snapshot even when
     * the source region is not present in region xml.
     */
    @Test(expected = AmazonClientException.class)
    public void copy_snapshot_with_fake_region_fails_during_presigned_url_creation() {

        final Request<CopySnapshotRequest> request =
                new CopySnapshotRequestMarshaller().marshall(CopySnapshotRequest.builder()
                                                                                .sourceRegion("fake-region")
                                                                                .sourceSnapshotId("fake-snapshot-id")
                                                                                .destinationRegion("us-east-1")
                                                                                .build());
        final GeneratePreSignUrlRequestHandler handler = new GeneratePreSignUrlRequestHandler();
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
                .snapshotId(), USER_ID));

        ModifySnapshotAttributeRequest.Builder request = ModifySnapshotAttributeRequest.builder();
        request.attribute("createVolumePermission");
        request.snapshotId(sourceSnapshot.snapshotId());
        request.operationType("add");
        request.userIds(USER_ID);
        source.modifySnapshotAttribute(request.build());

        assertTrue(userHasCreateVolumePermission(source, sourceSnapshot
                .snapshotId(), USER_ID));
    }

    /**
     * Tests that we can reset the createVolumePermission snapshot attribute we
     * modified earlier in these tests.
     */
    private void testResetSnapshotAttribute() {
        assertTrue(userHasCreateVolumePermission(source, sourceSnapshot
                .snapshotId(), USER_ID));

        ResetSnapshotAttributeRequest.Builder request = ResetSnapshotAttributeRequest.builder();
        request.snapshotId(sourceSnapshot.snapshotId());
        request.attribute("createVolumePermission");
        source.resetSnapshotAttribute(request.build());

        assertFalse(userHasCreateVolumePermission(source, sourceSnapshot
                .snapshotId(), USER_ID));
    }

    private boolean snapshotListContainsSnapshot(List<Snapshot> snapshots, String expectedId) {
        for (Snapshot snapshot : snapshots) {
            if (snapshot.snapshotId().equals(expectedId)) {
                return true;
            }
        }

        return false;
    }
}
