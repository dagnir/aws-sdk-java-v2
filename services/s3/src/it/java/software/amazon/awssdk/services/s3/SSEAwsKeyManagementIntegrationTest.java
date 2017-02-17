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
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.HttpMethod;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.kms.utils.KmsTestKeyCache;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.SseAwsKeyManagementParams;
import software.amazon.awssdk.services.s3.model.SseCustomerKey;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.transfer.Copy;
import software.amazon.awssdk.services.s3.transfer.Download;
import software.amazon.awssdk.services.s3.transfer.PersistableUpload;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.TransferManagerConfiguration;
import software.amazon.awssdk.services.s3.transfer.Upload;
import software.amazon.awssdk.services.s3.transfer.internal.UploadPartRequestFactory;
import software.amazon.awssdk.test.AwsTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

@Category(S3Categories.Slow.class)
public class SSEAwsKeyManagementIntegrationTest extends AwsTestBase {

    /**
     * The size of the file being used for this integration testing.
     */
    public static final long UNENCRYPTED_OBJECT_CONTENT_LENGTH = 30 * MB;
    /**
     * The name of the file created in the file system for integration testing.
     */
    public static final String FILE_NAME = "java-sdk-temp-file-"
                                           + System.currentTimeMillis();
    /**
     * The bucket name in Amazon S3 used for testing.
     */
    private static final String BUCKET_NAME = "java-sdk-sse-trent-"
                                              + System.currentTimeMillis();
    /**
     * The destination bucket name in Amazon S3 for copy requests.
     */
    private static final String COPY_DEST_BUCKET_NAME = "java-sdk-sse-trent-dest"
                                                        + System.currentTimeMillis();
    /**
     * The name of the object in Amazon S3 that is not encrypted.
     */
    private static final String UNENCRYPTED_OBJECT = "java-sdk-unencrypted-object-key-"
                                                     + System.currentTimeMillis();
    /**
     * The name of the object in Amazon S3 that is encrypted using the AWS KMS.
     */
    private static final String ENCRYPTED_OBJECT = "java-sdk-sse-kms-encrypted-object-key-"
                                                   + System.currentTimeMillis();
    /**
     * The name of the object in Amazon S3 that is Server Side encrypted with
     * customer provided key
     */
    private static final String SSEC_ENCRYPTED_OBJECT = "java-sdk-sse-customer-encrypted-object-key-"
                                                        + System.currentTimeMillis();
    /**
     * The name of the object in Amazon S3 that is Server Side encrypted with
     * AWS KMS Default Key Id.
     */
    private static final String SSE_AWS_KMS_DEFAULT_KEY_ENCRYPTED_OBJECT = "java-sdk-sse-awskms-encrypted-object-key-"
                                                                           + System.currentTimeMillis();
    /**
     * The name of the object in Amazon S3 that is Server Side encrypted with
     * AWS KMS Non Default Key Id
     */
    private static final String SSE_AWS_KMS_NON_DEFAULT_KEY_ENCRYPTED_OBJECT = "java-sdk-sse-awskms-non-default-encrypted-object-key-"
                                                                               + System.currentTimeMillis();
    /**
     * The default Key Id to to be used when performing SSE with Key from AWS
     * Key Management Service
     */
    private static final SseAwsKeyManagementParams DEFAULT_KEY_ID = new SseAwsKeyManagementParams();
    /**
     * The reference to the file in the file system used for integration
     * testing.
     */
    public static File randomFile = null;
    /**
     * The name of the file where the Amazon S3 object is to be downloaded.
     */
    public static String DOWNLOAD_FILE_NAME = "java-sdk-temp-download-file-" + System.currentTimeMillis();
    /**
     * The non default Key Id to to be used when performing SSE with Key from AWS Key Management
     * Service
     */
    private static SseAwsKeyManagementParams nonDefaultKmsKeyId;
    /**
     * The SSE-C key to be used for integration testing.
     */
    private static SseCustomerKey sseKey = null;
    /**
     * Reference to Amazon S3 client used for testing purposes.
     */
    private static AmazonS3Client s3Https = null;

