package software.amazon.awssdk.services.s3.transfer;

import static software.amazon.awssdk.services.s3.internal.Constants.MB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.junit.Test;

import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.GroupGrantee;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.transfer.Transfer.TransferState;
import software.amazon.awssdk.test.util.RandomTempFile;

/** Integration tests for TransferManager. */
public class TransferFileManagerIntegrationTest extends TransferManagerTestBase {

    /** Tests file uploads, transfer events, etc, for a single-part upload. */
    @Test
    public void testSinglePartFileUpload() throws Exception {
        initializeTransferManager();
        s3.createBucket(bucketName);
        String keyName = "file1";

        RandomTempFile file = new RandomTempFile(keyName,   3*MB);

        // Upload the first file with a progress listener specified in the request
        TestTransferProgressListener progressListener1 = new TestTransferProgressListener();
        Upload upload = tm.upload(new PutObjectRequest(bucketName, keyName, file)
            .<PutObjectRequest>withGeneralProgressListener(progressListener1));

        upload.waitForCompletion();

        // Test the first upload (which used a multipart upload)
        assertEquals(upload.getState(), TransferState.Completed);
        assertEquals(file.length(), upload.getProgress().getBytesTransferred());
        assertEquals(upload.getProgress().getTotalBytesToTransfer(), file.length());

        assertFalse(progressListener1.duplicateEventsSeen);

        assertSinglePartETag(upload.waitForUploadResult().getETag());
        assertEquals(bucketName, upload.waitForUploadResult().getBucketName());
        assertEquals(keyName, upload.waitForUploadResult().getKey());
        assertTrue(progressListener1.bytesTransferred);
        assertTrue(progressListener1.transferCompleted);
        assertFalse(progressListener1.transferFailed);
        ObjectMetadata retrievedMetadata = s3.getObjectMetadata(bucketName, keyName);
        assertNotEmpty(retrievedMetadata.getContentType());
        super.assertFileEqualsStream(file, s3.getObject(bucketName, keyName).getObjectContent());
    }

    /** Tests that we can delete files after we upload them with transfermanager. */
    @Test
    public void testDeletingFileAfterUploading() throws Exception {
        initializeTransferManager();
        s3.createBucket(bucketName);
        String keyName = "file2";

        RandomTempFile file = new RandomTempFile(keyName,  25*MB);

        Upload upload = tm.upload(bucketName, keyName, file);
        upload.waitForCompletion();
        assertEquals(TransferState.Completed, upload.getState());

        System.out.println("File: " + file.getAbsolutePath());
        assertTrue(file.delete());
    }

    /** Tests file uploads, transfer events, etc, for a multipart upload. */
    @Test
    public void testMultipartFileUpload() throws Exception {
        initializeTransferManager();
        s3.createBucket(bucketName);
        String keyName = "file3";

        RandomTempFile file = new RandomTempFile(keyName,  25*MB);

        // Use all request parameters for the second file
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, keyName, file);
        putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        putObjectRequest.setStorageClass(StorageClass.ReducedRedundancy);
        ObjectMetadata expectedMetadata = new ObjectMetadata();
        expectedMetadata.addUserMetadata("foo", "bar");
        expectedMetadata.setContentEncoding("ContentEncoding");
        putObjectRequest.setMetadata(expectedMetadata);
        Upload upload = tm.upload(putObjectRequest);
        TestTransferProgressListener progressListener2 = new TestTransferProgressListener();
        upload.addProgressListener(progressListener2);

        upload.waitForCompletion();

        // Test the second upload (which used advanced request params)
        assertEquals(TransferState.Completed, upload.getState());
        assertEquals(file.length(), upload.getProgress().getBytesTransferred());
        assertEquals(file.length(), upload.getProgress().getTotalBytesToTransfer());
        assertMultipartETag(upload.waitForUploadResult().getETag());
        assertEquals(bucketName, upload.waitForUploadResult().getBucketName());
        assertEquals(keyName, upload.waitForUploadResult().getKey());

