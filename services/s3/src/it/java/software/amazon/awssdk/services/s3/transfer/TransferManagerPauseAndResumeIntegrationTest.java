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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.crypto.spec.SecretKeySpec;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartListing;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ResponseHeaderOverrides;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.transfer.exception.FileLockException;
import software.amazon.awssdk.services.s3.transfer.exception.PauseException;
import software.amazon.awssdk.services.s3.transfer.internal.S3SyncProgressListener;
import software.amazon.awssdk.services.s3.transfer.model.UploadResult;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.util.json.Jackson;

@Category(S3Categories.ReallySlow.class)
public class TransferManagerPauseAndResumeIntegrationTest extends
                                                          S3IntegrationTestBase {
    private static final boolean DEBUG = false;
    private static final boolean cleanup = true;
    /** Test object size to be created in Amazon S3. */
    private static final long TEST_OBJECT_CONTENT_LENTH = 100 * MB;
    private static final long INTERRUUPT_SIZE = 30 * MB;
    private static final long START_BYTE = 10;
    private static final long END_BYTE = 94371840;

    //    private static final long TEST_OBJECT_CONTENT_LENTH = 20 * MB;
    //    private static final long INTERRUUPT_SIZE = 10 * MB;
    //    private static final long END_BYTE = TEST_OBJECT_CONTENT_LENTH - 1000;

    //    /** The name of the Object in Amazon S3 */
    //    private static final String key = "key";

    /** The bucket name in Amazon S3 used by the test cases. */
    private static final String bucketName = "java-sdk-tx-pause-"
                                             + System.currentTimeMillis();

    /** Reference to the transfer manager used for testing. */
    private TransferManager tm;

    /** Reference to the file to be uploaded by the test cases. */
    private File fileToUpload;

    /** The reference to the file downloaded from Amazon S3. */
    private File downloadFile;

    /**
     * Creates an empty state file and download file for all test cases.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        S3IntegrationTestBase.setUp();
        s3.createBucket(bucketName);
    }

    /**
     * Deletes the temporary files created for testing purposes.
     */
    @AfterClass
    public static void afterClass() {
        if (cleanup) {
            try {
                deleteBucketAndAllContents(bucketName);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Creates a bucket in Amazon S3 for the test case run.
     */
    @Before
    public void before() throws IOException {
        downloadFile = CryptoTestUtils.generateRandomAsciiFile(0, cleanup);
    }

    @After
    public void after() {
        if (cleanup) {
            if (fileToUpload != null) {
                if (fileToUpload.exists()) {
                    fileToUpload.delete();
                }
            }
            if (downloadFile != null) {
                if (downloadFile.exists()) {
                    downloadFile.delete();
                }
            }
        }
        if (tm != null) {
            tm.shutdownNow(false);
        }
    }

    private Download resumeDownload(TransferManager tm, PersistableDownload state) throws InterruptedException {
        FileLockException exLast = null;
        for (int i = 0; i < 20; i++) {
            try {
                return tm.resumeDownload(state);
            } catch (FileLockException ex) {
                exLast = ex;
                if (DEBUG) {
                    System.out.println("i=" + i + ", Failed to lock file: " + ex.getMessage());
                }
            }
            Thread.sleep(500);
        }
        throw exLast;
    }

    private Upload uploadToS3(final long contentLength, String key) throws IOException {
        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           fileToUpload);
        return tm.upload(putRequest);
    }

    /**
     * Creates a test object in S3 to be used for downloads.
     */
    private void createTestObjectForDownload(String key) throws Exception {
        Upload upload = uploadToS3(TEST_OBJECT_CONTENT_LENTH, key);
        upload.waitForUploadResult();
    }

    private void initializeTransferManager(int threadPoolSize) {
        tm = new TransferManager(s3,
                                 (ThreadPoolExecutor) Executors
                                         .newFixedThreadPool(threadPoolSize));
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMinimumUploadPartSize(10 * MB);
        configuration.setMultipartUploadThreshold(20 * MB);
        tm.setConfiguration(configuration);
    }

    private void initializeTransferManager(AmazonS3 s3Client,
                                           int threadPoolSize, long uploadPartSize, long uploadPartThreshold) {
        tm = new TransferManager(s3Client,
                                 (ThreadPoolExecutor) Executors
                                         .newFixedThreadPool(threadPoolSize));
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMinimumUploadPartSize(uploadPartSize);
        configuration.setMultipartUploadThreshold(uploadPartThreshold);
        tm.setConfiguration(configuration);
    }

    /**
     * This test case performs an pause and resume on a transfer manager file
     * upload. Checks if the state generated as part of pause has all
     * information present. Resumes the upload and checks if the final object in
     * Amazon S3 is same as the original file.
     */
    @Test
    public void testUploadPauseAndResume() throws Exception {
        initializeTransferManager(2);
        long contentLength = TEST_OBJECT_CONTENT_LENTH;
        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);
        final String key = UUID.randomUUID().toString();
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           fileToUpload);
        TestCallback callback = new TestCallback();

        Upload upload = tm.upload(putRequest, callback);

        while (upload.getProgress().getBytesTransferred() < 40 * MB) {
            ;
        }
        PersistableUpload persistableUploadReceivedThroughListenerCallback =
                (PersistableUpload) callback.getState();
        PersistableUpload uploadContext = (PersistableUpload) upload.pause();
        assertNotNull(uploadContext);
        assertEquals(uploadContext.getBucketName(), bucketName);
        assertEquals(uploadContext.getKey(), key);
        assertEquals(uploadContext.getFile(), fileToUpload.getAbsolutePath());
        assertNotNull(upload.getProgress());
        assertNotNull(persistableUploadReceivedThroughListenerCallback);
        assertEquals(uploadContext.getBucketName(),
                     persistableUploadReceivedThroughListenerCallback.getBucketName());
        assertEquals(uploadContext.getKey(),
                     persistableUploadReceivedThroughListenerCallback.getKey());
        assertEquals(uploadContext.getFile(),
                     persistableUploadReceivedThroughListenerCallback.getFile());

        String uploadId = uploadContext.getMultipartUploadId();
        PartListing parts = s3.listParts(new ListPartsRequest(bucketName, key,
                                                              uploadId));
        assertNotNull(parts.getParts());
        assertTrue(parts.getParts().size() > 0);

        String serializedContext = uploadContext.serialize();

        upload = tm.resumeUpload((PersistableUpload) PersistableTransfer
                .deserializeFrom(serializedContext));

        UploadResult result = upload.waitForUploadResult();
        assertNotNull(result);
        assertNotNull(result.getETag());

        S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, key));
        assertTrue(s3Object.getObjectMetadata().getContentLength() > 0);
        assertTrue(s3Object.getObjectMetadata().getContentLength() == contentLength);
        assertNotNull(s3Object.getObjectContent());
        assertFileEqualsStream(new File(fileToUpload.getAbsolutePath()),
                               s3Object.getObjectContent());
    }

    /**
     * This test case performs a pause on upload whose state cannot be saved. An
     * exception is thrown as the state of the transfer cannot be captured and
     * all the transfers are aborted.
     */
    @Test
    public void testPauseOnAUploadInSingleChunk() throws Exception {
        initializeTransferManager(s3, 2, TEST_OBJECT_CONTENT_LENTH, 2 * TEST_OBJECT_CONTENT_LENTH);
        long contentLength = 60 * MB;
        final String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);

        while (upload.getProgress().getBytesTransferred() < 40 * MB) {
            ;
        }
        try {
            upload.pause();
            fail("An exception should be thrown here saying that operation pause is not possible as the state cannot be captured in this case.");
        } catch (AmazonClientException ace) {
        }
    }

    /**
     * This test case tries to pause immediately after initiating an transfer
     * manager upload. Since the upload is likely possible not started, all the
     * transfers are cancelled.
     */
    @Test
    public void testPauseOnAUploadImmediately() throws Exception {
        initializeTransferManager(s3, 1, TEST_OBJECT_CONTENT_LENTH, 2 * TEST_OBJECT_CONTENT_LENTH);
        long contentLength = 60 * MB;
        final String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);
        try {
            upload.pause();
            fail("An exception should be thrown here saying that operation pause is not possible as the upload is yet to start and state cannot be captured in this case.");
        } catch (PauseException ace) {
            assertEquals(ace.getPauseStatus(),
                         PauseStatus.CANCELLED_BEFORE_START);
        }
    }

    /**
     * This test case performs an resume upload with invalid state information
     * like an upload id not present in Amazon S3.
     */
    @Test
    public void testResumeUploadWithAInCorrectUploadId() throws Exception {
        initializeTransferManager(2);

        long contentLength = TEST_OBJECT_CONTENT_LENTH;
        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);
        final String key = UUID.randomUUID().toString();
        PersistableUpload context = new PersistableUpload(bucketName, key,
                                                          fileToUpload.getAbsolutePath(), "incorrect-upload-id", 0L,
                                                          20 * MB);

        try {
            Upload upload = tm.resumeUpload(context);
            upload.waitForCompletion();
            fail("An amazon service exception must be thrown as the upload id given is not present in the s3 server side");
        } catch (AmazonServiceException ace) {
        }
    }

    /**
     * This test case performs a upload with input stream and tries to pause the
     * upload. The transfers must be aborted and the state capture must fail.
     */
    @Test
    public void testTryPauseOnUploadWithInputStream() {
        initializeTransferManager(s3, 1, 10 * MB, 20 * MB);
        long contentLength = 60 * MB;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        final String key = UUID.randomUUID().toString();
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           new RandomInputStream(contentLength), metadata);
        Upload upload = tm.upload(putRequest);
        while (upload.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PauseResult<PersistableUpload> pauseResult = upload.tryPause(true);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.CANCELLED);
        try {
            s3.getObject(bucketName, key);
            fail("The object should not be in Amazon S3 as the upload was aborted");
        } catch (AmazonServiceException ase) {
        }
    }

    /**
     * This test case performs a upload with client side encryption and tries to
     * pause the upload. The transfers must be aborted and the state capture
     * must fail.
     */
    @Test
    public void testTryPauseOnUploadWithEncryptionClient() throws IOException {
        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);

        long contentLength = 200 * MB;
        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);
        final String key = UUID.randomUUID().toString();
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           fileToUpload);
        Upload upload = tm.upload(putRequest);
        while (upload.getProgress().getBytesTransferred() < 40 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(true);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.CANCELLED);
        try {
            encS3.getObject(bucketName, key);
            fail("The object should not be in Amazon S3 as the upload was aborted");
        } catch (AmazonServiceException ase) {
        }

    }

    /**
     * This test performs a upload with a file of size lesser than the upload
     * threshold for multi-part upload. Tries to pause the upload. The transfers
     * must be aborted and the state capture must fail.
     *
     * @throws InterruptedException
     */
    @Test
    public void testTryPauseOnUploadWithFileSizeLessThanUploadThreshold()
            throws IOException, InterruptedException {
        //TODO document why this is changed.
        initializeTransferManager(s3, 2, TEST_OBJECT_CONTENT_LENTH, 2 * TEST_OBJECT_CONTENT_LENTH);
        long contentLength = 60 * MB;
        final String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);
        while (upload.getProgress().getBytesTransferred() < 20 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(true);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.CANCELLED);

        try {
            s3.getObject(bucketName, key);
            fail("The object should not be available in Amazon S3 as the upload was aborted");
        } catch (AmazonServiceException e) {
        }
    }

    /**
     * This test case tries to pause an upload of a file with size greater than
     * the multi part upload threshold. The transfers must be aborted and the
     * state capture must be successful.
     */
    @Test
    public void testTryPauseOnUploadWithFileSizeGreaterThanUploadThreshold()
            throws IOException {
        initializeTransferManager(s3, 2, 10 * MB, 20 * MB);
        long contentLength = 60 * MB;
        final String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);
        while (upload.getProgress().getBytesTransferred() < 20 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(true);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNotNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.SUCCESS);
        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName,
                                                                key, uploadContext.getMultipartUploadId()));
    }

    /**
     * This test case performs a upload with input stream and tries to pause the
     * upload. The transfers must not be aborted as the force abort is disabled
     * on pause and the state capture must fail. The file must be uploaded
     * successfully to Amazon S3
     */
    @Test
    public void testPauseForceAbortDisabledOnUploadWithInputStream()
            throws AmazonServiceException, AmazonClientException,
                   InterruptedException, IOException {
        initializeTransferManager(s3, 2, 10 * MB, 20 * MB);
        long contentLength = 60 * MB;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);
        final String key = UUID.randomUUID().toString();
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           new FileInputStream(fileToUpload), metadata);
        Upload upload = tm.upload(putRequest);
        while (upload.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PauseResult<PersistableUpload> pauseResult = upload.tryPause(false);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.NO_EFFECT);

        upload.waitForUploadResult();

        S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, key));
        assertTrue(s3Object.getObjectMetadata().getContentLength() > 0);
        assertTrue(s3Object.getObjectMetadata().getContentLength() == contentLength);
        assertNotNull(s3Object.getObjectContent());
        assertFileEqualsStream(new File(fileToUpload.getAbsolutePath()),
                               s3Object.getObjectContent());
    }

    /**
     * This test case performs a upload with client side encryption and tries to
     * pause the upload. The transfers must not be aborted as the force abort is
     * disabled on pause and the state capture must fail. The file must be
     * uploaded successfully to Amazon S3
     */
    @Test
    public void testPauseForceAbortDisabledOnUploadWithEncryptionClient()
            throws IOException, AmazonServiceException, AmazonClientException,
                   InterruptedException {
        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);

        long contentLength = 200 * MB;
        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);

        final String key = UUID.randomUUID().toString();
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           fileToUpload);
        Upload upload = tm.upload(putRequest);
        while (upload.getProgress().getBytesTransferred() < 40 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(false);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();

        assertNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.NO_EFFECT);

        upload.waitForUploadResult();

        S3Object s3Object = encS3.getObject(new GetObjectRequest(bucketName,
                                                                 key));
        assertTrue(s3Object.getObjectMetadata().getContentLength() > 0);
        assertNotNull(s3Object.getObjectContent());
        assertFileEqualsStream(new File(fileToUpload.getAbsolutePath()),
                               s3Object.getObjectContent());
    }

    /**
     * This test case performs a upload with a file of size lesser than multi
     * part upload threshold and tries to pause the upload. The transfers must
     * not be aborted as the force abort is disabled on pause and the state
     * capture must fail. The file must be uploaded successfully to Amazon S3
     */
    @Test
    public void testPauseForceAbortDisabledOnUploadWithFileSizeLessThanUploadThreshold()
            throws IOException, AmazonServiceException, AmazonClientException,
                   InterruptedException {
        initializeTransferManager(s3, 2, TEST_OBJECT_CONTENT_LENTH, 2 * TEST_OBJECT_CONTENT_LENTH);
        long contentLength = 60 * MB;
        final String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);
        while (upload.getProgress().getBytesTransferred() < 20 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(false);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.NO_EFFECT);
        upload.waitForUploadResult();

        S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, key));
        assertTrue(s3Object.getObjectMetadata().getContentLength() > 0);
        assertTrue(s3Object.getObjectMetadata().getContentLength() == contentLength);
        assertNotNull(s3Object.getObjectContent());
        assertFileEqualsStream(new File(fileToUpload.getAbsolutePath()),
                               s3Object.getObjectContent());

    }

    /**
     * This test case performs a upload with a file of size greater than multi
     * part upload threshold and tries to pause the upload. The transfers must
     * be aborted in this case because the file is uploaded in multi part
     * parallel mode on pause and the state capture must be success.
     */
    @Test
    public void testPauseForceAbortDisabledOnUploadWithFileSizeGreaterThanUploadThreshold()
            throws IOException {
        initializeTransferManager(s3, 2, 10 * MB, 20 * MB);
        long contentLength = 60 * MB;

        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);
        final String key = UUID.randomUUID().toString();

        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           fileToUpload);
        Upload upload = tm.upload(putRequest);
        while (upload.getProgress().getBytesTransferred() < 20 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(false);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNotNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.SUCCESS);
        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName,
                                                                key, uploadContext.getMultipartUploadId()));
    }

    /**
     * This test case performs a upload with a client side encryption enabled.
     * On pause an exception must be thrown as state capture cannot be done on
     * such upload. Also the transfers must be aborted.
     */
    @Test
    public void testUploadPauseWithClientSideEncryption() throws Exception {

        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);

        long contentLength = 200 * MB;
        fileToUpload = CryptoTestUtils.generateRandomAsciiFile(contentLength, cleanup);
        final String key = UUID.randomUUID().toString();
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key,
                                                           fileToUpload);
        Upload upload = tm.upload(putRequest);
        while (upload.getProgress().getBytesTransferred() < 40 * MB) {
            ;
        }
        try {
            upload.pause();
            fail("An exception should be thrown as pause is not possible on encrypted uplaods");
        } catch (PauseException ace) {
            assertEquals(ace.getPauseStatus(), PauseStatus.CANCELLED);
        }
    }

    /**
     * Tries to pause a multi part upload and checks if the state generated has
     * all the information.
     */
    @Test
    public void testUploadPauseSave() throws Exception {
        initializeTransferManager(s3, 2, 10 * MB, 20 * MB);
        long contentLength = 60 * MB;
        final String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);
        while (upload.getProgress().getBytesTransferred() < 20 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(false);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNotNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.SUCCESS);

        uploadContext = Jackson.fromJsonString(uploadContext.serialize(),
                                               PersistableUpload.class);
        assertNotNull(uploadContext);
        assertNotNull(uploadContext.getBucketName());
        assertNotNull(uploadContext.getKey());
        assertNotNull(uploadContext.getFile());
        assertNotNull(uploadContext.getMultipartUploadId());
        assertNotNull(uploadContext.getMutlipartUploadThreshold());
        assertNotNull(uploadContext.getPartSize());
        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName,
                                                                key, uploadContext.getMultipartUploadId()));
    }

    /**
     * Tests pause on a un encrypted file download. The resume should start from
     * where the pause left.
     */
    @Test
    public void testDownloadPauseNonEncryptedFile() throws Exception {
        initializeTransferManager(50);
        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        TestCallback callback = new TestCallback();
        Download download = tm.download(new GetObjectRequest(bucketName, key),
                                        downloadFile, callback);

        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();
        PersistableTransfer persistableUploadReceivedThroughListenerCallback = callback
                .getState();
        assertNotNull(persistableUploadReceivedThroughListenerCallback);
        download = resumeDownload(tm, state);
        download.waitForCompletion();
        S3Object s3Object = s3.getObject(bucketName, key);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());
    }

    /**
     * Tests pause on a encrypted file download. The resume should start from
     * where the pause left and download file must be same as the one in Amazon
     * S3.
     */
    @Test
    public void testDownloadPauseEncryptedFile() throws Exception {
        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);
        final String key = UUID.randomUUID().toString();

        createTestObjectForDownload(key);
        Download download = tm.download(bucketName, key, downloadFile);

        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();

        download = resumeDownload(tm, state);
        download.waitForCompletion();

        S3Object s3Object = encS3.getObject(bucketName, key);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());
    }

    /**
     * Tests pause on a un encrypted file download with range specified. The
     * resume should start from where the pause left and download file must be
     * same as the one in Amazon S3.
     */
    @Test
    public void testDownloadPauseOnRangeGetNonEncryptedFile() throws Exception {
        initializeTransferManager(50);

        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName,
                                                                 key);

        getObjectRequest.setRange(10, END_BYTE);

        Download download = tm.download(getObjectRequest, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();

        download = resumeDownload(tm, state);
        download.waitForCompletion();

        S3Object s3Object = s3.getObject(getObjectRequest);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());
    }

    /**
     * Tests pause on a encrypted file download with range specified. The resume
     * should start from where the pause left and download file must be same as
     * the one in Amazon S3.
     */
    @Test
    public void testDownloadPauseOnRangeGetEncryptedFile() throws Exception {

        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);
        final String key = UUID.randomUUID().toString();

        createTestObjectForDownload(key);
        long start = START_BYTE;
        long end = END_BYTE;

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName,
                                                                 key);
        getObjectRequest.setRange(start, end);
        Download download = tm.download(getObjectRequest, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }

        PersistableDownload state = (PersistableDownload) download.pause();
        download = resumeDownload(tm, state);
        download.waitForCompletion();

        getObjectRequest.setRange(start, end);
        S3Object s3Object = encS3.getObject(getObjectRequest);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());
    }

    /**
     * Tests pause on a requester pays bucket. The resume should start from
     * where the pause left and download file must be same as the one in Amazon
     * S3.
     */
    @Test
    public void testDownloadPauseOnRequesterPaysBucket() throws Exception {
        initializeTransferManager(50);
        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        s3.enableRequesterPays(bucketName);

        final long start = START_BYTE;
        final long end = END_BYTE;

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        getObjectRequest.setRange(start, end);

        Download download = tm.download(getObjectRequest, downloadFile);

        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();
        if (DEBUG) {
            System.err.println("paused: downloadFile.length()=" + downloadFile.length());
        }

        download = resumeDownload(tm, state);
        download.waitForCompletion();

        getObjectRequest.setRange(start, end);
        S3Object s3Object = s3.getObject(getObjectRequest);
        String errmsg = "downloadFile: " + downloadFile
                        + ", bucketName=" + bucketName + ", key=" + key + ", start="
                        + start + ", end=" + end;
        assertFileEqualsStream(errmsg, downloadFile, s3Object.getObjectContent());
    }

    @Test
    public void testDownloadPauseOnRequesterPaysBucket2() throws Exception {
        initializeTransferManager(50);

        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        s3.enableRequesterPays(bucketName);

        final long start = START_BYTE;
        final long end = END_BYTE;

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        getObjectRequest.setRange(start, end);

        Download download = tm.download(getObjectRequest, downloadFile);

        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();
        if (DEBUG) {
            System.err.println("paused: downloadFile.length()=" + downloadFile.length());
        }

        DownloadCallable.setTesting(true);
        try {
            download = resumeDownload(tm, state);
            download.waitForCompletion();

            getObjectRequest.setRange(start, end);
            S3Object s3Object = s3.getObject(getObjectRequest);
            String errmsg = "downloadFile: " + downloadFile
                            + ", bucketName=" + bucketName + ", key=" + key + ", start="
                            + start + ", end=" + end;
            assertFileEqualsStream(errmsg, downloadFile, s3Object.getObjectContent());
        } finally {
            DownloadCallable.setTesting(false);
        }
    }

    /**
     * Tests pause on a encrypted file (Authenticated Encryption) download. The
     * resume should start from where the pause left and download file must be
     * same as the one in Amazon S3.
     */
    @Test
    public void testPauseDownloadWithAuthenticatedEncryptionEnabled()
            throws Exception {
        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")), new CryptoConfiguration(
                CryptoMode.AuthenticatedEncryption));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);

        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        Download download = tm.download(bucketName, key, downloadFile);

        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();
        download = resumeDownload(tm, state);
        download.waitForCompletion();

        S3Object s3Object = encS3.getObject(bucketName, key);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());
    }

    /**
     * Tests pause on a encrypted file (Authenticated Encryption) download with
     * range specified. The resume should start from where the pause left and
     * download file must be same as the one in Amazon S3.
     */
    @Test
    public void testPauseDownloadRangeGetWithAuthenticatedEncryptionEnabled()
            throws Exception {
        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")), new CryptoConfiguration(
                CryptoMode.AuthenticatedEncryption));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);
        final String key = UUID.randomUUID().toString();

        createTestObjectForDownload(key);
        Download download = null;
        int i = 0;
        do {
            try {
                download = tm.download(bucketName, key, downloadFile);
            } catch (AmazonS3Exception ex) {
                ex.printStackTrace();
                Thread.sleep(3000);
            }
        } while (download == null && i++ < 3);

        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();
        download = resumeDownload(tm, state);
        download.waitForCompletion();

        S3Object s3Object = encS3.getObject(bucketName, key);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());
    }

    /**
     * Tests pause on a encrypted file (Strict Authenticated Encryption)
     * download. The resume should fail.
     */
    @Test
    public void testPauseDownloadWithStrictAuthenticatedEncryptionEnabled()
            throws Exception {
        AmazonS3EncryptionClient encS3 = new AmazonS3EncryptionClient(
                credentials, new EncryptionMaterials(new SecretKeySpec(
                new byte[32], "AES")), new CryptoConfiguration(
                CryptoMode.StrictAuthenticatedEncryption));
        initializeTransferManager(encS3, 2, 10 * MB, 20 * MB);

        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        Download download = tm.download(bucketName, key, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();

        try {
            download = resumeDownload(tm, state);
            download.waitForCompletion();
            fail("A security Exception must be thrown here as we are trying to do a range get in Strict Authenticated Encryption mode");
        } catch (AmazonClientException e) {
        }
    }

    /**
     * Tests pause on a download with response header overrides. The state must
     * have the response header information and must use when resume
     */
    @Test
    public void testDownloadPauseWithResponseHeadersSetInOriginalRequest()
            throws Exception {
        initializeTransferManager(50);
        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName,
                                                                 key);
        getObjectRequest.setResponseHeaders(new ResponseHeaderOverrides()
                                                    .withContentType("text/plain"));

        Download download = tm.download(getObjectRequest, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();

        PersistableDownload downloadState = Jackson.fromJsonString(
                state.serialize(), PersistableDownload.class);
        assertEquals(downloadState.getResponseHeaders().getContentType(),
                     "text/plain");

        download = resumeDownload(tm, downloadState);
        download.waitForCompletion();

        S3Object s3Object = s3.getObject(getObjectRequest);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());
    }

    /**
     * This test case tests the abort functionality introduced in {@link Upload}
     * interface. The upload must be aborted and the object must not be in
     * Amazon S3.
     */
    @Test
    public void testUploadAbort() throws IOException {
        initializeTransferManager(s3, 2, 10 * MB, 20 * MB);
        long contentLength = 60 * MB;
        String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);
        while (upload.getProgress().getBytesTransferred() < 20 * MB) {
            ;
        }
        upload.abort();

        try {
            s3.getObject(bucketName, key);
            fail("The object should not be available in Amazon S3 as the upload was aborted");
        } catch (AmazonServiceException e) {
        }

    }

    /**
     * Performs a pause and resume on an upload transfer. Asserts that the
     * progress is 100 percent after the resume upload completes.
     */
    @Test
    public void testUploadProgressAfterResume()
            throws IOException, AmazonServiceException, AmazonClientException, InterruptedException {
        initializeTransferManager(s3, 2, 10 * MB, 20 * MB);
        long contentLength = 60 * MB;
        final String key = UUID.randomUUID().toString();
        Upload upload = uploadToS3(contentLength, key);
        while (upload.getProgress().getBytesTransferred() < 20 * MB) {
            ;
        }

        PauseResult<PersistableUpload> pauseResult = upload.tryPause(true);
        PersistableUpload uploadContext = pauseResult.getInfoToResume();
        assertNotNull(uploadContext);
        assertEquals(pauseResult.getPauseStatus(), PauseStatus.SUCCESS);

        upload = tm.resumeUpload(uploadContext);

        upload.waitForCompletion();

        assertTrue(contentLength == upload.getProgress().getBytesTransferred());
        assertEquals(100.00, upload.getProgress().getPercentTransferred(), .001);
    }

    /**
     * Performs a pause and resume on an download transfer. Asserts that the
     * progress is 100 percent after the resume download completes.
     */
    @Test
    public void testDownloadProgressAfterResume() throws Exception {
        initializeTransferManager(50);
        final String key = UUID.randomUUID().toString();
        createTestObjectForDownload(key);
        Download download = tm.download(new GetObjectRequest(bucketName, key),
                                        downloadFile);

        while (download.getProgress().getBytesTransferred() < INTERRUUPT_SIZE) {
            ;
        }
        PersistableDownload state = (PersistableDownload) download.pause();

        download = resumeDownload(tm, state);
        download.waitForCompletion();
        S3Object s3Object = s3.getObject(bucketName, key);
        assertFileEqualsStream(downloadFile, s3Object.getObjectContent());

        assertTrue(TEST_OBJECT_CONTENT_LENTH == download.getProgress().getBytesTransferred());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
    }

    static class TestCallback extends S3SyncProgressListener {
        private PersistableTransfer state;

        public PersistableTransfer getState() {
            return state;
        }

        @Override
        public void onPersistableTransfer(PersistableTransfer pauseTransfer) {
            this.state = pauseTransfer;
        }
    }
}
