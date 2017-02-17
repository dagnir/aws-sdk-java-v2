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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Date;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.HttpMethod;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyPartRequest;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.SSEAlgorithm;
import software.amazon.awssdk.services.s3.model.SseCustomerKey;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.test.AwsTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.Base64;
import software.amazon.awssdk.util.Md5Utils;

/**
 * Integration tests for the SSE-CPK feature.
 */
public class SseCustomerKeyHttpIntegrationTest extends AwsTestBase {
    /** The bucket created and used by these tests. */
    private static final String bucketName = "java-server-side-encryption-integ-test-" + new Date().getTime();
    /** The key used in these tests. */
    private static final String KEY = "key";
    private static final long SINGLE_UPLOAD_OBJECT_SIZE = 100000L;
    private static final long MB = 1024 * 1024;
    private static final int PART_NUMBER = 5;
    private static final long PART_SIZE = 8 * MB;
    protected static AmazonS3Client s3;
    protected static AmazonS3Client s3Https;
    /** The file containing the test data uploaded to S3. */
    private static File file_singleUpload = null;
    private static File file_multipartUpload = null;
    private static SecretKey secretKey;
    private static String secretKey_b64;

    @AfterClass
    public static void tearDown() throws Exception {
        if (file_singleUpload != null) {
            file_singleUpload.delete();
        }
        if (file_multipartUpload != null) {
            file_multipartUpload.delete();
        }
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
        s3.shutdown();
        s3Https.shutdown();
    }

    /**
     * Creates and initializes all the test resources needed for these tests.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        file_singleUpload = new RandomTempFile("get-object-integ-test-single-upload", SINGLE_UPLOAD_OBJECT_SIZE);
        file_multipartUpload = new RandomTempFile("get-object-integ-test-multipart-upload", PART_NUMBER * PART_SIZE);
        setUpCredentials();
        s3 = new AmazonS3Client(credentials,
                                // Deliberately set to http so the requests with customer keys would
                                // fail
                                new ClientConfiguration().withProtocol(Protocol.HTTP));
        s3Https = new AmazonS3Client(credentials);
        secretKey = generateSecretKey();
        secretKey_b64 = Base64.encodeAsString(secretKey.getEncoded());
        s3Https.createBucket(bucketName);
        s3Https.putObject(new PutObjectRequest(bucketName, KEY,
                                               file_singleUpload).withSseCustomerKey(new SseCustomerKey(
                secretKey)));
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

        try {
            s3.putObject(putObjectRequest);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }
    }

    @Test
    public void testGetObject() throws IOException {
        GetObjectRequest request = new GetObjectRequest(bucketName, KEY);
        File destination = createTempFile(bucketName, KEY);
        /* GetObject with the correct key. */
        request.setSSECustomerKey(new SseCustomerKey(secretKey_b64));
        try {
            s3.getObject(request, destination);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }
    }

    @Test
    public void testMultipartUpload() throws IOException {
        SseCustomerKey serverSideEncryptionKey = new SseCustomerKey(secretKey);

        InitiateMultipartUploadRequest initRequest =
                new InitiateMultipartUploadRequest(bucketName, KEY)
                        .withSSECustomerKey(serverSideEncryptionKey);
        try {
            s3.initiateMultipartUpload(initRequest);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }

        String uploadId = "foo";
        long offset = 0;
        int part = 1;
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
        try {
            s3.uploadPart(uploadPartRequest);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }
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
         * http CopyObject request with a destination SSECustomerKey
         * should be rejected
         */
        try {
            s3.copyObject(copyRequest);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }

        /*
         * http CopyObject request with a source SSECustomerKey
         * should be rejected
         */
        copyRequest
                .withSourceSseCustomerKey(new SseCustomerKey(newSecretKey_b64));
        try {
            s3.copyObject(copyRequest);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }
    }

    @Test
    public void testCopyPart_SSE_To_SSE() {
        testPutObject();
        final String destinationKey = "copy-part-sse-to-sse";
        SseCustomerKey serverSideEncryptionKey = new SseCustomerKey(secretKey_b64);

        /* destination SSECustomerKey and http don't mix. */
        CopyPartRequest copyRequest = new CopyPartRequest()
                .withSourceBucketName(bucketName)
                .withSourceKey(KEY)
                .withDestinationBucketName(bucketName)
                .withDestinationKey(destinationKey)
                .withUploadId("foo")
                .withFirstByte(0L)
                .withLastByte(100L)
                .withPartNumber(1)
                .withDestinationSseCustomerKey(serverSideEncryptionKey);

        try {
            s3.copyPart(copyRequest);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }

        /* source SSECustomerKey and http don't mix. */
        copyRequest = new CopyPartRequest()
                .withSourceBucketName(bucketName)
                .withSourceKey(KEY)
                .withDestinationBucketName(bucketName)
                .withDestinationKey(destinationKey)
                .withUploadId("foo")
                .withFirstByte(0L)
                .withLastByte(100L)
                .withPartNumber(1)
                .withSourceSseCustomerKey(serverSideEncryptionKey)
        ;

        try {
            s3.copyPart(copyRequest);
            fail("secret key and http don't mix");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("HTTPS must be used"));
        }
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        File tmp = File.createTempFile(bucketName, KEY);
        tmp.deleteOnExit();
        return tmp;
    }

    @Test
    public void testPresignedUrlWithSSEC() throws Exception {
        GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(
                bucketName, KEY, HttpMethod.GET);
        presignedUrlRequest.setSSECustomerKeyAlgorithm(SSEAlgorithm.getDefault());

        S3Object s3Object = s3Https.getObject(new GetObjectRequest(bucketName,
                                                                   KEY).withSSECustomerKey(new SseCustomerKey(secretKey)));
        URL url = s3Https.generatePresignedUrl(presignedUrlRequest);

        HttpRequestBase httpRequest = null;

        httpRequest = new HttpGet(URI.create(url.toExternalForm()));
        httpRequest.setHeader(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY,
                              secretKey_b64);
        httpRequest.setHeader(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                              Md5Utils.md5AsBase64(secretKey.getEncoded()));
        httpRequest.setHeader(
                Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);

        HttpResponse response = new DefaultHttpClient().execute(httpRequest);
        assertEquals((Integer) 200, (Integer) response.getStatusLine()
                                                      .getStatusCode());
        assertStreamEqualsStream(s3Object.getObjectContent(), response
                .getEntity().getContent());
    }
}
