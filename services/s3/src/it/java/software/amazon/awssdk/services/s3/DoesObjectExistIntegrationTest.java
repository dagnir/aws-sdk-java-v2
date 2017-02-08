package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.util.StringInputStream;

public class DoesObjectExistIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET_NAME = "does-object-exist-" + System.currentTimeMillis();

    @BeforeClass
    public static void setupFixture() {
        s3.createBucket(BUCKET_NAME);
    }

    @AfterClass
    public static void tearDownFixture() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
    }

    @Test
    public void doesObjectExist_WhenObjectExists_ReturnsTrue() throws Exception {
        final String objectName = "some-object";
        s3.putObject(BUCKET_NAME, objectName, new StringInputStream("content"), null);
        assertTrue(s3.doesObjectExist(BUCKET_NAME, objectName));
    }

    @Test
    public void doesObjectExist_WhenObjectDoesNotExist_ReturnsFalse() throws Exception {
        assertFalse(s3.doesObjectExist(BUCKET_NAME, "non-existent-object-name"));
    }

    @Test(expected = AmazonS3Exception.class)
    public void doesObjectExist_NonExistentBucket_ThrowsException() {
        // Should throw an access denied exception since we don't own the bucket
        s3.doesObjectExist("aws-java-sdk", "some-object");
    }
}
