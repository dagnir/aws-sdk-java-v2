package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;

public class RestoreObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String bucketName = "java-sdk-" + new Date().getTime();

    private static final String key = "key";

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();

        // Put a permanent S3 object
        s3.createBucket(bucketName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength("Hello S3 Java client world!!!".getBytes().length);
        s3.putObject(bucketName, key, new ByteArrayInputStream("Hello S3 Java client world!!!".getBytes()), metadata);
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(bucketName);
    }

    @Test
    public void testRestoreObject() {

        try {
            s3.restoreObject(new RestoreObjectRequest(bucketName, key, 1));
            fail();
        } catch (AmazonServiceException e) {
            assertTrue(e.getMessage().contains("Restore is not allowed, as object's storage class is not GLACIER"));
        }

        ObjectMetadata metadata = s3.getObjectMetadata(bucketName, key);
        assertNull(metadata.getOngoingRestore());
        assertNull(metadata.getRestoreExpirationTime());
    }
}
