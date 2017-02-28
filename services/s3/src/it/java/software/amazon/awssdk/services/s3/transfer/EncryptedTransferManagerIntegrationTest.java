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

package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Integration tests for TransferManager, using the encrypted S3 client.
 */
public class EncryptedTransferManagerIntegrationTest extends TransferManagerTestBase {

    /**
     * Tests that we can use the S3 encryption client to upload multipart
     * uploads to S3 using TransferManager and then download them correctly.
     */
    @Test
    public void testEncryptedMultipartUpload() throws Exception {
        AmazonS3EncryptionClient s3EncryptionClient = new AmazonS3EncryptionClient(credentials, new EncryptionMaterials(generateAsymmetricKeyPair()));
        tm = new TransferManager(s3EncryptionClient);
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMinimumUploadPartSize(10 * MB);
        configuration.setMultipartUploadThreshold(20 * MB);
        tm.setConfiguration(configuration);

        s3.createBucket(bucketName);
        String smallFileKey = "small";
        String largeFileKey = "large";
        RandomTempFile smallFile = new RandomTempFile(smallFileKey, 2 * MB);
        RandomTempFile largeFile = new RandomTempFile(largeFileKey, 25 * MB);
        tm.upload(new PutObjectRequest(bucketName, smallFileKey, smallFile)).waitForCompletion();
        tm.upload(new PutObjectRequest(bucketName, largeFileKey, largeFile)).waitForCompletion();

        // Make sure the data in S3 really was encrypted
        S3Object undecryptedSmallObject = s3.getObject(bucketName, smallFileKey);
        S3Object undecryptedLargeObject = s3.getObject(bucketName, largeFileKey);
        assertFalse(doesFileEqualStream(smallFile, undecryptedSmallObject.getObjectContent()));
        assertFalse(doesFileEqualStream(largeFile, undecryptedLargeObject.getObjectContent()));

        // Make sure we can decrypt the data and that it matches exactly
        S3Object decryptedSmallObject = s3EncryptionClient.getObject(bucketName, smallFileKey);
        S3Object decryptedLargeObject = s3EncryptionClient.getObject(bucketName, largeFileKey);
        assertFileEqualsStream(smallFile, decryptedSmallObject.getObjectContent());
        assertFileEqualsStream(largeFile, decryptedLargeObject.getObjectContent());

        // Make sure we can download the data to a file with TransferManager
        File tempFile = File.createTempFile("s3-tran-man-integ-test-", ".tmp");
        tempFile.deleteOnExit();
        tm.download(bucketName, largeFileKey, tempFile).waitForCompletion();
        assertFileEqualsFile(largeFile, tempFile);
        tm.download(bucketName, smallFileKey, tempFile).waitForCompletion();
        assertFileEqualsFile(smallFile, tempFile);
    }

    private KeyPair generateAsymmetricKeyPair() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(1024, new SecureRandom());
            return keyGenerator.generateKeyPair();
        } catch (Exception e) {
            fail("Unable to generate asymmetric keys: " + e.getMessage());
            return null;
        }
    }
}
