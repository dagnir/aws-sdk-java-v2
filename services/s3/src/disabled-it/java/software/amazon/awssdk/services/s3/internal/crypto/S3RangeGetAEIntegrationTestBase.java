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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.deleteBucketAndAllContents;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateRandomAsciiFile;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.getTestKeyPair;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;
import static software.amazon.awssdk.util.StringUtils.UTF8;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public abstract class S3RangeGetAEIntegrationTestBase {
    private static final String TEST_BUCKET = tempBucketName(S3RangeGetAEIntegrationTestBase.class);
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;
    private static boolean get_only = false;

    @BeforeClass
    public static void setup() throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        tryCreateBucket(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    protected abstract CryptoMode cryptoMode();

    private CryptoConfiguration newCryptoConfiguration() {
        return new CryptoConfiguration().withCryptoMode(cryptoMode());
    }

    /**
     * Test range-get S3 object with metadata in v2 format using RSA kek.
     */
    @Test
    public void testWithMetaDataRSA() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                getTestKeyPair());
        doTestWithKekMaterial(kekMaterial, false);
    }

    /**
     * Test range-get S3 object with metadata in v2 format using AES kek.
     */
    @Test
    public void testWithMetadata() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                CryptoTestUtils.getTestSecretKey());
        doTestWithKekMaterial(kekMaterial, false);
    }

    private void doTestWithKekMaterial(EncryptionMaterials kekMaterial, boolean instructionFile) throws Exception {
        // s3v2 will put using v2 format but is able to read either v1 or v2 formats
        CryptoConfiguration config = newCryptoConfiguration();
        if (instructionFile) {
            config.setStorageMode(CryptoStorageMode.InstructionFile);
        }

        AmazonS3EncryptionClient s3v2 = new AmazonS3EncryptionClient(
                awsTestCredentials(),
                kekMaterial,
                config
        );
        final int pt_size = 100;
        final String bucketName = TEST_BUCKET;
        String v2key;

        String plaintext = null;
        if (get_only) {
            v2key = "encrypted-140325-012844-v2.txt";
        } else {
            String yymmdd_hhmmss = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());
            String key = "encrypted-" + yymmdd_hhmmss;
            v2key = key + "-v2.txt";
            System.err.println(bucketName + "/" + v2key);
            File file = generateRandomAsciiFile(pt_size);
            plaintext = FileUtils.readFileToString(file);
            System.err.println(plaintext);

            // upload file to s3 using v2 and get it back
            s3v2.putObject(bucketName, v2key, file);
        }
        int[][] test_ranges = {
                {1, 0},  // last 10 bytes
                {100, 9},  // last 10 bytes
                {0, 0},     // first byte
                {99, 99},   // last byte
                {99, 100},  // 1 past the last byte
                {0, 9},     // first 10 bytes
                {90, 99},   // last 10 bytes
                {90, 116},  // last 10 bytes
                {90, 120},  // last 10 bytes
        };
        for (int[] test_range : test_ranges) {
            int beginIndex = test_range[0];
            int endIndex = test_range[1];
            GetObjectRequest req = new GetObjectRequest(bucketName, v2key).withRange(beginIndex, endIndex);
            S3Object s3object;
            try {
                s3object = s3v2.getObject(req);
                if (CryptoMode.StrictAuthenticatedEncryption.equals(cryptoMode())) {
                    fail();
                }
            } catch (SecurityException ex) {
                if (CryptoMode.StrictAuthenticatedEncryption.equals(cryptoMode())) {
                    continue;
                }
                throw ex;
            }
            long instanceLen = s3object.getObjectMetadata().getInstanceLength();
            if (cryptoMode() == CryptoMode.AuthenticatedEncryption) {
                // Length of ciphertext = plaintext length + tag size 
                long physicalLen = pt_size + ContentCryptoScheme.AES_GCM.getTagLengthInBits() / 8;
                assertTrue(physicalLen == instanceLen);
            } else {
                long cipherBlockSize = ContentCryptoScheme.AES_CBC.getBlockSizeInBytes();
                long offset = cipherBlockSize - (pt_size % cipherBlockSize);
                long physicalLen = pt_size + offset;
                assertTrue(physicalLen == instanceLen);
            }
            byte[] retrieved = IOUtils.toByteArray(s3object.getObjectContent());
            int expectedLen;

            if (endIndex < beginIndex) {
                expectedLen = plaintext.length();
                beginIndex = 0;
            } else {
                expectedLen = Math.min(plaintext.length() - beginIndex,
                                       Math.max(0, endIndex - beginIndex + 1));
            }
            //            System.out.println("retrieved.length=" + retrieved.length);
            assertTrue(expectedLen == retrieved.length);
            if (retrieved.length > 0) {
                String result = new String(retrieved, UTF8);
                System.out.println(result);
                if (!get_only) {
                    String expected = plaintext.substring(beginIndex, beginIndex + expectedLen);
                    assertEquals(expected, result);
                }
            }
        }
        if (cleanup) {
            // A S3 raw client used to inspect the raw data
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            s3.deleteObject(bucketName, v2key);
            if (instructionFile) {
                s3.deleteObject(bucketName, v2key + ".instruction");
            }
        }
    }

    /**
     * Test range-get S3 object with instruction file in v2 format using RSA
     * kek.
     */
    @Test
    public void testWithInstFileRSA() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                getTestKeyPair());
        doTestWithKekMaterial(kekMaterial, true);
    }

    /**
     * Test range-get S3 object with instruction file in v2 format using AES
     * kek.
     */
    @Test
    public void testWithInstFile() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                CryptoTestUtils.getTestSecretKey());
        doTestWithKekMaterial(kekMaterial, true);
    }
}
