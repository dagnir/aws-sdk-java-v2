package software.amazon.awssdk.services.s3.transfer;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.runtime.io.SdkFilterInputStream;

/**
 * Simple InputStream wrapper for tests that throws an IOException after a
 * certain amount of data has been read.
 */
class CorruptionInputStream extends SdkFilterInputStream {

    /** Flag indicating whether this InputStream has thrown an exception yet. */
    private boolean thrownException = false;

    /** Tracks the current position in the InputStream's data. */
    private long bytesRead;

    /** The point in the data at which an IOException is to be thrown. */
    private long corruptedDataMark;

    public CorruptionInputStream(InputStream in) {
        super(in);
    }

    public void setCorruptedDataMark(long corruptedDataMark) {
        this.corruptedDataMark = corruptedDataMark;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!thrownException && bytesRead >= corruptedDataMark) {
            thrownException = true;
            throw new IOException("Data Corruption from CorruptionInputStream");
        }

        bytesRead += len;
        return super.read(b, off, len);
    }
}