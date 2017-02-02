package software.amazon.awssdk.services.s3.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import software.amazon.awssdk.test.util.RandomTempFile;

public class UnreliableRepeatableFileInputStreamIntegrationTest {
    private static final int TEMP_FILE_SIZE = 1024*1024*8+2345;

    /**
     * Tests that our unreliable repeatable file input stream throws an
     * exception, but can be reset back to the beginning.
     */
    @Test
    public void testUnreliableFileInputStream() throws Exception {
        RandomTempFile randomTempFile = new RandomTempFile("s3-encryption-error-recovery", TEMP_FILE_SIZE);
        UnreliableRepeatableFileInputStream inputStream = new UnreliableRepeatableFileInputStream(randomTempFile);
        inputStream.mark(-1);

        byte[] buffer = new byte[1024];
        int lastBytesRead = 0;
        boolean hasTriggeredError = false;

        while (lastBytesRead >= 0) {
            try {
                lastBytesRead = inputStream.read(buffer);
            } catch (Exception e) {
                hasTriggeredError = true;
                inputStream.reset();
            }
        }

        assertTrue(hasTriggeredError);
        inputStream.close();
    }
}