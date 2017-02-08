package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CanonicalGrantee;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.EmailAddressGrantee;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.Grantee;
import software.amazon.awssdk.services.s3.model.GroupGrantee;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.Owner;
import software.amazon.awssdk.services.s3.model.Permission;

/**
 * Integration tests for S3 ACL operations on buckets and objects.
 *
 * @author fulghum@amazon.com
 */
public class AclIntegrationTest extends S3IntegrationTestBase {

    /** The name of the bucket these tests will create, test on and delete */
    private String bucketName = "acl-integration-test-" + new Date().getTime();

    /** The key of the object these tests will create, test on and delete */
    private String key = "key";

    /** Releases all test resources */
    @After
    public void tearDown() {
        s3.deleteObject(bucketName, key);
        s3.deleteBucket(bucketName);
    }

    /**
     * Tests that we can set and read custom ACLs on buckets and objects.
     */
    @Test
    public void testCustomAcls() {
        s3.createBucket(bucketName);
        s3.putObject(bucketName, key, new ByteArrayInputStream("foobarbazbar".getBytes()), new ObjectMetadata());

        AccessControlList customAcl = s3.getBucketAcl(bucketName);
        customAcl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);
        customAcl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);
        customAcl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);
        customAcl.grantPermission(GroupGrantee.AllUsers, Permission.ReadAcp);

        s3.setBucketAcl(bucketName, customAcl);
        s3.setObjectAcl(bucketName, key, customAcl);
        AccessControlList bucketAcl = s3.getBucketAcl(bucketName);
        AccessControlList objectAcl = s3.getBucketAcl(bucketName);


        AccessControlList[] accessControls = new AccessControlList[] {bucketAcl, objectAcl};
        for (int index = 0; index < accessControls.length; index++ ) {
            AccessControlList acl = accessControls[index];

            // S3 converts email grantees to canonical grantees, so we have to do a little more work to validate them
            assertEquals(customAcl.getOwner(), acl.getOwner());
            assertTrue(doesAclContainsCanonicalGrant(acl, "aws-dr-eclipse", Permission.Read));
            assertTrue(doesAclContainGroupGrant(acl, GroupGrantee.AllUsers, Permission.ReadAcp));
            assertEquals(5, acl.getGrantsAsList().size());
        }
    }

    @Test
    public void testCreateBucketWithAcl() {
        AccessControlList acl = new AccessControlList();

        for ( Permission permission : Permission.values() ) {
            acl.grantPermission(new CanonicalGrantee(AWS_DR_ECLIPSE_ACCT_ID), permission);
            acl.grantPermission(GroupGrantee.AuthenticatedUsers, permission);
            acl.grantPermission(new EmailAddressGrantee(AWS_DR_TOOLS_EMAIL_ADDRESS), permission);

        }

        acl.grantPermission(new EmailAddressGrantee(AWS_DR_TOOLS_EMAIL_ADDRESS), Permission.Read);
        s3.createBucket(new CreateBucketRequest(bucketName).withAccessControlList(acl));
        AccessControlList aclRead = s3.getBucketAcl(bucketName);

        assertEquals(16, aclRead.getGrantsAsList().size());

        Set<Grant> expectedGrants = translateEmailAclsIntoCanonical(acl);

        for ( Grant expected : expectedGrants ) {
            assertTrue("Didn't find expectd grant " + expected, aclRead.getGrantsAsList().contains(expected));
        }
    }

    /**
     * Tests that we can set and read canned ACLs on buckets and objects.
     */
    @Test
    public void testCannedAcls() {
        AssertionFailedError assertionFailure = null;
        s3.createBucket(bucketName);
        s3.putObject(bucketName, key, new ByteArrayInputStream("foobarbazbar".getBytes()), new ObjectMetadata());
        Owner bucketOwner = s3.getBucketAcl(bucketName).getOwner();

        // Public Read Canned ACL
        s3.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
        s3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
        for (int i=0; i < 2; i++) {
            try {
                AccessControlList bucketAcl = s3.getBucketAcl(bucketName);
                AccessControlList objectAcl = s3.getObjectAcl(bucketName, key);
                AccessControlList[] accessControls = new AccessControlList[] {
                        bucketAcl, objectAcl };
                for (int index = 0; index < accessControls.length; index++) {
                    AccessControlList acl = accessControls[index];
                    assertEquals(bucketOwner, acl.getOwner());
                    assertTrue(doesAclContainGroupGrant(acl, GroupGrantee.AllUsers, Permission.Read));
                    assertEquals(2, acl.getGrantsAsList().size());
                }

                // Authenticated Read Canned ACL
                s3.setBucketAcl(bucketName, CannedAccessControlList.AuthenticatedRead);
                s3.setObjectAcl(bucketName, key, CannedAccessControlList.AuthenticatedRead);
                bucketAcl = s3.getBucketAcl(bucketName);
                objectAcl = s3.getObjectAcl(bucketName, key);

                accessControls = new AccessControlList[] {bucketAcl, objectAcl};
                for (int index = 0; index < accessControls.length; index++) {
                    AccessControlList acl = accessControls[index];
                    assertEquals(bucketOwner, acl.getOwner());
                    assertTrue(doesAclContainGroupGrant(acl, GroupGrantee.AuthenticatedUsers, Permission.Read));
                    assertEquals(2, acl.getGrantsAsList().size());
                }

                // Private Canned ACL
                s3.setBucketAcl(bucketName, CannedAccessControlList.Private);
                s3.setObjectAcl(bucketName, key, CannedAccessControlList.Private);
                bucketAcl = s3.getBucketAcl(bucketName);
                objectAcl = s3.getObjectAcl(bucketName, key);

                accessControls = new AccessControlList[] {bucketAcl, objectAcl};
                for (int index = 0; index < accessControls.length; index++ ) {
                    AccessControlList acl = accessControls[index];
                    assertEquals(bucketOwner, acl.getOwner());
                    assertEquals(1, acl.getGrantsAsList().size());
                }
            } catch(AssertionFailedError e) {
                assertionFailure = e;
                // eventual consistency?
                // Let's suspend for a sec, then retry
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                continue;
            }
            return;
        }
        throw assertionFailure;
    }


    /*
     * Private Test Helper Methods
     */

    /**
     * Returns true if the specified ACL contains a canonical grant with the
     * specified display name and permission.
     *
     * @param acl
     *            The AccessControlList to check.
     * @param expectedDisplayName
     *            The canonical grantee's display name being searched for.
     * @param expectedPermission
     *            The grantee's permission being searched for.
     *
     * @return True if the specified ACL contains a canonical grant with the
     *         specified display name and permission, otherwise false.
     */
    private boolean doesAclContainsCanonicalGrant(AccessControlList acl,
            String expectedDisplayName, Permission expectedPermission) {

        for (Grant grant: acl.getGrantsAsList()) {
            Grantee grantee = grant.getGrantee();
            Permission permission = grant.getPermission();

            if (grantee instanceof CanonicalGrantee) {
                CanonicalGrantee canonicalGrantee = (CanonicalGrantee) grantee;
                if (canonicalGrantee.getDisplayName().equals(
                        expectedDisplayName)
                        && permission.equals(expectedPermission)) {
                    return true;
                }
            }
        }

        return false;
    }

}
