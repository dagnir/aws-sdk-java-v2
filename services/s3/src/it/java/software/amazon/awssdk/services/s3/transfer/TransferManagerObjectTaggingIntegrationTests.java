package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertNull;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Encryption;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.ObjectTaggingTestUtil;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.ObjectTagging;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;

/**
 * Object tagging related integration tests for {@link TransferManager}.
 */
public class TransferManagerObjectTaggingIntegrationTests extends S3IntegrationTestBase {
    private static final String KEY_PREFIX = "test-object-";
    private static final String BUCKET = "java-object-tagging-bucket-" + System.currentTimeMillis();
    private static final long MULTIPART_UPLOAD_PART_SIZE = 5 * MB;
    private static final long MULTIPART_UPLOAD_TEST_FILE_SIZE = MULTIPART_UPLOAD_PART_SIZE + 1;

    private static TransferManagerConfiguration config = new TransferManagerConfiguration();
    private static AmazonS3Encryption encryptionS3;

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        encryptionS3 = new AmazonS3EncryptionClient(credentials,
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()));
        s3.createBucket(BUCKET);
        config.setMultipartUploadThreshold(MULTIPART_UPLOAD_TEST_FILE_SIZE - 1);
        config.setMinimumUploadPartSize(MULTIPART_UPLOAD_PART_SIZE);
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET);
    }

    @Test
    public void testUploadNotPromotedToMultipartIfTagsSetUsingNormalClient() throws IOException, InterruptedException {
        testUploadNotPromotedToMultipartIfTagsSet(s3);
    }

    @Test
    public void testUploadNotPromotedToMultipartOfTagsSetUsingEncryptionClient() throws InterruptedException, IOException {
        testUploadNotPromotedToMultipartIfTagsSet(encryptionS3);
    }

    public void testUploadNotPromotedToMultipartIfTagsSet(AmazonS3 s3Client) throws InterruptedException, IOException {
        TransferManager tm = new TransferManager(s3Client);
        tm.setConfiguration(config);

        String key = makeKey();
        ObjectTagging tags = new ObjectTagging(Arrays.asList(
            new Tag("foo", "bar")
        ));

        PutObjectRequest request = new PutObjectRequest(BUCKET, key, generateFileForMultipartUpload())
                .withTagging(tags);
        tm.upload(request).waitForCompletion();

        S3Object obj = s3.getObject(new GetObjectRequest(BUCKET, key).withPartNumber(1));
        assertNull(obj.getObjectMetadata().getPartCount());
        List<Tag> retrievedTags = s3.getObjectTagging(new GetObjectTaggingRequest(BUCKET, key)).getTagSet();
        ObjectTaggingTestUtil.assertTagSetsAreEquals(tags.getTagSet(), retrievedTags);
    }

    private String makeKey() {
        return KEY_PREFIX + System.currentTimeMillis();
    }

    public File generateFileForMultipartUpload() throws IOException {
        return CryptoTestUtils.generateRandomAsciiFile(MULTIPART_UPLOAD_TEST_FILE_SIZE);
    }
}
