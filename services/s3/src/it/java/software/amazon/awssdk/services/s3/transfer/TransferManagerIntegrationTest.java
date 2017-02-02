package software.amazon.awssdk.services.s3.transfer;

import static software.amazon.awssdk.services.s3.internal.Constants.MB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CancellationException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.AbortedException;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.SSECustomerKey;
import software.amazon.awssdk.services.s3.transfer.Transfer.TransferState;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.test.util.RandomTempFile;

/** Integration tests for TransferManager. */
@Category(S3Categories.ReallySlow.class)
public class TransferManagerIntegrationTest extends TransferManagerTestBase
        implements ObjectMetadataProvider {

    private static final long DEFAULT_TEST_OBJECT_CONTENT_LENTH = 2 * MB;

    /**
     * Tests to see if the shutdown on transfer makes the upload to fail when
     * the upload is in progress. Also tries to shutdown once an upload is
     * completed and check if the shutdown in success. Tries to perform an
     * upload on a transfer manager that is already shutdown.
     */
    @Test
    public void testShutdownNow() throws Exception {
        long contentLength = 100 * MB;
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(contentLength);

        // Verify that we can shutdown while transfers are in progress
        initializeTransferManager(2);
        Upload upload = uploadRandomInputStream(contentLength, objectMetadata);
        tm.shutdownNow();
        try{
            upload.waitForCompletion();
            fail("Should get an Aborted Exception here.");
        }catch(AbortedException ae) { }

        assertEquals(TransferState.Failed, upload.getState());

        // Verify that we can shutdown after completing all transfers
        s3 = new AmazonS3Client(credentials);
        initializeTransferManager(2);
        upload = uploadRandomInputStream(contentLength, objectMetadata);
        upload.waitForCompletion();
        tm.shutdownNow();

        // Verify that new uploads are rejected by TransferManager
        try {
            uploadRandomInputStream(contentLength, objectMetadata);
            fail("Expected an error since TransferManager is shutdown");
        } catch (Exception e) {
        }

        s3 = new AmazonS3Client(credentials);
    }

    /**
     * Tests that multiple TransferManager objects can be created and
     * used without interfering with each other.
     */
    @Test
    public void testMultipleTransferManagerObjects() throws Exception {
        initializeTransferManager(3);
        long contentLength = 50*MB;

        createTempFile(contentLength);
        Upload upload = tm.upload(bucketName, KEY, tempFile);
        upload.waitForCompletion();
        tm.shutdownNow();

        // Create a second TransferManager instance
        s3 = new AmazonS3Client(credentials);
        initializeTransferManager(3);
        upload = tm.upload(bucketName, KEY,tempFile);
        upload.waitForCompletion();
        assertEquals(TransferState.Completed, upload.getState());
    }

    /**
     * Tests that an IO Exception during a part upload for a resetable stream
     * correctly resets the stream and uploads the correct data to Amazon S3.
     */
    @Test
    public void testUploadPartCorruption() throws Exception {
        initializeTransferManager(DEFAULT_THREAD_POOL_SIZE, 5 * MB, 5 * MB,
                DEFAULT_MULTIPART_COPY_THRESHOLD);

        long contentLength = 10*MB;
        createTempFile(contentLength);

        byte[] byteArray = FileUtils.readFileToByteArray(tempFile);
        CorruptionInputStream stream = new CorruptionInputStream(new ByteArrayInputStream(byteArray));
        stream.setCorruptedDataMark(contentLength - 100);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(contentLength);
        Upload upload = tm.upload(bucketName, KEY, stream, objectMetadata);
        upload.waitForCompletion();
        assertFileEqualsStream(tempFile, s3.getObject(bucketName, KEY).getObjectContent());
    }

    /** Tests that we can upload data directly from a stream.  */
    @Test
    public void testMultipartStreamUploads() throws Exception {
        initializeTransferManager();

        // Test a stream using multipart upload
        long contentLength = 500*MB;
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(contentLength);
        Upload upload = uploadRandomInputStream(contentLength, objectMetadata);
        while (upload.isDone() == false) Thread.sleep(100);

        assertEquals(TransferState.Completed, upload.getState());
        assertEquals((Long) contentLength, (Long) upload.getProgress().getBytesTransferred());
        assertEquals((Long) contentLength, (Long) upload.getProgress().getTotalBytesToTransfer());
        assertMultipartETag(upload.waitForUploadResult().getETag());
    }

    /**
     * Tests that waitForException waits for all the parts to upload
     * when uploading a file.
     */
    @Test
    public void testWaitForExceptionForMultiPartUploads() throws Exception {
        initializeTransferManager();

        long fileSize = 70*MB;
        Upload upload = tm.upload(bucketName, KEY, new RandomTempFile("multipartUpload", fileSize));
        upload.waitForException();

        assertTrue(upload.isDone());
        assertEquals(fileSize, upload.getProgress().getBytesTransferred());
    }

    /**
     * Tests that we can do a multipart upload with a customer provided
     * server-side encryption key, and then download the data again.
     */
    @Test
    public void testMultipartSSECustomerKeySupport() throws Exception {
        initializeTransferManager();

        SSECustomerKey sseCustomerKey = new SSECustomerKey(generateSecretKey());

        // 50MB upload will trigger the multipart upload process
        uploadObjectWithSSECustomerKey(50*MB, sseCustomerKey);

        // Retrive the file using the same SSECustomerKey to decrypt it server-side
        File destinationFile = File.createTempFile(this.getClass().getName(), "" + System.currentTimeMillis());
        destinationFile.deleteOnExit();
        ObjectMetadata metadata = s3.getObject(new GetObjectRequest(bucketName, KEY).withSSECustomerKey(sseCustomerKey), destinationFile);
        assertNotNull(metadata.getSSECustomerKeyMd5());
        destinationFile.delete();
    }

    /**
     * Tests that we can do a single part upload with a customer provided
     * server-side encryption key, and then download the data again.
     */
    @Test
    public void testSinglePartSSECustomerKeySupport() throws Exception {
        initializeTransferManager();

        SSECustomerKey sseCustomerKey = new SSECustomerKey(generateSecretKey());

        // 5MB upload will result in a single part upload
        uploadObjectWithSSECustomerKey(5*MB, sseCustomerKey);

        // Retrive the file using the same SSECustomerKey to decrypt it server-side
        File destinationFile = File.createTempFile(this.getClass().getName(), "" + System.currentTimeMillis());
        destinationFile.deleteOnExit();
        ObjectMetadata metadata = s3.getObject(new GetObjectRequest(bucketName, KEY).withSSECustomerKey(sseCustomerKey), destinationFile);
        assertNotNull(metadata.getSSECustomerKeyMd5());
        destinationFile.delete();
    }

    /**
     * Uploads an object to S3 of the specified size, and using the specified
     * server-side encryption key.
     *
     * @param contentLength
     *            The size of the data to upload.
     * @param sseCustomerKey
     *            The customer provided server-side encryption to use to encrypt
     *            the upload.
     */
    private void uploadObjectWithSSECustomerKey(long contentLength, SSECustomerKey sseCustomerKey) throws Exception {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(contentLength);
        PutObjectRequest request = new PutObjectRequest(bucketName, KEY, new RandomInputStream(contentLength), objectMetadata)
                .withSSECustomerKey(sseCustomerKey);

        Upload multipartStreamUpload = tm.upload(request);
        while (multipartStreamUpload.isDone() == false) Thread.sleep(100);

        assertEquals(TransferState.Completed, multipartStreamUpload.getState());
        assertEquals((Long) contentLength, (Long) multipartStreamUpload.getProgress().getBytesTransferred());
        assertEquals((Long) contentLength, (Long) multipartStreamUpload.getProgress().getTotalBytesToTransfer());
    }

    /** Tests that we can upload data directly from a stream.  */
    @Test
    public void testSinglePartStreamUploads() throws Exception {
        initializeTransferManager();

        // Test a stream using PutObject when we don't specify the content length
        long undeclaredContentLength = 25*MB;
        Upload singlePartStreamUpload = uploadRandomInputStream(undeclaredContentLength, new ObjectMetadata());
        singlePartStreamUpload.waitForCompletion();

        assertEquals(singlePartStreamUpload.getState(), TransferState.Completed);
        assertEquals((Long) undeclaredContentLength, (Long) singlePartStreamUpload.getProgress().getBytesTransferred());
        assertSinglePartETag(singlePartStreamUpload.waitForUploadResult().getETag());
    }

    /**
     * Tests a normal download operation. Uploads an object to Amazon S3 and
     * tries to download using Transfer Manager. Asserts that the content length
     * of object uploaded and downloaded are same.
     */
    @Test
    public void testDownload() throws Exception {
        initializeTransferManager();
        createTestObject();

        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();
        Download download = tm.download(new GetObjectRequest(bucketName, KEY), tempFile);
        TestProgressListener testListener = new TestProgressListener();
        download.addProgressListener(testListener);

        assertFalse(download.isDone());
        while (download.getState() == TransferState.Waiting)
            Thread.sleep(100);
        // sometimes download completes before the below line is executed.
        // So you might see the assertion failing with actual state Completed
        assertEquals(TransferState.InProgress, download.getState());
        assertTrue(download.getProgress().getPercentTransferred() < 100.00);
        download.waitForCompletion();
        assertTrue(download.isDone());
        assertEquals((Long) DEFAULT_TEST_OBJECT_CONTENT_LENTH, (Long) tempFile.length());
        assertTrue(testListener.seenStarted);
        assertTrue(testListener.seenCompleted);
        assertFalse(testListener.seenCanceled);
        assertFalse(testListener.seenFailed);
    }

    /**
     * Tests the download abort operation and asserts to see if the state of the
     * download is Canceled.
     */
    @Test
    public void testCanceledDownload() throws Exception {
        initializeTransferManager();

        createTestObject();
        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();
        Download download = tm.download(new GetObjectRequest(bucketName, KEY), tempFile);
        TestProgressListener testListener = new TestProgressListener();
        download.addProgressListener(testListener);

        while (download.getProgress().getBytesTransferred() == 0L);
        download.abort();
        Thread.sleep(1000 * 10);
        assertTrue(tempFile.length() > 0);
        assertTrue(tempFile.length() <= DEFAULT_TEST_OBJECT_CONTENT_LENTH);
        assertEquals(TransferState.Canceled, download.getState());
        assertTrue(testListener.seenStarted);
        assertTrue(testListener.seenCanceled);
        assertFalse(testListener.seenCompleted);
        assertFalse(testListener.seenFailed);
    }

    /**
     * Downloads an object from Amazon S3 by specifying the range of the object.
     */
    @Test
    public void testDownloadWithRange() throws Exception {
        initializeTransferManager();

        createTestObject();
        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();
        Download download = tm.download(
                new GetObjectRequest(bucketName, KEY).withRange(1024, DEFAULT_TEST_OBJECT_CONTENT_LENTH-1024 ), tempFile);

        download.waitForCompletion();
        System.out.println("Download Status: " + download.getState());
        assertTrue(download.isDone());
        // + 1 since S3 ranges are inclusive
        long expectedLength = DEFAULT_TEST_OBJECT_CONTENT_LENTH - 2048 + 1;
        assertEquals((Long) expectedLength, (Long) tempFile.length());
        assertEquals(100.00, download.getProgress().getPercentTransferred(), .001);
        assertEquals(expectedLength,download.getProgress().getBytesTransferred());
    }

    /**
     * Test transfer manager download by specifying Etag constraints on the
     * request. Download should have been canceled and the file with zero bytes.
     */
    @Test
    public void testDownloadWithConstraints() throws Exception {
        initializeTransferManager();

        createTestObject();
        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();
        Download download = tm.download(
                new GetObjectRequest(bucketName, KEY).withMatchingETagConstraint("fake-eTag"), tempFile);
        download.waitForCompletion();
        assertEquals(TransferState.Canceled, download.getState());
        assertTrue(download.isDone());
        assertEquals((Long) 0L, (Long) tempFile.length());
    }

    @Test
    public void testDownloadShouldTimeout() throws Exception {
        initializeTransferManager();

        createTestObject(50 * MB);
        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();
        Download download = tm.download(
                new GetObjectRequest(bucketName, KEY).withMatchingETagConstraint("fake-eTag"), tempFile, 1L);
        try {
            download.waitForCompletion();
            Assert.fail("Download should have timed out");
        } catch (CancellationException e) {}
    }

    @Test
    public void testDownloadShouldNotTimeout() throws Exception {
        initializeTransferManager();

        createTestObject(1 * MB);
        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();
        Download download = tm.download(
                new GetObjectRequest(bucketName, KEY).withMatchingETagConstraint("fake-eTag"), tempFile, 20000L);
        download.waitForCompletion();
    }

    /**
     * Triggers a download failure by providing a directory instead of file to
     * download.
     */
    @Test
    public void testDownloadFailure() throws Exception {
        initializeTransferManager();

        createTestObject();

        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();
        directory = new File(tempFile.getParentFile(), "dir");
        assertTrue((directory.exists() && directory.isDirectory()) || directory.mkdirs());
        Download download = tm.download(
                new GetObjectRequest(bucketName, KEY), directory);
        TestProgressListener testListener = new TestProgressListener();
        download.addProgressListener(testListener);

        try {
            download.waitForCompletion();
            fail("Expected an exception");
        } catch ( AmazonClientException expected ) { }

        AmazonClientException ex = download.waitForException();
        assertNotNull(ex);
        assertTrue(download.isDone());
        assertEquals(TransferState.Failed, download.getState());
        assertTrue(testListener.seenStarted);
        assertTrue(testListener.seenFailed);
        assertFalse(testListener.seenCanceled);
        assertFalse(testListener.seenCompleted);
    }

    /**
     * Test that transfer manager with single thread does not go into deadlock
     * while downloading an object in parallel.
     *
     * This test should typically be done is less than a minute. Giving a grace
     * timeout value of 2 minutes in case of slow network.
     */
    @Test (timeout = 2 * 60 * 1000)
    public void testParallelDownloadsWithSingleThreadDoNotCauseDeadlock() throws Exception {
        initializeTransferManager(1);

        final long contentLength = 40 * MB;
        File uploadFile = new RandomTempFile(fileName, contentLength);
        tm.upload(bucketName, KEY, uploadFile).waitForCompletion();

        tempFile = File.createTempFile("java-tran-man-test-", "-download");
        tempFile.deleteOnExit();

        Download download = tm.download(bucketName, KEY, tempFile);
        download.waitForCompletion();

        assertEquals(TransferState.Completed, download.getState());
        assertEquals(contentLength, download.getProgress().getBytesTransferred());
        assertTrue(download.isDone());
    }

    /**
     * Tries to download the bucket contents to a directory.
     */
    @Test
    public void testDownloadDirectory() throws Exception {
        initializeTransferManager();

        long contentLength = 1024 * 1024;
        tempFile = getRandomTempFile(fileName, contentLength);
        String[] keys = new String[] { "a",
                    "b/c",
                    "b/d",
                    "b/e/f",
                    "b/e/g",
                    "b/e/h/i",
                    "b/e/h/j",
                    "b/e/h/k/l",
                    "b/e/h/k/m",
                    };

        for (String key : keys) {
            s3.putObject(bucketName, key, tempFile);
        }

        MultipleFileDownload download = tm.downloadDirectory(bucketName, "", directory);
        TestProgressListener testListener = new TestProgressListener();
        download.addProgressListener(testListener);

        while(TransferState.Waiting == download.getState()) Thread.sleep(100);
        assertEquals(TransferState.InProgress, download.getState());

        long bytesTransferred = 0;
        while ( !download.isDone() ) {
            long newBytesTransferred = download.getProgress().getBytesTransferred();
            assertTrue(newBytesTransferred >= bytesTransferred);
            bytesTransferred = newBytesTransferred;
            Thread.sleep(100);
        }
        Thread.sleep(5 * 1000);

        // Check the progress listener has received all events as expected
        assertTrue(testListener.seenStarted);
        assertTrue(testListener.seenCompleted);
        assertFalse(testListener.seenCanceled);
        assertFalse(testListener.seenFailed);
        assertEquals((Long) download.getProgress().getBytesTransferred(), (Long) testListener.totalBytesTransferred);
        assertEquals((Long) download.getProgress().getTotalBytesToTransfer(), (Long) testListener.totalBytesTransferred);
        assertKeysToDirectory(directory, keys, contentLength);
        assertEquals(TransferState.Completed, download.getState());
        assertEquals((Long) (keys.length * contentLength), (Long) download.getProgress().getBytesTransferred());
        assertEquals((Long) (keys.length * contentLength), (Long) download.getProgress().getTotalBytesToTransfer());
        assertTrue(download.isDone());

        deleteDirectoryAndItsContents(directory);
        createTempDirectory();

        // Try again with a smaller subset
        int startKey = 3;
        String prefix = "b/e";
        createTempDirectory();
        download = tm.downloadDirectory(bucketName, prefix, directory);
        download.waitForCompletion();
        assertKeysToDirectory(directory,
                Arrays.copyOfRange(keys, 3, keys.length), contentLength);
        int sublistLength = keys.length - startKey;
        assertEquals(TransferState.Completed, download.getState());
        assertEquals((Long) (sublistLength * contentLength), (Long) download.getProgress().getBytesTransferred());
        assertEquals((Long) (sublistLength * contentLength), (Long) download.getProgress().getTotalBytesToTransfer());
        assertTrue(download.isDone());
    }

    /**
     * Test we can download from an empty bucket
     * @throws Exception
     */
    @Test(timeout = 1000 * 2)
    public void testDownloadEmptyDirectory() throws Exception {
        initializeTransferManager();

         assertTrue(s3.listObjects(bucketName).getObjectSummaries().isEmpty());

         MultipleFileDownload download = tm.downloadDirectory(bucketName, "", directory);
         download.waitForCompletion();
         assertEquals(TransferState.Completed, download.getState());
         assertEquals((Integer) directory.listFiles().length, (Integer) 0);

         download = tm.downloadDirectory(bucketName, "", directory);
         download.waitForCompletion();
         assertEquals(TransferState.Completed, download.getState());
         assertEquals((Integer) directory.listFiles().length, (Integer) 0);
    }

    @Test
    public void testDownloadDirectoryContainsZeroLengthObject() throws InterruptedException {
        String prefix = "contains-zero-length-file";
        initializeTransferManager();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0L);
        s3.putObject(bucketName, prefix + "/zerolen.txt", new ByteArrayInputStream(new byte[0]), metadata);

        tm.downloadDirectory(bucketName, prefix, directory).waitForCompletion();

        assertTrue(new File(new File(directory, prefix), "zerolen.txt").exists());
    }

    /**
     * Test that we can download from a bucket with a lot of objects
     */
    @Test
    public void testDownloadDirectoryWithManyObjects() throws Exception {

        initializeTransferManager();

        final long contentLength = 1024;
        final int numOfObjects = 1000;
        tempFile = new RandomTempFile(fileName, contentLength);
        String[] keys = new String[numOfObjects];
        for(int i = 0; i < numOfObjects; i++) {
            keys[i] = "keys" + i;
        }

        for (String key : keys) {
            s3.putObject(bucketName, key, tempFile);
        }

        MultipleFileDownload download = tm.downloadDirectory(bucketName, "", directory);

        while(TransferState.Waiting == download.getState()) Thread.sleep(100);
        assertEquals(TransferState.InProgress, download.getState());

        long bytesTransferred = 0;
        while ( !download.isDone() ) {
            long newBytesTransferred = download.getProgress().getBytesTransferred();
            assertTrue(newBytesTransferred >= bytesTransferred);
            bytesTransferred = newBytesTransferred;
            Thread.sleep(100);
         }
        assertKeysToDirectory(directory, keys, contentLength);
        assertEquals(TransferState.Completed, download.getState());
        assertEquals((Long) (keys.length * contentLength), (Long) download.getProgress().getBytesTransferred());
        assertEquals((Long) (keys.length * contentLength), (Long) download.getProgress().getTotalBytesToTransfer());
        assertTrue(download.isDone());
    }

    /**
     * Test that we can abort the operation when downloading from a bucket
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testCanceledDownloadDirectory() throws IOException, InterruptedException {
        initializeTransferManager();

        final long contentLength = 1024 * 1024;
        final int numOfObjects = 20;
        tempFile = new RandomTempFile(fileName, contentLength);
        String[] keys = new String[numOfObjects];
        for(int i = 0; i < numOfObjects; i++) {
            keys[i] = "keys" + i;
        }

        for (String key : keys) {
            s3.putObject(bucketName, key, tempFile);
        }

        MultipleFileDownload download = tm.downloadDirectory(bucketName, "", directory);

        TestProgressListener testListener = new TestProgressListener();
        download.addProgressListener(testListener);
        while (download.getProgress().getBytesTransferred() == 0L);
        System.out.println("here");
        download.abort();
        Thread.sleep(1000 * 5);

        assertTrue(testListener.seenStarted);
        assertTrue(testListener.seenCanceled);
        assertFalse(testListener.seenFailed);
        assertFalse(testListener.seenCompleted);
        assertEquals((Long) download.getProgress().getBytesTransferred(), (Long) testListener.totalBytesTransferred);
        assertTrue(download.getProgress().getTotalBytesToTransfer() > testListener.totalBytesTransferred);

        long totalLen = folderSize(directory);
        assertTrue(totalLen > 0);
        assertTrue(totalLen < contentLength * numOfObjects);
        assertEquals(TransferState.Canceled, download.getState());
    }

    /**
     * In the case of virtual directories matching the key names, we favor
     * the virtual directories over the keys.
     */
    @Test
    public void testDownloadDirectoryKeyOverlap() throws Exception {
        initializeTransferManager();

        long contentLength = 1L;
        tempFile= getRandomTempFile("oneByte", contentLength);
        String[] keys = new String[] { "a",
                    "b/c",
                    "b/d",
                    "b/d/f",
                    "b/d/g",
                    "b/d/g/i",
                    "b/d/g/j",
                    "b/d/g/j/l",
                    "b/d/g/j/m",
                    };
        for (String key : keys) {
            s3.putObject(bucketName, key, tempFile);
        }

        MultipleFileDownload download = tm.downloadDirectory(bucketName, "", directory);
        download.waitForCompletion();

        String[] expectedDownloadedKeys = new String[] { "a",
                "b/c",
                "b/d/f",
                "b/d/g/i",
                "b/d/g/j/l",
                "b/d/g/j/m",
                };

        assertKeysToDirectory(directory, expectedDownloadedKeys, contentLength);
        int expectedNumDownloaded = expectedDownloadedKeys.length;
        assertEquals(TransferState.Completed, download.getState());
        assertEquals((Long) (expectedNumDownloaded * contentLength), (Long) download.getProgress().getBytesTransferred());
        assertEquals((Long) (expectedNumDownloaded * contentLength), (Long) download.getProgress().getTotalBytesToTransfer());
        assertTrue(download.isDone());
    }

    @Test
    public void testDownloadDirectoryFailure() throws Exception {
        initializeTransferManager();

        long contentLength = 1L;
        tempFile = getRandomTempFile(fileName, contentLength);
        String[] keys = new String[] { "a",
                    "b/c",
                    "b/d",
                    "b/e/f",
                    "b/e/g",
                    "b/e/h/i",
                    };
        for (String key : keys) {
            s3.putObject(bucketName, key, tempFile);
        }

        // Create a directory where one of the files should go in order to make
        // the download fail.
        int numExpectedFiles = keys.length - 1;
        File conflict = new File(directory, keys[numExpectedFiles]);
        assertTrue(conflict.mkdirs());

        MultipleFileDownload download = tm.downloadDirectory(bucketName, "", directory);

        AmazonClientException ex = download.waitForException();
        assertNotNull(ex);

        try {
            download.waitForCompletion();
            fail("Expected an exception");
        } catch ( AmazonClientException expected ) {
        }

        assertKeysToDirectory(directory,
                Arrays.copyOfRange(keys, 0, numExpectedFiles), contentLength);
        assertTrue(download.isDone());
        assertEquals(TransferState.Failed, download.getState());
        assertEquals((Long) (numExpectedFiles * contentLength), (Long) download.getProgress().getBytesTransferred());
        assertEquals((Long) (keys.length * contentLength), (Long) download.getProgress().getTotalBytesToTransfer());
    }

    /**
     * Tests the uploadFileList method, with the root directory as the common
     * base directory.
     */
    @Test
    public void testUploadFileListWithRootParentDirectory() throws Exception {
        initializeTransferManager();

        File[] tempFiles = new File[2];
        for (int i = 0; i < tempFiles.length; i++) {
            tempFiles[i] = getRandomTempFile(fileName + i, 1024*1024);
        }

        File rootDirectory = new File("/");
        assertEquals(null, rootDirectory.getParent());

        MultipleFileUpload upload = tm.uploadFileList(bucketName, "", rootDirectory, Arrays.asList(tempFiles));
        upload.waitForCompletion();

        for (File tempFile : tempFiles) {
            String keyPath = tempFile.getAbsolutePath();
            if (keyPath.startsWith("/")) keyPath = keyPath.substring(1);

            // If the object doesn't exist, this will throw
            // an exception and fail this test
            s3.getObjectMetadata(bucketName, keyPath);
            tempFile.delete();
        }
    }

    /**
     * Test upload directory functionality including sub directories.
     */
    @Test
    public void testUploadDirectory() throws Exception {
        initializeTransferManager();

        long contentLength = 1024 * 1024;
        tempFile = getRandomTempFile(fileName, contentLength);
        String[] keys = new String[] { "a",
                    "b/c",
                    "b/d",
                    "b/e/f",
                    "b/e/g",
                    "b/e/h/i",
                    "b/e/h/j",
                    "b/e/h/k/l",
                    "b/e/h/k/m",
                    };
        for (String key : keys) {
            FileUtils.copyFile(tempFile, new File(directory, key));
        }

        MultipleFileUpload upload = tm.uploadDirectory(bucketName, "", directory, true);

        TestProgressListener testListener = new TestProgressListener();
        upload.addProgressListener(testListener);

        while (TransferState.Waiting == upload.getState())
            Thread.sleep(100);
        assertEquals(TransferState.InProgress, upload.getState());
        // Checks if the list of sub transfers is equal to the number of files being uploaded.
        assertEquals((Integer)upload.getSubTransfers().size(),(Integer)keys.length);
        long bytesTransferred = 0;
        while ( !upload.isDone() ) {
            long newBytesTransferred = upload.getProgress().getBytesTransferred();
            assertTrue(newBytesTransferred >= bytesTransferred);
            bytesTransferred = newBytesTransferred;
            Thread.sleep(100);
        }

        assertTrue(testListener.seenStarted);
        assertTrue(testListener.seenCompleted);
        assertFalse(testListener.seenFailed);
        assertFalse(testListener.seenCanceled);
        assertEquals((Long) upload.getProgress().getBytesTransferred(), (Long) testListener.totalBytesTransferred);
        assertEquals((Long) upload.getProgress().getTotalBytesToTransfer(), (Long) testListener.totalBytesTransferred);

        ObjectListing listObjects = s3.listObjects(bucketName);
        int i = 0;
        for ( S3ObjectSummary summary : listObjects.getObjectSummaries() ) {
            assertEquals(keys[i++], summary.getKey());
            assertEquals((Long) contentLength, (Long) summary.getSize());
        }
        assertEquals((Integer) keys.length, (Integer) i);

        assertEquals(TransferState.Completed, upload.getState());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getBytesTransferred());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getTotalBytesToTransfer());
        assertTrue(upload.isDone());
    }

    /**
     * Tests upload directory and also passes metadata along with the upload.
     */
    @Test
    public void testUploadDirectoryWithMetadata() throws Exception {
        initializeTransferManager();

        long contentLength = 1024 * 1024;
        tempFile = getRandomTempFile(fileName, contentLength);
        String[] keys = new String[] { "a",
                    "b/c",
                    "b/d",
                    "b/e/f",
                    "b/e/g",
                    "b/e/h/i",
                    "b/e/h/j",
                    "b/e/h/k/l",
                    "b/e/h/k/m",
                    };
        for (String key : keys) {
            FileUtils.copyFile(tempFile, new File(directory, key));
        }

        MultipleFileUpload upload = tm.uploadDirectory(bucketName, "", directory, true,this);

        while(TransferState.Waiting == upload.getState()) Thread.sleep(100);
        assertEquals(TransferState.InProgress, upload.getState());

        long bytesTransferred = 0;
        while ( !upload.isDone() ) {
            long newBytesTransferred = upload.getProgress().getBytesTransferred();
            assertTrue(newBytesTransferred >= bytesTransferred);
            bytesTransferred = newBytesTransferred;
            Thread.sleep(100);
        }

        ObjectListing listObjects = s3.listObjects(bucketName);
        int i = 0;
        for ( S3ObjectSummary summary : listObjects.getObjectSummaries() ) {
            assertEquals(keys[i++], summary.getKey());
            assertEquals((Long) contentLength, (Long) summary.getSize());

            // Check if object metadata for server side encryption is set
            ObjectMetadata metadata=s3.getObjectMetadata(bucketName, summary.getKey());
            assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION,metadata.getSSEAlgorithm());
        }
        assertEquals((Integer) keys.length, (Integer) i);

        assertEquals(TransferState.Completed, upload.getState());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getBytesTransferred());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getTotalBytesToTransfer());
        assertTrue(upload.isDone());
    }

    /**
     * Test that whether we can upload directory without files (may contain empty sub-directories)
     * @throws Exception
     */
    @Test(timeout = 1000 * 4)
    public void testUploadEmptyDirectory() throws Exception {
         initializeTransferManager();

         MultipleFileUpload upload = tm.uploadDirectory(bucketName, "", directory, true);
         upload.waitForCompletion();
         assertEquals(TransferState.Completed, upload.getState());

         File subDir = new File(directory, directory.getName() + "-subDirectoryDownload");
         assertTrue(subDir.mkdir());

         upload = tm.uploadDirectory(bucketName, "", directory, true);
         upload.waitForCompletion();
         assertEquals(TransferState.Completed, upload.getState());
    }

    /**
     * Tests upload directory by having a directory prefix for all the uploads.
     */
    @Test
    public void testUploadDirectoryWithPrefix() throws Exception {
        initializeTransferManager();

        long contentLength = 1024 * 1024;
        tempFile = getRandomTempFile(fileName, contentLength);
        String[] keys = new String[] { "a",
                    "b/c",
                    "b/d",
                    "b/e/f",
                    "b/e/g",
                    "b/e/h/i",
                    "b/e/h/j",
                    "b/e/h/k/l",
                    "b/e/h/k/m",
                    };
        for (String key : keys) {
            FileUtils.copyFile(tempFile, new File(directory, key));
        }

        String directoryPrefix = "virtualDir/foo";
        MultipleFileUpload upload = tm.uploadDirectory(bucketName, directoryPrefix, directory, true);

        while (TransferState.Waiting == upload.getState())
            Thread.sleep(100);
        assertEquals(TransferState.InProgress, upload.getState());

        long bytesTransferred = 0;
        while ( !upload.isDone() ) {
            long newBytesTransferred = upload.getProgress().getBytesTransferred();
            assertTrue(newBytesTransferred >= bytesTransferred);
            bytesTransferred = newBytesTransferred;
            Thread.sleep(100);
        }

        ObjectListing listObjects = s3.listObjects(bucketName);
        int i = 0;
        for ( S3ObjectSummary summary : listObjects.getObjectSummaries() ) {
            assertEquals(directoryPrefix + "/" + keys[i++], summary.getKey());
            assertEquals((Long) contentLength, (Long) summary.getSize());
        }
        assertEquals((Integer) keys.length, (Integer) i);

        assertEquals(TransferState.Completed, upload.getState());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getBytesTransferred());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getTotalBytesToTransfer());
        assertTrue(upload.isDone());
    }

    /**
     * Tests upload directory with includeSubDirectories disabled.
     */
    @Test
    public void testUploadDirectoryNoRecurse() throws Exception {
        initializeTransferManager();

        long contentLength = 1024 * 1024;
        tempFile = getRandomTempFile(fileName, contentLength);
        String[] keys = new String[] { "a",
                    "b/c",
                    "b/d",
                    "b/e/f",
                    "b/e/g",
                    "b/e/h/i",
                    "b/e/h/j",
                    "b/e/h/k/l",
                    "b/e/h/k/m",
                    };
        for (String key : keys) {
            FileUtils.copyFile(tempFile, new File(directory, key));
        }

        String directoryPrefix = "virtualDir/singleFile";
        MultipleFileUpload upload = tm.uploadDirectory(bucketName, directoryPrefix, directory, false);

        long bytesTransferred = 0;
        while ( !upload.isDone() ) {
            long newBytesTransferred = upload.getProgress().getBytesTransferred();
            assertTrue(newBytesTransferred >= bytesTransferred);
            bytesTransferred = newBytesTransferred;
            Thread.sleep(100);
        }

        ObjectListing listObjects = s3.listObjects(bucketName);
        int i = 0;
        for ( S3ObjectSummary summary : listObjects.getObjectSummaries() ) {
            assertEquals(directoryPrefix + "/" + keys[i++], summary.getKey());
            assertEquals((Long) contentLength, (Long) summary.getSize());
        }
        assertEquals((Integer) 1, (Integer) i);

        assertEquals(TransferState.Completed, upload.getState());
        assertEquals((Long) contentLength, (Long) upload.getProgress().getBytesTransferred());
        assertEquals((Long) contentLength, (Long) upload.getProgress().getTotalBytesToTransfer());
        assertTrue(upload.isDone());
    }

    /** Tests that we can abort uploads older than a certain date. */
    @Test
    public void testAbortUpload() throws Exception {
        initializeTransferManager();
        Date beforeUpload = new Date(System.currentTimeMillis() - 10*60*MINUTES);
        String uploadId = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, KEY)).getUploadId();
        Date afterUpload = new Date(System.currentTimeMillis() + 60*MINUTES);

        // Make sure our upload exists
        Thread.sleep(1000);
        assertTrue(doesUploadExist(uploadId));

        // Make sure it isn't aborted by a call with an earlier date
        tm.abortMultipartUploads(bucketName, beforeUpload);
        Thread.sleep(1000);
        assertTrue(doesUploadExist(uploadId));

        // Make sure it's aborted by a call with an later date
        tm.abortMultipartUploads(bucketName, afterUpload);
        Thread.sleep(1000);
        assertFalse(doesUploadExist(uploadId));
    }

    /**
     * Uploads more than one file with size greater than the minimum upload
     * threshold using transfer manager that is initialized with a thread of
     * size 1. The upload of directory must succeed.
     */
    @Test
    public void testMultipartUploadFilesGreaterThanThreads() throws Exception {
        initializeTransferManager(1);

        long contentLength = 25 * MB;
        tempFile = getRandomTempFile(fileName, contentLength);
        String[] keys = new String[] { "a", "b/c", "b/d" };
        for (String key : keys) {
            FileUtils.copyFile(tempFile, new File(directory, key));
        }

        MultipleFileUpload upload = tm.uploadDirectory(bucketName, "", directory, true);

        TestProgressListener testListener = new TestProgressListener();
        upload.addProgressListener(testListener);

        while (TransferState.Waiting == upload.getState())
            Thread.sleep(100);
        assertEquals(TransferState.InProgress, upload.getState());
        // Checks if the list of sub transfers is equal to the number of files being uploaded.
        assertEquals((Integer)upload.getSubTransfers().size(),(Integer)keys.length);
        long bytesTransferred = 0;
        while ( !upload.isDone() ) {
            long newBytesTransferred = upload.getProgress().getBytesTransferred();
            assertTrue(newBytesTransferred >= bytesTransferred);
            bytesTransferred = newBytesTransferred;
            Thread.sleep(100);
        }
        // The number of physical bytes transferred is higher since there are
        // additional payload created in the initiate and complete multi-part
        // upload requests.
        assertTrue(upload.getProgress().getBytesTransferred() <= testListener.totalBytesTransferred);
        assertTrue(upload.getProgress().getTotalBytesToTransfer() <= testListener.totalBytesTransferred);

        ObjectListing listObjects = s3.listObjects(bucketName);
        int i = 0;
        for ( S3ObjectSummary summary : listObjects.getObjectSummaries() ) {
            assertEquals(keys[i++], summary.getKey());
            assertEquals((Long) contentLength, (Long) summary.getSize());
        }
        assertEquals((Integer) keys.length, (Integer) i);

        assertEquals(TransferState.Completed, upload.getState());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getBytesTransferred());
        assertEquals((Long) (keys.length * contentLength), (Long) upload.getProgress().getTotalBytesToTransfer());
        assertTrue(upload.isDone());
    }

    @Override
    public void provideObjectMetadata(File file, ObjectMetadata metadata) {
        metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);

    }

    private void createTestObject() throws Exception {
        createTestObject(DEFAULT_TEST_OBJECT_CONTENT_LENTH);
    }

    private void createTestObject(long testObjectSize) throws Exception {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(testObjectSize);
        Upload upload = tm.upload(bucketName, KEY,new RandomInputStream(testObjectSize) , objectMetadata);
        upload.waitForUploadResult();
    }

    private long folderSize(File directory) {
        long totalLen = 0;
        for (File file : directory.listFiles()) {
            if ( file.isFile() )
                totalLen += file.length();
            else
                totalLen += folderSize(file);
        }
        return totalLen;
    }
}
