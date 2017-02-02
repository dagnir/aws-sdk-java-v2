package software.amazon.awssdk.services.s3.internal.crypto;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.InputSubstream;
import software.amazon.awssdk.services.s3.internal.S3CryptoTestClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.MultipartUploadListing;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PartListing;
import software.amazon.awssdk.services.s3.model.PartSummary;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.services.s3.util.UnreliableRepeatableFileInputStream;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Integration tests for the Amazon S3 Encryption Client.
 */
public abstract class S3MultipartUploadCryptoIntegrationTestBase extends S3IntegrationTestBase {
    private static final boolean cleanup = true;
    /** Length of the random temp file to upload */
    private static final int RANDOM_OBJECT_DATA_LENGTH = 10*1024*1024;

    /** Suffix appended to the end of instruction files */
    private static final String INSTRUCTION_SUFFIX = ".instruction";

    /** Name of the test bucket these tests will create, test, delete, etc */
    private String expectedBucketName = tempBucketName(S3MultipartUploadCryptoIntegrationTestBase.class);

    /** Name of the file that will be temporarily generated on disk, and then stored in S3 */
    private String expectedObjectName = "S3V2MultipartUploadCryptoIntegrationTest-" + new Date().getTime();

    /** The temporary file to be uploaded, and the temporary file to be retrieved. */
    private File temporaryFile;
    private File retrievedTemporaryFile;

    /** Asymmetric crypto key pair for use in encryption and decryption. */
    private KeyPair keyPair;

    /** Encryption client using object metadata for crypto metadata storage. */
    private AmazonS3 s3_metadata;

    /** Encryption client using a separate instruction file for crypto metadata storage. */
    private AmazonS3 s3_instructionFile;

    protected abstract CryptoMode cryptoMode();

    private CryptoConfiguration newCryptoConfiguration() {
        return new CryptoConfiguration().withCryptoMode(cryptoMode());
    }

    /**
     * Set up the tests.  Get AWS credentials, generate asymmetric keys, construct encryption providers, and create a test bucket.
     */
    @Before
    public void setUpClients() throws Exception {
        super.setUp();
        generateAsymmetricKeyPair();
        createTemporaryFiles();

        s3_metadata = new S3CryptoTestClient(credentials,
                new EncryptionMaterials(keyPair),
                // subclass specifies the crypto mode to use
                newCryptoConfiguration()
                .withIgnoreMissingInstructionFile(false));
        s3_metadata.createBucket(expectedBucketName);

        CryptoConfiguration cryptoConfig = newCryptoConfiguration()
                .withIgnoreMissingInstructionFile(false);
        cryptoConfig.setStorageMode(CryptoStorageMode.InstructionFile);
        s3_instructionFile = new S3CryptoTestClient(credentials,
                new EncryptionMaterials(keyPair), cryptoConfig);
    }

    /** Tests that a multipart upload can be created, listed and aborted. */
    @Test
    public void testAborted() throws Exception {
        String bucketName = expectedBucketName;
        InitiateMultipartUploadResult initiateResult = s3_metadata.initiateMultipartUpload(
                new InitiateMultipartUploadRequest(bucketName, "key")
                    .withCannedACL(CannedAccessControlList.PublicRead)
                    .withStorageClass(StorageClass.ReducedRedundancy));
        String uploadId = initiateResult.getUploadId();
        uploadParts(bucketName, uploadId, s3_metadata);

        listMultipartUploads(bucketName, s3_metadata);
        listMultipartUploads(bucketName, uploadId, s3_metadata);
        listParts(bucketName, uploadId, s3_metadata);

        abortMultipartUpload(bucketName, uploadId, s3_metadata);
    }

    /**
     * Ensure that any created test resources are correctly released.
     */
    @After
    public void tearDown() {
        if (temporaryFile != null) {
            temporaryFile.delete();
            retrievedTemporaryFile.delete();
        }
        if (cleanup)
            deleteBucketAndAllContents(expectedBucketName);
    }


