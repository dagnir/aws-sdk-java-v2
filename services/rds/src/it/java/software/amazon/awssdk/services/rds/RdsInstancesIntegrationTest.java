package software.amazon.awssdk.services.rds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.services.rds.model.CopyDBSnapshotRequest;
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
 * @author Jason Fulghum <fulghum@amazon.com>
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
        String databaseInstanceName    = "java-integ-test-db-" + System.currentTimeMillis();
        String readReplicaInstanceName = "java-integ-read-replica-db-" + System.currentTimeMillis();
        databaseInstancesToRelease.add(databaseInstanceName);
        databaseInstancesToRelease.add(readReplicaInstanceName);

        // Create a DB
        DBInstance createdInstance = rds.createDBInstance(
                new CreateDBInstanceRequest()
                    .withAllocatedStorage(5)
                    .withDBInstanceClass(DB_INSTANCE_CLASS)
                    .withDBInstanceIdentifier(databaseInstanceName)
                    .withEngine(ENGINE)
                    .withDBName("integtestdb")
                    .withMasterUsername("admin")
                    .withMasterUserPassword("password-with-at-least-8-chars")
                    .withMultiAZ(true)
                    .withPort(PORT)
                    .withPubliclyAccessible(true)
                    .withLicenseModel("general-public-license"));
        assertValidDbInstance(createdInstance);

        waitForDbInstanceToTransitionToState(databaseInstanceName, "available");

        // Describe DB log files
        DescribeDBLogFilesResult describeDBLogFilesResult = rds.describeDBLogFiles(new DescribeDBLogFilesRequest().withDBInstanceIdentifier(databaseInstanceName));
        assertNotNull(describeDBLogFilesResult.getDescribeDBLogFiles().size());
        assertNotNull(describeDBLogFilesResult.getDescribeDBLogFiles().get(0).getLastWritten());
        String logFileName = describeDBLogFilesResult.getDescribeDBLogFiles().get(0).getLogFileName();
        assertNotNull(logFileName);
        assertNotNull(describeDBLogFilesResult.getDescribeDBLogFiles().get(0).getSize());

        // Download DB log portion
        DownloadDBLogFilePortionResult downloadDBLogFilePortionResult = rds.downloadDBLogFilePortion(new DownloadDBLogFilePortionRequest().withDBInstanceIdentifier(databaseInstanceName).withLogFileName(logFileName));
        assertNotNull(downloadDBLogFilePortionResult.getLogFileData());
        assertNotNull(downloadDBLogFilePortionResult.getAdditionalDataPending());

        // Create a Read Replica
        DBInstance createdReadReplicaInstance = rds.createDBInstanceReadReplica(
                new CreateDBInstanceReadReplicaRequest(readReplicaInstanceName, databaseInstanceName)
                    .withAutoMinorVersionUpgrade(true)
                    .withDBInstanceClass(DB_INSTANCE_CLASS)
                    .withPort(PORT));
        assertValidDbInstance(createdReadReplicaInstance);

        // Describe our read replica DB
        waitForDbInstanceToTransitionToState(readReplicaInstanceName, "available");
        Thread.sleep(120 * 1000);
        List<DBInstance> dbInstances = rds.describeDBInstances(new DescribeDBInstancesRequest()
            .withDBInstanceIdentifier(readReplicaInstanceName)
            .withMaxRecords(20)).getDBInstances();
        assertEquals(1, dbInstances.size());
        assertValidDbInstance(dbInstances.get(0));
        assertNotEmpty(dbInstances.get(0).getReadReplicaSourceDBInstanceIdentifier());

        assertEquals(0, dbInstances.get(0).getReadReplicaDBInstanceIdentifiers().size());
        assertEquals(dbInstances.get(0).getPubliclyAccessible(), true);
        assertTrue(dbInstances.get(0).getStatusInfos().size()>0);

        DBInstanceStatusInfo replicationStatus=null;
        for (int i = 0; i < dbInstances.get(0).getStatusInfos().size(); i++) {
            if (dbInstances.get(0).getStatusInfos().get(i).getStatusType()
                    .equals("read replication")) {
                replicationStatus = dbInstances.get(0).getStatusInfos().get(i);
                break;
            }
        }
        assertNotNull("Could not find a Read Replication status.", replicationStatus);
        assertEquals(replicationStatus.getStatus(), "replicating");
        assertTrue(replicationStatus.getNormal());
        assertTrue(replicationStatus.getMessage()==null || replicationStatus.getMessage().isEmpty());

        // Describe our master DB
        dbInstances = rds.describeDBInstances(
                new DescribeDBInstancesRequest()
                    .withDBInstanceIdentifier(databaseInstanceName)
                    .withMaxRecords(20)).getDBInstances();
        assertEquals(1, dbInstances.size());
        assertValidDbInstance(dbInstances.get(0));
        assertTrue(dbInstances.get(0).getMultiAZ());
        assertEquals(1, dbInstances.get(0).getReadReplicaDBInstanceIdentifiers().size());
        assertEquals(readReplicaInstanceName, dbInstances.get(0).getReadReplicaDBInstanceIdentifiers().get(0));
        assertNull(dbInstances.get(0).getReadReplicaSourceDBInstanceIdentifier());
        assertEquals(dbInstances.get(0).getPubliclyAccessible(), true);//


        // Modify it
        waitForDbInstanceToTransitionToState(databaseInstanceName, "available");
        DBInstance modifiedInstance = rds.modifyDBInstance(
                new ModifyDBInstanceRequest()
                    .withDBInstanceIdentifier(databaseInstanceName)
                    .withAllocatedStorage(6)
                    .withMultiAZ(true)
                    .withMasterUserPassword("password-with-at-least-8-chars"));
        assertValidDbInstance(modifiedInstance);


        // Reboot it
        DBInstance rebootedInstance = rds.rebootDBInstance(
                new RebootDBInstanceRequest()
                    .withDBInstanceIdentifier(databaseInstanceName));
        assertValidDbInstance(rebootedInstance);


        // Create a snapshot
        waitForDbInstanceToTransitionToState(databaseInstanceName, "available");
        snapshotIdentifier = "java-integ-test-snapshot-" + new Date().getTime();
        DBSnapshot createdSnapshot = rds.createDBSnapshot(
                new CreateDBSnapshotRequest()
                    .withDBSnapshotIdentifier(snapshotIdentifier)
                    .withDBInstanceIdentifier(databaseInstanceName));
        assertValidSnapshot(createdSnapshot);


        // Describe our snapshot
        List<DBSnapshot> dbSnapshots = rds.describeDBSnapshots(
                new DescribeDBSnapshotsRequest()
                    .withDBSnapshotIdentifier(snapshotIdentifier)
        ).getDBSnapshots();
        assertEquals(1, dbSnapshots.size());
        assertValidSnapshot(dbSnapshots.get(0));

        // Copy our snapshot
        // Currently, only automated snapshot can be copied. Automated snapshot
        // is available every day during the preferred backup window, and there
        // is no easy way get an automated snapshot during our test.
