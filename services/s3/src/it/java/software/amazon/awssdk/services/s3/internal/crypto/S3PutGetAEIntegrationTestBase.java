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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.bytesOf;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateRandomAsciiFile;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.getTestKeyPair;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.valueOf;
import static software.amazon.awssdk.util.StringUtils.UTF8;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.util.json.Jackson;

/**
 * Generate some plaintext, upload to S3 using v1 and v2 encryption formats,
 * compare the results to make sure they are as expected.
 *
 * @author hchar
 */
public abstract class S3PutGetAEIntegrationTestBase {
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(S3PutGetAEIntegrationTestBase.class);
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;

    @BeforeClass
    public static void setup() throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    protected abstract CryptoMode cryptoMode();

    /**
     * Test encrypting and decrypting S3 object with metadata in both v1 and v2
     * formats using RSA kek.
     */
    @Test
    public void testWihMetaDataRSA() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                getTestKeyPair());
        doTestWithMetadata(kekMaterial);
    }

    /**
     * Test encrypting and decrypting S3 object with metadata in both v1 and v2
     * formats using AES kek.
     */
    @Test
    public void testWithMetadata() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                new SecretKeySpec(new byte[16], "AES"));
        doTestWithMetadata(kekMaterial);
    }

    private void doTestWithMetadata(EncryptionMaterials kekMaterial) throws Exception {
        String yymmdd_hhmmss = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());
        final String bucketName = TEST_BUCKET, key = "encrypted-" + yymmdd_hhmmss;
        String v1key = key + "-v1.txt";
        String v2key = key + "-v2.txt";
        System.err.println(key + "/" + bucketName);

        // s3v1 will put using v1 format but is able to read either v1 or v2 formats
        AmazonS3EncryptionClient s3v1 = new AmazonS3EncryptionClient(
                awsTestCredentials(),
                kekMaterial
        );
        // s3v2 will put using v2 format but is able to read either v1 or v2 formats
        AmazonS3EncryptionClient s3v2 = new AmazonS3EncryptionClient(
                awsTestCredentials(), kekMaterial,
                new CryptoConfiguration()
                        .withCryptoMode(cryptoMode()));
        // A S3 raw client used to inspect the raw data
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());

        File file = generateRandomAsciiFile(100);
        final String plaintext = FileUtils.readFileToString(file);
        System.err.println(plaintext);

        // upload file to s3 using v1 and get it back
        s3v1.putObject(bucketName, v1key, file);
        // verify s3v1 is able to read back and decrypt the s3 object
        S3Object s3object = s3v1.getObject(bucketName, v1key);
        assertEquals(plaintext, valueOf(s3object));
        // verify s3v2 is able to read back and decrypt the v1 format
        if (CryptoMode.StrictAuthenticatedEncryption.equals(cryptoMode())) {
            try {
                s3object = s3v2.getObject(bucketName, v1key);
                fail();
            } catch (SecurityException expected) {
                // Ignored or expected.
            }
        } else {
            s3object = s3v2.getObject(bucketName, v1key);
            assertEquals(plaintext, valueOf(s3object));
        }
        // Check the raw user metadata
        s3object = s3.getObject(bucketName, v1key);
        Map<String, String> map = s3object.getObjectMetadata().getUserMetadata();
        System.err.println(map.size() + ": " + map);
        assertNull(map.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNull(map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        byte[] v1raw = bytesOf(s3object);
        assertFalse(Arrays.equals(v1raw, plaintext.getBytes(UTF8)));

        // upload file to s3 using v2 and get it back
        s3v2.putObject(bucketName, v2key, file);
        // verify s3v2 is able to read back and decrypt the s3 object
        s3object = s3v2.getObject(bucketName, v2key);
        assertEquals(plaintext, valueOf(s3object));
        // verify s3v1 is able to read back and decrypt the v2 format
        s3object = s3v1.getObject(bucketName, v2key);
        assertEquals(plaintext, valueOf(s3object));

        // Check the raw user metadata
        s3object = s3.getObject(bucketName, v2key);
        map = s3object.getObjectMetadata().getUserMetadata();
        System.err.println(map.size() + ": " + map);
        assertEquals(ContentCryptoScheme.AES_GCM.getCipherAlgorithm(), map.get(Headers.CRYPTO_CEK_ALGORITHM));
        if (kekMaterial.getKeyPair() == null) {
            assertEquals(S3KeyWrapScheme.AESWrap,
                         map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        } else {
            assertEquals(
                    S3KeyWrapScheme.RSA_ECB_OAEPWithSHA256AndMGF1Padding,
                    map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        }
        byte[] v2raw = bytesOf(s3object);
        assertFalse(Arrays.equals(v2raw, plaintext.getBytes(UTF8)));
        assertFalse(Arrays.equals(v1raw, v2raw));

        if (cleanup) {
            s3.deleteObject(bucketName, v1key);
            s3.deleteObject(bucketName, v2key);
        }
    }

    /**
     * Test encrypting and decrypting S3 object with instruction file in both v1
     * and v2 formats using RSA kek.
     */
    @Test
    public void testWithInstFileRSA() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                getTestKeyPair());
        doTestWithInstFile(kekMaterial);
    }

    /**
     * Test encrypting and decrypting S3 object with instruction file in both v1
     * and v2 formats using AES kek.
     */
    @Test
    public void testWithInstFile() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                new SecretKeySpec(new byte[16], "AES"));
        doTestWithInstFile(kekMaterial);
    }

    public void doTestWithInstFile(EncryptionMaterials kekMaterial) throws Exception {
        String yymmdd_hhmmss = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());
        String bucketName = TEST_BUCKET, key = "encrypted-" + yymmdd_hhmmss;
        String v1key = key + "-v1.txt";
        String v2key = key + "-v2.txt";
        System.err.println(key + "/" + bucketName);

        // s3v1 will put using v1 format but is able to read either v1 or v2 formats
        AmazonS3EncryptionClient s3v1 = new AmazonS3EncryptionClient(
                awsTestCredentials(),
                kekMaterial,
                new CryptoConfiguration()
                        .withStorageMode(CryptoStorageMode.InstructionFile)
        );
        // s3v2 will put using v2 format but is able to read either v1 or v2 formats
        AmazonS3EncryptionClient s3v2 = new AmazonS3EncryptionClient(
                awsTestCredentials(),
                kekMaterial,
                new CryptoConfiguration()
                        .withStorageMode(CryptoStorageMode.InstructionFile)
                        .withCryptoMode(cryptoMode())
        );
        // A S3 raw client used to inspect the raw data
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());

        File file = CryptoTestUtils.generateRandomAsciiFile(100);
        final String plaintext = FileUtils.readFileToString(file);
        System.err.println(plaintext);

        // upload file to s3 using v1 and get it back
        s3v1.putObject(bucketName, v1key, file);
        // verify s3v1 is able to read back and decrypt the s3 object

        S3Object s3object = s3v1.getObject(bucketName, v1key);
        assertEquals(plaintext, valueOf(s3object));
        // verify s3v2 is able to read back and decrypt the v1 format
        if (CryptoMode.StrictAuthenticatedEncryption.equals(cryptoMode())) {
            try {
                s3object = s3v2.getObject(bucketName, v1key);
                fail();
            } catch (SecurityException expected) {
                // Ignored or expected.
            }
        } else {
            s3object = s3v2.getObject(bucketName, v1key);
            assertEquals(plaintext, valueOf(s3object));
        }

        // Check the instruction file for v1 format
        s3object = s3v1.getObject(bucketName, v1key + ".instruction");
        String json = ContentCryptoMaterial.parseInstructionFile(s3object);
        @SuppressWarnings("unchecked")
        Map<String, String> imap = Jackson.fromJsonString(json, Map.class);
        assertNull(imap.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNull(imap.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));

        // Check the raw user metadata
        s3object = s3.getObject(bucketName, v1key);
        Map<String, String> map = s3object.getObjectMetadata().getUserMetadata();
        System.err.println(map.size() + ": " + map);
        assertNull(map.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNull(map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        byte[] v1raw = bytesOf(s3object);
        assertFalse(Arrays.equals(v1raw, plaintext.getBytes(UTF8)));

        // upload file to s3 using v2 and get it back
        s3v2.putObject(bucketName, v2key, file);
        // verify s3v2 is able to read back and decrypt the s3 object
        if (CryptoMode.StrictAuthenticatedEncryption.equals(cryptoMode())) {
            try {
                s3object = s3v2.getObject(bucketName, v1key);
                fail();
            } catch (SecurityException expected) {
                // Ignored or expected.
            }
        } else {
            s3object = s3v2.getObject(bucketName, v1key);
            assertEquals(plaintext, valueOf(s3object));
        }
        // verify s3v1 is able to read back and decrypt the v2 format
        s3object = s3v1.getObject(bucketName, v2key);
        assertEquals(plaintext, valueOf(s3object));

        // Check the instruction file for v2 format
        try {
            s3object = s3v2.getObject(bucketName, v2key + ".instruction");
            if (CryptoMode.StrictAuthenticatedEncryption.equals(cryptoMode())) {
                fail(); // instruction file is un-encrypted so it should have failed
            }
        } catch (SecurityException ex) {
            if (!CryptoMode.StrictAuthenticatedEncryption.equals(cryptoMode())) {
                throw ex;   // unexpected
            }
            s3object = s3.getObject(bucketName, v2key + ".instruction");
        }
        json = ContentCryptoMaterial.parseInstructionFile(s3object);
        @SuppressWarnings("unchecked")
        Map<String, String> imap2 = Jackson.fromJsonString(json, Map.class);
        imap = imap2;
        assertEquals(ContentCryptoScheme.AES_GCM.getCipherAlgorithm(), imap.get(Headers.CRYPTO_CEK_ALGORITHM));
        if (kekMaterial.getKeyPair() == null) {
            assertEquals(S3KeyWrapScheme.AESWrap,
                         imap.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        } else {
            assertEquals(
                    S3KeyWrapScheme.RSA_ECB_OAEPWithSHA256AndMGF1Padding,
                    imap.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        }

        // Check the raw user metadata
        s3object = s3.getObject(bucketName, v2key);
        map = s3object.getObjectMetadata().getUserMetadata();
        System.err.println(map.size() + ": " + map);
        assertNull(map.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNull(map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        byte[] v2raw = bytesOf(s3object);
        assertFalse(Arrays.equals(v2raw, plaintext.getBytes(UTF8)));
        assertFalse(Arrays.equals(v1raw, v2raw));

        if (cleanup) {
            s3.deleteObject(bucketName, v1key);
            s3.deleteObject(bucketName, v1key + ".instruction");
            s3.deleteObject(bucketName, v2key);
            s3.deleteObject(bucketName, v2key + ".instruction");
        }
    }
}
