package software.amazon.awssdk.services.glacier.transfer;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.test.util.RandomTempFile;

// hchar: last took ~4 hours to run
public class NoAccountIdIntegrationTest extends GlacierIntegrationTestBase {
    private static final long contentLength = 1024 * 1024 * 100 + 123;
    private static final boolean cleanup = true;
    private File randomTempFile;
    private File downloadFile;

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("NoAccountIdIntegrationTest-", contentLength);
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
     * archives without an account ID specified.
     */
    @Test
    public void testGlacierUtilsWithNoAccountId() throws Exception {
        initializeClient();
        ArchiveTransferManager glacierUtils = newArchiveTransferManager();

        // Upload
        UploadResult uploadResult = glacierUtils.upload(vaultName, "archiveDescription", randomTempFile);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);

        int retries=0;
        // Download
        for (;;) {
            try {
                glacierUtils.download(vaultName, archiveId, downloadFile);
                break;
            } catch(QueueDoesNotExistException ex) {
                ex.printStackTrace();
                if (retries++ > 3)
                    break;
            }
        }
        assertFileEqualsFile(randomTempFile, downloadFile);
    }
}
