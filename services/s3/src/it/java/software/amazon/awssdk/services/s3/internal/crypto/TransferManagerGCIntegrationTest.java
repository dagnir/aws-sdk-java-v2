package software.amazon.awssdk.services.s3.internal.crypto;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.SyncProgressListener;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.Upload;

/**
 * Test when the TransferManager is garbage collected before the upload has
 * completed.
 */
public class TransferManagerGCIntegrationTest {
    private static final String TEST_BUCKET = CryptoTestUtils
            .tempBucketName(TransferManagerGCIntegrationTest.class);
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;

    @BeforeClass
    public static void setup() throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    private static boolean WAIT_INSIDE = true;
    
    @Test
    public void testWaitInside() throws IOException, InterruptedException {
        Listener listener = new Listener();
        Upload upload = uploadLargeFile(listener, WAIT_INSIDE);
        assertFalse(listener.hasFailed());
        assertTrue(upload.isDone());
    }

    @Test
    public void testWaitOutside() throws IOException, InterruptedException {
        Listener listener = new Listener();
        Upload upload = uploadLargeFile(listener, !WAIT_INSIDE);
        for (;;) {
            System.err.println("triggering GC");
            System.gc();
            if (upload.isDone()) {
                assertFalse(listener.hasFailed());
                return;
            }
            Thread.sleep(5000);
        }
    }

    public Upload uploadLargeFile(Listener listener, boolean waitInside) throws IOException, InterruptedException {
        File file = CryptoTestUtils.generateRandomAsciiFile(3*1024*1024);  // 3MB
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        TransferManager tm = new TransferManager(s3);
        PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, "somekey", file)
            .withGeneralProgressListener(listener);
        Upload upload = tm.upload(req);
        if (waitInside) {
            for (;;) {
                System.err.println("triggering GC");
                System.gc();
                if (upload.isDone())
                    return upload;
                Thread.sleep(15000);
            }
        }
        Thread.sleep(1000);
        return upload;
    }
    
    private static class Listener extends SyncProgressListener {
        private boolean failed;
        public boolean hasFailed() {
            return failed;
        }

        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            if (progressEvent.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT) {
                System.err.println(progressEvent);
                failed = true;
            }
            else
                System.out.println(progressEvent);
        }
    }
}
