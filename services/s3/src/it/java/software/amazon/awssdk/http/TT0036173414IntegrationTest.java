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

package software.amazon.awssdk.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;


public class TT0036173414IntegrationTest {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET =
            CryptoTestUtils.tempBucketName(TT0036173414IntegrationTest.class);
    private static final String TEST_KEY = "testkey";
    private static AmazonS3Client s3;

    public static AWSCredentials awsTestCredentials() throws IOException {
        return new PropertiesCredentials(new File(
                System.getProperty("user.home")
                + "/.aws/awsTestAccount.properties"));
    }

    @BeforeClass
    public static void setup() throws Exception {
        s3 = new AmazonS3EncryptionClient(awsTestCredentials(),
                                          new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()));
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
    }

    @AfterClass
    public static void tearDown() {
        // *** Important to clear the test config so as not to affect other tests
        AmazonHttpClient.configUnreliableTestConditions(null);
        if (cleanup) {
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
        }
        s3.shutdown();
    }

    @Test
    public void testFakeRuntimeException_Once() {
        try {
            AmazonHttpClient.configUnreliableTestConditions(
                    new UnreliableTestConfig()
                            .withMaxNumErrors(1)
                            .withBytesReadBeforeException(99)
                            .withFakeIOException(false)
                            .withResetIntervalBeforeException(1)
                                                           );
            InputStream is = new ByteArrayInputStream(new byte[100]);
            ObjectMetadata omd = new ObjectMetadata();
            omd.setContentLength(100);
            s3.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY, is, omd));
            Assert.fail();
        } catch (RuntimeException expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void testFakeIOException_Once() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                        .withMaxNumErrors(1)
                        .withBytesReadBeforeException(99)
                        .withFakeIOException(true)
                        .withResetIntervalBeforeException(1)
                                                       );
        InputStream is = new ByteArrayInputStream(new byte[100]);
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(100);
        PutObjectResult result = s3.putObject(new PutObjectRequest(TEST_BUCKET,
                                                                   TEST_KEY, is, omd));
        System.out.println(result);
    }

    @Test
    public void testFakeIOException_Twice() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                        .withMaxNumErrors(2)
                        .withBytesReadBeforeException(99)
                        .withFakeIOException(true)
                        .withResetIntervalBeforeException(1)
                                                       );
        InputStream is = new ByteArrayInputStream(new byte[100]);
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(100);
        PutObjectResult result = s3.putObject(new PutObjectRequest(TEST_BUCKET,
                                                                   TEST_KEY, is, omd));
        System.out.println(result);
    }

    @Test
    public void testFakeIOException_MaxRetries() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                        .withMaxNumErrors(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY)
                        .withBytesReadBeforeException(99)
                        .withFakeIOException(true)
                        .withResetIntervalBeforeException(1)
                                                       );
        InputStream is = new ByteArrayInputStream(new byte[100]);
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(100);
        PutObjectResult result = s3.putObject(new PutObjectRequest(TEST_BUCKET,
                                                                   TEST_KEY, is, omd));
        System.out.println(result);
    }

    @Test
    public void testFakeIOException_OneTooMany() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                        .withMaxNumErrors(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY + 1)
                        .withBytesReadBeforeException(99)
                        .withFakeIOException(true)
                        .withResetIntervalBeforeException(1)
                                                       );
        InputStream is = new ByteArrayInputStream(new byte[100]);
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(100);
        try {
            s3.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY, is, omd));
            Assert.fail();
        } catch (AmazonClientException expected) {
            expected.printStackTrace();
        }
    }
}