    /**
     * Tests that we can upload an encrypted multipart object using the instruction file storage mode for the crypto metadata.
     */
    @Test
    public void testUploadWithInstructionFile() throws Exception {
        long firstPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - 5*1024*1024;
        long secondPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - firstPartLength;

        InitiateMultipartUploadResult initiateMultipartUpload = s3_instructionFile
                .initiateMultipartUpload(new InitiateMultipartUploadRequest(
                        expectedBucketName, expectedObjectName));
        String uploadId = initiateMultipartUpload.getUploadId();

        UploadPartResult uploadPartResult = s3_instructionFile.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withFile(temporaryFile)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        UploadPartResult uploadPartResult2 = s3_instructionFile.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withLastPart(true)
            .withFile(temporaryFile)
            .withFileOffset(firstPartLength)
            .withPartNumber(2)
            .withPartSize(secondPartLength)
            .withUploadId(uploadId));

        ArrayList<PartETag> partETags = new ArrayList<PartETag>();
        partETags.add(uploadPartResult.getPartETag());
        partETags.add(uploadPartResult2.getPartETag());

        s3_instructionFile.completeMultipartUpload(new CompleteMultipartUploadRequest(expectedBucketName, expectedObjectName, uploadId, partETags));

        // Test that the instruction file was written
        S3Object instructionFile = s3.getObject(expectedBucketName, expectedObjectName + INSTRUCTION_SUFFIX);
        assertNotNull(instructionFile);

        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
        s3_instructionFile.getObject(retrieveRequest, retrievedTemporaryFile);
        assertFileEqualsStream(temporaryFile, new FileInputStream(retrievedTemporaryFile));
    }


    /**
     * Tests that the encryption client throws an error when a caller specifies
     * a part size that doesn't line up with the cipher block size (except for
     * the last part, where padding is allowed).
     */
    @Test
    public void testBadPartSize() throws Exception {
        InitiateMultipartUploadResult initiateMultipartUpload = s3_metadata.initiateMultipartUpload(new InitiateMultipartUploadRequest(expectedBucketName, expectedObjectName));
        String uploadId = initiateMultipartUpload.getUploadId();

        int badPartSize = JceEncryptionConstants.SYMMETRIC_CIPHER_BLOCK_SIZE * 1024 + 1;
        try {
            s3_metadata.uploadPart(new UploadPartRequest()
                .withBucketName(expectedBucketName)
                .withKey(expectedObjectName)
                .withFile(temporaryFile)
                .withPartNumber(1)
                .withPartSize(badPartSize)
                .withUploadId(uploadId));
            fail("Expected an AmazonClientException, but wasn't thrown");
        } catch (AmazonClientException ace) {}
    }

    /**
     * Tests that the encryption client throws an error if multiple parts are
     * specified as the last part in a multipart upload.
     */
    @Test
    public void testMultipleLastParts() throws Exception {
        long largeObjectLength = RANDOM_OBJECT_DATA_LENGTH;
        long firstPartLength = largeObjectLength - 5*1024*1024;
        long secondPartLength = largeObjectLength - firstPartLength;

        InitiateMultipartUploadResult initiateMultipartUpload =
            s3_metadata.initiateMultipartUpload(new InitiateMultipartUploadRequest(expectedBucketName, expectedObjectName));
        String uploadId = initiateMultipartUpload.getUploadId();

        s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withLastPart(true)
            .withFile(temporaryFile)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        try {
            s3_metadata.uploadPart(new UploadPartRequest()
                .withBucketName(expectedBucketName)
                .withKey(expectedObjectName)
                .withLastPart(true)
                .withFile(temporaryFile)
                .withFileOffset(firstPartLength)
                .withPartNumber(2)
                .withPartSize(secondPartLength)
                .withUploadId(uploadId));
            fail("Expected an AmazonClientException, but wasn't thrown");
        } catch (AmazonClientException ace) {}
    }

    /**
     * Tests that the encryption client throws an error when a user doesn't
     * specify which part is the last in a multipart upload, in order to prevent
     * data corruption.
     */
    @Test
    public void testLastPartNotSpecified() throws Exception {
        long largeObjectLength = RANDOM_OBJECT_DATA_LENGTH;
        long firstPartLength = largeObjectLength - 5*1024*1024;
        long secondPartLength = largeObjectLength - firstPartLength;

        InitiateMultipartUploadResult initiateMultipartUpload = s3_metadata.initiateMultipartUpload(new InitiateMultipartUploadRequest(expectedBucketName, expectedObjectName));
        String uploadId = initiateMultipartUpload.getUploadId();

        UploadPartResult uploadPartResult = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withFile(temporaryFile)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        UploadPartResult uploadPartResult2 = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withFile(temporaryFile)
            .withFileOffset(firstPartLength)
            .withPartNumber(2)
            .withPartSize(secondPartLength)
            .withUploadId(uploadId));

        ArrayList<PartETag> partETags = new ArrayList<PartETag>();
        partETags.add(uploadPartResult.getPartETag());
        partETags.add(uploadPartResult2.getPartETag());

        try {
            s3_metadata.completeMultipartUpload(new CompleteMultipartUploadRequest(expectedBucketName, expectedObjectName, uploadId, partETags));
            fail("Expected an AmazonClientException, but wasn't thrown");
        } catch (AmazonClientException ace) {}
    }

    /**
     * Tests that we can encrypt each part of a multipart upload, then download
     * the finished object, decrypt it and verify that the contents are
     * identical.
     */
    @Test
    public void testUploadWithMetadata() throws Exception {
        long firstPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - 5*1024*1024;
        long secondPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - firstPartLength;

        InitiateMultipartUploadResult initiateMultipartUpload = s3_metadata.initiateMultipartUpload(new InitiateMultipartUploadRequest(expectedBucketName, expectedObjectName));
        String uploadId = initiateMultipartUpload.getUploadId();

        UploadPartResult uploadPartResult = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withFile(temporaryFile)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        UploadPartResult uploadPartResult2 = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withLastPart(true)
            .withFile(temporaryFile)
            .withFileOffset(firstPartLength)
            .withPartNumber(2)
            .withPartSize(secondPartLength)
            .withUploadId(uploadId));

        ArrayList<PartETag> partETags = new ArrayList<PartETag>();
        partETags.add(uploadPartResult.getPartETag());
        partETags.add(uploadPartResult2.getPartETag());

        s3_metadata.completeMultipartUpload(new CompleteMultipartUploadRequest(expectedBucketName, expectedObjectName, uploadId, partETags));

        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
        s3_metadata.getObject(retrieveRequest, retrievedTemporaryFile);
        assertFileEqualsStream(temporaryFile, new FileInputStream(retrievedTemporaryFile));
    }

    /**
     * Tests an encrypted multipart upload with recoverable IO errors thrown
     * from the stream.  Since the underlying stream is resetable, the encryption
     * client should be able to recover.
     */
    @Test
    public void testUploadWithMetadataRecoverableError() throws Exception {
        long firstPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - 5*1024*1024;
        long secondPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - firstPartLength;

        InitiateMultipartUploadResult initiateMultipartUpload = s3_metadata.initiateMultipartUpload(new InitiateMultipartUploadRequest(expectedBucketName, expectedObjectName));
        String uploadId = initiateMultipartUpload.getUploadId();
        InputStream is = new UnreliableRepeatableFileInputStream(temporaryFile)
                        .disableClose();    // requires explicit release

        UploadPartResult uploadPartResult = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withInputStream(is)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        UploadPartResult uploadPartResult2 = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withLastPart(true)
            .withInputStream(is)
            .withPartNumber(2)
            .withPartSize(secondPartLength)
            .withUploadId(uploadId));

        ArrayList<PartETag> partETags = new ArrayList<PartETag>();
        partETags.add(uploadPartResult.getPartETag());
        partETags.add(uploadPartResult2.getPartETag());

        s3_metadata.completeMultipartUpload(new CompleteMultipartUploadRequest(expectedBucketName, expectedObjectName, uploadId, partETags));

        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
        s3_metadata.getObject(retrieveRequest, retrievedTemporaryFile);
        assertFileEqualsStream(temporaryFile, new FileInputStream(retrievedTemporaryFile));
    }

    /**
     * Tests an encrypted multipart upload with recoverable IO errors thrown
     * from the stream.  Since the underlying stream is resetable, the encryption
     * client should be able to recover.
     */
    @Test
    public void testUploadWithMetadataRecoverableError2() throws Exception {
        long firstPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - 5*1024*1024;
        long secondPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - firstPartLength;

        InitiateMultipartUploadResult initiateMultipartUpload = s3_metadata.initiateMultipartUpload(new InitiateMultipartUploadRequest(expectedBucketName, expectedObjectName));
        String uploadId = initiateMultipartUpload.getUploadId();

        InputStream part1Stream = new InputSubstream(
                new UnreliableRepeatableFileInputStream(temporaryFile)
                        .disableClose(),    // requires explicit release
                0, firstPartLength, false);

        InputStream part2Stream = new InputSubstream(
                new UnreliableRepeatableFileInputStream(temporaryFile)
                        .disableClose(),    // requires explicit release
                firstPartLength, secondPartLength, true);

        UploadPartResult uploadPartResult = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withInputStream(part1Stream)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        UploadPartResult uploadPartResult2 = s3_metadata.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withLastPart(true)
            .withInputStream(part2Stream)
            .withPartNumber(2)
            .withPartSize(secondPartLength)
            .withUploadId(uploadId));

        ArrayList<PartETag> partETags = new ArrayList<PartETag>();
        partETags.add(uploadPartResult.getPartETag());
        partETags.add(uploadPartResult2.getPartETag());

        s3_metadata.completeMultipartUpload(new CompleteMultipartUploadRequest(expectedBucketName, expectedObjectName, uploadId, partETags));

        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
        s3_metadata.getObject(retrieveRequest, retrievedTemporaryFile);
        assertFileEqualsStream(temporaryFile, new FileInputStream(retrievedTemporaryFile));
    }

    /*
     * Private Helper Methods
     */

    /*
     * Generates temporary files for uploading and downloading
     */
    private void createTemporaryFiles() throws IOException {
        temporaryFile = new RandomTempFile(expectedObjectName, RANDOM_OBJECT_DATA_LENGTH);
        temporaryFile.deleteOnExit();
        retrievedTemporaryFile = File.createTempFile("retrieved-", expectedObjectName);
        retrievedTemporaryFile.deleteOnExit();
    }

    /*
     * Generates an asymmetric key pair for use in encrypting and decrypting.
     */
    private void generateAsymmetricKeyPair() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(1024, new SecureRandom());
            keyPair = keyGenerator.generateKeyPair();
        } catch (Exception e) {
            fail("Unable to generate asymmetric keys: " + e.getMessage());
        }
    }
    private List<PartETag> uploadParts(String bucketName, String uploadId, AmazonS3 s3) throws AmazonServiceException, AmazonClientException, InterruptedException {
        List<PartETag> partETags = new ArrayList<PartETag>();

        UploadPartResult uploadPartResult = s3.uploadPart(new UploadPartRequest()
            .withBucketName(bucketName)
            .withInputStream(new RandomInputStream(RANDOM_OBJECT_DATA_LENGTH))
            .withKey("key")
            .withPartNumber(1)
            .withPartSize(RANDOM_OBJECT_DATA_LENGTH)
            .withUploadId(uploadId));
        assertEquals(1, uploadPartResult.getPartNumber());
        assertNotEmpty(uploadPartResult.getETag());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        uploadPartResult = s3.uploadPart(new UploadPartRequest()
            .withBucketName(bucketName)
            .withInputStream(new RandomInputStream(RANDOM_OBJECT_DATA_LENGTH))
            .withKey("key")
            .withPartNumber(2)
            .withPartSize(RANDOM_OBJECT_DATA_LENGTH)
            .withUploadId(uploadId));
        assertEquals(2, uploadPartResult.getPartNumber());
        assertNotEmpty(uploadPartResult.getETag());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        return partETags;
    }
    private void listParts(String bucketName, String uploadId, AmazonS3 s3) {
        PartListing listPartsResult = s3.listParts(new ListPartsRequest(bucketName, "key", uploadId)
            .withMaxParts(100)
            .withPartNumberMarker(new Integer(0))
            .withEncodingType("url"));
        assertEquals(bucketName, listPartsResult.getBucketName());
        assertEquals("key", listPartsResult.getKey());
        assertEquals(100, listPartsResult.getMaxParts().intValue());
        assertEquals(0, listPartsResult.getPartNumberMarker().intValue());
        assertNotNull(listPartsResult.getNextPartNumberMarker());
        assertEquals(0, listPartsResult.getPartNumberMarker().intValue());
        assertEquals(StorageClass.ReducedRedundancy.toString(), listPartsResult.getStorageClass());
        assertEquals(uploadId, listPartsResult.getUploadId());
        assertTrue(listPartsResult.getParts().size() > 0);
        PartSummary part = listPartsResult.getParts().get(0);
        assertNotEmpty(part.getETag());
        assertNotNull(part.getLastModified());
        assertTrue(part.getPartNumber() > 0);
        assertTrue(RANDOM_OBJECT_DATA_LENGTH == part.getSize());

        assertNotNull(listPartsResult.getOwner());
        assertNotEmpty(listPartsResult.getOwner().getDisplayName());
        assertNotEmpty(listPartsResult.getOwner().getId());

        assertNotNull(listPartsResult.getInitiator());
        assertNotEmpty(listPartsResult.getInitiator().getDisplayName());
        assertNotEmpty(listPartsResult.getInitiator().getId());
        assertEquals("url", listPartsResult.getEncodingType());
    }

    private void listMultipartUploads(String bucketName, String uploadId, AmazonS3 s3) {
        // Test all the request parameters for ListMultipartUploads
        MultipartUploadListing listMultipartUploadsResult = s3.listMultipartUploads(
                new ListMultipartUploadsRequest(bucketName)
                    .withKeyMarker("key")
                    .withMaxUploads(100)
                    .withUploadIdMarker(uploadId));
        assertEquals(bucketName, listMultipartUploadsResult.getBucketName());
        assertEquals("key", listMultipartUploadsResult.getKeyMarker());
        assertEquals(100, listMultipartUploadsResult.getMaxUploads());
        assertEquals(uploadId, listMultipartUploadsResult.getUploadIdMarker());
        assertNull(listMultipartUploadsResult.getEncodingType());
    }

    private void listMultipartUploads(String bucketName, AmazonS3 s3) {
        // Now test some multipart upload data
        MultipartUploadListing listMultipartUploadsResult = s3.listMultipartUploads(
                new ListMultipartUploadsRequest(bucketName)
                    .withEncodingType("url"));
        assertEquals(bucketName, listMultipartUploadsResult.getBucketName());
        assertNotNull(listMultipartUploadsResult.getNextKeyMarker());
        assertNotNull(listMultipartUploadsResult.getNextUploadIdMarker());
        assertTrue(listMultipartUploadsResult.getMultipartUploads().size() > 0);
        MultipartUpload multiPartUpload = listMultipartUploadsResult.getMultipartUploads().get(0);
        assertNotNull(multiPartUpload.getInitiated());
        assertNotEmpty(multiPartUpload.getKey());
        assertNotEmpty(multiPartUpload.getStorageClass());
        assertNotEmpty(multiPartUpload.getUploadId());

        assertNotNull(multiPartUpload.getOwner());
        assertNotEmpty(multiPartUpload.getOwner().getDisplayName());
        assertNotEmpty(multiPartUpload.getOwner().getId());

        assertNotNull(multiPartUpload.getInitiator());
        assertNotEmpty(multiPartUpload.getInitiator().getDisplayName());
        assertNotEmpty(multiPartUpload.getInitiator().getId());
        
        // EncodingType parameter should be returned in the response
        assertEquals("url", listMultipartUploadsResult.getEncodingType());
    }

    private void abortMultipartUpload(String bucketName, String uploadId, AmazonS3 s3) {
        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, "key", uploadId));
    }
}
