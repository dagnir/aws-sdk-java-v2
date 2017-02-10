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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.util.IOUtils;
import software.amazon.awssdk.util.StringInputStream;

/**
 * Tests for AmazonS3Client#getObjectMetadata operations with partNumber.
 */
public class GetObjectMetadataIntegrationTest extends S3IntegrationTestBase {

    private static final Log LOG = LogFactory.getLog(GetObjectMetadataIntegrationTest.class);

    /** The bucket created and used by these tests */
    private final static String BUCKET_NAME = "java-get-object-metadata-integ-test-" + new Date().getTime();

    /** The key used in these tests */
    private final static String NON_MULTIPART_OBJECT_KEY = "nonMultiPartkey";

    /** The file containing the test data uploaded to S3 */
    private static File file;

    /** The size of the file containing the test data uploaded to S3 */
    private final static long FILE_SIZE = 3 * MB;

    /**************Variables for testing multi part object***************/
    /** The key used for testing multipart object */
    private final static String MULTIPART_OBJECT_KEY = "multiPartkey";

    /** The unique Id that is returned when we initiate a MultipartUpload */
    private static String uploadId;

    /** The input stream that holds the TEST_STRING */
    private static InputStream inputStream;

    /** The minimum size of a part in multipart object */
    private final static long MIN_SIZE_FIRST_PART_IN_MB = 5 * MB;

    /** The test content that is used for testing part 2 of multipart object. Contains only ASCII characters */
    private final static String TEST_STRING = "This is the content to be uploaded in part 2 of multipart object";

    /** The size of the TEST_STRING **/
    private final static long TEST_STRING_LENGTH = TEST_STRING.length();

    /**
     * Creates and initializes all the test resources needed for these tests.
     */
    @BeforeClass
    public static void setup() throws Exception {
        s3.createBucket(BUCKET_NAME);
        file = getRandomTempFile("foo", FILE_SIZE);
        file.deleteOnExit();

        s3.putObject(new PutObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, file));
        doMultiPartUpload();
    }

    /**
     * Uploads a two part object to s3.
     *
     * @throws Exception
     */
    private static void doMultiPartUpload() throws Exception{
        InitiateMultipartUploadResult initiateResult = s3.initiateMultipartUpload(
                new InitiateMultipartUploadRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                    .withStorageClass(StorageClass.ReducedRedundancy));
        uploadId = initiateResult.getUploadId();

        try {
            List<PartETag> partETags = uploadParts(BUCKET_NAME, uploadId);
            s3.completeMultipartUpload(new CompleteMultipartUploadRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY, uploadId, partETags));
        } catch (Exception exception) {
            System.out.println("Exception occured during multipartUpload with ID " + uploadId );
            s3.abortMultipartUpload(new AbortMultipartUploadRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY, uploadId));
            throw exception;
        }
    }

    private static List<PartETag> uploadParts(String bucketName, String uploadId) throws AmazonServiceException, AmazonClientException, InterruptedException, IOException {
        List<PartETag> partETags = new ArrayList<PartETag>();

        UploadPartResult uploadPartResult = s3.uploadPart(new UploadPartRequest()
            .withBucketName(bucketName)
            .withInputStream(new RandomInputStream(MIN_SIZE_FIRST_PART_IN_MB))
            .withKey(MULTIPART_OBJECT_KEY)
            .withPartNumber(1)
            .withPartSize(MIN_SIZE_FIRST_PART_IN_MB)
            .withUploadId(uploadId));
        assertEquals(1, uploadPartResult.getPartNumber());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        inputStream = new StringInputStream(TEST_STRING);
        uploadPartResult = s3.uploadPart(new UploadPartRequest()
            .withBucketName(bucketName)
            .withInputStream(inputStream)
            .withKey(MULTIPART_OBJECT_KEY)
            .withPartNumber(2)
            .withPartSize(TEST_STRING_LENGTH)
            .withUploadId(uploadId));
        assertEquals(2, uploadPartResult.getPartNumber());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        return partETags;
    }

    /**
     * When part number is set as 1 on a non-multipart object,
     * the header "x-amz-mp-parts-count" is not present.
     */
    @Test
    public void getObjectMetadataOnNonMultiPartObjectGivenPartNumberAsOneReturnsNoPartsCount() {
        ObjectMetadata metadata = s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY).withPartNumber(1));
        assertNull(metadata.getRawMetadataValue("x-amz-mp-parts-count"));
        assertNull(metadata.getPartCount());
    }

    /**
     * When part number is set as 1 on a multipart object,
     * the header "x-amz-mp-parts-count" contains the total part count of the object.
     */
    @Test
    public void getObjectMetadataOnMultiPartObjectGivenPartNumberAsOneReturnsTotalPartsCount() {
        ObjectMetadata metadata = s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY).withPartNumber(1));
        assertEquals(2, Integer.parseInt(metadata.getRawMetadataValue("x-amz-mp-parts-count").toString()));
        assertEquals(2, metadata.getPartCount().intValue());
    }

    /**
     * When a part number between 1 and part count of the multipart object is set,
     * the header "x-amz-mp-parts-count" contains the total part count of the object.
     */
    @Test
    public void getObjectMetadataOnMultiPartObjectGivenValidPartNumberReturnsTotalPartsCount() {
        ObjectMetadata metadata = s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY).withPartNumber(2));
        assertEquals(2, Integer.parseInt(metadata.getRawMetadataValue("x-amz-mp-parts-count").toString()));
        assertEquals(2, metadata.getPartCount().intValue());
    }

    /**
     * When no part number is set in getObjectMetadataRequest,
     * the header "x-amz-mp-parts-count" is not present.
     */
    @Test
    public void getObjectMetadataOnMultiPartObjectGivenNoPartNumberReturnsNoPartsCount() {
        ObjectMetadata metadata = s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY));
        assertNull(metadata.getRawMetadataValue("x-amz-mp-parts-count"));
        assertNull(metadata.getPartCount());
    }

    /**
     * When part number less than 1 is set on a non-multipart object,
     * 400 bad request is returned.
     */
    @Test
    public void getObjectMetadataOnNonMultiPartObjectGivenPartNumberLessThanOneReturns400BadRequest() {
        try {
            s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY)
                                                                    .withPartNumber(-1));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals(400, ase.getStatusCode());
        }
    }

    /**
     * When part number less than 1 is set on a multipart object,
     * 400 bad request is returned.
     */
    @Test
    public void getObjectMetadataOnMultiPartObjectGivenPartNumberLessThanOneReturns400BadRequest() {
        try {
            s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                                                                    .withPartNumber(0));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals(400, ase.getStatusCode());
        }
    }

    /**
     * When part number less than 1 is set on a non-multipart object,
     * 416 Request Range Not satisfiable is returned.
     */
    @Test
    public void getObjectMetadataOnNonMultiPartObjectGivenPartNumberGreaterThanActualPartCountReturns416RequestRangeNotSatisfiable() {
        try {
            s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY)
                                                                    .withPartNumber(2));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals(416, ase.getStatusCode());
        }
    }

    /**
     * When part number less than 1 is set on a multipart object,
     * 416 Request Range Not satisfiable is returned.
     */
    @Test
    public void getObjectMetadataOnMultiPartObjectGivenPartNumberGreaterThanActualPartCountReturns416RequestRangeNotSatisfiable() {
        try {
            s3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                                                                    .withPartNumber(3));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals(416, ase.getStatusCode());
        }
    }

    /**
     * Closes all the resources after the tests have finished
     */
    @AfterClass
    public static void tearDown() throws Exception {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
        IOUtils.closeQuietly(inputStream, LOG);
    }
}
