/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.newBouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.S3CryptoTestClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.services.s3.util.UnreliableRepeatableFileInputStream;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.BinaryUtils;
import software.amazon.awssdk.util.Md5Utils;

//import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Integration tests for the Amazon S3 Encryption Client.
 */
@Category(S3Categories.Slow.class)
public class S3CryptoIntegrationTest extends S3IntegrationTestBase {

    /** Length of the random temp file to upload. */
    private static final int RANDOM_OBJECT_DATA_LENGTH = 32 * 1024;

    /** Suffix appended to the end of instruction files. */
    private static final String INSTRUCTION_SUFFIX = ".instruction";

    /** Name of the test bucket these tests will create, test, delete, etc. */
    private String expectedBucketName = "integ-test-bucket-" + new Date().getTime();

    /** Name of the file that will be temporarily generated on disk, and then stored in S3. */
    private String expectedObjectName = "integ-test-file-" + new Date().getTime();

    /** The temporary file to be uploaded, and the temporary file to be retrieved. */
    private File temporaryFile;
    private File retrievedTemporaryFile;

    /** Asymmetric crypto key pair for use in encryption and decryption. */
    private KeyPair keyPair;

    /** Symmetric secret key for use in encryption and decryption. */
    private SecretKey symmetricKey;

    /** Encryption providers.  */
    private AmazonS3 defaultAsymmetricEncryption;
    private AmazonS3 bouncyCastleAsymmetricEncryption;
    private AmazonS3 instructionFileAsymmetricEncryption;

    private AmazonS3EncryptionClient defaultSymmetricEncryption;
    private AmazonS3 bouncyCastleSymmetricEncryption;
    private AmazonS3 instructionFileSymmetricEncryption;

    private AmazonS3 defaultMaterialsProviderEncryption;

    private AmazonS3EncryptionClient bouncyCastleMaterialsProviderEncryption;
    private AmazonS3EncryptionClient instructionFileMaterialsProviderEncryption;

