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

import static software.amazon.awssdk.services.s3.internal.Constants.MB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.util.IOUtils;

/**
 * Tests for AmazonS3Client#getObject operation with partNumber.
 */
public class GetObjectByPartNumberIntegrationTest extends S3IntegrationTestBase {

    private static final Log LOG = LogFactory.getLog(GetObjectByPartNumberIntegrationTest.class);

    /** The bucket created and used by these tests */
    private final static String BUCKET_NAME = "java-get-object-partnumber-integ-test-" + new Date().getTime();

    /** The key used for testing non multipart objects */
    private final static String NON_MULTIPART_OBJECT_KEY = "nonMultiPartkey";

    /** The file containing the test nonMultiPartObject data uploaded to S3 */
    private static File file;

    /** The size of the file containing the test data uploaded to S3 */
    private final static long FILE_SIZE = 1 * MB;

    /** The inputStream containing the test data uploaded to S3 */
    private static byte[] tempData;

    /**************Variables for testing multi part object***************/
    /** The key used for testing multipart object */
    private final static String MULTIPART_OBJECT_KEY = "multiPartkey";

    /** The unique Id that is returned when we initiate a MultipartUpload */
    private static String uploadId;

    /** The input stream that holds the TEST_STRING */
    private static InputStream inputStream;

    /** The minimum size of a part in multipart object */
    private final static int MIN_SIZE_FIRST_PART_IN_MB = 5 * MB;

    /** The test content that is used for testing part 2 of multipart object. Contains only ASCII characters */
    private final static String TEST_STRING = "This is the content to be uploaded in part 2 of multipart object";

    /** The size of the TEST_STRING **/
    private final static long TEST_STRING_LENGTH = TEST_STRING.length();

    /** The start byte used when specifying a range in the request */
    private final static long START_RANGE = 100;

    /**
     * Creates and initializes all the test resources needed for these tests.
     */
    @BeforeClass
    public static void setup() throws Exception {
        s3.createBucket(BUCKET_NAME);
        file = getRandomTempFile("foo", FILE_SIZE);
        file.deleteOnExit();
        tempData = org.apache.commons.io.FileUtils.readFileToByteArray(file);

        s3.putObject(new PutObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, file));

        doMultiPartUpload();
    }

    /**
     * Uploads a two part object to s3.
     *
     * @throws Exception
     */
    private static void doMultiPartUpload() throws Exception {
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

    private static List<PartETag> uploadParts(String bucketName, String uploadId) throws Exception {
        List<PartETag> partETags = new ArrayList<PartETag>();

        UploadPartRequest uploadPartRequest = new UploadPartRequest()
                .withBucketName(bucketName)
                .withInputStream(new RandomInputStream(MIN_SIZE_FIRST_PART_IN_MB))
                .withKey(MULTIPART_OBJECT_KEY)
                .withPartNumber(1)
                .withPartSize(MIN_SIZE_FIRST_PART_IN_MB)
                .withUploadId(uploadId);
        // This is to avoid the exception "Failed to reset the request input stream" that may occur during retry.
        uploadPartRequest.getRequestClientOptions().setReadLimit(MIN_SIZE_FIRST_PART_IN_MB + 1);
        UploadPartResult uploadPartResult = s3.uploadPart(uploadPartRequest);
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
     * Tests when part number is not set in a non-multipart object,
     * the entire object is returned.
     * @throws Exception
     */
    @Test
    public void testGetObjectOnNonMultiPartObjectGivenNoPartNumberReturnsEntireObject() throws Exception {
        S3Object object = s3.getObject(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY));

        assertEquals(FILE_SIZE, object.getObjectMetadata().getContentLength());
        Assert.assertArrayEquals(tempData, IOUtils.toByteArray(object.getObjectContent()));
    }

    /**
     * Tests when part number is set as 1 in a non-multipart object,
     * the entire object is returned.
     * @throws Exception
     */
    @Test
    public void testGetObjectOnNonMultiPartObjectGivenPartNumberAsOneReturnsEntireObject() throws Exception {
        S3Object object = s3.getObject(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY)
                .withPartNumber(1));

        assertEquals(FILE_SIZE, object.getObjectMetadata().getContentLength());
        Assert.assertArrayEquals(tempData, IOUtils.toByteArray(object.getObjectContent()));
    }

    /**
     * Tests when a valid part number between 1 and part count of the multipart object is included in the getObjectRequest,
     * the corresponding part in the multipart object is returned.
     * @throws Exception
     */
    @Test
    public void testGetObjectOnMultiPartObjectGivenValidPartNumberReturnsCorrespondingPart() throws Exception{
        S3Object object = s3.getObject(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                                            .withPartNumber(2));
        assertEquals(TEST_STRING.length(), object.getObjectMetadata().getContentLength());
        Assert.assertArrayEquals(TEST_STRING.getBytes(), IOUtils.toByteArray(object.getObjectContent()));
    }

    /**
     * Tests when part number less than one is set on non-multipart object,
     * 400 bad request is returned.
     */
    @Test
    public void testGetObjectOnNonMultiPartObjectGivenPartNumberLessThanOneReturns400BadRequest() {
        try {
            s3.getObject(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY)
                    .withPartNumber(0));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals("InvalidArgument", ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertEquals(400, ase.getStatusCode());
        }
    }

    /**
     * Tests when part number less than one is set on multipart object,
     * 400 bad request is returned.
     */
    @Test
    public void testGetObjectOnMultiPartObjectGivenPartNumberLessThanOneReturns400BadRequest() {
        try {
            s3.getObject(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                    .withPartNumber(-1));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals("InvalidArgument", ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertEquals(400, ase.getStatusCode());
        }
    }

    /**
     * Tests when specified part number is greater than the actual part count on non-multipart object,
     * 416 Request Range Not satisfiable is returned.
     */
    @Test
    public void testGetObjectOnNonMultiPartObjectGivenPartNumberGreaterThanActualPartCountReturns416RequestRangeNotSatisfiable() {
        try {
            s3.getObject(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY)
                    .withPartNumber(2));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals("InvalidPartNumber", ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertEquals(416, ase.getStatusCode());
        }
    }

    /**
     * Tests when specified part number is greater than the actual part count in multipart object,
     * 416 Request Range Not satisfiable is returned.
     */
    @Test
    public void testGetObjectOnMultiPartObjectGivenPartNumberGreaterThanActualPartCountReturns416RequestRangeNotSatisfiable() {
        try {
            s3.getObject(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                    .withPartNumber(5));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals("InvalidPartNumber", ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertEquals(416, ase.getStatusCode());
        }
    }

    /**
     * Tests when both partNumber and range are set in the request,
     * an AmazonS3Exception with 400 status code is returned.
     *
     * TODO Uncomment this test case when S3 updates the coral model
     * to handle this case.
     */
/*    @Test
    public void testGetObjectOnMultiPartObjectGivenPartNumberAndRangeReturns400() {
        try {
            s3.getObject(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                    .withPartNumber(1)
                    .withRange(START_RANGE));
            fail("Expected an AmazonS3Exception, but wasn't thrown");
        } catch (AmazonS3Exception ase) {
            assertEquals("InvalidPartNumber", ase.getErrorCode());
            assertEquals(400, ase.getStatusCode());
        }
    }*/

    /**
     * Closes all the resources after the tests have finished
     */
    @AfterClass
    public static void tearDown() throws Exception {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
        IOUtils.closeQuietly(inputStream, LOG);
    }
}
