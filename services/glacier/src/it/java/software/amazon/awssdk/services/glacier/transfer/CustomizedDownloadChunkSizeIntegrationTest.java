package software.amazon.awssdk.services.glacier.transfer;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.internal.StaticCredentialsProvider;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.test.util.RandomTempFile;

// hchar: last took ~14 hours to run this test
public class CustomizedDownloadChunkSizeIntegrationTest extends GlacierIntegrationTestBase {
    private static final long contentLength = 1024 * 1024 * 3 - 123;
    private static final boolean DEBUG = false;
    private static final boolean cleanup = true;
    private File randomTempFile;
    private File downloadFile;

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("CustomizedDownloadChunkSizeIntegrationTest", contentLength);
        downloadFile = new File(randomTempFile.getParentFile(),
                randomTempFile.getName() + ".download");
        
    }

    @After
    public void teanDown() {
        System.getProperties().remove("software.amazon.awssdk.services.glacier.transfer.downloadChunkSizeInMB");
        if (cleanup) {
            randomTempFile.delete();
            downloadFile.delete();
        }
    }

    @Test
    public void testGlacierUtilsWithCustomizedDownloadChunkSize() throws Exception {

        initializeClient();
        ArchiveTransferManager glacierUtils = newArchiveTransferManager();

        // Upload
        UploadResult uploadResult = glacierUtils.upload(vaultName, "archiveDescription", randomTempFile);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);

        // Download
        if (DEBUG)
            System.out.println("1) downloadFile=" + downloadFile + ", downloadFile.length()=" + downloadFile.length());

        // Bad chunk size, not power of 2
        System.setProperty("software.amazon.awssdk.services.glacier.transfer.downloadChunkSizeInMB", "13");
        try {
            glacierUtils.download(vaultName, archiveId, downloadFile);
        } catch (AmazonClientException e) {
            assertNotNull(e.getMessage());
        }
        if (DEBUG)
            System.out.println("2) downloadFile=" + downloadFile + ", downloadFile.length()=" + downloadFile.length());

        // Customized chunk size 1 MB
        System.setProperty("software.amazon.awssdk.services.glacier.transfer.downloadChunkSizeInMB", "1");

        int retry = 0;
        for (;;) {
            try {
                glacierUtils.download(vaultName, archiveId, downloadFile);
                if (DEBUG)
                    System.out.println("4) downloadFile=" + downloadFile + ", downloadFile.length()=" + downloadFile.length());
                break;
            } catch(QueueDoesNotExistException ex) {
                if (retry++ >= 3)
                    throw ex;
                Thread.sleep(1000);
                System.out.println("Retrying download: " + retry + "\n"
                        + " downloadFile=" + downloadFile
                        + ", downloadFile.length()=" + downloadFile.length());
            }
        }
        assertFileEqualsFile(randomTempFile, downloadFile);
    }

    private ArchiveTransferManager newArchiveTransferManager() {
        return new ArchiveTransferManager(glacier, new StaticCredentialsProvider(credentials), new ClientConfiguration());
    }
}
