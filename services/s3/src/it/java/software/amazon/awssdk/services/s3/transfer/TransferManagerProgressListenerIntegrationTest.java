package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.transfer.internal.AbstractTransfer;
import software.amazon.awssdk.test.util.RandomTempFile;

/** Integration tests for progress event notifications when using TransferManager. */
public class TransferManagerProgressListenerIntegrationTest extends TransferManagerTestBase {

    /** Tests that blocking method (e.g. waitForCompletion()) won't hang infinitely if executed in ProgressListener callback. **/
    @Test(timeout = 30 * 1000)
    public void testBlockingMethodCallsInProgressListenerCallback() throws Exception {
        initializeTransferManager();
        s3.createBucket(bucketName);

        // Single chunk upload
        String singleChunkUploadKeyName = "file4";
        RandomTempFile singleChunkUploadFile = new RandomTempFile(singleChunkUploadKeyName,  10*MB);
        Upload singleChunkUpload = tm.upload(new PutObjectRequest(bucketName, singleChunkUploadKeyName, singleChunkUploadFile));

        ProgressTestListenerWithBlockCallback blockingListener = new ProgressTestListenerWithBlockCallback(singleChunkUpload);
        singleChunkUpload.addProgressListener(blockingListener);

        blockingListener.waitForAllCallbacksDone();
        assertTrue(blockingListener.startEventCallbackTriggered);
        assertTrue(blockingListener.startEventCallbackReturned);
        assertTrue(blockingListener.completeEventCallbackTriggered);
        assertTrue(blockingListener.completeEventCallbackReturned);

        // Multi-part upload
        String multiPartUploadKeyName = "file5";
        RandomTempFile multiPartUploadFile = new RandomTempFile(singleChunkUploadKeyName,  30*MB);
        Upload multiPartUpload = tm.upload(new PutObjectRequest(bucketName, multiPartUploadKeyName, multiPartUploadFile));

        ProgressTestListenerWithBlockCallback progressListenerWithBlockCallback2 = new ProgressTestListenerWithBlockCallback(multiPartUpload);
        multiPartUpload.addProgressListener(progressListenerWithBlockCallback2);

        progressListenerWithBlockCallback2.waitForAllCallbacksDone();
        assertTrue(progressListenerWithBlockCallback2.startEventCallbackTriggered);
        assertTrue(progressListenerWithBlockCallback2.startEventCallbackReturned);
        assertTrue(progressListenerWithBlockCallback2.completeEventCallbackTriggered);
        assertTrue(progressListenerWithBlockCallback2.completeEventCallbackReturned);
    }

    private static final String TEST_BUCKET = "java-sdk-tx-man-1403759179368";
    protected static final boolean GET_ONLY = false;

    /**
     * Tests that the listener is notified with COMPLETE event after the file is fully written.
     */
    @Test
    public void testCompleteEventForDownloadIntoFile() throws IOException,
            AmazonServiceException, AmazonClientException, InterruptedException {
        initializeTransferManager();
        long contentLength = 30 * MB;
        String keyName = "file";

        if (GET_ONLY) {
            bucketName = TEST_BUCKET;
        } else {
            s3.createBucket(bucketName);
            // Upload a random file
            RandomTempFile file = new RandomTempFile(keyName, contentLength);
            s3.putObject(bucketName, keyName, file);
        }

        // Download the object back into a file
        final File destFile = java.io.File.createTempFile("dowloaded", "");
        destFile.deleteOnExit();
        Download download = tm.download(bucketName, keyName, destFile);
        CompleteEventTestListener testListener = new CompleteEventTestListener((AbstractTransfer)download);
        download.addProgressListener(testListener);

        download.waitForCompletion();
        Thread.sleep(5 * 1000);

        Assert.assertFalse("Duplicate COMPLETE events were sent to the listener.", testListener.duplicateCompleteEvent);
        Assert.assertTrue("We sent the COMPLETE event too early!!", testListener.transferIsDoneUponCompleteEvent);
        Assert.assertTrue(contentLength == download.getProgress().getBytesTransferred());
    }
}
