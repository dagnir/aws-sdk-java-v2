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
import static software.amazon.awssdk.services.s3.internal.Constants.GB;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CopyPartRequest;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.TransferManagerConfiguration;
import software.amazon.awssdk.services.s3.transfer.Upload;
import software.amazon.awssdk.test.util.RandomInputStream;

public class S3MultipartCopyAEIntegrationTest extends S3IntegrationTestBase {

    /** Length of the random temp file to upload. */
    private static final long RANDOM_OBJECT_DATA_LENGTH = 6 * GB;
    private final String BUFFER_MULTIPART_UPLOAD_PROPERTY = "software.amazon.awssdk.services.s3.transfer.bufferMultipartUploads";
    TransferManager tm;
    /** Name of the source bucket we copy from. */
    private String sourceBucketName = "hchar-test-5";
    /** Name of the target bucket we copy to. */
    private String targetBucketName = "hchar-test-5";
    /** Name of the source Object we copy from. */
    private String sourceObject = "integ-test-source-object-" + new Date().getTime();
    /** Name of the target Object we copy to. */
    private String targetObject = "integ-test-target-object-" + new Date().getTime();
    /** Encryption client using object metadata for crypto metadata storage. */
    private AmazonS3 s3_metadata;

    static List<PartETag> GetETags(List<CopyPartResult> responses) {
        List<PartETag> etags = new ArrayList<PartETag>();
        for (CopyPartResult response : responses) {
            etags.add(response.getPartETag());
        }
        return etags;
    }

    /**
     * Generates a sample asymmetric key pair for use in encrypting and decrypting.
     * <p>
     * For real applications, you'll want to save the key pair somewhere so
     * you can share it.
     * <p>
     * Several good online sources also explain how to create an RSA key pair
     * from the command line using OpenSSL, for example:
     * http://en.wikibooks.org/wiki/Transwiki:Generate_a_keypair_using_OpenSSL
     */
    private static KeyPair generateAsymmetricKeyPair() throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024, new SecureRandom());
        return keyGenerator.generateKeyPair();
    }

    /**
     * Set up the tests.  Get AWS credentials, generate asymmetric keys, construct encryption providers, and create a test bucket and object.
     */
    @Before
    public void setUpClients() throws Exception {
        if (!CryptoTestUtils.runTimeConsumingTests()) {
            return;
        }
        super.setUp();

        System.setProperty(BUFFER_MULTIPART_UPLOAD_PROPERTY, "true");

        //upload the large object to do the test
        s3.createBucket(sourceBucketName);
        s3.createBucket(targetBucketName);
        tm = new TransferManager(s3, (ThreadPoolExecutor) Executors.newFixedThreadPool(50));
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMinimumUploadPartSize(10 * MB);
        configuration.setMultipartUploadThreshold(20 * MB);
        tm.setConfiguration(configuration);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(RANDOM_OBJECT_DATA_LENGTH);

        Upload upload = tm.upload(sourceBucketName, sourceObject, new RandomInputStream(RANDOM_OBJECT_DATA_LENGTH), objectMetadata);
        upload.waitForCompletion();

        //set up the encrypo client
        generateAsymmetricKeyPair();
        EncryptionMaterials encryptionMaterials = new EncryptionMaterials(generateAsymmetricKeyPair());
        s3_metadata = new AmazonS3EncryptionClient(credentials,
                                                   encryptionMaterials,
                                                   new CryptoConfiguration().withCryptoMode(CryptoMode.AuthenticatedEncryption));
    }

    /**
     * Ensure that any created test resources are correctly released.
     */
    @After
    public void tearDown() {
        if (!CryptoTestUtils.runTimeConsumingTests()) {
            return;
        }
        deleteBucketAndAllContents(sourceBucketName);
        deleteBucketAndAllContents(targetBucketName);
        tm.shutdownNow();

    }

    @Test
    public void testMultipartCopyCrypto() {
        if (!CryptoTestUtils.runTimeConsumingTests()) {
            System.out.println("Please set the environment variable RUN_TIME_CONSUMING_TESTS to run testMultipartCopyCrypto");
            return;
        }

        List<CopyPartResult> copyResponses = new ArrayList<CopyPartResult>();

        // Get object size.
        GetObjectMetadataRequest metadata = new GetObjectMetadataRequest(sourceBucketName, sourceObject);

        ObjectMetadata metadataResult = s3.getObjectMetadata(metadata);
        long objectSize = metadataResult.getContentLength(); // in bytes


        InitiateMultipartUploadRequest initiateRequest =
                new InitiateMultipartUploadRequest(targetBucketName, targetObject, metadataResult);

        InitiateMultipartUploadResult initResult =
                s3_metadata.initiateMultipartUpload(initiateRequest);


        // Copy parts.
        long partSize = 5 * MB;

        long bytePosition = 0;

        for (int i = 1; bytePosition < objectSize; i++) {
            CopyPartRequest copyRequest = new CopyPartRequest()
                    .withDestinationBucketName(targetBucketName)
                    .withDestinationKey(targetObject)
                    .withSourceBucketName(sourceBucketName)
                    .withSourceKey(sourceObject)
                    .withUploadId(initResult.getUploadId())
                    .withFirstByte(bytePosition)
                    .withLastByte(((bytePosition + partSize) >= objectSize) ?
                                  (objectSize - 1) : (bytePosition + partSize - 1))
                    .withPartNumber(i);

            copyResponses.add(s3_metadata.copyPart(copyRequest));
            bytePosition += partSize;
        }

        CompleteMultipartUploadRequest completeRequest = new
                CompleteMultipartUploadRequest(
                targetBucketName,
                targetObject,
                initResult.getUploadId(),
                GetETags(copyResponses));

        s3_metadata.completeMultipartUpload(completeRequest);

        metadataResult = s3_metadata.getObjectMetadata(targetBucketName, targetObject);
        assertEquals(RANDOM_OBJECT_DATA_LENGTH, metadataResult.getContentLength());
    }

}