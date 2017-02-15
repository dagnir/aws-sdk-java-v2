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
import java.io.FileInputStream;
import java.util.Arrays;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.Upload;
import software.amazon.awssdk.util.Md5Utils;

// https://issues.amazon.com/JAVA-1035
public class PutFileInputStreamIntegrationTest extends S3IntegrationTestBase {

    private static String bucketName = CryptoTestUtils.tempBucketName(PutFileInputStreamIntegrationTest.class);
    private static String key = "key";
    private static AmazonS3Client s3;
    private static File file;
    private static long contentLength;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        s3 = new AmazonS3TestClient(credentials,
                                    new ClientConfiguration().withSignerOverride("AWSS3V4SignerType"));
        CryptoTestUtils.tryCreateBucket(s3, bucketName);
        // make the content length 100 byte larger than the default mark-and-reset limit
        contentLength = new PutObjectRequest(bucketName, key, file).getReadLimit() + 100;
        file = CryptoTestUtils.generateRandomAsciiFile(contentLength);
        Assert.assertTrue(contentLength > 0);
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
        s3.shutdown();
    }

    @Test
    public void testPutExceedDefaultResetSize() throws Exception {
        FileInputStream fis = new FileInputStream(file);
        ObjectMetadata meta = new ObjectMetadata();
        s3.putObject(new PutObjectRequest(bucketName, key, fis, meta));
        fis.close();
        byte[] actual = Md5Utils.computeMD5Hash(s3.getObject(bucketName, key).getObjectContent());
        byte[] expected = Md5Utils.computeMD5Hash(file);
        Assert.assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testUploadExceedDefaultResetSize() throws Exception {
        TransferManager tm = new TransferManager(s3);
        FileInputStream fis = new FileInputStream(file);
        ObjectMetadata meta = new ObjectMetadata();
        Upload upload = tm.upload(new PutObjectRequest(bucketName, key, fis, meta));
        upload.waitForCompletion();
        fis.close();
        tm.shutdownNow(false);
        byte[] actual = Md5Utils.computeMD5Hash(s3.getObject(bucketName, key).getObjectContent());
        byte[] expected = Md5Utils.computeMD5Hash(file);
        Assert.assertTrue(Arrays.equals(expected, actual));
    }
}
