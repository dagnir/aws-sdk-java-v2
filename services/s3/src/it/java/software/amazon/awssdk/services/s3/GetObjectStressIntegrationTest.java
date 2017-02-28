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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.TransferManagerConfiguration;
import software.amazon.awssdk.services.s3.transfer.Upload;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Performs a get object operation on S3 with large size files.
 */
@Category(S3Categories.ReallySlow.class)
public class GetObjectStressIntegrationTest extends S3IntegrationTestBase {

    /** Length of the random temp file to upload */
    private static final long RANDOM_OBJECT_DATA_LENGTH = 1024 * 1024 * 1024 * 3L;

    /** Name of the test bucket these tests will create, test, delete, etc */
    private String expectedBucketName = "integ-test-bucket-"
            + new Date().getTime();

    /**
     * Name of the file that will be temporarily generated on disk, and then
     * stored in S3
     */
    private String expectedObjectName = "integ-test-file-"
            + new Date().getTime();

    /**
     * The temporary file to be uploaded.
     */
    private File temporaryFile;

    /** Asymmetric crypto key pair for use in encryption and decryption. */
    private KeyPair keyPair;

    /** Encryption providers */
    private AmazonS3 defaultAsymmetricEncryption;

    /** Transfer manager configuration */
    private static TransferManagerConfiguration configuration = new TransferManagerConfiguration();

    /** Transfer manager to upload the files. */
    private static TransferManager transfer = null;

    /** Prefix for the encrypted file name. */
    private static final String cryptoClientPrefix = "crypto-";

    @Before
    public void setup() throws Exception {
        super.setUp();

        generateAsymmetricKeyPair();

        temporaryFile = new RandomTempFile(expectedObjectName,
                RANDOM_OBJECT_DATA_LENGTH);

        // Default encryption clients
        defaultAsymmetricEncryption = new AmazonS3EncryptionClient(credentials,
                new EncryptionMaterials(keyPair));

        s3.createBucket(expectedBucketName);

        configuration.setMinimumUploadPartSize(1024 * 1024 * 1024);
        transfer = new TransferManager(defaultAsymmetricEncryption);
        transfer.setConfiguration(configuration);
        Upload t = transfer.upload(expectedBucketName, cryptoClientPrefix
                + expectedObjectName, temporaryFile);

        t.waitForCompletion();
    }

    @After
    public void tearDown() throws Exception {
        if (temporaryFile != null) {
            temporaryFile.delete();
        }
        deleteBucketAndAllContents(expectedBucketName);
    }

    /**
     * Performs a range GET on the encrypted . Checks if the retrieved stream
     * matches with the corresponding bytes read from the file.
     */
    @Test
    public void testGetRange() throws IOException {
        // An arbitrary range of bytes within the 32 byte test file.
        long rangeBegin = 5, rangeEnd = RANDOM_OBJECT_DATA_LENGTH;
        int numberOfBytesToSkip = 5;
        int bytesSkipped = 0;
        // GET the object with both the encryption client and the standard
        // client
        GetObjectRequest retrieveCryptoObjectRequest = new GetObjectRequest(
                expectedBucketName, cryptoClientPrefix + expectedObjectName);
        retrieveCryptoObjectRequest.setRange(rangeBegin, rangeEnd);
        S3Object cryptoObject = defaultAsymmetricEncryption
                .getObject(retrieveCryptoObjectRequest);

        FileInputStream fis = new FileInputStream(temporaryFile);
        while (numberOfBytesToSkip > 0) {
            bytesSkipped = (int) fis.skip(numberOfBytesToSkip);
            numberOfBytesToSkip -= bytesSkipped;
            if (numberOfBytesToSkip > 0 && bytesSkipped == -1) {
                throw new AmazonClientException("Not able to skip "
                        + numberOfBytesToSkip + " bytes in the file "
                        + temporaryFile.getAbsolutePath());
            }
        }
        assertStreamEqualsStream(fis, cryptoObject.getObjectContent());
    }

    /*
     * Generates an asymmetric key pair for use in encrypting and decrypting.
     */
    private void generateAsymmetricKeyPair() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(1024, new SecureRandom());
            keyPair = keyGenerator.generateKeyPair();
        } catch (Exception e) {
            fail("Unable to generate asymmetric keys: " + e.getMessage());
        }
    }
}