    /**
     * The http client to be used for running presigned url's.
     */
    private static DefaultHttpClient httpClient = null;

    /**
     * Reference to the transfer manager used for testing.
     */
    private static TransferManager tm = null;

    /**
     * AWS KMS client to generate non default Key id
     */
    private static AWSKMSClient kmsClient = null;

    /**
     * Creates the buckets in Amazon S3 to be used for this testing. Also upload
     * objects to S3 that will be used by individual test cases.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        s3Https = new AmazonS3Client(credentials);
        s3Https.setEndpoint("https://s3-us-west-2.amazonaws.com");

        kmsClient = new AWSKMSClient(credentials);
        kmsClient.setEndpoint("https://kms.us-west-2.amazonaws.com");
        nonDefaultKmsKeyId = new SseAwsKeyManagementParams(KmsTestKeyCache.getInstance(Regions.US_WEST_2, credentials)
                                                                          .getNonDefaultKeyId());
        tm = new TransferManager(s3Https);
        TransferManagerConfiguration tmConfig = new TransferManagerConfiguration();
        tmConfig.setMultipartCopyThreshold(tmConfig.getMultipartUploadThreshold());
        tmConfig.setMultipartCopyPartSize(tmConfig.getMinimumUploadPartSize());
        tm.setConfiguration(tmConfig);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(UNENCRYPTED_OBJECT_CONTENT_LENGTH);
        s3Https.createBucket(BUCKET_NAME);
        s3Https.createBucket(COPY_DEST_BUCKET_NAME);

        randomFile = new RandomTempFile(FILE_NAME,
                                        UNENCRYPTED_OBJECT_CONTENT_LENGTH);

        httpClient = constructHTTPClient();

        s3Https.putObject(new PutObjectRequest(BUCKET_NAME, UNENCRYPTED_OBJECT,
                                               randomFile));

        SecretKey secretKey = generateSecretKey();
        sseKey = new SseCustomerKey(secretKey);

        s3Https.putObject(new PutObjectRequest(BUCKET_NAME,
                                               SSEC_ENCRYPTED_OBJECT, randomFile).withSseCustomerKey(sseKey));

        s3Https.putObject(new PutObjectRequest(BUCKET_NAME,
                                               SSE_AWS_KMS_NON_DEFAULT_KEY_ENCRYPTED_OBJECT, randomFile)
                                  .withSseAwsKeyManagementParams(nonDefaultKmsKeyId));

        s3Https.putObject(new PutObjectRequest(BUCKET_NAME,
                                               SSE_AWS_KMS_DEFAULT_KEY_ENCRYPTED_OBJECT, randomFile)
                                  .withSseAwsKeyManagementParams(DEFAULT_KEY_ID));
    }

    private static DefaultHttpClient constructHTTPClient() throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        TrustStrategy ts = new TrustStrategy() {

            @Override
            public boolean isTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
                return true;
            }
        };

        SSLSocketFactory sf = null;

        try {
            sf = new SSLSocketFactory(ts,
                                      SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Exception e) {
            throw e;
        }

        Scheme https = new Scheme("https", 443, sf);
        SchemeRegistry sr = httpClient.getConnectionManager()
                                      .getSchemeRegistry();
        sr.register(https);

        return httpClient;
    }

    @AfterClass
    public static void tearDown() {
        try {
            CryptoTestUtils.deleteBucketAndAllContents(s3Https, BUCKET_NAME);
        } catch (Exception ex) {
            LogFactory.getLog(SSEAwsKeyManagementIntegrationTest.class)
                      .warn("Skipping failure to delete bucket " + BUCKET_NAME, ex);
        }
        try {
            CryptoTestUtils.deleteBucketAndAllContents(s3Https,
                                                       COPY_DEST_BUCKET_NAME);
        } catch (Exception ex) {
            LogFactory.getLog(SSEAwsKeyManagementIntegrationTest.class)
                      .warn("Skipping failure to delete bucket " + COPY_DEST_BUCKET_NAME, ex);
        }
        if (randomFile != null) {
            assertTrue(randomFile.delete());
        }

        File downloadedFile = new File(DOWNLOAD_FILE_NAME);
        if (downloadedFile.exists()) {
            downloadedFile.delete();
        }

        System.clearProperty(SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY);

        s3Https.shutdown();
        kmsClient.shutdown();
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

    private void assertResult(File expected, long contentLength,
                              S3Object s3Object) {
        assertEquals((Long) s3Object.getObjectMetadata().getContentLength(),
                     Long.valueOf(UNENCRYPTED_OBJECT_CONTENT_LENGTH));
        assertFileEqualsStream(expected, s3Object.getObjectContent());
    }

    /**
     * Tests by uploading a object to Amazon S3. Server Side Encryption is
     * enabled with Key being used from AWS KMS non default key id. Asserts by
     * retrieving the object and compares it with the content of the file.
     */
    @Test
    public void putObjectSSEAwsKeyManagementNonDefaultKeyId() {

        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
                                                                 ENCRYPTED_OBJECT, randomFile);
        putObjectRequest.setSseAwsKeyManagementParams(nonDefaultKmsKeyId);
        PutObjectResult result = s3Https.putObject(putObjectRequest);
        Assert.assertNotNull(result.getMetadata().getSSEAwsKmsKeyId());

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests multipart upload of an object to Amazon S3. Server Side Encryption
     * is enabled with Key being used from AWS KMS default key id. Asserts by
     * retrieving the object and compares it with the content of the file.
     */
    @Test
    public void multipartUploadSSEAwsKeyManagementDefaultKeyId() throws InterruptedException {

        InitiateMultipartUploadResult initiateResult = s3Https
                .initiateMultipartUpload(new InitiateMultipartUploadRequest(
                        BUCKET_NAME, ENCRYPTED_OBJECT)
                                                 .withSSEAwsKeyManagementParams(DEFAULT_KEY_ID));
        final String uploadId = initiateResult.getUploadId();

        Thread.sleep(10000);

        UploadPartRequestFactory requestFactory = new UploadPartRequestFactory(
                new PutObjectRequest(BUCKET_NAME, ENCRYPTED_OBJECT, randomFile),
                uploadId, 5 * MB);

        List<PartETag> partETags = new ArrayList<PartETag>();
        while (requestFactory.hasMoreRequests()) {
            partETags.add(s3Https.uploadPart(
                    requestFactory.getNextUploadPartRequest()).getPartETag());
        }

        s3Https.completeMultipartUpload(new CompleteMultipartUploadRequest(
                BUCKET_NAME, ENCRYPTED_OBJECT, uploadId, partETags));

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests multipart upload of an object to Amazon S3. Server Side Encryption
     * is enabled with Key being used from AWS KMS non default key id. Asserts
     * by retrieving the object and compares it with the content of the file.
     */
    @Test
    public void multipartUploadSSEAwsKeyManagementNonDefaultKeyId() {

        InitiateMultipartUploadResult initiateResult = s3Https
                .initiateMultipartUpload(new InitiateMultipartUploadRequest(
                        BUCKET_NAME, ENCRYPTED_OBJECT)
                                                 .withSSEAwsKeyManagementParams(nonDefaultKmsKeyId));
        final String uploadId = initiateResult.getUploadId();

        UploadPartRequestFactory requestFactory = new UploadPartRequestFactory(
                new PutObjectRequest(BUCKET_NAME, ENCRYPTED_OBJECT, randomFile),
                uploadId, 5 * MB);

        List<PartETag> partETags = new ArrayList<PartETag>();
        while (requestFactory.hasMoreRequests()) {
            partETags.add(s3Https.uploadPart(
                    requestFactory.getNextUploadPartRequest()).getPartETag());
        }

        s3Https.completeMultipartUpload(new CompleteMultipartUploadRequest(
                BUCKET_NAME, ENCRYPTED_OBJECT, uploadId, partETags));

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests by uploading a object to Amazon S3. Server Side Encryption is
     * enabled with Key being used from AWS KMS default key id. Asserts by
     * retrieving the object and compares it with the content of the file.
     */
    @Test
    public void putObjectSSEAwsKeyManagementDefaultKeyId() throws IOException {

        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
                                                                 ENCRYPTED_OBJECT, randomFile);
        putObjectRequest.setSseAwsKeyManagementParams(DEFAULT_KEY_ID);
        PutObjectResult result = s3Https.putObject(putObjectRequest);
        Assert.assertNotNull(result.getMetadata().getSSEAwsKmsKeyId());

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests copying a un-encrypted object to an Server Side Encryption with AWS
     * KMS Object. The key Id used is non default. Asserts by retrieving the
     * object from destination and compares it with the content of the file.
     */
    @Test
    public void copyUnEncryptedObjectToSSEWithAwsKeyManagementNonDefaultKeyId() {
        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              UNENCRYPTED_OBJECT, COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setSseAwsKeyManagementParams(nonDefaultKmsKeyId);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests retrieving an SSE AWS KMS enabled Amazon S3 object to a file.
     * Asserts that the contents of the file download is same as the file
     * being uploaded.
     */
    @Test
    public void getObjectSSEAwsKmsKeyEnabledToFile() {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME,
                                                                 SSE_AWS_KMS_DEFAULT_KEY_ENCRYPTED_OBJECT);
        s3Https.getObject(getObjectRequest, new File(DOWNLOAD_FILE_NAME));

        assertFileEqualsFile(randomFile, new File(DOWNLOAD_FILE_NAME));
    }

    /**
     * Tests copying a un-encrypted object to an Server Side Encryption with AWS
     * KMS Object. The key Id used is default. Asserts by retrieving the object
     * and compares it with the content of the file.
     */
    @Test
    public void copyUnEncryptedObjectToSSEWithAwsKeyManagementDefaultKeyId() {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              UNENCRYPTED_OBJECT, COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setSseAwsKeyManagementParams(DEFAULT_KEY_ID);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests copying a SSE-C encrypted object to an Server Side Encryption with
     * AWS KMS Object. The key Id used is non default. Asserts by retrieving the
     * object and compares it with the content of the file.
     */
    @Test
    public void copySSECObjectToSSEAwsKeyManagementNonDefaultKeyId() {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSEC_ENCRYPTED_OBJECT, COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setSourceSseCustomerKey(sseKey);
        copyRequest.setSseAwsKeyManagementParams(nonDefaultKmsKeyId);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests copying a SSE-C encrypted object to an Server Side Encryption with
     * AWS KMS Object. The key Id used is default. Asserts by retrieving the
     * object and compares it with the content of the file.
     */
    @Test
    public void copySSECObjectToSSEAwsKeyManagementDefaultKeyId() {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSEC_ENCRYPTED_OBJECT, COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setSourceSseCustomerKey(sseKey);
        copyRequest.setSseAwsKeyManagementParams(DEFAULT_KEY_ID);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests copying a Server Side Encryption with AWS KMS Object to SSE-C
     * encrypted object. The key Id used is non default. Asserts by retrieving
     * the object and compares it with the content of the file.
     */
    @Test
    public void copySSEAwsKeyManagementNonDefaultKeyIdObjectToSSECustomerKey() {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSE_AWS_KMS_NON_DEFAULT_KEY_ENCRYPTED_OBJECT,
                                                              COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setDestinationSseCustomerKey(sseKey);
        s3Https.copyObject(copyRequest);

        S3Object s3Object = s3Https.getObject(new GetObjectRequest(
                COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT)
                                                      .withSSECustomerKey(sseKey));
        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH, s3Object);
    }

    /**
     * Tests copying a Server Side Encryption with AWS KMS Object to SSE-C
     * encrypted object. The key Id used is default. Asserts by retrieving the
     * object and compares it with the content of the file.
     */
    @Test
    public void copySSEAwsKeyManagementDefaultKeyIdObjectToSSECustomerKey() {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSE_AWS_KMS_DEFAULT_KEY_ENCRYPTED_OBJECT,
                                                              COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setDestinationSseCustomerKey(sseKey);
        s3Https.copyObject(copyRequest);

        S3Object s3Object = s3Https.getObject(new GetObjectRequest(
                COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT)
                                                      .withSSECustomerKey(sseKey));
        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH, s3Object);
    }

    /**
     * Tests copying a Server Side Encryption with AWS KMS Object to
     * un-encrypted object. The key Id used is non default. Asserts by
     * retrieving the object and compares it with the content of the file.
     */
    @Test
    public void copySSEAwsKeyManagementNonDefaultKeyIdObjectToUnEncryptedObject() {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSE_AWS_KMS_NON_DEFAULT_KEY_ENCRYPTED_OBJECT,
                                                              COPY_DEST_BUCKET_NAME, UNENCRYPTED_OBJECT);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, UNENCRYPTED_OBJECT));
    }

