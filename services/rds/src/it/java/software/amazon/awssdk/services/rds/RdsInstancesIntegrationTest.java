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

package software.amazon.awssdk.services.rds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.rds.model.CreateDBInstanceReadReplicaRequest;
import software.amazon.awssdk.services.rds.model.CreateDBInstanceRequest;
import software.amazon.awssdk.services.rds.model.CreateDBSnapshotRequest;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DBInstanceStatusInfo;
import software.amazon.awssdk.services.rds.model.DBSnapshot;
import software.amazon.awssdk.services.rds.model.DeleteDBInstanceRequest;
import software.amazon.awssdk.services.rds.model.DeleteDBSnapshotRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBLogFilesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBLogFilesResult;
import software.amazon.awssdk.services.rds.model.DescribeDBSnapshotsRequest;
import software.amazon.awssdk.services.rds.model.DescribeEventsRequest;
import software.amazon.awssdk.services.rds.model.DownloadDBLogFilePortionRequest;
import software.amazon.awssdk.services.rds.model.DownloadDBLogFilePortionResult;
import software.amazon.awssdk.services.rds.model.Event;
import software.amazon.awssdk.services.rds.model.ModifyDBInstanceRequest;
import software.amazon.awssdk.services.rds.model.PromoteReadReplicaRequest;
import software.amazon.awssdk.services.rds.model.RebootDBInstanceRequest;
import software.amazon.awssdk.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;
import software.amazon.awssdk.services.rds.model.RestoreDBInstanceToPointInTimeRequest;

