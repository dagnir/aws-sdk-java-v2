package software.amazon.awssdk.services.s3.internal.crypto;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateKeyPair;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateRandomAsciiFile;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.security.KeyPair;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.services.s3.util.UnreliableRepeatableFileInputStream;

/**
 * Integration tests for retrying the last part in a multi-part upload.
 */
public abstract class S3LastPartUploadIntegrationTestBase extends S3IntegrationTestBase {
    private static final boolean cleanup = true;

    /** Length of the random temp file to upload */
    private static final int RANDOM_OBJECT_DATA_LENGTH = 1024;

    /** Name of the test bucket these tests will create, test, delete, etc */
    private String bucket;

    /** The temporary file to be uploaded, and the temporary file to be retrieved. */
    private File temporaryFile;

    /** Asymmetric crypto key pair for use in encryption and decryption. */
    private KeyPair keyPair;

    /** Encryption client using object metadata for crypto metadata storage. */
    private AmazonS3EncryptionClient s3;

    protected abstract CryptoMode cryptoMode();

    private CryptoConfiguration newCryptoConfiguration() {
        return new CryptoConfiguration().withCryptoMode(cryptoMode());
    }

    @Before
    public void setUpClients() throws Exception {
        super.setUp();
        bucket = tempBucketName(S3LastPartUploadIntegrationTestBase.class);
        keyPair = generateKeyPair("RSA", 1024);
        temporaryFile = generateRandomAsciiFile(RANDOM_OBJECT_DATA_LENGTH);
        s3 = new AmazonS3EncryptionClient(credentials,
                new EncryptionMaterials(keyPair),
                // subclass specifies the crypto mode to use
                newCryptoConfiguration());
        s3.createBucket(bucket);

        CryptoConfiguration cryptoConfig = newCryptoConfiguration();
        cryptoConfig.setStorageMode(CryptoStorageMode.InstructionFile);
    }

    @After
    public void tearDown() {
        if (temporaryFile != null) {
            temporaryFile.delete();
        }
        if (cleanup)
            deleteBucketAndAllContents(bucket);
    }

    /**
     * Tests the intrinsic retry (up to 3 times) works on the last-part of a
     * multi-part upload.
     */
    @Test
    public void testMultipartLastPart_IntrinsicRetry() throws Exception {
        long lastPartLength = 1024;
        final String key = "testMultipartLastPart_1_Retry";
        InitiateMultipartUploadResult initiateMultipartUpload =
            s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key));
        String uploadId = initiateMultipartUpload.getUploadId();
        UnreliableRepeatableFileInputStream is =
            new UnreliableRepeatableFileInputStream(temporaryFile)
            .withNumberOfErrors(1)  // 1 failure
            .disableClose() // requires explicit release
            ;

        UploadPartResult result = s3.uploadPart(new UploadPartRequest()
            .withBucketName(bucket)
            .withKey(key)
            .withLastPart(true)
            .withInputStream(is)
            .withPartNumber(1)
            .withPartSize(lastPartLength)
            .withUploadId(uploadId))
            ;
        is.release();
        s3.completeMultipartUpload(new CompleteMultipartUploadRequest(
                bucket, key, uploadId, Arrays .asList(result.getPartETag())));
        S3Object s3Object = s3.getObject(bucket, key);
        byte[] md5file = CryptoTestUtils.md5of(temporaryFile);
        byte[] md5s3object = CryptoTestUtils.md5of(s3Object);
        assertTrue(Arrays.equals(md5file, md5s3object));
    }

    /**
     * Tests the intrinsic retry fails on uploading the last part of a
     * multi-part upload when the number of failure exceeded the maximum.
     */
    @Test(expected=AmazonClientException.class)
    public void testMultipartLastPart_ExceedIntrinsicRetries() throws Exception {
        long lastPartLength = 1024;
        final String key = "testMultipartLastPart_ExceedIntrinsicRetries";

        InitiateMultipartUploadResult initiateMultipartUpload =
            s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key));
        String uploadId = initiateMultipartUpload.getUploadId();
        UnreliableRepeatableFileInputStream is = new UnreliableRepeatableFileInputStream(temporaryFile)
                .withNumberOfErrors(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY + 1)
                .disableClose() // requires explicit release
                ;
        try {
            UploadPartResult result = s3.uploadPart(new UploadPartRequest()
                .withBucketName(bucket)
                .withKey(key)
                .withLastPart(true)
                .withInputStream(is)
                .withPartNumber(1)
                .withPartSize(lastPartLength)
                .withUploadId(uploadId))
                ;
        } finally {
            is.release();
        }
        fail();
    }

    /**
     * Tests extrinsic retry.
     */
    @Test
    public void testMultipartLastPart_ExtrinsicRetry() throws Exception {
        long lastPartLength = 1024;
        final String key = "testMultipartLastPart_ExtrinsicRetry";

        InitiateMultipartUploadResult initiateMultipartUpload =
            s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key));
        String uploadId = initiateMultipartUpload.getUploadId();
        {   // Number of failures exceed the default number of retries
            UnreliableRepeatableFileInputStream is =
                new UnreliableRepeatableFileInputStream(temporaryFile)
                .withNumberOfErrors(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY + 1)
                .disableClose() // requires explicit release
                ;
            try {
                UploadPartResult result = s3.uploadPart(new UploadPartRequest()
                    .withBucketName(bucket)
                    .withKey(key)
                    .withLastPart(true)
                    .withInputStream(is)
                    .withPartNumber(1)
                    .withPartSize(lastPartLength)
                    .withUploadId(uploadId))
                    ;
                fail();
            } catch(AmazonClientException expected) {
            } finally {
                is.release();
            }
        }
        {   // Extrinsic retry
            UnreliableRepeatableFileInputStream is =
                new UnreliableRepeatableFileInputStream(temporaryFile)
                .disableClose();    // requires explicit release
            try {
                UploadPartResult result = s3.uploadPart(new UploadPartRequest()
                    .withBucketName(bucket)
                    .withKey(key)
                    .withLastPart(true)
                    .withInputStream(is)
                    .withPartNumber(1)
                    .withPartSize(lastPartLength)
                    .withUploadId(uploadId))
                    ;
            } finally {
                is.release();
            }
        }
    }
}
