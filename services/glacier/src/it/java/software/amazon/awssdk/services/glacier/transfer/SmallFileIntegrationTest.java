package software.amazon.awssdk.services.glacier.transfer;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.internal.StaticCredentialsProvider;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

// hchar: took ~4.5 to run
public class SmallFileIntegrationTest extends GlacierIntegrationTestBase {
    private static final long contentLength = 1024 * 1024 - 123;
    private static final boolean cleanup = true;
    private File randomTempFile;
    private File downloadFile;

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("SmallFileIntegrationTest-", contentLength, true);
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

    /**
     * Tests that the GlacierUtils class can correctly upload and download
     * binary archives using the single part upload process.
     */
    @Test
    public void testGlacierUtilsWithSmallBinaryFile() throws Exception {
        initializeClient();
        ArchiveTransferManager archiveTx = newArchiveTransferManager();

        // Upload
        UploadResult uploadResult = archiveTx.upload(accountId, vaultName, "archive-description", randomTempFile);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);

        // Download
        downloadFile.createNewFile();
        archiveTx.download(accountId, vaultName, archiveId, downloadFile);
        assertFileEqualsFile(randomTempFile, downloadFile);
    }
}