        // AbstractTransfer#waitForCompletion() returns as soon as UploadMonitor.isDone() returns true.
        // This might happen before the progress listener is notified by the complete event.
        Thread.sleep(1000);
        assertFalse(progressListener2.duplicateEventsSeen);
        assertTrue(progressListener2.transferCompleted);
        assertFalse(progressListener2.transferFailed);
        ObjectMetadata retrievedMetadata = s3.getObjectMetadata(bucketName, keyName);
        assertNotEmpty(retrievedMetadata.getContentType());
        assertEquals(1, retrievedMetadata.getUserMetadata().size());
        assertEquals("bar", retrievedMetadata.getUserMetadata().get("foo"));
        assertEquals("ContentEncoding", retrievedMetadata.getContentEncoding());
        super.assertFileEqualsStream(file, s3.getObject(bucketName, keyName).getObjectContent());
        assertTrue(doesAclContainGroupGrant(s3.getObjectAcl(bucketName, keyName),
                GroupGrantee.AllUsers, Permission.Read));
    }

    /**
     * This tests tries to initiate a TransferManager on a thread pool that is
     * already shutdown. Asserts that no events have been fired from the
     * transfer manager.
     * @throws IOException
     */
    @Test
    public void testTransferProgressThreadpoolShutdown() throws IOException {
        final long lengthInBytes = 3 *MB;
        final TestTransferProgressListener listener = new TestTransferProgressListener();
        final File file = CryptoTestUtils.generateRandomAsciiFile(lengthInBytes, true);
        final ExecutorService threadPool = Executors.newFixedThreadPool(1);

        tm = new TransferManager(s3, threadPool);

        threadPool.shutdown();
        try {
            tm.upload(new PutObjectRequest(bucketName, "key", file), listener);
            fail("Upload cannot be scheduled as the thread pool is already shutdown");
        } catch (RejectedExecutionException expected) { }

        assertFalse(listener.transferStarted);
        assertFalse(listener.bytesTransferred);
        assertFalse(listener.duplicateEventsSeen);
        assertFalse(listener.transferFailed);
        assertFalse(listener.transferCompleted);
    }

    /**
     * Try performing an simple single chunk upload upload for a bucket that doesn't exists.
     * The transfer manager upload should fail. Asserts that the events are triggered.
     */
    @Test
    public void testTransferProgressInvalidBucketNameOneChunkUpload()
            throws IOException, InterruptedException {
        final long lengthInBytes = 3 * MB;
        final TestTransferProgressListener listener = new TestTransferProgressListener();
        final File file = CryptoTestUtils.generateRandomAsciiFile(
                lengthInBytes, true);
        final ExecutorService threadPool = Executors.newFixedThreadPool(1);

        tm = new TransferManager(s3, threadPool);

        Upload upload = tm.upload(new PutObjectRequest("bucket-not-exists",
                "key", file), listener);

        upload.waitForException();

        assertTrue(listener.transferStarted);
        assertFalse(listener.bytesTransferred);
        assertFalse(listener.duplicateEventsSeen);
        assertFalse(listener.transferCanceled);
        assertTrue(listener.transferFailed);
        assertFalse(listener.transferCompleted);
    }

    /**
     * Try performing an multipart parallel upload for a bucket that doesn't exists.
     * The transfer manager upload should fail. Asserts that the events are triggered.
     */
    @Test
    public void testTransferProgressInvalidBucketNameMultipartUpload()
            throws IOException, InterruptedException {
        final long lengthInBytes = 30 * MB;
        final TestTransferProgressListener listener = new TestTransferProgressListener();
        final File file = CryptoTestUtils.generateRandomAsciiFile(
                lengthInBytes, true);
        final ExecutorService threadPool = Executors.newFixedThreadPool(1);

        tm = new TransferManager(s3, threadPool);

        Upload upload = tm.upload(new PutObjectRequest("bucket-not-exists",
                "key", file), listener);

        try {
            upload.waitForUploadResult();
        }catch (AmazonS3Exception expected) { }

        assertTrue(listener.transferStarted);
        assertFalse(listener.bytesTransferred);
        assertFalse(listener.duplicateEventsSeen);
        assertFalse(listener.transferCanceled);
        assertTrue(listener.transferFailed);
        assertFalse(listener.transferCompleted);
    }

    /**
     * Aborts a multipart parallel upload and asserts if the events are triggered.
     */
    @Test
    public void testTransferProgressMultipartUploadAbort()
            throws IOException, InterruptedException {
        final long lengthInBytes = 30 * MB;
        final TestTransferProgressListener listener = new TestTransferProgressListener();
        final File file = CryptoTestUtils.generateRandomAsciiFile(
                lengthInBytes, true);
        final ExecutorService threadPool = Executors.newFixedThreadPool(1);

        tm = new TransferManager(s3, threadPool);

        Upload upload = tm.upload(
                new PutObjectRequest(bucketName, "key", file), listener);

        while (upload.getProgress().getBytesTransferred() < 10 * MB) {
            continue;
        }

        upload.abort();

        try {
            upload.waitForException();
        } catch (CancellationException expected) { }

        assertTrue(listener.transferStarted);
        assertTrue(listener.bytesTransferred);
        assertFalse(listener.duplicateEventsSeen);
        assertTrue(listener.transferCanceled);
        assertFalse(listener.transferFailed);
        assertFalse(listener.transferCompleted);
    }
}
