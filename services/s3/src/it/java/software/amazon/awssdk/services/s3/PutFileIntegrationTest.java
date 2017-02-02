package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CanonicalGrantee;
import software.amazon.awssdk.services.s3.model.EmailAddressGrantee;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.GroupGrantee;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Integration tests for uploading files to Amazon S3.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class PutFileIntegrationTest extends S3IntegrationTestBase {

    private static String bucketName = "put-object-integ-test-" + new Date().getTime();
    private static String key = "key";
    private static long contentLength = 443L;
    private static ObjectMetadata expectedMetadata;
    private static File file;

    /** Releases all resources created by tests */
    @AfterClass
    public static void tearDown() {
        s3.deleteObject(bucketName, key);
        s3.deleteBucket(bucketName);
        file.delete();
    }

    /**
     * Tests that uploading a file with a known file extension will correctly
     * upload the object, set it's content type and content disposition.
     */
    @Test
    public void testPutFileWithRecognizedMimeType() throws Exception {
        PutObjectResult result = s3.putObject(bucketName, key, file);
        assertNotEmpty(result.getETag());
        assertNull(result.getVersionId());

        S3Object object = s3.getObject(bucketName, key);
        assertFileEqualsStream(file, object.getObjectContent());

        ObjectMetadata s3Metadata = object.getObjectMetadata();
        assertNull(s3Metadata.getCacheControl());
        assertTrue(contentLength == s3Metadata.getContentLength());
        assertNull(s3Metadata.getContentEncoding());
        assertEquals("text/plain", s3Metadata.getContentType());
        assertNotNull(s3Metadata.getETag());
        assertNotNull(s3Metadata.getLastModified());
        assertTrue(s3Metadata.getUserMetadata().isEmpty());
    }

    /**
     * Tests that uploading a file with an unknown file extension will correctly
     * upload the data, set content disposition, and a default content type.
     */
    @Test
    public void testPutFileWithUnrecognizedMimeType() throws Exception {
        File file = super.getRandomTempFile( "foo", contentLength );
        s3.putObject(bucketName, key, file);

        S3Object object = s3.getObject(bucketName, key);
        assertFileEqualsStream(file, object.getObjectContent());

        ObjectMetadata s3Metadata = object.getObjectMetadata();
        assertNull(s3Metadata.getCacheControl());
        assertTrue(contentLength == s3Metadata.getContentLength());
        assertNull(s3Metadata.getContentEncoding());
        assertEquals("application/octet-stream", s3Metadata.getContentType());
        assertNotNull(s3Metadata.getETag());
        assertNotNull(s3Metadata.getLastModified());
        assertTrue(s3Metadata.getUserMetadata().isEmpty());

        file.delete();
    }

    /**
     * Tests that uploading a file with metadata will correctly upload the
     * object and honor the specified metadata.
     */
    @Test
    public void testPutFileWithMetadata() throws Exception {
        s3.putObject(new PutObjectRequest(bucketName, key, file)
            .withMetadata(expectedMetadata));

        S3Object object = s3.getObject(bucketName, key);
        assertFileEqualsStream(file, object.getObjectContent());

        ObjectMetadata metadata = object.getObjectMetadata();
        assertMetadataEqual(expectedMetadata, metadata);
        assertTrue(contentLength == metadata.getContentLength());
    }

    /**
     * Tests that uploading a file and specifying a canned ACL will correctly
     * upload the object and set the correct canned ACL.
     */
    @Test
    public void testPutFileWithCannedAcl() throws Exception {
        s3.putObject(new PutObjectRequest(bucketName, key, file)
            .withCannedAcl(CannedAccessControlList.PublicRead));

        S3Object object = s3.getObject(bucketName, key);
        assertFileEqualsStream(file, object.getObjectContent());

        ObjectMetadata metadata = object.getObjectMetadata();
        assertTrue(contentLength == metadata.getContentLength());
        assertNotNull(metadata.getETag());
        assertNotNull(metadata.getLastModified());
        assertTrue(metadata.getUserMetadata().isEmpty());

        AccessControlList acl = s3.getObjectAcl(bucketName, key);
        assertTrue(2 == acl.getGrantsAsList().size());
        assertTrue(doesAclContainGroupGrant(acl, GroupGrantee.AllUsers, Permission.Read));
    }

    /**
     * Tests that uploading a file and specifying an ACL will correctly upload
     * the object and set the correct ACL.
     */
    @Test
    public void testPutFileWithAcl() throws Exception {
        AccessControlList acl = new AccessControlList();

        for ( Permission permission : Permission.values() ) {
            acl.grantPermission(new CanonicalGrantee(AWS_DR_ECLIPSE_ACCT_ID), permission);
            acl.grantPermission(GroupGrantee.AuthenticatedUsers, permission);
            acl.grantPermission(new EmailAddressGrantee(AWS_DR_TOOLS_EMAIL_ADDRESS), permission);
        }

        s3.putObject(new PutObjectRequest(bucketName, key, file).withAccessControlList(acl));

        S3Object object = s3.getObject(bucketName, key);
        assertFileEqualsStream(file, object.getObjectContent());

        ObjectMetadata metadata = object.getObjectMetadata();
        assertTrue(contentLength == metadata.getContentLength());
        assertNotNull(metadata.getETag());
        assertNotNull(metadata.getLastModified());
        assertTrue(metadata.getUserMetadata().isEmpty());

        AccessControlList aclRead = s3.getObjectAcl(bucketName, key);
        assertTrue(15 == aclRead.getGrantsAsList().size());

        Set<Grant> expectedGrants = translateEmailAclsIntoCanonical(acl);

        for ( Grant expected : expectedGrants ) {
            assertTrue("Didn't find expectd grant " + expected, aclRead.getGrantsAsList().contains(expected));
        }
    }

    /**
     * Tests that uploading a file with explicit metadata and specifying a
     * canned ACL will correctly upload the object, honor the specified
     * metadata, set the correct canned ACL, and upload the correct data.
     */
    @Test
    public void gotestPutFileWithMetadataAndCannedAcl() throws Exception {
        s3.putObject(new PutObjectRequest(bucketName, key, file)
            .withMetadata(expectedMetadata)
            .withCannedAcl(CannedAccessControlList.AuthenticatedRead));

        S3Object object = s3.getObject(bucketName, key);
        assertFileEqualsStream(file, object.getObjectContent());

        ObjectMetadata metadata = object.getObjectMetadata();
        assertMetadataEqual(expectedMetadata, metadata);
        assertTrue(contentLength == metadata.getContentLength());

        AccessControlList acl = s3.getObjectAcl(bucketName, key);
        assertTrue(2 == acl.getGrantsAsList().size());
        assertTrue(doesAclContainGroupGrant(acl, GroupGrantee.AuthenticatedUsers, Permission.Read));
    }


    /*
     * Private Test Helper Functions
     */

    /**
     * Initializes test resources.
     */
    @BeforeClass
    public static void initializeTestData() throws Exception {
        s3.createBucket(bucketName);

        expectedMetadata = new ObjectMetadata();
        expectedMetadata.setCacheControl("custom-cache-control");
        expectedMetadata.setContentDisposition("custom-disposition");
        expectedMetadata.setContentEncoding("custom-encoding");
        expectedMetadata.setContentType("custom-content-type");
        expectedMetadata.addUserMetadata("foo", "bar");
        expectedMetadata.addUserMetadata("baz", "bash");

        file = S3IntegrationTestBase.getRandomTempFile( "foo.txt", contentLength );
    }

    /**
     * Asserts that the two specified S3ObjectMetadata objects are equivalent.
     *
     * @param expectedMetadata
     *            The metadata object containing the expected values.
     * @param metadata
     *            The metadata object being tested.
     */
    private void assertMetadataEqual(ObjectMetadata expectedMetadata, ObjectMetadata metadata) {
        assertEquals(expectedMetadata.getCacheControl(), metadata.getCacheControl());
        assertEquals(expectedMetadata.getContentDisposition(), metadata.getContentDisposition());
        assertEquals(expectedMetadata.getContentEncoding(), metadata.getContentEncoding());
        assertEquals(expectedMetadata.getContentType(), metadata.getContentType());

        Map<String, String> expectedUserMetadata = expectedMetadata.getUserMetadata();
        Map<String, String> userMetadata = metadata.getUserMetadata();
        assertTrue(expectedUserMetadata.size() == userMetadata.size());

        for ( java.util.Iterator iterator = expectedUserMetadata.keySet().iterator(); iterator.hasNext(); ) {
            String key = (String)iterator.next();
            assertTrue(userMetadata.containsKey(key));
            assertEquals(expectedUserMetadata.get(key), userMetadata.get(key));
        }
    }

}
