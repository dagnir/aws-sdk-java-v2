package software.amazon.awssdk.services.glacier.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

public class LargeFileWithListenerIntegrationTest extends GlacierIntegrationTestBase {
    private static final long contentLength = 1024L * 1024L * 100 + 123;
    private static final boolean cleanup = true;
    private File randomTempFile;
    private File downloadFile;
    private TestGlacierProgressListener progressListener = new TestGlacierProgressListener();

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("LargeFileWithListenerIntegrationTest", contentLength, true);
        assertEquals(contentLength, randomTempFile.length());
        downloadFile = new File(randomTempFile.getParentFile(),
                                randomTempFile.getName() + ".download");
    }

    @After
    public void teanDown() {
        if (cleanup) {
            randomTempFile.delete();
            downloadFile.delete();
        }
    }

    private ArchiveTransferManager newArchiveTransferManager() {
        return new ArchiveTransferManager(glacier, new StaticCredentialsProvider(credentials), new ClientConfiguration());
    }

    // hchar: this test took about 4 hours to finish last time

    /**
     * Tests that the progress listener can work correctly when doing multipart upload and download.
     */
    @Test
    public void testLargeBinaryFileWithProgressListener() throws Exception {
        progressListener.reset();
        initializeClient();
        ArchiveTransferManager manager = newArchiveTransferManager();
        // Upload
        UploadResult uploadResult = manager.upload(accountId, vaultName,
                                                   "archiveDescription", randomTempFile, progressListener);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);
        assertEquals(contentLength, progressListener.getTotalRequestBytesTransferred());
        assertTrue(progressListener.transferStarted);
        assertTrue(progressListener.transferCompleted);
        progressListener.reset();

        initializeUnreliableClient(true);
        manager = newArchiveTransferManager();

        // Download
        manager.download(accountId, vaultName, archiveId, downloadFile, progressListener);
        assertFileEqualsFile(randomTempFile, downloadFile);

        assertTrue(progressListener.transferPreparing);
        assertEquals(contentLength, progressListener.getTotalResponseBytesTransferred());
        assertTrue(progressListener.transferStarted);
        assertTrue(progressListener.transferCompleted);
    }

    // Create an unreliable glacier client, you can denote whether it is recoverable or not.
    private void initializeUnreliableClient(boolean recoverable) throws FileNotFoundException, IOException {
        setUpCredentials();
        glacier = new UnreliableGlaicerClient(credentials, recoverable);
    }
}
