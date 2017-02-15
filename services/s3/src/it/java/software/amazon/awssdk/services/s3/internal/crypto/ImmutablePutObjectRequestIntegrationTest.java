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
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.deleteBucketAndAllContents;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class ImmutablePutObjectRequestIntegrationTest {
    private static final String TEST_BUCKET = tempBucketName(ImmutablePutObjectRequestIntegrationTest.class);
    private static boolean cleanup = true;
    private static AmazonS3Client s3;

    @BeforeClass
    public static void setup() throws Exception {
        s3 = new AmazonS3EncryptionClient(awsTestCredentials(),
                                          new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()));
        tryCreateBucket(s3, TEST_BUCKET);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    @Test
    public void test() throws Exception {
        byte[] bytes = new byte[100];
        InputStream is = new ByteArrayInputStream(bytes);
        String key = "key";
        final ObjectMetadata meta = new ObjectMetadata();
        final Map<String, String> immutableMap = Collections.emptyMap();
        meta.setUserMetadata(immutableMap);
        try {
            meta.addUserMetadata("name", "value");
            fail();
        } catch (UnsupportedOperationException expected) {
        }
        meta.setContentLength(bytes.length);
        PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, key, is, meta);
        // Put it
        s3.putObject(req);
        // Get it back
        S3Object s3obj = s3.getObject(TEST_BUCKET, key);
        String ucl = s3obj.getObjectMetadata().getUserMetadata()
                          .get(Headers.UNENCRYPTED_CONTENT_LENGTH);
        long uclLen = Long.parseLong(ucl);
        byte[] ba = IOUtils.toByteArray(s3obj.getObjectContent());
        assertTrue(uclLen == ba.length);
    }
}
