/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.s3.crypto;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoRuntime;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.util.IOUtils;

/**
 * Integration tests for the Amazon S3 Encryption Client
 * with StrictAuthenticatedEncryption CryptoMode.
 */
public class S3GetObjectCryptoIntegrationTest extends S3IntegrationTestBase {

    /** The bucket created and used by these tests */
    private final static String BUCKET_NAME = "java-get-object-crypto-integ-test-" + new Date().getTime();

    /** The key used for testing nonMultiPart object */
    private final static String NON_MULTIPART_OBJECT_KEY = "nonMultiPartCryptoKey";

    /** Length of the random nonMultiPartObject file to upload */
    private static final long RANDOM_FILE_DATA_LENGTH = 1 * MB;

    /** Begin and End values in the range */
    private static final long RANGE_BEGIN = 5;
    private static final long RANGE_END = RANDOM_FILE_DATA_LENGTH - 20;

    /** The key used for testing MultiPart object */
    private final static String MULTIPART_OBJECT_KEY = "multiPartkeyCryptoKey";

    /** The unique Id that is returned when we initiate a MultipartUpload */
    private static String uploadId;

    /** The minimum size of a part in MultiPartObject */
    private final static long MIN_SIZE_FIRST_PART_IN_MB = 5 * MB;

    /** The test content that is used for testing part 2 of MultiPartObject. Contains only ASCII characters */
    private final static String TEST_STRING = "This is the content to be uploaded in part 2 of multipart object";

    /** The size of the TEST_STRING **/
    private final static long TEST_STRING_LENGTH = TEST_STRING.length();


    /** Encryption provider with StrictAuthenticatedEncryption */
    private static AmazonS3EncryptionClient strictAEClient;

    /** The file containing the nonMultiPartObject data uploaded to S3 */
    private static File nonMultiPartDataFile;

    /** The file containing the first part data of MultiPartObject uploaded to S3 */
    private static File firstPartDataFile;

    /** The name of the file containing the nonMultiPartObject data uploaded to S3 */
    private static final String nonMultiPartDataFileName = "nonMultiPartObject";

    /** The name of the file containing the first part data of MultiPartObject uploaded to S3 */
    private static String firstPartDataFileName = "multiPartObject";

    /** The byte array containing the nonMultiPartObject data uploaded to S3 */
    private static byte[] nonMultiPartByteData;

    /** The byte array containing the first part data of MultiPartObject uploaded to S3 */
    private static byte[] firstPartByteData;