//        rds.copyDBSnapshot(new CopyDBSnapshotRequest()
//                .withSourceDBSnapshotIdentifier(snapshotIdentifier)
//                .withTargetDBSnapshotIdentifier(snapshotCopyIdentifier));

        // Restore from our snapshot
        waitForSnapshotToTransitionToState(snapshotIdentifier, "available");
        String restoredDatabaseInstanceName = "java-integ-test-restored-database-" + System.currentTimeMillis();
        databaseInstancesToRelease.add(restoredDatabaseInstanceName);
        DBInstance restoredInstanceFromSnapshot = rds.restoreDBInstanceFromDBSnapshot(
                new RestoreDBInstanceFromDBSnapshotRequest()
                    .withDBInstanceClass(DB_INSTANCE_CLASS)
                    .withPort(PORT)
                    .withMultiAZ(true)
                    .withDBInstanceIdentifier(restoredDatabaseInstanceName)
                    .withDBSnapshotIdentifier(snapshotIdentifier)
                    .withLicenseModel("general-public-license"));
        assertValidDbInstance(restoredInstanceFromSnapshot);
        assertEquals(restoredInstanceFromSnapshot.getPubliclyAccessible(), true);
        // Wait for it to start up so that we don't have problems deleting the snapshot
        waitForDbInstanceToTransitionToState(restoredDatabaseInstanceName, "available");


        // Restore from a point in time
        Date restoreTime = rds.describeDBInstances(
                new DescribeDBInstancesRequest()
                    .withDBInstanceIdentifier(databaseInstanceName)
        ).getDBInstances().get(0).getLatestRestorableTime();
        restoredDatabaseInstanceName = "java-integ-test-restored-database-" + new Date().getTime();
        databaseInstancesToRelease.add(restoredDatabaseInstanceName);
        DBInstance restoredInstanceToPointInTime = rds.restoreDBInstanceToPointInTime(
                new RestoreDBInstanceToPointInTimeRequest()
                    .withDBInstanceClass(DB_INSTANCE_CLASS)
                    .withPort(PORT)
                    .withMultiAZ(true)
                    .withRestoreTime(restoreTime)
                    .withSourceDBInstanceIdentifier(databaseInstanceName)
                    .withTargetDBInstanceIdentifier(restoredDatabaseInstanceName)
        );
        assertValidDbInstance(restoredInstanceToPointInTime);
        assertEquals(restoredInstanceToPointInTime.getPubliclyAccessible(), true);
        // Wait for it to start up so that we don't have problems deleting the snapshot
        waitForDbInstanceToTransitionToState(restoredDatabaseInstanceName, "available");


        // Delete our snapshot
        waitForSnapshotToTransitionToState(snapshotIdentifier, "available");
        DBSnapshot deleteDBSnapshot = rds.deleteDBSnapshot(
                new DeleteDBSnapshotRequest()
                    .withDBSnapshotIdentifier(snapshotIdentifier));
        assertValidSnapshot(deleteDBSnapshot);


        // Event Operations
        List<Event> events = rds.describeEvents(
                new DescribeEventsRequest()
                    .withMaxRecords(20)
                    .withSourceType("db-instance")
                    .withSourceIdentifier(databaseInstanceName)
                    .withStartTime(startTime)
                    .withEndTime(new Date())
        ).getEvents();
        assertFalse(events.isEmpty());
        assertValidEvent(events.get(0));

        // Promote a read replica
        DBInstance promoteReadReplicaInstance = rds.promoteReadReplica(new PromoteReadReplicaRequest().withDBInstanceIdentifier(readReplicaInstanceName));
        assertValidDbInstance(promoteReadReplicaInstance);

        // To check whether the read replica become a stand alone DB
        waitForDbInstanceToTransitionToState(readReplicaInstanceName, "available");
        databaseInstancesToRelease.add(readReplicaInstanceName + "second");
        createdReadReplicaInstance = rds.createDBInstanceReadReplica(
                new CreateDBInstanceReadReplicaRequest(readReplicaInstanceName + "second", readReplicaInstanceName)
                    .withAutoMinorVersionUpgrade(true)
                    .withDBInstanceClass(DB_INSTANCE_CLASS)
                    .withPort(PORT));
        assertValidDbInstance(createdReadReplicaInstance);

        // Delete it
        DBInstance deletedInstance = rds.deleteDBInstance(
                new DeleteDBInstanceRequest()
                    .withDBInstanceIdentifier(databaseInstanceName)
                    .withSkipFinalSnapshot(true));
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
        assertNotNull(event.getDate());
        assertNotEmpty(event.getMessage());
        assertNotEmpty(event.getSourceIdentifier());
        assertNotEmpty(event.getSourceType());
    }

    /**
     * Asserts that the specified snapshot's fields are correctly populated.
     *
     * @param snapshot
     *            The snapshot to test.
     */
    private void assertValidSnapshot(DBSnapshot snapshot) {
        assertNotNull(snapshot.getAllocatedStorage());
        assertNotEmpty(snapshot.getAvailabilityZone());
        assertNotEmpty(snapshot.getDBInstanceIdentifier());
        assertNotEmpty(snapshot.getDBSnapshotIdentifier());
        assertTrue(snapshot.getEngine().startsWith("mysql"));
        assertNotNull(snapshot.getInstanceCreateTime());
        assertNotEmpty(snapshot.getMasterUsername());
        assertNotNull(snapshot.getPort());
        assertNotEmpty(snapshot.getStatus());

        // Snapshot create time is only populated once the
        // snapshot is finished being created and available
        if (snapshot.getStatus().equals("available")) {
            assertNotNull(snapshot.getSnapshotCreateTime());
        }
    }

    /**
     * Asserts that the specified DB instance's fields are correctly populated.
     *
     * @param instance
     *            The DB instance to test.
     */
    private void assertValidDbInstance(DBInstance instance) {
        assertNotNull(instance.getAllocatedStorage());
        assertNotEmpty(instance.getLicenseModel());
        assertNotNull(instance.getBackupRetentionPeriod());
        assertNotEmpty(instance.getDBInstanceClass());
        assertNotEmpty(instance.getDBInstanceIdentifier());
        assertNotEmpty(instance.getDBInstanceStatus());
        assertNotEmpty(instance.getDBName());
        assertEquals(1, instance.getDBSecurityGroups().size());
        assertNotEmpty(instance.getEngine());
        assertNotEmpty(instance.getMasterUsername());
        assertNotEmpty(instance.getPreferredBackupWindow());
        assertNotEmpty(instance.getPreferredMaintenanceWindow());
        assertFalse(instance.getDBParameterGroups().isEmpty());
        assertNotEmpty(instance.getDBParameterGroups().get(0).getDBParameterGroupName());
        assertNotEmpty(instance.getDBParameterGroups().get(0).getDBParameterGroupName());


        // The following fields are only populated once the DB
        // instance is available.
        if (instance.getDBInstanceStatus().equals("available")) {
            assertNotEmpty(instance.getEndpoint().getAddress());
            assertNotNull(instance.getEndpoint().getPort());
            assertNotEmpty(instance.getAvailabilityZone());
            assertNotNull(instance.getInstanceCreateTime());
        }
    }

}
