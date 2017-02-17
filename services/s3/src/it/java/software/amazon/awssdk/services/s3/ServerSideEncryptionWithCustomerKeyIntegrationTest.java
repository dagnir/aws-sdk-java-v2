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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.CopyPartRequest;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.SseCustomerKey;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.Base64;

/**
 * Integration tests for the SSE-CPK feature.
 */
@Category(S3Categories.ReallySlow.class)
public class ServerSideEncryptionWithCustomerKeyIntegrationTest extends S3IntegrationTestBase {

    /** The bucket created and used by these tests. */
    private static final String bucketName = "java-server-side-encryption-integ-test-" + new Date().getTime();

    /** The key used in these tests. */
    private static final String KEY = "key";
    private static final long SINGLE_UPLOAD_OBJECT_SIZE = 100000L;
    private static final long MB = 1024 * 1024;
    private static final int PART_NUMBER = 5;
    private static final long PART_SIZE = 8 * MB;
    /** The file containing the test data uploaded to S3. */
    private static File file_singleUpload = null;
    private static File file_multipartUpload = null;
    private static SecretKey secretKey;
    private static String secretKey_b64;

    @AfterClass
    public static void tearDown() throws Exception {
        deleteBucketAndAllContents(bucketName);

        if (file_singleUpload != null) {
            file_singleUpload.delete();
        }
        if (file_multipartUpload != null) {
            file_multipartUpload.delete();
        }
    }

