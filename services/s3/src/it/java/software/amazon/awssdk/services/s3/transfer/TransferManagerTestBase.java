package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.test.util.RandomTempFile;

/** Integration tests for TransferManager. */
public class TransferManagerTestBase extends S3IntegrationTestBase {

    protected static final int MINUTES = 1000;
    protected static final String MULTIPART_UPLOAD_ETAG_PATTERN = "^[0-9a-zA-Z]*-\\d*$";
    protected static final String PUT_OBJECT_ETAG_PATTERN = "^[0-9a-zA-Z]*$";

    /**
     * Name of the bucket in Amazon S3 used for testing.
     */
    protected String bucketName = "java-sdk-tx-man-"
            + System.currentTimeMillis();

    /**
     * Name of the Amazon S3 object used for testing.
     */
    protected static final String KEY = "key";

    /**
     * Reference to the Transfer manager instance used for testing.
     */
    protected TransferManager tm;

    /**
     * References to the temporary file being used for testing purposes.
     */
    protected static File tempFile;

    /**
     * Name of the temporary file being used in the testing.
     */
    protected static final String fileName = "java-sdk-transfer-test-"
            + System.currentTimeMillis();

    /**
     * Reference to the directory used for testing.
     */
    protected static File directory;

    /**
     * Name of the directory used in the testing.
     */
    protected static final String directoryName = "java-sdk-transfer-man-directory-"
            + System.currentTimeMillis();

    /**
     * Default thread pool size to be used by the transfer manager.
     */
    protected static final int DEFAULT_THREAD_POOL_SIZE = 50;

    /**
     * Default upload threshold for multi part uploads.
     */
    protected static final long DEFAULT_MULTIPART_UPLOAD_THRESHOLD = 20 * MB;

    /**
     * Default part size for multi part uploads.
     */
    protected static final long DEFAULT_MULTIPART_UPLOAD_PART_SIZE = 10 * MB;

    /**
     * Default copy threshold for multi part copies.
     */
    protected static final long DEFAULT_MULTIPART_COPY_THRESHOLD = 20 * MB;

    /**
     * Sets up the Amazon S3 client, Creates an Amazon S3 bucket and also
     * creates a temporary directory for the test case.
     */
    @Before
    public void initializeS3Client() throws Exception {
        S3IntegrationTestBase.setUp();
        s3.createBucket(bucketName);
        createTempDirectory();
    }

    /** Releases all resources created by tests. */
    @After
    public void tearDown() {
        try {
            deleteBucketAndAllContents(bucketName);
        } catch (Exception e) {
        }
        tm.shutdownNow(false);

        if (tempFile != null) {
            if (tempFile.exists()) {
                assertTrue(tempFile.delete());
            }
        }
        if (directory != null) {
            deleteDirectoryAndItsContents(directory);
        }
    }

    /**
     * Returns true if an multi part upload exists with the given upload id else
     * returns false.
     */
    protected boolean doesUploadExist(String uploadId) {
        List<MultipartUpload> uploads = s3.listMultipartUploads(
                new ListMultipartUploadsRequest(bucketName))
                .getMultipartUploads();
        for (MultipartUpload upload : uploads) {
            if (upload.getUploadId().equals(uploadId))
                return true;
        }

        return false;
    }

    /**
     * Initializes a transfer manager with default thread pool size
     */
    protected void initializeTransferManager() {
        initializeTransferManager(DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Initializes the transfer manager with the given thread pool size and
     * default configuration.
     */
    protected void initializeTransferManager(int threadPoolSize) {
        initializeTransferManager(threadPoolSize,
                DEFAULT_MULTIPART_UPLOAD_PART_SIZE,
                DEFAULT_MULTIPART_UPLOAD_THRESHOLD,
                DEFAULT_MULTIPART_COPY_THRESHOLD);
    }

    /**
     * Initializes the transfer manager with the given thread pool size and
     * configuration.
     */
    protected void initializeTransferManager(int threadPoolSize,
            long uploadPartSize, long uploadThreshold, long copyThreshold) {
        tm = new TransferManager(s3,
                (ThreadPoolExecutor) Executors
                        .newFixedThreadPool(threadPoolSize));
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMinimumUploadPartSize(uploadPartSize);
        configuration.setMultipartUploadThreshold(uploadThreshold);
        configuration.setMultipartCopyThreshold(copyThreshold);
        tm.setConfiguration(configuration);
    }

    /**
     * Generates a secret key to used for testing purposes.
     */
    protected static SecretKey generateSecretKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256, new SecureRandom());
            return generator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate key", e);
        }
    }

    /**
     * Asserts that the given ETag satisfies the multi part ETag pattern.
     */
    protected void assertMultipartETag(String eTag) {
        assertNotNull(eTag);
        assertTrue("Not a multipart ETag: " + eTag,
                eTag.matches(MULTIPART_UPLOAD_ETAG_PATTERN));
    }

    /**
     * Asserts that the given ETag satisfies the single put object upload ETag
     * pattern.
     */
    protected void assertSinglePartETag(String eTag) {
        assertNotNull(eTag);
        assertTrue("Not a single part ETag: " + eTag,
                eTag.matches(PUT_OBJECT_ETAG_PATTERN));
    }

    /**
     * Uploads a input stream with random data of given length to the Amazon S3.
     */
    protected Upload uploadRandomInputStream(long contentLength,
            ObjectMetadata metadata) {
        return tm.upload(bucketName, KEY, new RandomInputStream(contentLength),
                metadata);
    }

    /**
     * Creates a temp file and fills it with random data.
     */
    protected void createTempFile(long contentLength) throws IOException {
        tempFile = new RandomTempFile(fileName, contentLength);
    }

    /**
     * Creates a new directory in the System's temporary folder.
     */
    protected void createTempDirectory() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        directory = new File(baseDir, directoryName);
        if (!directory.exists()) {
            assertTrue(directory.mkdir());
        }
    }

    /**
     * Deletes a directory and its contents.
     */
    protected void deleteDirectoryAndItsContents(File dir) {
        if (!dir.exists())
            return;
        if (!dir.isDirectory()) {
            assertTrue(dir.delete());
            return;
        }

        String[] childs = dir.list();
        for (String child : childs) {
            File fileToDelete = new File(dir.getAbsolutePath(), child);
            deleteDirectoryAndItsContents(fileToDelete);
        }

        if (dir.list().length == 0)
            assertTrue(dir.delete());
    }

    /**
     * Checks if the contents of the directory matches the given keys.
     */
    protected void assertKeysToDirectory(File directory, String[] keys,
            long contentLength) {

        @SuppressWarnings("rawtypes")
        Iterator iter = FileUtils.iterateFiles(directory, null, true);

        Set<String> setAbsolutePath = new HashSet<String>();
        Set<String> setKeys = new HashSet<String>(Arrays.asList(keys));

        while (iter.hasNext()) {
            File f = (File) iter.next();
            assertEquals((Long) contentLength, (Long) f.length());
            setAbsolutePath.add(f.getAbsolutePath()
                    .substring(directory.getAbsolutePath().length() + 1)
                    .replaceAll("\\\\", "/"));
        }
        assertEquals(setKeys, setAbsolutePath);

    }

}
