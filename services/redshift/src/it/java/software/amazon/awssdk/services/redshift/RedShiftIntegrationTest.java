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
 * on an "AS IS" BASIS, oUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.redshift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.services.redshift.model.Cluster;
import software.amazon.awssdk.services.redshift.model.CreateClusterRequest;
import software.amazon.awssdk.services.redshift.model.CreateClusterSnapshotRequest;
import software.amazon.awssdk.services.redshift.model.DeleteClusterRequest;
import software.amazon.awssdk.services.redshift.model.DeleteClusterSnapshotRequest;
import software.amazon.awssdk.services.redshift.model.DescribeClusterSnapshotsRequest;
import software.amazon.awssdk.services.redshift.model.DescribeClusterSnapshotsResult;
import software.amazon.awssdk.services.redshift.model.DescribeClustersRequest;
import software.amazon.awssdk.services.redshift.model.DescribeClustersResult;
import software.amazon.awssdk.services.redshift.model.DescribeEventsRequest;
import software.amazon.awssdk.services.redshift.model.DescribeEventsResult;
import software.amazon.awssdk.services.redshift.model.ModifyClusterRequest;
import software.amazon.awssdk.services.redshift.model.RebootClusterRequest;
import software.amazon.awssdk.services.redshift.model.RestoreFromClusterSnapshotRequest;
import software.amazon.awssdk.services.redshift.model.RotateEncryptionKeyRequest;
import software.amazon.awssdk.services.redshift.model.Snapshot;

public class RedShiftIntegrationTest extends IntegrationTestBase {

    private static final String MASTER_USER_NAME = "master";
    private static final String PASSWORD = "Passwork" + System.currentTimeMillis();
    private static final String CLUSTER_ID = "java-cluster-id" + System.currentTimeMillis();
    private static final String NODE_TYPE = "ds1.xlarge";
    private static final String SNAPSHOT_ID = "snapshot" + System.currentTimeMillis();

    /** The identifier used to rename the created cluster. */
    private static final String NEW_CLUSTER_ID = "java-cluster-id-new" + System.currentTimeMillis();

    /** Identifier for the cluster created from the snapshot.*/
    private static final String SNAPSHOT_CLUSTER_ID = "java-cluster-id-snapshot" + System.currentTimeMillis();

    @AfterClass
    public static void tearDown() {
        // Delete Snapshot
        redshift.deleteClusterSnapshot(DeleteClusterSnapshotRequest.builder()
                                               .snapshotIdentifier(SNAPSHOT_ID).build());
        assertEquals(0, redshift.describeClusterSnapshots(DescribeClusterSnapshotsRequest.builder()
                                                                  .clusterIdentifier(SNAPSHOT_ID).build()).snapshots().size());

        redshift.deleteCluster(DeleteClusterRequest.builder()
                                       .clusterIdentifier(NEW_CLUSTER_ID)
                                       .skipFinalClusterSnapshot(true).build());
        redshift.deleteCluster(DeleteClusterRequest.builder()
                                       .clusterIdentifier(SNAPSHOT_CLUSTER_ID)
                                       .skipFinalClusterSnapshot(true).build());
    }

