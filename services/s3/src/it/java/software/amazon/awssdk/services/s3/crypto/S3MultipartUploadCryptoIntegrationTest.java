package software.amazon.awssdk.services.s3.crypto;

import static org.junit.Assert.assertNotNull;
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
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.InputSubstream;
import software.amazon.awssdk.services.s3.internal.crypto.JceEncryptionConstants;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptedInitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.services.s3.util.UnreliableRepeatableFileInputStream;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Integration tests for the Amazon S3 Encryption Client.
 */
@Category(S3Categories.Slow.class)
public class S3MultipartUploadCryptoIntegrationTest extends S3IntegrationTestBase {
    private static final boolean cleanup = true;
    /** Length of the random temp file to upload */
    private static final int RANDOM_OBJECT_DATA_LENGTH = 10*1024*1024;

    /** Suffix appended to the end of instruction files */
    private static final String INSTRUCTION_SUFFIX = ".instruction";

    /** Name of the test bucket these tests will create, test, delete, etc */
    private String expectedBucketName = "java-sdk-crypto-integ-bucket-" + System.currentTimeMillis();

    /** Name of the file that will be temporarily generated on disk, and then stored in S3 */
    private String expectedObjectName = "integ-test-file-" + new Date().getTime();

    /** The temporary file to be uploaded, and the temporary file to be retrieved. */
    private File temporaryFile;
    private File retrievedTemporaryFile;

    /** Asymmetric crypto key pair for use in encryption and decryption. */
    private KeyPair keyPair;

    /** Encryption client using object metadata for crypto metadata storage. */
    private AmazonS3 s3_metadata;

    /** Encryption client using object metadata for crypto metadata storage, and using materialProvider. */
    private AmazonS3 s3_metadata_materialProvider;

    /** Encryption client using a separate instruction file for crypto metadata storage. */
    private AmazonS3 s3_instructionFile;

    /** Encryption client using a separate instruction file for crypto metadata storage and using materialProvider */
    private AmazonS3 s3_instructionFile_materialProvider;

