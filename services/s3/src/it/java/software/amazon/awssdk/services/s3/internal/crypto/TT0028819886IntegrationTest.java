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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class TT0028819886IntegrationTest extends S3IntegrationTestBase {
    private static final String TEST_BUCKET = CryptoTestUtils
            .tempBucketName(TT0028819886IntegrationTest.class);
    private static boolean cleanup = true;
    final EncryptionMaterials kekMaterial = new EncryptionMaterials(
            new SecretKeySpec(new byte[16], "AES"));
    private AmazonS3Client s3v1;
    private AmazonS3Client s3v2;

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
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    @Before
    public void before() throws Exception {
        setUpCredentials();
        s3v1 = new AmazonS3EncryptionClient(
                credentials, kekMaterial);
        s3v2 = new AmazonS3EncryptionClient(
                credentials, kekMaterial,
                new CryptoConfiguration().withCryptoMode(CryptoMode.AuthenticatedEncryption));
    }

    @After
    public void after() throws Exception {
        AmazonS3Client[] s3s = {s3v1, s3v2};
        for (AmazonS3Client s3 : s3s) {
            s3.shutdown();
        }
    }

    @Test
    public void testLessThanClaimed() throws Exception {
        AmazonS3Client[] s3s = {s3v1, s3v2};
        for (AmazonS3Client s3 : s3s) {
            try {
                doTestInconsistentLength(s3, 16, 17);
                fail();
            } catch (AmazonClientException expected) {

            }
        }
    }

    @Test
    public void testMoreThanClaimed() throws Exception {
        AmazonS3Client[] s3s = {s3v1, s3v2};
        for (AmazonS3Client s3 : s3s) {
            try {
                doTestInconsistentLength(s3, 16, 15);
                fail();
            } catch (AmazonClientException expected) {
            }
        }
    }

    @Test
    public void testEqualToClaimed() throws Exception {
        AmazonS3Client[] s3s = {s3v1, s3v2};
        for (AmazonS3Client s3 : s3s) {
            doTestInconsistentLength(s3, 16, 16);
        }
    }

    public void doTestInconsistentLength(AmazonS3Client s3, int size,
                                         int contentLength) throws Exception {
        byte[] bytes = new byte[size];
        System.err.println("bytes.length: " + bytes.length);
        InputStream is = new ByteArrayInputStream(bytes);
        String version = s3 == s3v1 ? "v1" : "v2";
        String key = TEST_BUCKET + "-" + size + ".encrypted-" + version;
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(contentLength);
        PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, key, is, meta);
        // Put it
        s3.putObject(req);
        // Get it back
        S3Object s3obj = s3.getObject(TEST_BUCKET, key);
        String ucl = s3obj.getObjectMetadata().getUserMetadata()
                          .get(Headers.UNENCRYPTED_CONTENT_LENGTH);
        long uclLen = Long.parseLong(ucl);
        System.err.println("size: " + size + ", uclLen: " + uclLen
                           + ", contentLength: " + contentLength);
        byte[] ba = IOUtils.toByteArray(s3obj.getObjectContent());
        System.err.println("ba.length: " + ba.length + ", uclLen: " + uclLen);
        assertTrue(uclLen == ba.length);
    }
}
