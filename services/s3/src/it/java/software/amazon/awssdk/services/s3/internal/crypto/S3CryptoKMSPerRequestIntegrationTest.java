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

package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.kms.utils.KmsTestKeyCache;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.S3CryptoTestClient;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.KMSEncryptionMaterials;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.StringMapBuilder;
import software.amazon.awssdk.util.json.Jackson;

/**
 * A simple integration test for Trent.
 */
public class S3CryptoKMSPerRequestIntegrationTest extends S3IntegrationTestBase {

    /** Length of the random temp file to upload. */
    private static final int RANDOM_OBJECT_DATA_LENGTH = 32 * 1024;
    private static String nonDefaultKmsKeyId;
    /** Name of the test bucket these tests will create, test, delete, etc. */
    private final String expectedBucketName = tempBucketName(getClass());
    /** Name of the file that will be temporarily generated on disk, and then stored in S3. */
    private final String expectedObjectName = "integ-test-file-" + new Date().getTime();
    /** The temporary file to be uploaded, and the temporary file to be retrieved. */
    private File temporaryFile;
    private File retrievedTemporaryFile;

    private AmazonS3 defaultSymmetricEncryption;
    private AmazonS3 defaultSymmetricDecryption;

    protected CryptoMode cryptoMode() {
        return null;
    }

    private CryptoConfiguration newCryptoConfiguration() {
        return new CryptoConfiguration()
                .withCryptoMode(cryptoMode())
                .withIgnoreMissingInstructionFile(false);
    }

    /**
     * Set up the tests.  Get AWS credentials, generate asymmetric keys, construct encryption providers, and create a test bucket.
     */
    @Before
    public void setUpClients() throws Exception {
        // This is necessary to make it work under JDK1.6
        // http://www.masterthought.net/section/blog_article/article/connecting-to-db2-with-java-sun-jdk-1-6-from-osx#.VECRp4vF8ik
        CryptoRuntime.enableBouncyCastle();

        super.setUp();
        createTemporaryFiles();
        s3.createBucket(expectedBucketName);

        AWSKMSClient kms = new AWSKMSClient(credentials);
        kms.configureRegion(Regions.US_EAST_1);
        nonDefaultKmsKeyId = KmsTestKeyCache.getInstance(Regions.US_EAST_1, credentials).getNonDefaultKeyId();

        defaultSymmetricEncryption = new S3CryptoTestClient(kms, credentials,
                                                            new KMSEncryptionMaterials(nonDefaultKmsKeyId),
                                                            newCryptoConfiguration());

        defaultSymmetricDecryption = new S3CryptoTestClient(kms, credentials,
                                                            null,
                                                            newCryptoConfiguration());

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
        CryptoTestUtils.deleteBucketAndAllContents(s3, expectedBucketName);
    }

    /**
     * Test putting and getting an object with symmetric encryption using the default JCE
     * crypto provider.
     */
    @Test
    public void testDefaultSymmetricEncryption() throws Exception {
        String objectNamePrefix = "defaultSym-";
        testPutObject(defaultSymmetricEncryption, objectNamePrefix);
        testGetObject(defaultSymmetricDecryption, objectNamePrefix);
        testGetObjectWithoutDecrypting(objectNamePrefix);
        testEncryptionInfoIsPresentInMetadata(objectNamePrefix);
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
     * Puts an object in S3 with some metadata. Uses the Public Key for encryption.
     */
    private void testPutObject(AmazonS3 s3Client, String namePrefix)
            throws IOException, InterruptedException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(RANDOM_OBJECT_DATA_LENGTH);
        metadata.addUserMetadata("foo", "bar");
        metadata.addUserMetadata("baz", "bash");
        InputStream uploadInputStream = new FileInputStream(temporaryFile);
        int retry = 0;
        for (; ; ) {
            try {
                EncryptedPutObjectRequest req = new EncryptedPutObjectRequest(
                        expectedBucketName, namePrefix + expectedObjectName,
                        uploadInputStream, metadata)
                        .withMaterialsDescription(new StringMapBuilder("extra1", "extra_value1").build());
                s3Client.putObject(req);
                return;
            } catch (AmazonS3Exception ex) {
                if (retry++ >= 3) {
                    throw ex;
                }
                ex.printStackTrace(System.err);
                Thread.sleep(3000);
                // retry
            }
        }
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
        CryptoMode cryptoMode = cryptoMode();

        assertNull(metadata.get(Headers.CRYPTO_KEY));
        assertNotNull(metadata.get(Headers.CRYPTO_KEY_V2));
        assertNotNull(metadata.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNotNull(metadata.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));

        if (cryptoMode == CryptoMode.AuthenticatedEncryption
            || cryptoMode == CryptoMode.StrictAuthenticatedEncryption) {
            assertNotNull(metadata.get(Headers.CRYPTO_TAG_LENGTH));
        } else {
            assertNull(metadata.get(Headers.CRYPTO_TAG_LENGTH));

        }

        assertNotNull(metadata.get(Headers.MATERIALS_DESCRIPTION));
        assertNotNull(metadata.get(Headers.UNENCRYPTED_CONTENT_LENGTH));

        String matdescStr = metadata.get(Headers.MATERIALS_DESCRIPTION);
        @SuppressWarnings("unchecked")
        Map<String, String> matdesc = Jackson.fromJsonString(matdescStr, Map.class);
        assertNotNull(matdesc.get(KMSEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
        // check per request encryption context
        assertEquals(matdesc.get("extra1"), "extra_value1");
    }
}
