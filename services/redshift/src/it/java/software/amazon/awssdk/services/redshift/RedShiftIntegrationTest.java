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
        redshift.deleteClusterSnapshot(new DeleteClusterSnapshotRequest()
                                               .withSnapshotIdentifier(SNAPSHOT_ID));
        assertEquals(0, redshift.describeClusterSnapshots(new DescribeClusterSnapshotsRequest()
                                                                  .withClusterIdentifier(SNAPSHOT_ID)).getSnapshots().size());

        redshift.deleteCluster(new DeleteClusterRequest()
                                       .withClusterIdentifier(NEW_CLUSTER_ID)
                                       .withSkipFinalClusterSnapshot(true));
        redshift.deleteCluster(new DeleteClusterRequest()
                                       .withClusterIdentifier(SNAPSHOT_CLUSTER_ID)
                                       .withSkipFinalClusterSnapshot(true));
    }

    @Test
    public void testServiceOperations() throws InterruptedException {

        // Create Cluster
        Cluster cluster = redshift.createCluster(new CreateClusterRequest()
                                                         .withMasterUsername(MASTER_USER_NAME)
                                                         .withMasterUserPassword(PASSWORD)
                                                         .withClusterIdentifier(CLUSTER_ID).withNodeType(NODE_TYPE)
                                                         .withClusterType("Single-Node")
                                                         .withEncrypted(true));
        isValidCluster(cluster, CLUSTER_ID);

        // Describe Clusters
        DescribeClustersResult describeClusterResult = redshift.describeClusters();
        assertTrue(describeClusterResult.getClusters().size() > 0);

        describeClusterResult = redshift.describeClusters(new DescribeClustersRequest().withClusterIdentifier(CLUSTER_ID));
        assertEquals(1, describeClusterResult.getClusters().size());
        cluster = describeClusterResult.getClusters().get(0);
        isValidCluster(cluster, CLUSTER_ID);

        waitForClusterToBeAvailable(CLUSTER_ID);

        // Create a snapshot
        Snapshot snapshot = redshift.createClusterSnapshot(new CreateClusterSnapshotRequest()
                                                                   .withClusterIdentifier(CLUSTER_ID)
                                                                   .withSnapshotIdentifier(SNAPSHOT_ID));
        isValidSnapshot(snapshot);

        // Describe snapshots
        DescribeClusterSnapshotsResult describeClusterSnapshotResult = redshift.describeClusterSnapshots();
        assertTrue(describeClusterSnapshotResult.getSnapshots().size() > 0);
        assertNotNull(describeClusterSnapshotResult.getSnapshots().get(0).getClusterCreateTime());
        assertTrue(describeClusterSnapshotResult.getSnapshots().get(0).getNumberOfNodes() > 0);
        assertNotNull(describeClusterSnapshotResult.getSnapshots().get(0).getClusterIdentifier());
        assertNotNull(describeClusterSnapshotResult.getSnapshots().get(0).getSnapshotIdentifier());
        assertNotNull(describeClusterSnapshotResult.getSnapshots().get(0).getStatus());

        describeClusterSnapshotResult = redshift.describeClusterSnapshots(new DescribeClusterSnapshotsRequest().withSnapshotIdentifier(SNAPSHOT_ID));
        assertEquals(1, describeClusterSnapshotResult.getSnapshots().size());
        isValidSnapshot(describeClusterSnapshotResult.getSnapshots().get(0));

        waitForSnapshotToBeAvailable(SNAPSHOT_ID);

        // restoring a new cluster from an existing snapshot.
        Cluster restoredCluster = redshift.restoreFromClusterSnapshot(new RestoreFromClusterSnapshotRequest()
                                                                              .withSnapshotIdentifier(SNAPSHOT_ID)
                                                                              .withAutomatedSnapshotRetentionPeriod(2)
                                                                              .withClusterIdentifier(SNAPSHOT_CLUSTER_ID));
        isValidCluster(restoredCluster, SNAPSHOT_CLUSTER_ID);
        assertTrue(restoredCluster.getAutomatedSnapshotRetentionPeriod() == 2);

        waitForClusterToBeAvailable(SNAPSHOT_CLUSTER_ID);

        // Rotate encryption key
        redshift.rotateEncryptionKey(new RotateEncryptionKeyRequest().withClusterIdentifier(CLUSTER_ID));

        waitForClusterToBeAvailable(CLUSTER_ID);

        // Reboot the cluster
        cluster = redshift.rebootCluster(new RebootClusterRequest().withClusterIdentifier(CLUSTER_ID));
        isValidCluster(cluster, CLUSTER_ID);

        waitForClusterToBeAvailable(CLUSTER_ID);

        // Modify the cluster identifier and allow version upgrade.
        cluster = redshift.modifyCluster(new ModifyClusterRequest()
                                                 .withClusterIdentifier(CLUSTER_ID)
                                                 .withAllowVersionUpgrade(true)
                                                 .withNewClusterIdentifier(NEW_CLUSTER_ID));
        assertTrue(cluster.getAllowVersionUpgrade());
        assertEquals(cluster.getClusterIdentifier(), NEW_CLUSTER_ID);

        waitForClusterToBeAvailable(NEW_CLUSTER_ID);

        // Describe events
        DescribeEventsResult describeEventsResult = redshift.describeEvents();
        assertTrue(describeEventsResult.getEvents().size() > 0);
        assertNotNull(describeEventsResult.getEvents().get(0).getSourceIdentifier());
        assertNotNull(describeEventsResult.getEvents().get(0).getDate());
        assertNotNull(describeEventsResult.getEvents().get(0).getMessage());
        assertNotNull(describeEventsResult.getEvents().get(0).getSourceType());
    }

    private void waitForClusterToBeAvailable(String clusterId) throws InterruptedException {
        int count = 0;
        final int MAX_ITERATION_TIME = 30;
        while (true) {
            DescribeClustersResult describeClusterResult = redshift.describeClusters(new DescribeClustersRequest().withClusterIdentifier(clusterId));
            String status = describeClusterResult.getClusters().get(0).getClusterStatus();
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
            DescribeClusterSnapshotsResult describeClusterSnapshotResult = redshift.describeClusterSnapshots(new DescribeClusterSnapshotsRequest().withSnapshotIdentifier(snapshotId));
            String status = describeClusterSnapshotResult.getSnapshots().get(0).getStatus();
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
        assertEquals(ClusterId, cluster.getClusterIdentifier());
        assertEquals(MASTER_USER_NAME, cluster.getMasterUsername());
        assertEquals(NODE_TYPE.toLowerCase(), cluster.getNodeType().toLowerCase());
        assertEquals(1, cluster.getNumberOfNodes().intValue());
    }

    private void isValidSnapshot(Snapshot snapshot) {
        assertEquals(CLUSTER_ID, snapshot.getClusterIdentifier());
        assertEquals(SNAPSHOT_ID, snapshot.getSnapshotIdentifier());
        assertEquals(MASTER_USER_NAME, snapshot.getMasterUsername());
        assertEquals(NODE_TYPE, snapshot.getNodeType());
        assertEquals(1, snapshot.getNumberOfNodes().intValue());
        assertNotNull(snapshot.getSnapshotCreateTime());
        assertNotNull(snapshot.getActualIncrementalBackupSizeInMegaBytes());
        assertNotNull(snapshot.getBackupProgressInMegaBytes());
        assertNotNull(snapshot.getCurrentBackupRateInMegaBytesPerSecond());
        assertNotNull(snapshot.getElapsedTimeInSeconds());
        assertNotNull(snapshot.getTotalBackupSizeInMegaBytes());
        assertNotNull(snapshot.getEstimatedSecondsToCompletion());
        assertNotNull(snapshot.getOwnerAccount());
        assertNotNull(snapshot.getAccountsWithRestoreAccess());
    }
}
