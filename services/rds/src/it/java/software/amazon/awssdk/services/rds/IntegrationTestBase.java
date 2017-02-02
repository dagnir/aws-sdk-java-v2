package software.amazon.awssdk.services.rds;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;

import software.amazon.awssdk.services.rds.model.DeleteDBInstanceRequest;
import software.amazon.awssdk.services.rds.model.DeleteDBParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.DeleteDBSecurityGroupRequest;
import software.amazon.awssdk.services.rds.model.DeleteDBSnapshotRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBSecurityGroupsRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBSnapshotsRequest;
import software.amazon.awssdk.services.rds.model.IPRange;
import software.amazon.awssdk.services.sns.AmazonSNSClient;
import software.amazon.awssdk.test.AWSIntegrationTestBase;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for all RDS integration tests. Loads AWS credentials from a
 * properties file on disk, provides helper methods for tests, and instantiates
 * the RDS client object for all tests to use.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public abstract class IntegrationTestBase extends AWSIntegrationTestBase {

    protected static final String ENGINE = "mysql5.1";

    /** The shared RDS client for all tests to use */
    protected static AmazonRDSClient rds;

    /** The shared SNS client for all tests to use */
    protected static AmazonSNSClient sns;

    /**
     * The name of a parameter group used by these tests which will be
     * automatically deleted after each test finishes running.
     */
    protected String parameterGroupName;

    /**
     * The name of a security group used by these tests which will be
     * automatically deleted after each test finishes running.
     */
    protected String securityGroupName;

    /**
     * The names of a database instance created by these tests which need to be
     * automatically deleted after each test finishes running.
     */
    protected List<String> databaseInstancesToRelease = new ArrayList<String>();


    /**
     * The name of a database snapshot created by these tests that will be
     * automatically deleted after each test finishes running.
     */
    protected String snapshotIdentifier;

    /**
     * The name of a snapshot copy created by these tests that will be
     * automatically deleted after each test finishes running.
     */
    protected String snapshotCopyIdentifier;



    /**
     * Loads the AWS account info for the integration tests and creates an
     * EC2 client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        rds = new AmazonRDSClient(getCredentials());
    }

    /** Releases all resources allocated by these tests */
    @After
    public void tearDown() throws Exception {
        if (parameterGroupName != null) {
            try {
                rds.deleteDBParameterGroup(new DeleteDBParameterGroupRequest().withDBParameterGroupName(parameterGroupName));
            } catch (Exception e) {}
        }

        if (securityGroupName != null) {
            try {
                rds.deleteDBSecurityGroup(new DeleteDBSecurityGroupRequest()
                    .withDBSecurityGroupName(securityGroupName));
            } catch (Exception e) {}
        }

        for (String databaseInstanceName : databaseInstancesToRelease) {
            try {
                waitForDbInstanceToTransitionToState(databaseInstanceName, "available");
                rds.deleteDBInstance(new DeleteDBInstanceRequest()
                .withDBInstanceIdentifier(databaseInstanceName)
                .withSkipFinalSnapshot(true));
            } catch (Exception e) {}
        }

        if (snapshotIdentifier != null) {
            try {
                waitForSnapshotToTransitionToState(snapshotIdentifier, "available");
                rds.deleteDBSnapshot(new DeleteDBSnapshotRequest()
                    .withDBSnapshotIdentifier(snapshotIdentifier));
            } catch (Exception e) {}
        }
    }


    /*
     * Helper methods
     */

    /**
     * Waits for the specified DB instance to transition to the specified state.
     * If the DB instance never transitions to that state after a set number of
     * retries, this method will fail the current test.
     *
     * @param dbInstanceName
     *            The name of the DB instance to wait for.
     * @param state
     *            The name of the expected state being waited for.
     */
    protected void waitForDbInstanceToTransitionToState(String dbInstanceName, String state) throws Exception {
        // Wait at most 90 minutes for a DB to transition states
        // Creating DBs on the integration stack is *really* slow :-(
        long timeout = System.currentTimeMillis() + (1000 * 60 * 90);

        while (System.currentTimeMillis() < timeout) {
            String status = rds.describeDBInstances(
                    new DescribeDBInstancesRequest()
                        .withDBInstanceIdentifier(dbInstanceName)
            ).getDBInstances().get(0).getDBInstanceStatus();

            System.out.println("Current status: " + status);
            if (status.equals(state)) return;

            Thread.sleep(30 * 1000);
        }

        fail("DB instance never transitioned to state " + state);
    }

    /**
     * Waits for the specified snapshot to transition to the specified state. If
     * the snapshot never transitions to that state after a set number of
     * retries, this method will fail the current test.
     *
     * @param snapshotName
     *            The name of the snapshot to wait for.
     * @param state
     *            The name of the expected state being waited for.
     */
    protected void waitForSnapshotToTransitionToState(String snapshotName, String state) throws Exception {
        // Wait at most 45 minutes for a snapshot to transition states
        long timeout = System.currentTimeMillis() + (1000 * 60 * 45);

        while (System.currentTimeMillis() < timeout) {
            String status = rds.describeDBSnapshots(
                    new DescribeDBSnapshotsRequest()
                        .withDBSnapshotIdentifier(snapshotName)
            ).getDBSnapshots().get(0).getStatus();

            System.out.println("Current satus: " + status);

            if (status.equals(state)) return;
            Thread.sleep(30 * 1000);
        }

        fail("DB snapshot never transitioned to state " + state);
    }

    /**
     * Waits for the specified IP range within a DB security group to transition
     * to the specified state. If the IP range never transitions to the
     * specified state after a set number of retries, this method will fail the
     * current test.
     *
     * @param securityGroupName
     *            The name of the DB security group containing the IP range to
     *            wait for.
     * @param cidrIpRange
     *            The CIDR IP range uniquely identifying the IP range to
     *            monitor.
     * @param state
     *            The name of the expected state being waited for.
     */
    protected void waitForSecurityGroupIPRangeToTransitionToState(String securityGroupName, String cidrIpRange, String state) throws Exception {
        for (int retries = 0; retries < 40; retries++) {
            Thread.sleep(30 * 1000);

            List<IPRange> ipRanges = rds.describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest()
                .withDBSecurityGroupName(securityGroupName)).getDBSecurityGroups().get(0).getIPRanges();

            IPRange ipRange = findIpRange(ipRanges, cidrIpRange);
            assertNotNull(ipRange);

            String status = ipRange.getStatus();
            System.out.println("Current satus: " + status);
            if (status.equals(state)) return;
        }

        fail("Security group IP range never transitioned to state " + state);
    }

    /**
     * Returns the IPRange from the specified list with the specified CIDR IP
     * range, or null if no matching IPRange is found in the list.
     *
     * @param ipRanges
     *            The list of IP ranges to search through.
     * @param cidrIpRange
     *            The CIDR IP range being searched for.
     *
     * @return The IPRange from the specified list with the specified CIDR IP
     *         range, or null if no matching IPRange is found in the list.
     */
    private IPRange findIpRange(List<IPRange> ipRanges, String cidrIpRange) {
        for (IPRange ipRange : ipRanges) {
            if (ipRange.getCIDRIP().equals(cidrIpRange)) return ipRange;
        }

        return null;
    }

    protected void assertNotEmpty(String str) {
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

}
