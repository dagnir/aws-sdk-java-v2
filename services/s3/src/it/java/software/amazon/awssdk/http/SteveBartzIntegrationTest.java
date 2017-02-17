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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;


public class SteveBartzIntegrationTest {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(SteveBartzIntegrationTest.class);
    private static AmazonS3EncryptionClient s3;

    public static AwsCredentials awsTestCredentials() throws IOException {
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
    public void test() throws Exception {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                        .withMaxNumErrors(4)
                        .withBytesReadBeforeException(0)
                        .withFakeIOException(false)
                        .withResetIntervalBeforeException(0)
                                                       );
        byte[] serializedValue = new byte[2704];
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(serializedValue.length);
        for (int i = 0; i < 2; i++) {
            assertTrue(
                    "i=" + i + ", metadata.getContentLength()="
                    + metadata.getContentLength(),
                    metadata.getContentLength() == serializedValue.length);
            try {
                s3.putObject(TEST_BUCKET, "testkey", new ByteArrayInputStream(serializedValue), metadata);
                break;
            } catch (RuntimeException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }
}