    @Test
    public void testServiceOperations() throws InterruptedException {

        // Create Cluster
        Cluster cluster = redshift.createCluster(CreateClusterRequest.builder()
                                                         .masterUsername(MASTER_USER_NAME)
                                                         .masterUserPassword(PASSWORD)
                                                         .clusterIdentifier(CLUSTER_ID).nodeType(NODE_TYPE)
                                                         .clusterType("Single-Node")
                                                         .encrypted(true).build());
        isValidCluster(cluster, CLUSTER_ID);

        // Describe Clusters
        DescribeClustersResult describeClusterResult = redshift.describeClusters(DescribeClustersRequest.builder().build());
        assertTrue(describeClusterResult.clusters().size() > 0);

        describeClusterResult = redshift.describeClusters(DescribeClustersRequest.builder().clusterIdentifier(CLUSTER_ID).build());
        assertEquals(1, describeClusterResult.clusters().size());
        cluster = describeClusterResult.clusters().get(0);
        isValidCluster(cluster, CLUSTER_ID);

        waitForClusterToBeAvailable(CLUSTER_ID);

        // Create a snapshot
        Snapshot snapshot = redshift.createClusterSnapshot(CreateClusterSnapshotRequest.builder()
                                                                   .clusterIdentifier(CLUSTER_ID)
                                                                   .snapshotIdentifier(SNAPSHOT_ID).build());
        isValidSnapshot(snapshot);

        // Describe snapshots
        DescribeClusterSnapshotsResult describeClusterSnapshotResult =
                redshift.describeClusterSnapshots(DescribeClusterSnapshotsRequest.builder().build());
        assertTrue(describeClusterSnapshotResult.snapshots().size() > 0);
        assertNotNull(describeClusterSnapshotResult.snapshots().get(0).clusterCreateTime());
        assertTrue(describeClusterSnapshotResult.snapshots().get(0).numberOfNodes() > 0);
        assertNotNull(describeClusterSnapshotResult.snapshots().get(0).clusterIdentifier());
        assertNotNull(describeClusterSnapshotResult.snapshots().get(0).snapshotIdentifier());
        assertNotNull(describeClusterSnapshotResult.snapshots().get(0).status());

        describeClusterSnapshotResult =
                redshift.describeClusterSnapshots(DescribeClusterSnapshotsRequest.builder().snapshotIdentifier(SNAPSHOT_ID).build());
        assertEquals(1, describeClusterSnapshotResult.snapshots().size());
        isValidSnapshot(describeClusterSnapshotResult.snapshots().get(0));

        waitForSnapshotToBeAvailable(SNAPSHOT_ID);

        // restoring a new cluster from an existing snapshot.
        Cluster restoredCluster = redshift.restoreFromClusterSnapshot(RestoreFromClusterSnapshotRequest.builder()
                                                                              .snapshotIdentifier(SNAPSHOT_ID)
                                                                              .automatedSnapshotRetentionPeriod(2)
                                                                              .clusterIdentifier(SNAPSHOT_CLUSTER_ID).build());
        isValidCluster(restoredCluster, SNAPSHOT_CLUSTER_ID);
        assertTrue(restoredCluster.automatedSnapshotRetentionPeriod() == 2);

        waitForClusterToBeAvailable(SNAPSHOT_CLUSTER_ID);

        // Rotate encryption key
        redshift.rotateEncryptionKey(RotateEncryptionKeyRequest.builder().clusterIdentifier(CLUSTER_ID).build());

        waitForClusterToBeAvailable(CLUSTER_ID);

        // Reboot the cluster
        cluster = redshift.rebootCluster(RebootClusterRequest.builder().clusterIdentifier(CLUSTER_ID).build());
        isValidCluster(cluster, CLUSTER_ID);

        waitForClusterToBeAvailable(CLUSTER_ID);

        // Modify the cluster identifier and allow version upgrade.
        cluster = redshift.modifyCluster(ModifyClusterRequest.builder()
                                                 .clusterIdentifier(CLUSTER_ID)
                                                 .allowVersionUpgrade(true)
                                                 .newClusterIdentifier(NEW_CLUSTER_ID).build());
        assertTrue(cluster.allowVersionUpgrade());
        assertEquals(cluster.clusterIdentifier(), NEW_CLUSTER_ID);

        waitForClusterToBeAvailable(NEW_CLUSTER_ID);

        // Describe events
        DescribeEventsResult describeEventsResult = redshift.describeEvents(DescribeEventsRequest.builder().build());
        assertTrue(describeEventsResult.events().size() > 0);
        assertNotNull(describeEventsResult.events().get(0).sourceIdentifier());
        assertNotNull(describeEventsResult.events().get(0).date());
        assertNotNull(describeEventsResult.events().get(0).message());
        assertNotNull(describeEventsResult.events().get(0).sourceType());
    }

    private void waitForClusterToBeAvailable(String clusterId) throws InterruptedException {
        int count = 0;
        final int MAX_ITERATION_TIME = 30;
        while (true) {
            DescribeClustersResult describeClusterResult =
                    redshift.describeClusters(DescribeClustersRequest.builder().clusterIdentifier(clusterId).build());
            String status = describeClusterResult.clusters().get(0).clusterStatus();
            System.out.println(status);
            if (status.toLowerCase().equals("available")) {
                Thread.sleep(1000 * 120);
                return;
            }

            Thread.sleep(1000 * 60);
            count++;
            if (count >= MAX_ITERATION_TIME) {
                throw new RuntimeException("Time out exceeds");
            }
        }
    }

    private void waitForSnapshotToBeAvailable(String snapshotId) throws InterruptedException {
        int count = 0;
        final int MAX_ITERATION_TIME = 1800;
        while (true) {
            DescribeClusterSnapshotsResult describeClusterSnapshotResult =
                    redshift.describeClusterSnapshots(DescribeClusterSnapshotsRequest.builder().snapshotIdentifier(snapshotId).build());
            String status = describeClusterSnapshotResult.snapshots().get(0).status();
            System.out.println(status);
            if (status.toLowerCase().equals("available")) {
                return;
            }
            Thread.sleep(60 * 1000);
            count++;
            if (count >= MAX_ITERATION_TIME) {
                throw new RuntimeException("Time out exceeds");
            }
        }
    }

    private void isValidCluster(Cluster cluster, String ClusterId) {
        assertEquals(ClusterId, cluster.clusterIdentifier());
        assertEquals(MASTER_USER_NAME, cluster.masterUsername());
        assertEquals(NODE_TYPE.toLowerCase(), cluster.nodeType().toLowerCase());
        assertEquals(1, cluster.numberOfNodes().intValue());
    }

    private void isValidSnapshot(Snapshot snapshot) {
        assertEquals(CLUSTER_ID, snapshot.clusterIdentifier());
        assertEquals(SNAPSHOT_ID, snapshot.snapshotIdentifier());
        assertEquals(MASTER_USER_NAME, snapshot.masterUsername());
        assertEquals(NODE_TYPE, snapshot.nodeType());
        assertEquals(1, snapshot.numberOfNodes().intValue());
        assertNotNull(snapshot.snapshotCreateTime());
        assertNotNull(snapshot.actualIncrementalBackupSizeInMegaBytes());
        assertNotNull(snapshot.backupProgressInMegaBytes());
        assertNotNull(snapshot.currentBackupRateInMegaBytesPerSecond());
        assertNotNull(snapshot.elapsedTimeInSeconds());
        assertNotNull(snapshot.totalBackupSizeInMegaBytes());
        assertNotNull(snapshot.estimatedSecondsToCompletion());
        assertNotNull(snapshot.ownerAccount());
        assertNotNull(snapshot.accountsWithRestoreAccess());
    }
}