    /**
     * Tests copying a Server Side Encryption with AWS KMS Object to
     * un-encrypted object. The key Id used is default. Asserts by retrieving
     * the object and compares it with the content of the file.
     */
    @Test
    public void copySSEAwsKeyManagementDefaultKeyIdObjectToUnEncryptedObject() {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSE_AWS_KMS_DEFAULT_KEY_ENCRYPTED_OBJECT,
                                                              COPY_DEST_BUCKET_NAME, UNENCRYPTED_OBJECT);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, UNENCRYPTED_OBJECT));
    }

    /**
     * Tests copying a Server Side Encryption with AWS KMS Object to Server Side
     * Encryption with AWS KMS Object object. The key Id used is default.
     * Asserts by retrieving the object and compares it with the content of the
     * file.
     */
    @Test
    public void copySSEAwsKeyManagementDefaultKeyIdObjectToNonDefaultKeyId() {
        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSE_AWS_KMS_DEFAULT_KEY_ENCRYPTED_OBJECT,
                                                              COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setSseAwsKeyManagementParams(nonDefaultKmsKeyId);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests copying a Server Side Encryption with AWS KMS Object to Server Side
     * Encryption with AWS KMS Object object. The key Id used is non default.
     * Asserts by retrieving the object and compares it with the content of the
     * file.
     */
    @Test
    public void copySSEAwsKeyManagementNonDefaultKeyIdObjectToDefaultKeyId() {
        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSE_AWS_KMS_NON_DEFAULT_KEY_ENCRYPTED_OBJECT,
                                                              COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setSseAwsKeyManagementParams(DEFAULT_KEY_ID);
        s3Https.copyObject(copyRequest);

        assertResult(randomFile, UNENCRYPTED_OBJECT_CONTENT_LENGTH,
                     s3Https.getObject(COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT));
    }

    /**
     * Tests put object with both SSE-C and SSE AWS KMS set. Should thrown an
     * error saying only one of them must be set.
     */
    @Test
    public void testPutObjectWithSSECAndSSEAwsKeySet() {
        AmazonS3Client s3Client = new AmazonS3Client(credentials);
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
                                                                 ENCRYPTED_OBJECT, randomFile);
        s3Client.setEndpoint("http://s3.amazonaws.com");
        putObjectRequest.setSseAwsKeyManagementParams(DEFAULT_KEY_ID);

        try {
            putObjectRequest.setSseCustomerKey(sseKey);
            fail("Should throw an error as both SSE-C key and SSE AWS KMS cannot be set");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Tests put object with HTTPS not set. Should thrown an exception as SSE
     * operations should be performed only with HTTPS.
     */
    @Test
    public void testPutObjectWithHttpsNotSet() {

        AmazonS3Client s3Client = new AmazonS3Client(credentials);
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
                                                                 ENCRYPTED_OBJECT, randomFile);
        s3Client.setEndpoint("http://s3.amazonaws.com");
        s3Client.setSignerRegionOverride("us-east-1");
        putObjectRequest.setSseAwsKeyManagementParams(DEFAULT_KEY_ID);
        try {
            s3Client.putObject(putObjectRequest);
            fail("An error must be thrown as request to put the object is tried over HTTP and not HTTPS");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Tests put object with HTTPS not set. Should thrown an exception as SSE
     * operations should be performed only with HTTPS.
     */
    @Test
    public void testGetObjectWithHttpsNotSet() {

        AmazonS3Client s3Client = new AmazonS3Client(credentials,
                                                     new ClientConfiguration().withProtocol(Protocol.HTTP));
        s3Client.setEndpoint("https://s3-us-west-2.amazonaws.com");
        try {
            s3Client.getObject(new GetObjectRequest(BUCKET_NAME,
                                                    SSE_AWS_KMS_NON_DEFAULT_KEY_ENCRYPTED_OBJECT));
        } catch (Exception e) {
            fail("Ideally this test should fail as the original request is made using HTTP. But since the redirection hack is introduced, the test case succesfully retrieves the s3 object");
        }
    }

    /**
     * Tests multipart upload object with HTTPS not set. Should thrown an
     * exception as SSE operations should be performed only with HTTPS.
     */
    @Test
    public void testMultipartObjectWithHttpsNotSet() {

        AmazonS3Client s3Client = new AmazonS3Client(credentials,
                                                     new ClientConfiguration().withProtocol(Protocol.HTTP));
        try {
            s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(
                    BUCKET_NAME, ENCRYPTED_OBJECT)
                                                     .withSSEAwsKeyManagementParams(nonDefaultKmsKeyId));
            fail("An error must be thrown as request to get the object is tried over HTTP and not HTTPS");
        } catch (IllegalArgumentException e) {
            // expected
        }

        InitiateMultipartUploadResult initiateResult = s3Https
                .initiateMultipartUpload(new InitiateMultipartUploadRequest(
                        BUCKET_NAME, ENCRYPTED_OBJECT)
                                                 .withSSEAwsKeyManagementParams(nonDefaultKmsKeyId));
        try {
            s3Client.uploadPart(new UploadPartRequest()
                                        .withBucketName(BUCKET_NAME).withFile(randomFile)
                                        .withKey(ENCRYPTED_OBJECT)
                                        .withUploadId(initiateResult.getUploadId()));
            fail("An error must be thrown as request to get the object is tried over HTTP and not HTTPS");
        } catch (AmazonServiceException e) {
            // expected
        }
    }

    /**
     * Tests copy object with HTTPS not set. Should thrown an exception as SSE
     * operations should be performed only with HTTPS.
     */
    @Test
    public void testCopyObjectWithHttpsNotSet() {

        AmazonS3Client s3Client = new AmazonS3Client(credentials,
                                                     new ClientConfiguration().withProtocol(Protocol.HTTP));
        try {
            s3Client.copyObject(new CopyObjectRequest(BUCKET_NAME,
                                                      UNENCRYPTED_OBJECT, COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT)
                                        .withSseAwsKeyManagementParams(nonDefaultKmsKeyId));
            fail("An error must be thrown as request to get the object is tried over HTTP and not HTTPS");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Tests put object with SSE enabled using AWS KMS. The AWS KMS key id is
     * invalid. Asserts that an exception must be thrown from Amazon S3.
     */
    @Test
    public void testPutObjectWithInvalidAWSKeyId() {

        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
                                                                 ENCRYPTED_OBJECT, randomFile);
        putObjectRequest
                .setSseAwsKeyManagementParams(new SseAwsKeyManagementParams(
                        "invalid-key-id"));
        try {
            s3Https.putObject(putObjectRequest);
            fail("Put Object should fail as the AWS Key Management Key Id mentioned is invalid");
        } catch (AmazonServiceException e) {
            // expected
        }
    }

    /**
     * Tests retrieving a SSE AWS KMS Non default key Id encrypted object using
     * a presigned url. Asserts that the data retrieved is same as the random
     * File.
     */
    @Test
    public void testGetObjectPresignedUrlNonDefaultKeyId() throws Exception {
        GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(
                BUCKET_NAME, SSE_AWS_KMS_NON_DEFAULT_KEY_ENCRYPTED_OBJECT,
                HttpMethod.GET);

        HttpResponse response = connectToPresignedUrl(s3Https,
                                                      presignedUrlRequest);

        assertFileEqualsStream(randomFile, response.getEntity().getContent());
    }

    /**
     * Tests retrieving a SSE AWS KMS default key Id encrypted object using a
     * presigned url. Asserts that the data retrieved is same as the random
     * File.
     */
    @Test
    public void testGetObjectPresignedUrlDefaultKeyId()
            throws ClientProtocolException, IOException, InterruptedException {
        GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(
                BUCKET_NAME, SSE_AWS_KMS_DEFAULT_KEY_ENCRYPTED_OBJECT,
                HttpMethod.GET);

        HttpResponse response = connectToPresignedUrl(s3Https,
                                                      presignedUrlRequest);

        assertFileEqualsStream(randomFile, response.getEntity().getContent());
    }

    @Test
    public void testUploadDownloadUsingTransferManager()
            throws InterruptedException {
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
                                                                 ENCRYPTED_OBJECT, randomFile);
        putObjectRequest.setSseAwsKeyManagementParams(nonDefaultKmsKeyId);
        Upload upload = tm.upload(putObjectRequest);

        while (upload.getProgress().getBytesTransferred() < 10 * MB) {
            Thread.sleep(100);
        }

        PersistableUpload infoToResume = upload.pause();

        upload = tm.resumeUpload(infoToResume);
        upload.waitForCompletion();
        Download download = tm.download(new GetObjectRequest(BUCKET_NAME,
                                                             ENCRYPTED_OBJECT), new File(DOWNLOAD_FILE_NAME));
        download.waitForCompletion();
        assertFileEqualsFile(randomFile, new File(DOWNLOAD_FILE_NAME));
    }

    @Test
    public void testCopyUsingTransferManager() throws InterruptedException {

        CopyObjectRequest copyRequest = new CopyObjectRequest(BUCKET_NAME,
                                                              SSEC_ENCRYPTED_OBJECT, COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT);
        copyRequest.setSourceSseCustomerKey(sseKey);
        copyRequest.setSseAwsKeyManagementParams(DEFAULT_KEY_ID);

        Copy copy = tm.copy(copyRequest);
        copy.waitForCompletion();
        Download download = tm.download(new GetObjectRequest(
                COPY_DEST_BUCKET_NAME, ENCRYPTED_OBJECT), new File(
                DOWNLOAD_FILE_NAME));
        download.waitForCompletion();
        assertFileEqualsFile(randomFile, new File(DOWNLOAD_FILE_NAME));
    }

    private HttpResponse connectToPresignedUrl(AmazonS3Client s3,
                                               GeneratePresignedUrlRequest request)
            throws ClientProtocolException, IOException, InterruptedException {
        System.setProperty(
                SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY, "true");
        AmazonS3Client s3Client = new AmazonS3Client(credentials);
        s3Client.setEndpoint("https://s3-us-west-2.amazonaws.com");
        URL url = s3Client.generatePresignedUrl(request);
        System.clearProperty(SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY);
        Thread.sleep(10000);
        HttpRequestBase httpRequest = new HttpGet(URI.create(url
                                                                     .toExternalForm()));

        HttpResponse response = httpClient.execute(httpRequest);

        return response;
    }
}
