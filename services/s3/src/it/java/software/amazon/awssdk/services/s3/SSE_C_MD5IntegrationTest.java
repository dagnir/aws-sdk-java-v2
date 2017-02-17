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

package software.amazon.awssdk.services.s3;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SseCustomerKey;
import software.amazon.awssdk.services.s3.transfer.Download;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.util.Md5Utils;

// Used to verify the fix for https://tt.amazon.com/0049303822
public class SSE_C_MD5IntegrationTest extends S3IntegrationTestBase {
    private static String bucketName = CryptoTestUtils.tempBucketName("SSE-C-MD5IntegrationTest");
    private static String key = "key";
    private static AmazonS3Client s3;
    private static File file;
    private static long contentLength = 100;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        s3 = new AmazonS3TestClient(credentials);
        CryptoTestUtils.tryCreateBucket(s3, bucketName);
        file = CryptoTestUtils.generateRandomAsciiFile(contentLength);
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
        s3.shutdown();
    }

    @Test
    public void test() throws Exception {
        SseCustomerKey sse_c = new SseCustomerKey(CryptoTestUtils.getTestSecretKey());
        s3.putObject(new PutObjectRequest(bucketName, key, file).withSseCustomerKey(sse_c));

        TransferManager tm = new TransferManager(s3);
        File dest = new File("/tmp", UUID.randomUUID().toString());
        Download download = tm.download(new GetObjectRequest(bucketName, key)
                                                .withSSECustomerKey(sse_c), dest);
        download.waitForCompletion();
        tm.shutdownNow(false);
        byte[] expected = Md5Utils.computeMD5Hash(file);
        byte[] actual = Md5Utils.computeMD5Hash(dest);
        Assert.assertTrue(Arrays.equals(expected, actual));
    }
}