    /**
     * Set up the tests.  Get AWS credentials, generate asymmetric keys, construct encryption providers, and create a test bucket.
     * Upload a single object and multi part object to S3.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();

        if(!CryptoRuntime.isBouncyCastleAvailable()) {
            CryptoRuntime.enableBouncyCastle();
        }
        CryptoRuntime.recheckAesGcmAvailablility();

        strictAEClient = new AmazonS3EncryptionClient(credentials,
                new EncryptionMaterials(new SecretKeySpec(new byte[32], "AES")),
                new CryptoConfiguration(CryptoMode.StrictAuthenticatedEncryption));

        createRandomFiles();
        strictAEClient.createBucket(BUCKET_NAME);
        doNonMultiPartUpload();
        doMultiPartUpload();
    }

    private static void createRandomFiles() throws Exception {
        nonMultiPartDataFile = getRandomTempFile(nonMultiPartDataFileName, RANDOM_FILE_DATA_LENGTH);
        nonMultiPartDataFile.deleteOnExit();
        nonMultiPartByteData = org.apache.commons.io.FileUtils.readFileToByteArray(nonMultiPartDataFile);

        firstPartDataFile = getRandomTempFile(firstPartDataFileName, MIN_SIZE_FIRST_PART_IN_MB);
        firstPartDataFile.deleteOnExit();
        firstPartByteData = org.apache.commons.io.FileUtils.readFileToByteArray(firstPartDataFile);
    }

    /**
     * Uploads a full object without parts
     * @throws Exception
     */
    private static void doNonMultiPartUpload() throws Exception {
        strictAEClient.putObject(new PutObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, nonMultiPartDataFile));
    }

    /**
     * Uploads a two part object to s3.
     * @throws Exception
     */
    private static void doMultiPartUpload() throws Exception {
        InitiateMultipartUploadResult initiateResult = strictAEClient.initiateMultipartUpload(
                new InitiateMultipartUploadRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                    .withStorageClass(StorageClass.ReducedRedundancy));
        uploadId = initiateResult.getUploadId();

        try {
            List<PartETag> partETags = uploadParts(BUCKET_NAME, uploadId);
            strictAEClient.completeMultipartUpload(new CompleteMultipartUploadRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY, uploadId, partETags));
        } catch (Exception exception) {
            System.out.println("Exception occured during multipartUpload with ID " + uploadId );
            strictAEClient.abortMultipartUpload(new AbortMultipartUploadRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY, uploadId));
            throw exception;
        }
    }

    private static List<PartETag> uploadParts(String bucketName, String uploadId) throws Exception {
        List<PartETag> partETags = new ArrayList<PartETag>();

        UploadPartResult uploadPartResult = strictAEClient.uploadPart(new UploadPartRequest()
            .withBucketName(bucketName)
            .withInputStream(new ByteArrayInputStream(firstPartByteData))
            .withKey(MULTIPART_OBJECT_KEY)
            .withPartNumber(1)
            .withPartSize(MIN_SIZE_FIRST_PART_IN_MB)
            .withUploadId(uploadId));
        assertEquals(1, uploadPartResult.getPartNumber());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        uploadPartResult = strictAEClient.uploadPart(new UploadPartRequest()
            .withBucketName(bucketName)
            .withInputStream(new StringInputStream(TEST_STRING))
            .withKey(MULTIPART_OBJECT_KEY)
            .withPartNumber(2)
            .withPartSize(TEST_STRING_LENGTH)
            .withUploadId(uploadId)
            .withLastPart(true));
        assertEquals(2, uploadPartResult.getPartNumber());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        return partETags;
    }

    /**
     * When getObject is called on nonMultiPartObject with no range and no partNumber,
     * returns the entire object.
     */
    @Test
    public void testGetObjectOnNonMultiPartObjectGivenNoRangeAndNoPartNumberReturnsEntireObject() throws Exception {
        S3Object cryptoObject = strictAEClient.getObject(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY));
        Assert.assertArrayEquals(nonMultiPartByteData, IOUtils.toByteArray(cryptoObject.getObjectContent()));
    }

    /**
     * When getObject is called on multiPartObject with no range and no partNumber,
     * returns the entire object.
     */
    @Test
    public void testGetObjectOnMultiPartObjectGivenNoRangeAndNoPartNumberReturnsEntireObject() throws Exception {
        S3Object cryptoObject = strictAEClient.getObject(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY));

        byte[] secondPartByteData = TEST_STRING.getBytes();
        byte[] multipartByteData = new byte[firstPartByteData.length + secondPartByteData.length];
        System.arraycopy(firstPartByteData, 0, multipartByteData, 0, firstPartByteData.length);
        System.arraycopy(secondPartByteData, 0, multipartByteData, firstPartByteData.length, secondPartByteData.length);

        Assert.assertArrayEquals(multipartByteData , IOUtils.toByteArray(cryptoObject.getObjectContent()));
    }

    /**
     * When getObject is called on nonMultiPartObject with specific range,
     * SecurityException is thrown.
     */
    @Test (expected = SecurityException.class)
    public void testGetObjectOnNonMultiPartObjectGivenValidRangeThrowsSecurityException() throws Exception {
        strictAEClient.getObject(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY)
                                                                .withRange(RANGE_BEGIN, RANGE_END));
    }

    /**
     * When getObject is called on multiPartObject with valid part number,
     * SecurityException is thrown.
     */
    @Test (expected = SecurityException.class)
    public void testGetObjectOnMultiPartObjectGivenValidPartNumberThrowsSecurityException() throws Exception {
        strictAEClient.getObject(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                                                                .withPartNumber(1));
    }

    /**
     * Closes all the resources after the tests have finished
     */
    @AfterClass
    public static void tearDown() throws Exception {
        CryptoTestUtils.deleteBucketAndAllContents(strictAEClient, BUCKET_NAME);
    }
}
