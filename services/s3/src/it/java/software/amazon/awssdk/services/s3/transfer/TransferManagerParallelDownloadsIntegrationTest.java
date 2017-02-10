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

package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;
import static software.amazon.awssdk.services.s3.transfer.internal.TransferManagerUtils.createDefaultExecutorService;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SSECustomerKey;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.transfer.Transfer.TransferState;
import software.amazon.awssdk.services.s3.transfer.exception.PauseException;
import software.amazon.awssdk.services.s3.transfer.internal.DownloadPartCallable;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.test.util.SdkAsserts;
import software.amazon.awssdk.test.util.TestExecutors;

@Category(S3Categories.Slow.class)
public class TransferManagerParallelDownloadsIntegrationTest extends
        S3IntegrationTestBase
{
    /** Reference to the Transfer manager instance used for testing */
    private static TransferManager tm;
    private static TransferManagerConfiguration tmConfig;

    /** The bucket used for these tests */
    private final static String BUCKET_NAME = "java-parallel-downloads-integ-test" + new Date().getTime();

    /** The versioning enabled bucket used for these tests */
    private final static String VERSION_ENABLED_BUCKET_NAME = "java-parallel-downloads-versioning-integ-test"
            + new Date().getTime();

    /** The key used for testing multipart object */
    private final static String MULTIPART_OBJECT_KEY = "multiPartkey";

    /** The key used for testing non multipart object */
    private final static String NON_MULTIPART_OBJECT_KEY = "nonMultiPartkey";

    /** The key used for testing multipart object with Server-side encryption */
    private final static String MULTIPART_OBJECT_KEY_WITH_SSE = "multiPartkey-sse";

    /** The key used for testing non multipart object with Server-side encryption */
    private final static String NON_MULTIPART_OBJECT_KEY_WITH_SSE = "nonMultiPartkey-sse";

    /** The customer provided server-side encryption key */
    private static final SSECustomerKey SSE_KEY = new SSECustomerKey(CryptoTestUtils.getTestSecretKey());

    /** The size of the multipart object uploaded to S3 */
    private final static long MULTIPART_OBJECT_SIZE = 20 * MB;

    /** The size of the non multipart object uploaded to S3 */
    private final static long NON_MULTIPART_OBJECT_SIZE = 12 * MB;

    /** Default upload threshold for multipart uploads */
    protected static final long DEFAULT_MULTIPART_UPLOAD_THRESHOLD = 15 * MB;

    /** Default part size for multipart uploads */
    protected static final long DEFAULT_MULTIPART_UPLOAD_PART_SIZE = 5 * MB;

    /** Default size used for interrupting downloads */
    private static final long INTERRUPT_SIZE = 10 * MB;

    /** Start of Range */
    private static final long START_BYTE = 100;

    /** End of Range */
    private static final long END_BYTE = MULTIPART_OBJECT_SIZE - 1000;

    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    /** File that contains the multipart data uploaded to S3 */
    private static File multiPartFile;

    /** File that contains the non multipart data uploaded to S3 */
    private static File nonMultiPartFile;

    /** File that contains the data downloaded from S3 */
    private static File downloadFile;

    /**
     * Creates and initializes all the test resources needed for these tests.
     */
    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        tm = new TransferManager(s3);

        tmConfig = new TransferManagerConfiguration();
        tmConfig.setMultipartUploadThreshold(DEFAULT_MULTIPART_UPLOAD_THRESHOLD);
        tmConfig.setMinimumUploadPartSize(DEFAULT_MULTIPART_UPLOAD_PART_SIZE);
        tm.setConfiguration(tmConfig);

        s3.createBucket(BUCKET_NAME);
        createVersionedBucket();

        uploadNonMultiPartFiles();
        uploadMultiPartFiles();

        downloadFile = new File(TEMP_DIR, "dst");
    }

    /**
     * Creates a bucket with versioning enabled.
     */
    private static void createVersionedBucket() {
        s3.createBucket(VERSION_ENABLED_BUCKET_NAME);
        BucketVersioningConfiguration configuration = new BucketVersioningConfiguration().withStatus("Enabled");
        s3.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(VERSION_ENABLED_BUCKET_NAME, configuration));
    }

    /**
     * Upload non multipart objects to s3.
     */
    private static void uploadNonMultiPartFiles() throws Exception {
        nonMultiPartFile = new RandomTempFile("nonmultipart", NON_MULTIPART_OBJECT_SIZE);
        assertEquals(NON_MULTIPART_OBJECT_SIZE, nonMultiPartFile.length());
        Upload myUpload = tm.upload(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, nonMultiPartFile);
        myUpload.waitForCompletion();

        // Upload object with Server-side encryption
        myUpload = tm.upload(new PutObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY_WITH_SSE, nonMultiPartFile).withSSECustomerKey(SSE_KEY));
        myUpload.waitForCompletion();
    }

    /**
     * Upload multipart objects to s3.
     */
    private static void uploadMultiPartFiles() throws Exception {
        multiPartFile = new RandomTempFile("multipart", MULTIPART_OBJECT_SIZE);
        assertEquals(MULTIPART_OBJECT_SIZE, multiPartFile.length());
        Upload myUpload = tm.upload(BUCKET_NAME, MULTIPART_OBJECT_KEY, multiPartFile);
        myUpload.waitForCompletion();

        // Upload object with Server-side encryption
        myUpload = tm.upload(new PutObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY_WITH_SSE, multiPartFile).withSSECustomerKey(SSE_KEY));
        myUpload.waitForCompletion();
    }

    /**
     * This test case performs a download on non-multipart object without any
     * interruptions, the entire object is returned.
     */
    @Test
    public void testDownloadOnNonMultiPartObjectWithoutInterruptionsReturnsEntireObject() throws Exception {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY);
        Download download = tm.download(req, downloadFile);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
        SdkAsserts.assertFileEqualsStream(downloadFile, s3.getObject(req).getObjectContent());
    }

    /**
     * This test ensures that the target directory is created on multi-part downloads so
     * that we're consistent with single-part downloads.
     * Addressing https://github.com/aws/aws-sdk-java/issues/853
     */
    @Test
    public void testDownloadMultipartToNonExistentDirectory() throws Exception {
        ExecutorService es = TestExecutors.blocksOnFirstCallFromCallableOfType(createDefaultExecutorService(), DownloadPartCallable.class);
        TransferManager tm = new TransferManager(s3, es);

        tm.setConfiguration(tmConfig);
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        File fileInNonExistingDirectory = fileInDirectoryThatDoesNotExist();

        Download download = tm.download(req, fileInNonExistingDirectory);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        assertEquals(fileInNonExistingDirectory.length(), download.getProgress().getBytesTransferred());
        SdkAsserts.assertFileEqualsStream(fileInNonExistingDirectory, s3.getObject(req).getObjectContent());
    }

    /**
     * This test case performs a download on a multipart object without any
     * interruptions, the entire object is returned.
     */
    @Test
    public void testDownloadOnMultiPartObjectWithoutInterruptionsReturnsEntireObject() throws Exception {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        Download download = tm.download(req, downloadFile);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
        SdkAsserts.assertFileEqualsStream(downloadFile, s3.getObject(req).getObjectContent());
    }

    /**
     * This test case performs a download on SSE encrypted non-multipart object without any
     * interruptions, the entire object is returned.
     */
    @Test
    public void testDownloadOnSseEncryptedNonMultiPartObjectWithoutInterruptionsReturnsEntireObject() throws Exception {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY_WITH_SSE).withSSECustomerKey(SSE_KEY);
        Download download = tm.download(req, downloadFile);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile, s3.getObject(req).getObjectContent());
    }

    /**
     * This test case performs a download on SSE encrypted multipart object without any
     * interruptions, the entire object is returned.
     */
    @Test
    public void testDownloadOnSseEncryptedMultiPartObjectWithoutInterruptionsReturnsEntireObject() throws Exception {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY_WITH_SSE).withSSECustomerKey(SSE_KEY);
        Download download = tm.download(req, downloadFile);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile, s3.getObject(req).getObjectContent());
    }

    /**
     * This test case starts a download on a multipart object and aborts, the
     * download state should be TransferState.Canceled.
     */
    @Test
    public void testAbortOnNonMultiPartObject() throws Exception {
        Download download = tm.download(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY), downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;
        download.abort();

        assertEquals(TransferState.Canceled, download.getState());
    }

    /**
     * This test case starts a download on a non multipart object and aborts
     * immediately, the download state should be TransferState.Canceled.
     */
    @Test
    public void testAbortImmediatelyOnMultiPartObject() throws Exception {
        Download download = tm.download(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY), downloadFile);
        while (download.getState() != TransferState.InProgress)
            ;
        download.abort();

        assertEquals(TransferState.Canceled, download.getState());
    }

    /**
     * This test case starts a download on a non multipart object and aborts
     * after partial download, the download state should be
     * TransferState.Canceled.
     */
    @Test
    public void testAbortAfterPartialDownloadOnMultiPartObject() throws Exception {
        Download download = tm.download(new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY), downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;

        download.abort();

        assertEquals(TransferState.Canceled, download.getState());
    }

    /**
     * This test case performs a pause on a non multipart object, and resume it.
     * The entire object should be returned.
     */
    @Test
    public void testDownloadPauseAndResumeOnNonMultiPartObjectReturnsEntireObject() throws Exception {
    	GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY);
        Download download = tm.download(req, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;

        PersistableDownload persistableDownload = download.pause();
        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile,
                s3.getObject(req).getObjectContent());

        assertEquals(downloadFile.length(), download.getProgress().getTotalBytesToTransfer());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
    }

    /**
     * This test case performs a pause on ranged non-multipart object request,
     * and resume it. Asserts that all the bytes in the given range are
     * returned.
     */
    @Test
    public void testDownloadPauseAndResumeOnNonMultiPartObjectWithRangeReturnsRangedObject() throws Exception {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY).withRange(100,
                NON_MULTIPART_OBJECT_SIZE - 1000);
        Download download = tm.download(getObjectRequest, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;

        PersistableDownload persistableDownload = download.pause();
        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile, s3.getObject(getObjectRequest).getObjectContent());

        assertEquals(downloadFile.length(), download.getProgress().getTotalBytesToTransfer());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
    }

    /**
     * This test case performs a pause immediately on a multipart object, and
     * resume it. The entire object should be returned.
     */
    @Test
    public void testDownloadPauseAndResumeImmediatelyOnMultiPartObjectReturnsEntireObject() throws Exception {
    	GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        Download download = tm.download(req, downloadFile);
        while (download.getState() != TransferState.InProgress)
            ;

        PersistableDownload persistableDownload = download.pause();

        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile,
                s3.getObject(req).getObjectContent());

        assertEquals(downloadFile.length(), download.getProgress().getTotalBytesToTransfer());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
    }

    /**
     * This test case performs a pause after a partial download on a multipart
     * object, and resume it. The entire object should be returned.
     */
    @Test
    public void testDownloadPauseAndResumeAfterPartialDownloadOnMultiPartObjectReturnsEntireObject() throws Exception {
    	GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        Download download = tm.download(req, downloadFile);
        while (download.getProgress().getBytesTransferred() < MULTIPART_OBJECT_SIZE)
            ;
        PersistableDownload persistableDownload = download.pause();
        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile,
                s3.getObject(req).getObjectContent());

        assertEquals(downloadFile.length(), download.getProgress().getTotalBytesToTransfer());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
    }

    /**
     * This test case performs a pause after download completes on a multipart
     * object, and resume it. the test returns entire object without any errors.
     */
    @Test
    public void testDownloadPauseAndResumeAfterDownloadCompleteOnMultiPartObjectReturnsEntireObject() throws Exception {
    	GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        Download download = tm.download(req, downloadFile);
        download.waitForCompletion();

        PersistableDownload persistableDownload = download.pause();
        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile,
                s3.getObject(req).getObjectContent());

        assertEquals(downloadFile.length(), download.getProgress().getTotalBytesToTransfer());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
    }

    /**
     * This test case performs to download a multipart object with specific
     * range, then pause and resumes. the object with in the given range is
     * returned. When range is set in GetObjectRequest, a serial download will
     * happen.
     */
    @Test
    public void testDownloadPauseAndResumeOnMultiPartObjectWithRangeReturnsDataWithinRange() throws Exception {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY)
                .withRange(START_BYTE, END_BYTE);
        Download download = tm.download(getObjectRequest, downloadFile);

        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;

        PersistableDownload persistableDownload = download.pause();
        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile, s3.getObject(getObjectRequest).getObjectContent());

        assertEquals(downloadFile.length(), download.getProgress().getTotalBytesToTransfer());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
    }

    /**
     * This test case performs a pause on an object, changes the object in S3
     * and resume it. The test should throw AmazonClientException.
     */
    @Test(expected = AmazonClientException.class)
    public void testDownloadPauseAndResumeWhileObjectModifiedAfterPauseThrowsAmazonClientException() throws Exception {
        Download download = tm.download(new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY), downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;

        PersistableDownload persistableDownload = download.pause();

        Upload upload = tm.upload(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, multiPartFile);
        upload.waitForCompletion();

        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();
    }

    /**
     * This test uploads an object to version enabled bucket, starts to download
     * the object without specifying the version id in request and pauses it,
     * uploads a new object with same key, then resumes download. The test
     * should throw AmazonClientException.
     */
    @Test(expected = AmazonClientException.class)
    public void testDownloadPauseAndResumeOnVersionedBucketWithNoVersionIdInRequestAndObjectModifiedAfterPauseThrowsAmazonClientException()
            throws Exception {
        Upload upload = tm.upload(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, nonMultiPartFile);
        upload.waitForCompletion();

        Download download = tm.download(new GetObjectRequest(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY),
                downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;
        PersistableDownload persistableDownload = download.pause();

        upload = tm.upload(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, multiPartFile);
        upload.waitForCompletion();

        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();
    }

    /**
     * This test uploads an object to version enabled bucket, starts to download
     * the object by specifying version id in the request and pauses it, uploads
     * a new object with same key, then resumes download. The test should return
     * the object with the given version id.
     */
    @Test
    public void testDownloadPauseAndResumeOnVersionedBucketWithVersionIdInRequestAndObjectModifiedAfterPauseReturnsExpectedObject()
            throws Exception {
        Upload upload = tm.upload(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, nonMultiPartFile);
        upload.waitForCompletion();
        String versionID = s3.getObjectMetadata(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY).getVersionId();
        assertNotNull(versionID);

        Download download = tm.download(
                new GetObjectRequest(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, versionID), downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;
        PersistableDownload persistableDownload = download.pause();

        upload = tm.upload(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, multiPartFile);
        upload.waitForCompletion();

        download = tm.resumeDownload(persistableDownload);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        SdkAsserts.assertFileEqualsStream(downloadFile,
                s3.getObject(new GetObjectRequest(VERSION_ENABLED_BUCKET_NAME, NON_MULTIPART_OBJECT_KEY, versionID))
                        .getObjectContent());

        assertEquals(downloadFile.length(), download.getProgress().getTotalBytesToTransfer());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(downloadFile.length(), download.getProgress().getBytesTransferred());
    }

    /**
     * This test case performs a pause on SSE encrypted non multipart object,
     * a PauseException should be thrown.
     */
    @Test (expected = PauseException.class)
    public void testDownloadPauseOnSseEncryptedNonMultiPartObjectThrowsPauseException() throws Exception {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, NON_MULTIPART_OBJECT_KEY_WITH_SSE).withSSECustomerKey(SSE_KEY);
        Download download = tm.download(req, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;

        download.pause();
    }

    /**
     * This test case performs a pause on SSE encrypted multipart object,
     * a PauseException should be thrown.
     */
    @Test (expected = PauseException.class)
    public void testDownloadPauseOnSseEncryptedMultiPartObjectThrowsPauseException() throws Exception {
        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY_WITH_SSE).withSSECustomerKey(SSE_KEY);
        Download download = tm.download(req, downloadFile);
        while (download.getProgress().getBytesTransferred() < INTERRUPT_SIZE)
            ;

        download.pause();
    }

    @Test
    public void testDownloadMultipartObjectToFileWithLessThanThreeCharactersInName() throws Exception {
        File destinationFile = new File(TEMP_DIR, "_");
        destinationFile.deleteOnExit();

        GetObjectRequest req = new GetObjectRequest(BUCKET_NAME, MULTIPART_OBJECT_KEY);
        Download download = tm.download(req, destinationFile);
        download.waitForCompletion();
    }

    @After
    public void tearDownAfterEachTest() throws Exception {
        if (downloadFile != null) {
            downloadFile.delete();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
        CryptoTestUtils.deleteBucketAndAllContents(s3, VERSION_ENABLED_BUCKET_NAME);
        tm.shutdownNow();
        multiPartFile.deleteOnExit();
        nonMultiPartFile.deleteOnExit();
    }

    private File fileInDirectoryThatDoesNotExist() {
        return new File(new File(TEMP_DIR, RandomStringUtils.randomAlphanumeric(10)), "dst");
    }
}
