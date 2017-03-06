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

import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.test.util.DateUtils.yyMMddhhmmss;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.SimpleMaterialProvider;
import software.amazon.awssdk.services.s3.model.UploadObjectRequest;
import software.amazon.awssdk.util.Md5Utils;
import software.amazon.awssdk.util.StringMapBuilder;

public class UploadObject2IntegrationTest extends S3IntegrationTestBase {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(UploadObject2IntegrationTest.class);
    private static final long OBJECT_SIZE = (10 << 20) - 16;   // 10M - 16 bytes
    private static final int PART_SIZE = 5 << 20;   // 5 M
    private static AmazonS3Client s3direct;
    private static AmazonS3EncryptionClient ae;
    private static File plaintextFile;

    @BeforeClass
    public static void beforeClass()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedOperationException {
        setUpCredentials();
        s3direct = new AmazonS3Client(credentials);
        ae = new AmazonS3EncryptionClient(
                credentials,
                new SimpleMaterialProvider()
                        .withLatest(new EncryptionMaterials(CryptoTestUtils.getTestSecretKey())
                                            .addDescription("name", "testsecretkey")
                                   ).addMaterial(new EncryptionMaterials(CryptoTestUtils.getTestPublicKeyPair())
                                                         .addDescription("name", "testpublickeypair")),
                new LegacyClientConfiguration(),
                new CryptoConfiguration()
                        .withCryptoMode(CryptoMode.AuthenticatedEncryption)
        );
        s3direct.createBucket(TEST_BUCKET);
        plaintextFile = CryptoTestUtils.generateRandomAsciiFile(OBJECT_SIZE);
    }

    @AfterClass
    public static void afterClass() {
        if (cleanup) {
            CryptoTestUtils.deleteBucketAndAllContents(s3direct, TEST_BUCKET);
        }
        ae.shutdown();
        s3direct.shutdown();
    }

    private static void p(Object o) {
        System.err.println(String.valueOf(o));
    }

    @Test
    public void testMaterialDescrption_forPublicKeyPair() throws IOException,
                                                                 InterruptedException, ExecutionException,
                                                                 NoSuchAlgorithmException, InvalidKeySpecException {
        final long start = System.nanoTime();
        // Initiate upload
        final String key = "testMaterialDescrption_forPublicKeyPair" + "-" + yyMMddhhmmss();
        UploadObjectRequest req =
                new UploadObjectRequest(TEST_BUCKET, key, plaintextFile)
                        .withPartSize(PART_SIZE)
                        .withMaterialsDescription(new StringMapBuilder("name", "testpublickeypair").build());
        ae.uploadObject(req);
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
        File dest = File.createTempFile(key, "test");
        dest.deleteOnExit();
        AmazonS3EncryptionClient client = new AmazonS3EncryptionClient(
                credentials,
                new EncryptionMaterials(CryptoTestUtils.getTestKeyPair()).addDescription("name", "testpublickeypair"),
                new CryptoConfiguration()
                        .withCryptoMode(CryptoMode.AuthenticatedEncryption));
        client.getObject(new GetObjectRequest(TEST_BUCKET, key), dest);
        byte[] srcMD5 = Md5Utils.computeMD5Hash(plaintextFile);
        byte[] destMD5 = Md5Utils.computeMD5Hash(dest);
        assertTrue(Arrays.equals(srcMD5, destMD5));
        client.shutdown();
        return;
    }

    @Test
    public void testMaterialDescrption_forSecretKey() throws IOException,
                                                             InterruptedException, ExecutionException, NoSuchAlgorithmException,
                                                             InvalidKeySpecException {
        final long start = System.nanoTime();
        // Initiate upload
        final String key = "testMaterialDescrption_forSecretKey" + "-" + yyMMddhhmmss();
        UploadObjectRequest req =
                new UploadObjectRequest(TEST_BUCKET, key, plaintextFile)
                        .withPartSize(PART_SIZE)
                        .withMaterialsDescription(new StringMapBuilder("name", "testsecretkey").build());
        ae.uploadObject(req);
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
        File dest = File.createTempFile(key, "test");
        dest.deleteOnExit();
        AmazonS3EncryptionClient client = new AmazonS3EncryptionClient(
                credentials,
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()).addDescription("name", "testsecretkey"),
                new CryptoConfiguration()
                        .withCryptoMode(CryptoMode.AuthenticatedEncryption));
        client.getObject(new GetObjectRequest(TEST_BUCKET, key), dest);
        byte[] srcMD5 = Md5Utils.computeMD5Hash(plaintextFile);
        byte[] destMD5 = Md5Utils.computeMD5Hash(dest);
        assertTrue(Arrays.equals(srcMD5, destMD5));
        client.shutdown();
        return;
    }

    @Test
    public void testDefaultMaterialDescrption() throws IOException,
                                                       InterruptedException, ExecutionException, NoSuchAlgorithmException,
                                                       InvalidKeySpecException {
        final long start = System.nanoTime();
        // Initiate upload
        final String key = "testDefaultMaterialDescrption" + "-" + yyMMddhhmmss();
        UploadObjectRequest req =
                new UploadObjectRequest(TEST_BUCKET, key, plaintextFile)
                        .withPartSize(PART_SIZE);
        ae.uploadObject(req);
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
        File dest = File.createTempFile(key, "test");
        dest.deleteOnExit();
        AmazonS3EncryptionClient client = new AmazonS3EncryptionClient(
                credentials,
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()).addDescription("name", "testsecretkey"),
                new CryptoConfiguration()
                        .withCryptoMode(CryptoMode.AuthenticatedEncryption));
        client.getObject(new GetObjectRequest(TEST_BUCKET, key), dest);
        byte[] srcMD5 = Md5Utils.computeMD5Hash(plaintextFile);
        byte[] destMD5 = Md5Utils.computeMD5Hash(dest);
        assertTrue(Arrays.equals(srcMD5, destMD5));
        client.shutdown();
        return;
    }
}
