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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.test.util.DateUtils.yyMMdd_hhmmss;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.UploadObjectObserver;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.UploadObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.util.Md5Utils;

public class UploadObjectIntegrationTest extends S3IntegrationTestBase {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(UploadObjectIntegrationTest.class);
    private static final long OBJECT_SIZE = (10 << 20) - 16;   // 10M - 16 bytes
    private static final int PART_SIZE = 5 << 20;   // 5 M
    private static AmazonS3Client s3direct;
    private static AmazonS3EncryptionClient eo;
    private static File plaintextFile;

    @BeforeClass
    public static void beforeClass() throws IOException {
        setUpCredentials();
        s3direct = new AmazonS3Client(credentials);
        eo = new AmazonS3EncryptionClient(
                credentials,
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()));
        s3direct.createBucket(TEST_BUCKET);
        plaintextFile = CryptoTestUtils.generateRandomAsciiFile(OBJECT_SIZE);
    }

    @AfterClass
    public static void afterClass() {
        if (cleanup) {
            CryptoTestUtils.deleteBucketAndAllContents(s3direct, TEST_BUCKET);
        }
        eo.shutdown();
        s3direct.shutdown();
    }

    private static void p(Object o) {
        System.err.println(String.valueOf(o));
    }

    private String doTest(AmazonS3EncryptionClient s3,
                          CryptoConfiguration cryptoConfig) throws IOException,
                                                                   InterruptedException, ExecutionException {
        return doTest(s3, cryptoConfig, false);
    }

    private String doTest(AmazonS3EncryptionClient s3,
                          CryptoConfiguration cryptoConfig, boolean testDiskLimit) throws IOException,
                                                                                          InterruptedException,
                                                                                          ExecutionException {
        final long start = System.nanoTime();
        // Initiate upload
        final String key = "doTest-" + cryptoConfig.getCryptoMode() + "-"
                           + cryptoConfig.getStorageMode() + "-" + yyMMdd_hhmmss();
        ObjectMetadata partUploadMetadata = new ObjectMetadata();
        partUploadMetadata.setHeader("testing_upload_part_header", "testing_header_123");
        UploadObjectRequest req =
                new UploadObjectRequest(TEST_BUCKET, key, plaintextFile)
                        .withPartSize(PART_SIZE)
                        .withUploadPartMetadata(partUploadMetadata);
        if (testDiskLimit) {
            req.withDiskLimit(PART_SIZE * 2);
        }

        s3.uploadObject(req);
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
        File dest = File.createTempFile(key, "test");
        dest.deleteOnExit();
        s3.getObject(new GetObjectRequest(TEST_BUCKET, key), dest);
        byte[] srcMD5 = Md5Utils.computeMD5Hash(plaintextFile);
        byte[] destMD5 = Md5Utils.computeMD5Hash(dest);
        assertTrue(Arrays.equals(srcMD5, destMD5));
        return key;
    }

    private boolean existsInstructionFile(String key) {
        try {
            s3direct.getObject(TEST_BUCKET, key + ".instruction");
            return true;
        } catch (AmazonS3Exception ex) {
            if (ex.getMessage().contains("The specified key does not exist")) {
                return false;
            } else {
                throw ex;
            }
        }
    }

    @Test
    public void testDefault()
            throws IOException, InterruptedException, ExecutionException {
        final String key = doTest(eo, new CryptoConfiguration());
        assertFalse(existsInstructionFile(key));
    }

    @Test
    public void testDiskLimit()
            throws IOException, InterruptedException, ExecutionException {
        final String key = doTest(eo, new CryptoConfiguration(), true);
        assertFalse(existsInstructionFile(key));
    }

    @Test
    public void testAE() throws IOException, InterruptedException,
                                ExecutionException {
        final CryptoConfiguration cryptoConfig = new CryptoConfiguration()
                .withCryptoMode(CryptoMode.AuthenticatedEncryption);
        final AmazonS3EncryptionClient s3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(
                CryptoTestUtils.getTestSecretKey()), cryptoConfig);
        final String key = doTest(s3, cryptoConfig);
        assertFalse(existsInstructionFile(key));
    }

    @Test
    public void testInstructionFile()
            throws IOException, InterruptedException, ExecutionException {
        final CryptoConfiguration cryptoConfig = new CryptoConfiguration()
                .withStorageMode(CryptoStorageMode.InstructionFile);
        final AmazonS3EncryptionClient s3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(
                CryptoTestUtils.getTestSecretKey()), cryptoConfig);
        final String key = doTest(s3, cryptoConfig);
        assertTrue(existsInstructionFile(key));
    }

    @Test
    public void testInstructionFileAE() throws IOException,
                                               InterruptedException, ExecutionException {
        final CryptoConfiguration cryptoConfig = new CryptoConfiguration()
                .withCryptoMode(CryptoMode.AuthenticatedEncryption)
                .withStorageMode(CryptoStorageMode.InstructionFile);
        final AmazonS3EncryptionClient s3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(
                CryptoTestUtils.getTestSecretKey()), cryptoConfig);
        final String key = doTest(s3, cryptoConfig);
        assertTrue(existsInstructionFile(key));
    }

    @Test
    public void testAbort()
            throws IOException, InterruptedException, ExecutionException {
        // Initiate upload
        final String key = "testAbort-" + yyMMdd_hhmmss();
        UploadObjectRequest req =
                new UploadObjectRequest(TEST_BUCKET, key, plaintextFile)
                        .withPartSize(PART_SIZE);
        BrokenUploadObjectObserver observer = new BrokenUploadObjectObserver();
        req.withUploadObjectObserver(observer);
        try {
            eo.uploadObject(req);
            fail("Should have thrown exception.");
        } catch (ExecutionException e) {
            assertEquals(e.getCause().getMessage(), "testing abort");
        }
    }

    private static class BrokenUploadObjectObserver extends UploadObjectObserver {
        @Override
        protected UploadPartResult uploadPart(UploadPartRequest reqUploadPart) {
            throw new RuntimeException("testing abort");
        }
    }
}