    /**
     * Set up the tests.  Get AWS credentials, generate asymmetric keys, construct encryption providers, and create a test bucket.
     */
    @Before
    public void setUpClients() throws Exception {
        super.setUp();

        generateAsymmetricKeyPair();
        generateSymmetricKey();

        createTemporaryFiles();

        CryptoConfiguration bouncyCastleConfig = new CryptoConfiguration().withCryptoProvider(newBouncyCastleProvider());
        CryptoConfiguration instructionFileConfig = new CryptoConfiguration().withStorageMode(CryptoStorageMode.InstructionFile);

        Map<String, String> asymmetricMatDesc = new HashMap<String, String>();
        asymmetricMatDesc.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.ASYMMETRIC_TYPE);
        Map<String, String> symmetricMatDesc = new HashMap<String, String>();
        symmetricMatDesc.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.SYMMETRIC_TYPE);
        // Default encryption clients
        EncryptionMaterials AsymmetricEncryptionMaterials = new TestEncryptionMaterials(keyPair, asymmetricMatDesc);
        EncryptionMaterials symmetricEncryptionMaterials = new TestEncryptionMaterials(symmetricKey, symmetricMatDesc);
        // This provider supports retrieval only based on materialDescription.
        EncryptionMaterialsProvider encryptionMaterialsProvider = new TestEncryptionProvider(symmetricEncryptionMaterials, AsymmetricEncryptionMaterials);
        defaultMaterialsProviderEncryption = new S3CryptoTestClient(credentials, encryptionMaterialsProvider);
        defaultAsymmetricEncryption = new S3CryptoTestClient(credentials, AsymmetricEncryptionMaterials);
        defaultSymmetricEncryption = new S3CryptoTestClient(credentials, symmetricEncryptionMaterials);

        // Bouncy castle encryption clients
        bouncyCastleMaterialsProviderEncryption = new S3CryptoTestClient(credentials, encryptionMaterialsProvider, bouncyCastleConfig);
        bouncyCastleAsymmetricEncryption = new S3CryptoTestClient(credentials, AsymmetricEncryptionMaterials, bouncyCastleConfig);
        bouncyCastleSymmetricEncryption = new S3CryptoTestClient(credentials, symmetricEncryptionMaterials, bouncyCastleConfig);

        // Instruction file encryption clients
        instructionFileMaterialsProviderEncryption = new S3CryptoTestClient(credentials, encryptionMaterialsProvider, instructionFileConfig);
        instructionFileAsymmetricEncryption = new S3CryptoTestClient(credentials, AsymmetricEncryptionMaterials, instructionFileConfig);
        instructionFileSymmetricEncryption = new S3CryptoTestClient(credentials, symmetricEncryptionMaterials, instructionFileConfig);

        s3.createBucket(expectedBucketName);
    }


    /**
     * Tests what happens when we can't decrypt the encrypted envelope key.
     */
    @Test
    public void testUnableToDecrypt() throws Exception {
        defaultAsymmetricEncryption.putObject(expectedBucketName, expectedObjectName, temporaryFile);

        // Test handling with the wrong encryption materials
        try {
            GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
            defaultSymmetricEncryption.getObject(retrieveRequest, retrievedTemporaryFile);
            fail("Expected an exception during decryption, but didn't get one");
        } catch (AmazonClientException ace) {
            assertTrue(ace.getMessage().contains("Unable to retrieve the client encryption materials"));
        }
    }

    /**
     * Tests how the encryption client handles retrying after IO errors.
     */
    @Test
    public void testRecoverableErrorHandling() throws Exception {
        int tempFileSize = 1024 * 1024 * 8 + 2345;
        RandomTempFile randomTempFile = new RandomTempFile("s3-encryption-error-recovery", tempFileSize);

        PutObjectRequest putObjectRequest = new PutObjectRequest(expectedBucketName, expectedObjectName, (File) null);
        putObjectRequest.setMetadata(new ObjectMetadata());
        putObjectRequest.getMetadata().setContentLength(tempFileSize);
        putObjectRequest.setInputStream(new UnreliableRepeatableFileInputStream(randomTempFile).disableClose());

        defaultAsymmetricEncryption.putObject(putObjectRequest);

        // Check that what we uploaded decrypts to the correct, original content
        S3Object cryptoObject = defaultAsymmetricEncryption.getObject(expectedBucketName, expectedObjectName);
        assertTrue(doesFileEqualStream(randomTempFile, cryptoObject.getObjectContent()));
    }

    /**
     * Test putting and getting a file with encryption.
     */
    @Test
    public void testFileEncryption() throws Exception {
        defaultAsymmetricEncryption.putObject(expectedBucketName, expectedObjectName, temporaryFile);

        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, expectedObjectName);
        ObjectMetadata metadata = defaultAsymmetricEncryption.getObject(retrieveRequest, retrievedTemporaryFile);

        assertNotNull(metadata.getUserMetadata().get(Headers.UNENCRYPTED_CONTENT_LENGTH));
        assertEquals(Long.toString(temporaryFile.length()), metadata.getUserMetadata().get(Headers.UNENCRYPTED_CONTENT_LENGTH));

        assertFileEqualsStream(temporaryFile, new FileInputStream(retrievedTemporaryFile));
    }

    /**
     * Test retrieving a range of bytes from a file in S3 rather than the entire file.  Check that the
     * encryption client retrieves exactly the same bytes as the standard client.
     */
    @Test
    public void testGetSpecificRange() throws Exception {
        String cryptoClientPrefix = "crypto-";
        String plainClientPrefix = "plain-";

        // An arbitrary range of bytes within the 32 byte test file.
        long rangeBegin = 5, rangeEnd = RANDOM_OBJECT_DATA_LENGTH - 5;

        // PUT the same object with both the encryption client and the standard client
        defaultAsymmetricEncryption.putObject(expectedBucketName, cryptoClientPrefix + expectedObjectName, temporaryFile);
        s3.putObject(expectedBucketName, plainClientPrefix + expectedObjectName, temporaryFile);

        // GET the object with both the encryption client and the standard client
        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, cryptoClientPrefix + expectedObjectName);
        retrieveRequest.setRange(rangeBegin, rangeEnd);
        S3Object cryptoObject = defaultAsymmetricEncryption.getObject(retrieveRequest);

        GetObjectRequest plainRetrieveRequest = new GetObjectRequest(expectedBucketName, plainClientPrefix + expectedObjectName);
        plainRetrieveRequest.setRange(rangeBegin, rangeEnd);
        S3Object plainObject = s3.getObject(plainRetrieveRequest);

        // Check that the contents are equal
        assertTrue(doesStreamEqualStream(plainObject.getObjectContent(), cryptoObject.getObjectContent()));
    }

    /** Tests that the encryption client can correctly upload and download zero length objects. */
    @Test
    public void testZeroLengthObject() throws Exception {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(0);
        defaultAsymmetricEncryption.putObject(expectedBucketName, expectedObjectName, new ByteArrayInputStream("".getBytes()), objectMetadata);

        S3Object object = defaultAsymmetricEncryption.getObject(expectedBucketName, expectedObjectName);
        assertStringEqualsStream("", object.getObjectContent());

        // Actual encrypted object will be padded out to the size of one
        // block.
        assertTrue(16L == object.getObjectMetadata().getContentLength());
    }

    /**
     * Test putting and getting an object with asymmetric encryption using the default JCE
     * crypto provider.
     */
    @Test
    public void testDefaultAsymmetricEncryption() throws Exception {
        String objectNamePrefix = "defaultAsym-";
        testPutObject(defaultAsymmetricEncryption, objectNamePrefix);
        testGetObject(defaultAsymmetricEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }

    /**
     * Test putting and getting an object with symmetric encryption using the default JCE
     * crypto provider.
     */
    @Test
    public void testDefaultSymmetricEncryption() throws Exception {
        String objectNamePrefix = "defaultSym-";
        testPutObject(defaultSymmetricEncryption, objectNamePrefix);
        testGetObject(defaultSymmetricEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }

    @Test
    public void testAsymmetricEncryptionPutRequest() throws Exception {
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.ASYMMETRIC_TYPE);
        String objectNamePrefix = "defaultAsym-";
        testEncryptedPutObject(defaultMaterialsProviderEncryption, objectNamePrefix, materialsDescription);
        testGetObject(defaultMaterialsProviderEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }

    @Test
    public void testSymmetricEncryptionPutRequest() throws Exception {
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.SYMMETRIC_TYPE);
        String objectNamePrefix = "defaultAsym-";
        testEncryptedPutObject(defaultMaterialsProviderEncryption, objectNamePrefix, materialsDescription);
        testGetObject(defaultMaterialsProviderEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }

    /**
     * Test putting and getting an object with asymmetric encryption using the BouncyCastle
     * crypto provider.
     */
    @Test
    public void testBouncyCastleAsymmetricEncryption() throws Exception {
        String objectNamePrefix = "bouncycastleAsym-";
        testPutObject(bouncyCastleAsymmetricEncryption, objectNamePrefix);
        testGetObject(bouncyCastleAsymmetricEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }

    /**
     * Test putting and getting an object with symmetric encryption using the BouncyCastle
     * crypto provider.
     */
    @Test
    public void testBouncyCastleSymmetricEncryption() throws Exception {
        String objectNamePrefix = "bouncycastleSym-";
        testPutObject(bouncyCastleSymmetricEncryption, objectNamePrefix);
        testGetObject(bouncyCastleSymmetricEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }

    @Test
    public void testBouncyCastleAsymmetricEncryptionPutRequest() throws Exception {
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.ASYMMETRIC_TYPE);
        String objectNamePrefix = "bouncycastleAsym-";
        testEncryptedPutObject(bouncyCastleMaterialsProviderEncryption, objectNamePrefix, materialsDescription);
        testGetObject(bouncyCastleMaterialsProviderEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }


    @Test
    public void testBouncyCastleSymmetricEncryptionPutRequest() throws Exception {
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.SYMMETRIC_TYPE);
        String objectNamePrefix = "bouncycastleAsym-";
        testEncryptedPutObject(bouncyCastleMaterialsProviderEncryption, objectNamePrefix, materialsDescription);
        testGetObject(bouncyCastleMaterialsProviderEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
    }

    /**
     * Test putting and getting an object with asymmetric encryption using the instruction file
     * storage mode.
     */
    @Test
    public void testInstructionFileAsymmetricEncryption() throws Exception {
        String objectNamePrefix = "instructionAsym-";
        testPutObject(instructionFileAsymmetricEncryption, objectNamePrefix);
        testGetObject(instructionFileAsymmetricEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testNoEncryptionInfoInMetadata(objectNamePrefix);
    }

    /**
     * Test putting and getting an object with symmetric encryption using the instruction file
     * storage mode.
     */
    @Test
    public void testInstructionFileSymmetricEncryption() throws Exception {
        String objectNamePrefix = "instructionSym-";
        testPutObject(instructionFileSymmetricEncryption, objectNamePrefix);
        testGetObject(instructionFileSymmetricEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testNoEncryptionInfoInMetadata(objectNamePrefix);
    }

    @Test
    public void testInstructionFileASymmetricEncryptionPutRequest() throws Exception {
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.ASYMMETRIC_TYPE);
        String objectNamePrefix = "instructionSym-";
        testEncryptedPutObject(instructionFileMaterialsProviderEncryption, objectNamePrefix, materialsDescription);
        testGetObject(instructionFileMaterialsProviderEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testNoEncryptionInfoInMetadata(objectNamePrefix);
    }

    @Test
    public void testInstructionFileSymmetricEncryptionPutRequest() throws Exception {
        Map<String, String> materialsDescription = new HashMap<String, String>();
        materialsDescription.put(TestEncryptionProvider.MATERIAL_TYPE, TestEncryptionProvider.SYMMETRIC_TYPE);
        String objectNamePrefix = "instructionSym-";
        testEncryptedPutObject(instructionFileMaterialsProviderEncryption, objectNamePrefix, materialsDescription);
        testGetObject(instructionFileMaterialsProviderEncryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testNoEncryptionInfoInMetadata(objectNamePrefix);
    }

    /**
     * Test that a client in metadata storage mode is able to retrieve an object that was previously stored in instruction file mode.
     *
     * NOTE: This test should provide a Log warning about being unable to decrypt an object.
     */
    @Test
    public void testFallbackToInstructionFile() throws Exception {
        String objectNamePrefix = "fallback-";

        // Put object with client in instruction file storage mode
        testPutObject(instructionFileAsymmetricEncryption, objectNamePrefix);

        // Get object with client in default metadata storage mode.
        // The default client should fallback to instruction file mode, and the test should succeed.
        testGetObject(defaultAsymmetricEncryption, objectNamePrefix);
    }

    /**
     * Test retrieving a non-encrypted object.  The object should be
     * retrieved anyway, even if it lacks encryption info in its metadata.
     */
    @Test
    public void testGetDecryptedObjectWhereObjectWasNotEncrypted() throws Exception {
        String objectNamePrefix = "notEncrypted-";

        // Put object with a client that does not do encryption
        testPutObject(s3, objectNamePrefix);

        // Get object with an encryption client.
        // The encryption client should retrieve the object without decrypting it, and the test should succeed.
        S3Object result = defaultAsymmetricEncryption.getObject(expectedBucketName, objectNamePrefix + expectedObjectName);
        assertFileEqualsStream(temporaryFile, result.getObjectContent());
    }

    /**
     * Test putting and retrieving an input stream with encryption where the content-length is not set
     * in the metadata.
     *
     * NOTE: This test should provide a Log warning about content length not specified.
     */
    @Test
    public void testNoContentLengthSpecified() throws Exception {
        InputStream uploadInputStream = new FileInputStream(temporaryFile);
        PutObjectRequest request = new PutObjectRequest(expectedBucketName, expectedObjectName, uploadInputStream, null);

        defaultAsymmetricEncryption.putObject(request);

        S3Object result = defaultAsymmetricEncryption.getObject(expectedBucketName, expectedObjectName);

        // Since we didn't know the Content-Length up front, the client can't set this field
        assertFalse(result.getObjectMetadata().getUserMetadata().containsKey(Headers.UNENCRYPTED_CONTENT_LENGTH));

        InputStream retrievedInputStream = result.getObjectContent();

        assertFileEqualsStream(temporaryFile, retrievedInputStream);
    }

    @Test
    public void testNonRepeatableInputStream() throws Exception {
        try {

            System.setProperty("software.amazon.awssdk.services.s3.enforceV4", "true");

            ByteArrayInputStream i1 = new ByteArrayInputStream("Hello ".getBytes());
            ByteArrayInputStream i2 = new ByteArrayInputStream("World".getBytes());

            SequenceInputStream input = new SequenceInputStream(i1, i2);
            PutObjectRequest request = new PutObjectRequest(expectedBucketName, expectedObjectName, input, null);

            defaultSymmetricEncryption.setRegion(RegionUtils.getRegion("us-east-1"));
            defaultSymmetricEncryption.putObject(request);

            S3Object result = defaultSymmetricEncryption.getObject(expectedBucketName, expectedObjectName);

            InputStream retrievedInputStream = result.getObjectContent();
            assertStringEqualsStream("Hello World", retrievedInputStream);

        } finally {
            System.clearProperty("software.amazon.awssdk.services.s3.enforceV4");
        }
    }

    /**
     * Test putting and deleting an object stored in ObjectMetadata storage mode
     */
    @Test
    public void testDeleteObjectInObjectMetadataStorageMode() throws Exception {
        String deleteFileSuffix = "-to-be-deleted";
        defaultAsymmetricEncryption.putObject(expectedBucketName, expectedObjectName + deleteFileSuffix, temporaryFile);
        defaultAsymmetricEncryption.deleteObject(expectedBucketName, expectedObjectName + deleteFileSuffix);

        List<S3ObjectSummary> objects = defaultAsymmetricEncryption.listObjects(expectedBucketName).getObjectSummaries();
        for (S3ObjectSummary summary : objects) {
            if (summary.getKey().equals(expectedObjectName + deleteFileSuffix)) {
                fail(String.format("'%s' was not deleted from bucket '%s'", summary.getKey(), summary.getBucketName()));
            }
        }
    }

    /**
     * Test putting and deleting an object stored in InstructionFile storage mode,
     * ensuring that the instruction file is also removed.
     */
    @Test
    public void testDeleteObjectInInstructionFileStorageMode() throws Exception {
        String deleteFileSuffix = "-to-be-deleted";
        instructionFileAsymmetricEncryption.putObject(expectedBucketName, expectedObjectName + deleteFileSuffix, temporaryFile);
        instructionFileAsymmetricEncryption.deleteObject(expectedBucketName, expectedObjectName + deleteFileSuffix);

        List<S3ObjectSummary> objects = defaultAsymmetricEncryption.listObjects(expectedBucketName).getObjectSummaries();
        for (S3ObjectSummary summary : objects) {
            if (summary.getKey().equals(expectedObjectName + deleteFileSuffix)) {
                fail(String.format("'%s' was not deleted from bucket '%s'", summary.getKey(), summary.getBucketName()));
            }
            if (summary.getKey().equals(expectedObjectName + deleteFileSuffix + INSTRUCTION_SUFFIX)) {
                fail(String.format("Instruction file '%s' was not deleted from bucket '%s'",
                                   summary.getKey(), summary.getBucketName()));
            }
        }
    }

    /**
     * Tests the default symmetric encryption by setting the MD5 instead of SDK calculating the MD5.
     */
    @Test
    public void testDefaultSymmetricEncryptionMD5PreCalculated() throws Exception {
        String cryptoClientPrefix = "crypto-MD5";
        ObjectMetadata metadata = new ObjectMetadata();

        byte[] md5Hash = Md5Utils.computeMD5Hash(new FileInputStream(temporaryFile));
        metadata.setContentMD5(BinaryUtils.toBase64(md5Hash));

        // PUT the same object with both the encryption client and the standard client
        defaultSymmetricEncryption.putObject(expectedBucketName, cryptoClientPrefix + expectedObjectName, new FileInputStream(temporaryFile), metadata);

        // GET the object with both the encryption client and the standard client
        GetObjectRequest retrieveRequest = new GetObjectRequest(expectedBucketName, cryptoClientPrefix + expectedObjectName);
        S3Object cryptoObject = defaultSymmetricEncryption.getObject(retrieveRequest);

        // Check that the contents are equal
        assertFileEqualsStream(temporaryFile, cryptoObject.getObjectContent());
    }

    @Test
    public void putObject_CanRetrieveResponseMetadatFromCache() throws FileNotFoundException {
        final String cryptoClientPrefix = "cached-metadata";
        final PutObjectRequest request = new PutObjectRequest(expectedBucketName,
                                                              cryptoClientPrefix +
                                                              expectedObjectName,
                                                              temporaryFile);
        defaultSymmetricEncryption.putObject(request);
        assertNotNull(defaultSymmetricEncryption.getCachedResponseMetadata(request).getHostId());
    }

    @Test
    public void uploadPart_CanRetrieveResponseMetadataFromCache() throws FileNotFoundException {
        final String keyName = "cached-metadata" + expectedObjectName;
        final String uploadId = defaultSymmetricEncryption.initiateMultipartUpload(
                new InitiateMultipartUploadRequest(expectedBucketName, keyName)).getUploadId();
        final UploadPartRequest request = new UploadPartRequest()
                .withBucketName(expectedBucketName)
                .withUploadId(uploadId)
                .withKey(keyName)
                .withFile(temporaryFile)
                .withLastPart(true)
                .withPartNumber(1);
        final UploadPartResult result = defaultSymmetricEncryption.uploadPart(request);
        defaultSymmetricEncryption.completeMultipartUpload(
                new CompleteMultipartUploadRequest()
                        .withBucketName(expectedBucketName)
                        .withKey(keyName)
                        .withUploadId(uploadId)
                        .withPartETags(Arrays.asList(new PartETag(1, result.getETag()))));
        assertNotNull(defaultSymmetricEncryption.getCachedResponseMetadata(request).getHostId());
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
        deleteBucketAndAllContents(expectedBucketName);
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

    /*
     * Generates a symmetric key for use in encrypting and decrypting.
     */
    private void generateSymmetricKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(128, new SecureRandom());
            symmetricKey = generator.generateKey();
        } catch (Exception e) {
            fail("Unable to generate symmetric key: " + e.getMessage());
        }
    }

    /*
     * Puts an object in S3 with some metadata. Uses the Public Key for encryption.
     */
    private void testPutObject(AmazonS3 s3Client, String namePrefix) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(RANDOM_OBJECT_DATA_LENGTH);
        metadata.addUserMetadata("foo", "bar");
        metadata.addUserMetadata("baz", "bash");

        InputStream uploadInputStream = new FileInputStream(temporaryFile);

        s3Client.putObject(expectedBucketName, namePrefix + expectedObjectName, uploadInputStream, metadata);
    }

    private void testEncryptedPutObject(AmazonS3 s3Client, String namePrefix, Map<String, String> materialsDescription)
            throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(RANDOM_OBJECT_DATA_LENGTH);
        metadata.addUserMetadata("foo", "bar");
        metadata.addUserMetadata("baz", "bash");

        InputStream uploadInputStream = new FileInputStream(temporaryFile);
        EncryptedPutObjectRequest req = new EncryptedPutObjectRequest(expectedBucketName, namePrefix + expectedObjectName, uploadInputStream, metadata);
        req.setMaterialsDescription(materialsDescription);
        s3Client.putObject(req);
    }

    /*
     * Gets an object from S3.  Uses the Private Key for decryption.
     */
    private void testGetObject(AmazonS3 s3Client, String namePrefix) throws IOException {
        S3Object result = s3Client.getObject(expectedBucketName, namePrefix + expectedObjectName);
        InputStream retrievedInputStream = result.getObjectContent();

        assertFileEqualsStream(temporaryFile, retrievedInputStream);
        assertEquals(Integer.toString(RANDOM_OBJECT_DATA_LENGTH),
                     result.getObjectMetadata().getUserMetadata().get(Headers.UNENCRYPTED_CONTENT_LENGTH));
    }

    /*
     * Gets an object from S3.  Tests that the object was encrypted and that the contents of
     * the encrypted object do not match the contents of the original object.
     */
    private void testGetObjectWithoutDecrypting(String namePrefix) throws IOException {
        InputStream retrievedInputStream = s3.getObject(expectedBucketName, namePrefix + expectedObjectName).getObjectContent();

        assertFalse(doesFileEqualStream(temporaryFile, retrievedInputStream));
    }

    /*
     * Gets object metadata from S3. Checks that the user metadata is intact.  Checks that the encryption
     * info fields exist in the metadata.
     */
    private void testEncryptionInfoIsPresentInMetadata(String namePrefix) {
        ObjectMetadata objectMetadata = s3.getObjectMetadata(expectedBucketName, namePrefix + expectedObjectName);
        Map<String, String> metadata = objectMetadata.getUserMetadata();

        /* There are two fields of user metadata named "bar" and "bash", plus extra fields containing
         * encryption information.
         */

        assertTrue(metadata.size() > 2);
        assertEquals("bar", metadata.get("foo"));
        assertEquals("bash", metadata.get("baz"));
        assertNotNull(metadata.get(Headers.CRYPTO_IV));
        assertNotNull(metadata.get(Headers.CRYPTO_KEY));
        assertNotNull(metadata.get(Headers.MATERIALS_DESCRIPTION));
        assertNotNull(metadata.get(Headers.UNENCRYPTED_CONTENT_LENGTH));
    }

    /*
     * Gets object metadata from S3. Checks that the user metadata is intact.  Checks that there is no
     * encryption info in the metadata.
     */
    private void testNoEncryptionInfoInMetadata(String namePrefix) {
        ObjectMetadata objectMetadata = s3.getObjectMetadata(expectedBucketName, namePrefix + expectedObjectName);
        Map<String, String> metadata = objectMetadata.getUserMetadata();

        assertEquals("bar", metadata.get("foo"));
        assertEquals("bash", metadata.get("baz"));
        assertNull(metadata.get(Headers.CRYPTO_IV));
        assertNull(metadata.get(Headers.CRYPTO_KEY));
        assertNull(metadata.get(Headers.MATERIALS_DESCRIPTION));
    }

    class TestEncryptionProvider implements EncryptionMaterialsProvider {
        static final String MATERIAL_TYPE = "TYPE";
        static final String ASYMMETRIC_TYPE = "ASYMMETRIC";
        static final String SYMMETRIC_TYPE = "SYMMETRIC";
        private final EncryptionMaterials symmetricEncryptionMaterials;
        private final EncryptionMaterials asymmetricEncryptionMaterials;

        public TestEncryptionProvider(EncryptionMaterials symmetricEncryptionMaterials,
                                      EncryptionMaterials asymmetricEncryptionMaterials) {
            this.symmetricEncryptionMaterials = symmetricEncryptionMaterials;
            this.asymmetricEncryptionMaterials = asymmetricEncryptionMaterials;
        }

        @Override
        public EncryptionMaterials getEncryptionMaterials(Map<String, String> materialsDescription) {
            if (materialsDescription.get(MATERIAL_TYPE).equals(ASYMMETRIC_TYPE)) {
                return asymmetricEncryptionMaterials;
            }

            if (materialsDescription.get(MATERIAL_TYPE).equals(SYMMETRIC_TYPE)) {
                return symmetricEncryptionMaterials;
            }

            throw new IllegalStateException("Invalid materialsDescription");
        }

        @Override
        public EncryptionMaterials getEncryptionMaterials() {
            throw new IllegalStateException("expected to use getEncryptionMaterials(Map<String, String> materialsDescription)");
        }

        @Override
        public void refresh() {
            // nothing for testing.
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

        public Map<String, String> getMaterialsDescription() {
            return matDesc;
        }
    }
}