    /**
     * Creates and initializes all the test resources needed for these tests.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();

        s3.createBucket(bucketName);

        file_singleUpload = new RandomTempFile("get-object-integ-test-single-upload", SINGLE_UPLOAD_OBJECT_SIZE);
        file_multipartUpload = new RandomTempFile("get-object-integ-test-multipart-upload", PART_NUMBER * PART_SIZE);

        secretKey = generateSecretKey();
        secretKey_b64 = Base64.encodeAsString(secretKey.getEncoded());
    }

    private static String initNewMultipartUpload(String key, boolean withSSE) {
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, key);

        if (withSSE) {
            initRequest.setSSECustomerKey(new SseCustomerKey(secretKey_b64));
        }

        InitiateMultipartUploadResult result = s3.initiateMultipartUpload(initRequest);
        return result.getUploadId();
    }

    private static SecretKey generateSecretKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(256, new SecureRandom());
            return generator.generateKey();
        } catch (Exception e) {
            fail("Unable to generate symmetric key: " + e.getMessage());
            return null;
        }
    }

    @Test
    public void testPutObject() {
        PutObjectRequest putObjectRequest =
                new PutObjectRequest(bucketName, KEY, file_singleUpload)
                        .withSseCustomerKey(new SseCustomerKey(secretKey));

        PutObjectResult putObject = s3.putObject(putObjectRequest);

        assertNull(putObject.getSSEAlgorithm());
        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     putObject.getSSECustomerAlgorithm());
        assertNotNull(putObject.getSSECustomerKeyMd5());

        /* Tests that PutObject with an incorrect md5 key hash will be rejected. */
        putObjectRequest.getSSECustomerKey().setMd5("foo");
        try {
            s3.putObject(putObjectRequest);
            fail("Exception is expected when the user specifies incorrect Md5 hash of the key.");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
            assertEquals("InvalidArgument", expected.getErrorCode());
        }
    }

    @Test
    public void testGetObject() throws IOException {
        testPutObject();

        GetObjectRequest request = new GetObjectRequest(bucketName, KEY);
        File destination = createTempFile(bucketName, KEY);

        /* GetObject request without the SSE parameters should be rejected. */
        try {
            s3.getObject(request, destination);
            fail("Exception is expected since the service doesn't allow getting an SSEed object without the SSE parameters.");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
            assertEquals("InvalidRequest", expected.getErrorCode());
        }

        /* GetObject with the correct key. */
        SseCustomerKey serverSideEncryptionKey = new SseCustomerKey(secretKey_b64);
        request.setSSECustomerKey(serverSideEncryptionKey);
        ObjectMetadata returnedMd = s3.getObject(request, destination);
        assertFileEqualsFile(file_singleUpload, destination);

        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     returnedMd.getSSECustomerAlgorithm());
        assertNotNull(returnedMd.getSSECustomerKeyMd5());
        assertNull(returnedMd.getSSEAlgorithm());


        /*
         * GetObject with the wrong key should not work, since the key hash will
         * not match the one stored in the server side.
         */
        SecretKey wrongAesKey = generateSecretKey();
        request.setSSECustomerKey(new SseCustomerKey(Base64.encodeAsString(wrongAesKey.getEncoded())));
        try {
            s3.getObject(request, destination);
            fail("Exception is expcted since the specified key is incorrect.");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 403, (Integer) expected.getStatusCode());
            assertEquals("AccessDenied", expected.getErrorCode());
        }

        /*
         * Test GetObjectMetadata operations
         */
        try {
            // first try a bad request with a missing encryption key
            s3.getObjectMetadata(new GetObjectMetadataRequest(bucketName, KEY));
            fail("Exception is expected since server-side encryption key isn't provided");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
        }

        // then try with the correct key and verify that we don't get an exception
        s3.getObjectMetadata(new GetObjectMetadataRequest(bucketName, KEY)
                                     .withSSECustomerKey(serverSideEncryptionKey));
    }

    @Test
    public void testMultipartUpload() throws IOException {
        SseCustomerKey serverSideEncryptionKey = new SseCustomerKey(secretKey);

        InitiateMultipartUploadRequest initRequest =
                new InitiateMultipartUploadRequest(bucketName, KEY)
                        .withSSECustomerKey(serverSideEncryptionKey);

        InitiateMultipartUploadResult initResult = s3.initiateMultipartUpload(initRequest);

        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     initResult.getSSECustomerAlgorithm());
        assertNotNull(initResult.getSSECustomerKeyMd5());
        assertNull(initResult.getSSEAlgorithm());

        String uploadId = initResult.getUploadId();
        List<PartETag> partETags = new ArrayList<PartETag>(PART_NUMBER);
        long offset = 0;

        for (int part = 1; part <= PART_NUMBER; part++) {
            UploadPartRequest uploadPartRequest = new UploadPartRequest()
                    .withUploadId(uploadId)
                    .withBucketName(bucketName)
                    .withKey(KEY)
                    .withFile(file_multipartUpload)
                    .withPartSize(PART_SIZE)
                    .withFileOffset(offset)
                    .withPartNumber(part)
                    .withLastPart(part == PART_NUMBER)
                    .withSSECustomerKey(serverSideEncryptionKey);
            UploadPartResult uploadPartResult = s3.uploadPart(uploadPartRequest);

            partETags.add(uploadPartResult.getPartETag());

            assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                         uploadPartResult.getSSECustomerAlgorithm());
            assertNotNull(uploadPartResult.getSSECustomerKeyMd5());
            assertNull(uploadPartResult.getSSEAlgorithm());

            offset += PART_SIZE;
        }

        CompleteMultipartUploadResult completeResult = s3
                .completeMultipartUpload(new CompleteMultipartUploadRequest(
                        bucketName, KEY, uploadId, partETags));

        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     completeResult.getSSECustomerAlgorithm());
        // CompleteMultipartUpload does not return the key hash
        assertNull(completeResult.getSSEAlgorithm());

        // Verify the content of the uploaded object
        GetObjectRequest request = new GetObjectRequest(bucketName, KEY)
                .withSSECustomerKey(serverSideEncryptionKey);
        File destination = createTempFile(bucketName, KEY);
        s3.getObject(request, destination);
        assertFileEqualsFile(file_multipartUpload, destination);
    }

    @Test
    public void testCopyObject_SSE_to_SSE() throws IOException {
        testPutObject();

        String destinationKey = "copy-sse-to-sse";
        SecretKey newSecretKey = generateSecretKey();
        String newSecretKey_b64 = Base64.encodeAsString(newSecretKey.getEncoded());
        CopyObjectRequest copyRequest = new CopyObjectRequest(
                bucketName, KEY, bucketName, destinationKey)
                .withDestinationSseCustomerKey(new SseCustomerKey(newSecretKey));

        /*
         * CopyObject request without the SSE parameters of the source object
         * should be rejected
         */
        try {
            s3.copyObject(copyRequest);
            fail("Exception is expected since the SSE parameters of the source object are not specified.");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
            assertEquals("InvalidRequest", expected.getErrorCode());
        }

        /*
         * CopyObject request with the incorrect SSE key of the source object
         * should also be rejected
         */
        copyRequest
                .withSourceSseCustomerKey(new SseCustomerKey(newSecretKey_b64));
        try {
            s3.copyObject(copyRequest);
            fail("Exception is expected since the SSE key of the source object is incorrect.");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
            assertEquals("InvalidRequest", expected.getErrorCode());
        }

        /* CopyObject with the correct source key. */
        copyRequest
                .withSourceSseCustomerKey(new SseCustomerKey(secretKey_b64));
        CopyObjectResult copyResult = s3.copyObject(copyRequest);

        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     copyResult.getSSECustomerAlgorithm());
        assertNotNull(copyResult.getSSECustomerKeyMd5());
        assertNull(copyResult.getSSEAlgorithm());

        // Verify the content of the copied object
        GetObjectRequest request = new GetObjectRequest(bucketName, destinationKey)
                .withSSECustomerKey(new SseCustomerKey(newSecretKey_b64));
        File destination = createTempFile(bucketName, destinationKey);
        s3.getObject(request, destination);
        assertFileEqualsFile(file_singleUpload, destination);
    }

    @Test
    public void testCopyObject_SSE_to_NonSSE() throws IOException {
        testPutObject();

        String destinationKey = "copy-sse-to-nonsse";
        CopyObjectRequest copyRequest = new CopyObjectRequest(
                bucketName, KEY, bucketName, destinationKey)
                .withSourceSseCustomerKey(new SseCustomerKey(secretKey_b64));

        CopyObjectResult copyResult = s3.copyObject(copyRequest);
        assertNull(copyResult.getSSECustomerAlgorithm());
        assertNull(copyResult.getSSECustomerKeyMd5());
        assertNull(copyResult.getSSEAlgorithm());

        // Verify the content of the copied object
        GetObjectRequest request = new GetObjectRequest(bucketName, destinationKey);
        File destination = createTempFile(bucketName, destinationKey);
        s3.getObject(request, destination);
        assertFileEqualsFile(file_singleUpload, destination);
    }

    @Test
    public void testCopyObject_NonSSE_to_SSE() throws IOException {
        // Put an unecrypted object
        s3.putObject(bucketName, KEY, file_singleUpload);

        String destinationKey = "copy-nonsse-to-sse";
        SseCustomerKey serverSideEncryptionKey = new SseCustomerKey(secretKey);
        CopyObjectRequest copyRequest = new CopyObjectRequest(
                bucketName, KEY, bucketName, destinationKey)
                .withDestinationSseCustomerKey(serverSideEncryptionKey);

        CopyObjectResult copyResult = s3.copyObject(copyRequest);

        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     copyResult.getSSECustomerAlgorithm());
        assertNotNull(copyResult.getSSECustomerKeyMd5());
        assertNull(copyResult.getSSEAlgorithm());

        // Verify that we cannot retrieve the copied object without the SSE key
        GetObjectRequest getRequest = new GetObjectRequest(bucketName, destinationKey);
        final File destination = createTempFile(bucketName, destinationKey);
        try {
            s3.getObject(getRequest, destination);
            fail("Exception is expected since the object was copied with SSE.");
        } catch (AmazonServiceException expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
            assertEquals("InvalidRequest", expected.getErrorCode());
        }

        // We can still retrieve the original object without the key
        s3.getObject(new GetObjectRequest(bucketName, KEY), destination);

        // Now verify the content of the encrypted copy
        getRequest
                .withSSECustomerKey(serverSideEncryptionKey);
        s3.getObject(getRequest, destination);
        assertFileEqualsFile(file_singleUpload, destination);
    }

    @Test
    public void testCopyPart_SSE_To_SSE() {
        testPutObject();
        final String destinationKey = "copy-part-sse-to-sse";
        final String uploadId = initNewMultipartUpload(destinationKey, true);
        SseCustomerKey serverSideEncryptionKey = new SseCustomerKey(secretKey_b64);

        CopyPartRequest copyRequest = new CopyPartRequest()
                .withSourceBucketName(bucketName)
                .withSourceKey(KEY)
                .withDestinationBucketName(bucketName)
                .withDestinationKey(destinationKey)
                .withUploadId(uploadId)
                .withFirstByte(0L)
                .withLastByte(100L)
                .withPartNumber(1)
                .withDestinationSseCustomerKey(serverSideEncryptionKey);

        /* Copy part without the SSE key for the source object. */
        try {
            s3.copyPart(copyRequest);
            fail("Exception is expected since the SSE key for the source object is not specified.");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
            assertEquals("InvalidRequest", expected.getErrorCode());
        }

        /* Copy part with the correct SSE key for the source object. */
        copyRequest
                .withSourceSseCustomerKey(serverSideEncryptionKey);
        CopyPartResult result = s3.copyPart(copyRequest);
        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     result.getSSECustomerAlgorithm());
        assertNotNull(result.getSSECustomerKeyMd5());
        assertNull(result.getSSEAlgorithm());

        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, destinationKey, uploadId));
    }

    @Test
    public void testCopyPart_NonSSE_To_SSE() {
        // Put an unecrypted object
        s3.putObject(bucketName, KEY, file_singleUpload);

        final String destinationKey = "copy-part-nonsse-to-sse";
        final String uploadId = initNewMultipartUpload(destinationKey, true);

        CopyPartRequest copyRequest = new CopyPartRequest()
                .withSourceBucketName(bucketName)
                .withSourceKey(KEY)
                .withDestinationBucketName(bucketName)
                .withDestinationKey(destinationKey)
                .withUploadId(uploadId)
                .withFirstByte(0L)
                .withLastByte(100L)
                .withPartNumber(1)
                .withDestinationSseCustomerKey(new SseCustomerKey(secretKey_b64));

        CopyPartResult result = s3.copyPart(copyRequest);
        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,
                     result.getSSECustomerAlgorithm());
        assertNotNull(result.getSSECustomerKeyMd5());
        assertNull(result.getSSEAlgorithm());

        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, destinationKey, uploadId));
    }

    @Test
    public void testCopyPart_SSE_To_NonSSE() {
        testPutObject();
        final String destinationKey = "copy-part-sse-to-nonsse";
        final String uploadId = initNewMultipartUpload(destinationKey, false);

        CopyPartRequest request = new CopyPartRequest()
                .withSourceBucketName(bucketName)
                .withSourceKey(KEY)
                .withDestinationBucketName(bucketName)
                .withDestinationKey(destinationKey)
                .withUploadId(uploadId)
                .withFirstByte(0L)
                .withLastByte(100L)
                .withPartNumber(1)
                .withSourceSseCustomerKey(new SseCustomerKey(secretKey));

        CopyPartResult result = s3.copyPart(request);

        assertNull(result.getSSECustomerAlgorithm());
        assertNull(result.getSSECustomerKeyMd5());
        assertNull(result.getSSEAlgorithm());

        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, destinationKey, uploadId));
    }

    @Test
    public void testServerSideEncryptionBadAlgorithm() {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, KEY, file_singleUpload);
        SseCustomerKey serverSideEncryptionKey = new SseCustomerKey(secretKey_b64);
        serverSideEncryptionKey.setAlgorithm("BAD");
        putObjectRequest.setSseCustomerKey(serverSideEncryptionKey);
        try {
            s3.putObject(putObjectRequest);
            fail("Exception is expected since the encryption algorithom is invalid.");
        } catch (AmazonS3Exception expected) {
            assertEquals((Integer) 400, (Integer) expected.getStatusCode());
            assertEquals("InvalidEncryptionAlgorithmError", expected.getErrorCode());
        }
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        File tmp = File.createTempFile(bucketName, KEY);
        tmp.deleteOnExit();
        return tmp;
    }
}
