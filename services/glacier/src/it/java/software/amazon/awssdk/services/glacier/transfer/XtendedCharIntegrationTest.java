package software.amazon.awssdk.services.glacier.transfer;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.internal.StaticCredentialsProvider;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

// hchar: took ~4.5 hours to run
public class XtendedCharIntegrationTest extends GlacierIntegrationTestBase {
    private File tempFile;
    private File downloadFile;

    @Before
    public void setup() throws IOException {
        tempFile = File.createTempFile("XtendedCharIntegrationTest", "bar");
        downloadFile = new File(tempFile.getParentFile(), ".download");
    }
    
    @After
    public void after() {
        tempFile.delete();
        downloadFile.delete();
    }

    @Test
    public void testGlacierUtilsWithExtendedCharacters() throws Exception {
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        outputStream.write(("\u00E4\u00E9\u00E4\u00E9\u00E4\u00E9\u00E4\u00E9\u00E4\u00E9" +
                            "\u00E4\u00E9\u00E4\u00E9\u00E4\u00E9\u00E4\u00E9\u00E4\u00E9").getBytes("UTF-8"));
        outputStream.close();

        initializeClient();
        ArchiveTransferManager archiveTx = newArchiveTransferManager();

        // Upload
        UploadResult uploadResult = archiveTx.upload(vaultName, "archiveDescription", tempFile);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);

        int retry = 0; 
        // Download
        try {
            archiveTx.download(vaultName, archiveId, downloadFile);
        } catch(QueueDoesNotExistException ex) {
            if (retry++ >= 3)
                throw ex;
            ex.printStackTrace(System.err);
            System.out.println("Retrying: " + retry);
        }
        assertFileEqualsFile(tempFile, downloadFile);
    }

    private ArchiveTransferManager newArchiveTransferManager() {
        return new ArchiveTransferManager(glacier, new StaticCredentialsProvider(credentials), new ClientConfiguration());
    }
}