/**
 * Integration tests for RDS DB instance operations.
 * <p>
 * These tests require a running database instance in order to execute all the
 * operations, so they take quite a while to run while starting up instances,
 * waiting for them to become available, restoring from snapshots, etc.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class RdsInstancesIntegrationTest extends IntegrationTestBase {

    private static final String DB_INSTANCE_CLASS = "db.m1.small";
    private static final int PORT = 1234;


    /**
     * Tests the RDS operations that require a running instance, including
     * snapshot operations, restore operations, db events, and creating,
     * describing, deleting database instances.
     */
    @Test
    public void testDBInstanceOperations() throws Exception {
        Date startTime = new Date();
        String databaseInstanceName = "java-integ-test-db-" + System.currentTimeMillis();
        String readReplicaInstanceName = "java-integ-read-replica-db-" + System.currentTimeMillis();
        databaseInstancesToRelease.add(databaseInstanceName);
        databaseInstancesToRelease.add(readReplicaInstanceName);

        // Create a DB
        DBInstance createdInstance = rds.createDBInstance(
                CreateDBInstanceRequest.builder()
                                       .allocatedStorage(5)
                                       .dbInstanceClass(DB_INSTANCE_CLASS)
                                       .dbInstanceIdentifier(databaseInstanceName)
                                       .engine(ENGINE)
                                       .dbName("integtestdb")
                                       .masterUsername("admin")
                                       .masterUserPassword("password-with-at-least-8-chars")
                                       .multiAZ(true)
                                       .port(PORT)
                                       .publiclyAccessible(true)
                                       .licenseModel("general-public-license").build());
        assertValidDbInstance(createdInstance);

        waitForDbInstanceToTransitionToState(databaseInstanceName, "available");

        // Describe DB log files
        DescribeDBLogFilesResult describeDBLogFilesResult =
                rds.describeDBLogFiles(DescribeDBLogFilesRequest.builder().dbInstanceIdentifier(databaseInstanceName).build());
        assertNotNull(describeDBLogFilesResult.describeDBLogFiles().size());
        assertNotNull(describeDBLogFilesResult.describeDBLogFiles().get(0).lastWritten());
        String logFileName = describeDBLogFilesResult.describeDBLogFiles().get(0).logFileName();
        assertNotNull(logFileName);
        assertNotNull(describeDBLogFilesResult.describeDBLogFiles().get(0).size());

        // Download DB log portion
        DownloadDBLogFilePortionResult downloadDBLogFilePortionResult = rds.downloadDBLogFilePortion(
                DownloadDBLogFilePortionRequest.builder().dbInstanceIdentifier(databaseInstanceName).logFileName(logFileName)
                                               .build());
        assertNotNull(downloadDBLogFilePortionResult.logFileData());
        assertNotNull(downloadDBLogFilePortionResult.additionalDataPending());

        // Create a Read Replica
        DBInstance createdReadReplicaInstance = rds.createDBInstanceReadReplica(
                CreateDBInstanceReadReplicaRequest.builder().dbInstanceIdentifier(readReplicaInstanceName)
                                                  .sourceDBInstanceIdentifier(databaseInstanceName)
                                                  .autoMinorVersionUpgrade(true)
                                                  .dbInstanceClass(DB_INSTANCE_CLASS)
                                                  .port(PORT).build());
        assertValidDbInstance(createdReadReplicaInstance);

        // Describe our read replica DB
        waitForDbInstanceToTransitionToState(readReplicaInstanceName, "available");
        Thread.sleep(120 * 1000);
        List<DBInstance> dbInstances = rds.describeDBInstances(DescribeDBInstancesRequest.builder()
                                                                                         .dbInstanceIdentifier(
                                                                                                 readReplicaInstanceName)
                                                                                         .maxRecords(20).build()).dbInstances();
        assertEquals(1, dbInstances.size());
        assertValidDbInstance(dbInstances.get(0));
        assertNotEmpty(dbInstances.get(0).readReplicaSourceDBInstanceIdentifier());

        assertEquals(0, dbInstances.get(0).readReplicaDBInstanceIdentifiers().size());
        assertEquals(dbInstances.get(0).publiclyAccessible(), true);
        assertTrue(dbInstances.get(0).statusInfos().size() > 0);

        DBInstanceStatusInfo replicationStatus = null;
        for (int i = 0; i < dbInstances.get(0).statusInfos().size(); i++) {
            if (dbInstances.get(0).statusInfos().get(i).statusType()
                           .equals("read replication")) {
                replicationStatus = dbInstances.get(0).statusInfos().get(i);
                break;
            }
        }
        assertNotNull("Could not find a Read Replication status.", replicationStatus);
        assertEquals(replicationStatus.status(), "replicating");
        assertTrue(replicationStatus.normal());
        assertTrue(replicationStatus.message() == null || replicationStatus.message().isEmpty());

        // Describe our master DB
        dbInstances = rds.describeDBInstances(
                DescribeDBInstancesRequest.builder()
                                          .dbInstanceIdentifier(databaseInstanceName)
                                          .maxRecords(20).build()).dbInstances();
        assertEquals(1, dbInstances.size());
        assertValidDbInstance(dbInstances.get(0));
        assertTrue(dbInstances.get(0).multiAZ());
        assertEquals(1, dbInstances.get(0).readReplicaDBInstanceIdentifiers().size());
        assertEquals(readReplicaInstanceName, dbInstances.get(0).readReplicaDBInstanceIdentifiers().get(0));
        assertNull(dbInstances.get(0).readReplicaSourceDBInstanceIdentifier());
        assertEquals(dbInstances.get(0).publiclyAccessible(), true);//


        // Modify it
        waitForDbInstanceToTransitionToState(databaseInstanceName, "available");
        DBInstance modifiedInstance = rds.modifyDBInstance(
                ModifyDBInstanceRequest.builder()
                                       .dbInstanceIdentifier(databaseInstanceName)
                                       .allocatedStorage(6)
                                       .multiAZ(true)
                                       .masterUserPassword("password-with-at-least-8-chars").build());
        assertValidDbInstance(modifiedInstance);


        // Reboot it
        DBInstance rebootedInstance = rds.rebootDBInstance(
                RebootDBInstanceRequest.builder()
                                       .dbInstanceIdentifier(databaseInstanceName).build());
        assertValidDbInstance(rebootedInstance);


        // Create a snapshot
        waitForDbInstanceToTransitionToState(databaseInstanceName, "available");
        snapshotIdentifier = "java-integ-test-snapshot-" + new Date().getTime();
        DBSnapshot createdSnapshot = rds.createDBSnapshot(
                CreateDBSnapshotRequest.builder()
                                       .dbSnapshotIdentifier(snapshotIdentifier)
                                       .dbInstanceIdentifier(databaseInstanceName).build());
        assertValidSnapshot(createdSnapshot);


        // Describe our snapshot
        List<DBSnapshot> dbSnapshots = rds.describeDBSnapshots(
                DescribeDBSnapshotsRequest.builder()
                                          .dbSnapshotIdentifier(snapshotIdentifier).build()).dbSnapshots();
        assertEquals(1, dbSnapshots.size());
        assertValidSnapshot(dbSnapshots.get(0));

        // Copy our snapshot
        // Currently, only automated snapshot can be copied. Automated snapshot
        // is available every day during the preferred backup window, and there
        // is no easy way get an automated snapshot during our test.
        //        rds.copyDBSnapshot(CopyDBSnapshotRequest.builder()
        //                .sourceDBSnapshotIdentifier(snapshotIdentifier)
        //                .targetDBSnapshotIdentifier(snapshotCopyIdentifier));

        // Restore from our snapshot
        waitForSnapshotToTransitionToState(snapshotIdentifier, "available");
        String restoredDatabaseInstanceName = "java-integ-test-restored-database-" + System.currentTimeMillis();
        databaseInstancesToRelease.add(restoredDatabaseInstanceName);
        DBInstance restoredInstanceFromSnapshot = rds.restoreDBInstanceFromDBSnapshot(
                RestoreDBInstanceFromDBSnapshotRequest.builder()
                                                      .dbInstanceClass(DB_INSTANCE_CLASS)
                                                      .port(PORT)
                                                      .multiAZ(true)
                                                      .dbInstanceIdentifier(restoredDatabaseInstanceName)
                                                      .dbSnapshotIdentifier(snapshotIdentifier)
                                                      .licenseModel("general-public-license").build());
        assertValidDbInstance(restoredInstanceFromSnapshot);
        assertEquals(restoredInstanceFromSnapshot.publiclyAccessible(), true);
        // Wait for it to start up so that we don't have problems deleting the snapshot
        waitForDbInstanceToTransitionToState(restoredDatabaseInstanceName, "available");


        // Restore from a point in time
        Date restoreTime = rds.describeDBInstances(
                DescribeDBInstancesRequest.builder()
                                          .dbInstanceIdentifier(databaseInstanceName).build()
                                                  ).dbInstances().get(0).latestRestorableTime();
        restoredDatabaseInstanceName = "java-integ-test-restored-database-" + new Date().getTime();
        databaseInstancesToRelease.add(restoredDatabaseInstanceName);
        DBInstance restoredInstanceToPointInTime = rds.restoreDBInstanceToPointInTime(
                RestoreDBInstanceToPointInTimeRequest.builder()
                                                     .dbInstanceClass(DB_INSTANCE_CLASS)
                                                     .port(PORT)
                                                     .multiAZ(true)
                                                     .restoreTime(restoreTime)
                                                     .sourceDBInstanceIdentifier(databaseInstanceName)
                                                     .targetDBInstanceIdentifier(restoredDatabaseInstanceName).build());
        assertValidDbInstance(restoredInstanceToPointInTime);
        assertEquals(restoredInstanceToPointInTime.publiclyAccessible(), true);
        // Wait for it to start up so that we don't have problems deleting the snapshot
        waitForDbInstanceToTransitionToState(restoredDatabaseInstanceName, "available");


        // Delete our snapshot
        waitForSnapshotToTransitionToState(snapshotIdentifier, "available");
        DBSnapshot deleteDBSnapshot = rds.deleteDBSnapshot(
                DeleteDBSnapshotRequest.builder()
                                       .dbSnapshotIdentifier(snapshotIdentifier).build());
        assertValidSnapshot(deleteDBSnapshot);


        // Event Operations
        List<Event> events = rds.describeEvents(
                DescribeEventsRequest.builder()
                                     .maxRecords(20)
                                     .sourceType("db-instance")
                                     .sourceIdentifier(databaseInstanceName)
                                     .startTime(startTime)
                                     .endTime(new Date()).build()).events();
        assertFalse(events.isEmpty());
        assertValidEvent(events.get(0));

        // Promote a read replica
        DBInstance promoteReadReplicaInstance =
                rds.promoteReadReplica(PromoteReadReplicaRequest.builder().dbInstanceIdentifier(readReplicaInstanceName).build());
        assertValidDbInstance(promoteReadReplicaInstance);

        // To check whether the read replica become a stand alone DB
        waitForDbInstanceToTransitionToState(readReplicaInstanceName, "available");
        databaseInstancesToRelease.add(readReplicaInstanceName + "second");
        createdReadReplicaInstance = rds.createDBInstanceReadReplica(
                CreateDBInstanceReadReplicaRequest.builder()
                                                  .dbInstanceIdentifier(readReplicaInstanceName + "second")
                                                  .sourceDBInstanceIdentifier(readReplicaInstanceName)
                                                  .autoMinorVersionUpgrade(true)
                                                  .dbInstanceClass(DB_INSTANCE_CLASS)
                                                  .port(PORT).build());
        assertValidDbInstance(createdReadReplicaInstance);

        // Delete it
        DBInstance deletedInstance = rds.deleteDBInstance(
                DeleteDBInstanceRequest.builder()
                                       .dbInstanceIdentifier(databaseInstanceName)
                                       .skipFinalSnapshot(true).build());
        databaseInstancesToRelease.remove(databaseInstanceName);
        assertValidDbInstance(deletedInstance);
    }

    /*
     * Private Test Utility Methods
     */

    /**
     * Asserts that the specified DB event's fields are correctly populated.
     *
     * @param event
     *            The DB event to test.
     */
    private void assertValidEvent(Event event) {
        assertNotNull(event.date());
        assertNotEmpty(event.message());
        assertNotEmpty(event.sourceIdentifier());
        assertNotEmpty(event.sourceType());
    }

    /**
     * Asserts that the specified snapshot's fields are correctly populated.
     *
     * @param snapshot
     *            The snapshot to test.
     */
    private void assertValidSnapshot(DBSnapshot snapshot) {
        assertNotNull(snapshot.allocatedStorage());
        assertNotEmpty(snapshot.availabilityZone());
        assertNotEmpty(snapshot.dbInstanceIdentifier());
        assertNotEmpty(snapshot.dbSnapshotIdentifier());
        assertTrue(snapshot.engine().startsWith("mysql"));
        assertNotNull(snapshot.instanceCreateTime());
        assertNotEmpty(snapshot.masterUsername());
        assertNotNull(snapshot.port());
        assertNotEmpty(snapshot.status());

        // Snapshot create time is only populated once the
        // snapshot is finished being created and available
        if (snapshot.status().equals("available")) {
            assertNotNull(snapshot.snapshotCreateTime());
        }
    }

    /**
     * Asserts that the specified DB instance's fields are correctly populated.
     *
     * @param instance
     *            The DB instance to test.
     */
    private void assertValidDbInstance(DBInstance instance) {
        assertNotNull(instance.allocatedStorage());
        assertNotEmpty(instance.licenseModel());
        assertNotNull(instance.backupRetentionPeriod());
        assertNotEmpty(instance.dbInstanceClass());
        assertNotEmpty(instance.dbInstanceIdentifier());
        assertNotEmpty(instance.dbInstanceStatus());
        assertNotEmpty(instance.dbName());
        assertEquals(1, instance.dbSecurityGroups().size());
        assertNotEmpty(instance.engine());
        assertNotEmpty(instance.masterUsername());
        assertNotEmpty(instance.preferredBackupWindow());
        assertNotEmpty(instance.preferredMaintenanceWindow());
        assertFalse(instance.dbParameterGroups().isEmpty());
        assertNotEmpty(instance.dbParameterGroups().get(0).dbParameterGroupName());
        assertNotEmpty(instance.dbParameterGroups().get(0).dbParameterGroupName());


        // The following fields are only populated once the DB
        // instance is available.
        if (instance.dbInstanceStatus().equals("available")) {
            assertNotEmpty(instance.endpoint().address());
            assertNotNull(instance.endpoint().port());
            assertNotEmpty(instance.availabilityZone());
            assertNotNull(instance.instanceCreateTime());
        }
    }

}
