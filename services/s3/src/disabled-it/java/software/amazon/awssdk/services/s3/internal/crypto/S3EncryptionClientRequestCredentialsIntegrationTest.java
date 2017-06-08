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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SimpleMaterialProvider;

/**
 * Tests that credentials overridden at the request level take precedence over the client for
 * {@link AmazonS3EncryptionClient}
 */
public class S3EncryptionClientRequestCredentialsIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = "java-sdk-encrypt-request-credentials-" + System.currentTimeMillis();
    private static final String KEY_NAME = "some-key";

    private static AmazonS3EncryptionClient s3EncryptionClient;

    @BeforeClass
    public static void setupFixture() throws NoSuchAlgorithmException, InvalidKeySpecException {
        s3.createBucket(BUCKET);
        SimpleMaterialProvider materialsProvider = new SimpleMaterialProvider()
                .withLatest(new EncryptionMaterials(CryptoTestUtils.getTestKeyPair()));
        // The client credentials should not be used in this test.
        s3EncryptionClient = new AmazonS3EncryptionClient(new AwsCredentials("invalid", "invalid"),
                                                          materialsProvider);
    }

    @AfterClass
    public static void tearDownFixture() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET);
    }

    /**
     * Per TT0067068018. The credentials set in the request were not being honored when deleting the
     * instructions file. We were copying the bucket name and key name to a new
     * {@link DeleteObjectRequest} but not any of the other request config like the credentials
     */
    @Test
    public void credentialsOveriddenInRequest_TakesPrecedenceOverClient() throws IOException {
        // This is the override credentials to be used.
        AwsCredentials overrideCrentials = credentials;

        // Put Object with credentials overridden
        File toUpload = CryptoTestUtils.generateRandomAsciiFile(100);
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET, KEY_NAME, toUpload);
        putObjectRequest.setRequestCredentials(overrideCrentials);
        s3EncryptionClient.putObject(putObjectRequest);

        // Delete Object with credentials overridden
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(BUCKET, KEY_NAME);
        deleteObjectRequest.setRequestCredentials(overrideCrentials);
        s3EncryptionClient.deleteObject(deleteObjectRequest);

        assertEquals(BUCKET, deleteObjectRequest.getBucketName());
        assertEquals(KEY_NAME, deleteObjectRequest.getKey());
    }
}