    /**
     * Set up the tests.  Get AWS credentials, generate asymmetric keys, construct encryption providers, and create a test bucket.
     */
    @Before
    public void setUpClients() throws Exception {
        super.setUp();
        generateAsymmetricKeyPair();
        createTemporaryFiles();

        s3_metadata = new AmazonS3EncryptionClient(credentials, new EncryptionMaterials(keyPair));
        s3_metadata.createBucket(expectedBucketName);

        TestEncryptionProvider provider = new TestEncryptionProvider(keyPair);
        s3_metadata_materialProvider = new AmazonS3EncryptionClient(credentials, provider);
        s3_metadata_materialProvider.createBucket(expectedBucketName);

        CryptoConfiguration cryptoConfig = new CryptoConfiguration();
        cryptoConfig.setStorageMode(CryptoStorageMode.InstructionFile);
        s3_instructionFile = new AmazonS3EncryptionClient(credentials, new EncryptionMaterials(keyPair), cryptoConfig);
        s3_instructionFile_materialProvider = new AmazonS3EncryptionClient(credentials, provider, cryptoConfig);
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

        InitiateMultipartUploadResult initiateMultipartUpload = s3_instructionFile.initiateMultipartUpload(new InitiateMultipartUploadRequest(expectedBucketName, expectedObjectName));
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


    @Test
    public void testUploadWithInstructionFileEncryptedUploadRequest() throws Exception {
        long firstPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - 5*1024*1024;
        long secondPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - firstPartLength;

        EncryptedInitiateMultipartUploadRequest request = new EncryptedInitiateMultipartUploadRequest(expectedBucketName, expectedObjectName);
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.ASYMMETRIC_TYPE);

        request.setMaterialsDescription(materialsDescription);
        InitiateMultipartUploadResult initiateMultipartUpload = s3_instructionFile_materialProvider.initiateMultipartUpload(request);
        String uploadId = initiateMultipartUpload.getUploadId();

        UploadPartResult uploadPartResult = s3_instructionFile_materialProvider.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withFile(temporaryFile)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        UploadPartResult uploadPartResult2 = s3_instructionFile_materialProvider.uploadPart(new UploadPartRequest()
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

        s3_instructionFile_materialProvider.completeMultipartUpload(new CompleteMultipartUploadRequest(expectedBucketName, expectedObjectName, uploadId, partETags));

        // Test that the instruction file was written
        S3Object instructionFile = s3.getObject(expectedBucketName, expectedObjectName + INSTRUCTION_SUFFIX);
        assertNotNull(instructionFile);

        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
        s3_instructionFile_materialProvider.getObject(retrieveRequest, retrievedTemporaryFile);
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


    @Test
    public void testUploadWithMetadataEncryptedMultiPartRequest() throws Exception {
        long firstPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - 5*1024*1024;
        long secondPartLength = (long) RANDOM_OBJECT_DATA_LENGTH - firstPartLength;

        EncryptedInitiateMultipartUploadRequest request = new EncryptedInitiateMultipartUploadRequest(expectedBucketName, expectedObjectName);
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.ASYMMETRIC_TYPE);
        request.setMaterialsDescription(materialsDescription);
        InitiateMultipartUploadResult initiateMultipartUpload = s3_metadata_materialProvider.initiateMultipartUpload(request);
        String uploadId = initiateMultipartUpload.getUploadId();

        UploadPartResult uploadPartResult = s3_metadata_materialProvider.uploadPart(new UploadPartRequest()
            .withBucketName(expectedBucketName)
            .withKey(expectedObjectName)
            .withFile(temporaryFile)
            .withPartNumber(1)
            .withPartSize(firstPartLength)
            .withUploadId(uploadId));

        UploadPartResult uploadPartResult2 = s3_metadata_materialProvider.uploadPart(new UploadPartRequest()
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

        s3_metadata_materialProvider.completeMultipartUpload(new CompleteMultipartUploadRequest(expectedBucketName, expectedObjectName, uploadId, partETags));

        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
        s3_metadata_materialProvider.getObject(retrieveRequest, retrievedTemporaryFile);
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

    private class TestEncryptionProvider implements EncryptionMaterialsProvider {
        static final String MATERIAL_TYPE = "TYPE";
        static final String ASYMMETRIC_TYPE = "ASYMMETRIC";
        private EncryptionMaterials encryptionMaterials;

        public TestEncryptionProvider(KeyPair keyPair) {
            Map<String, String> matDesc = new HashMap<String, String>();
            matDesc.put(MATERIAL_TYPE, ASYMMETRIC_TYPE);
            this.encryptionMaterials = new TestEncryptionMaterials(keyPair, matDesc);
        }

        @Override
        public EncryptionMaterials getEncryptionMaterials(Map<String, String> materialsDescription) {
            if (materialsDescription.get(MATERIAL_TYPE).equals(ASYMMETRIC_TYPE)) {
                return encryptionMaterials;
            }
            throw new IllegalStateException("Invalid materialsDescription");
        }

        @Override
        public EncryptionMaterials getEncryptionMaterials() {
            throw new IllegalStateException("expected to use getEncryptionMaterials(Map<String, String> materialsDescription)");
        }

        @Override
        public void refresh() {

        }
    }

    class TestEncryptionMaterials extends EncryptionMaterials {

        private Map<String, String> matDesc;

        public TestEncryptionMaterials(KeyPair keyPair, Map<String, String> matDesc) {
            super(keyPair);
            this.matDesc = matDesc;
        }

        public TestEncryptionMaterials(SecretKey key, Map<String, String> matDesc) {
            super(key);
            this.matDesc = matDesc;
        }

        public Map<String, String> getMaterialsDescription(){
            return matDesc;
        }
    }
 }
