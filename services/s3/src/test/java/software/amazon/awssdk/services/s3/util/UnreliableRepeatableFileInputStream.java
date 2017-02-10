package software.amazon.awssdk.services.s3.util;

import java.io.File;
import java.io.IOException;
import software.amazon.awssdk.runtime.io.ResettableInputStream;
import software.amazon.awssdk.util.FakeIOException;

/**
 * Subclass of RepeatableFileInputStream that throws an error during the first
 * read through the data, but allows RepeatableFileInputStream to reset the file
 * back to the beginning to read all the data successfully the next time.
 *
 * Used for triggering retry functionality in tests to verify that different types
 * of requests can be successfully retried after errors.
 */
public class UnreliableRepeatableFileInputStream extends ResettableInputStream {
    /**
     * Max number of errors that can be triggered.
     */
    private int maxNumberOfErrors = 1;
    /**
     * Current number of errors that have been triggered.
     */
    private int currNumberOfErrors;
    private int position;
    private final boolean isFakeIOException;

    public UnreliableRepeatableFileInputStream(File file) throws IOException {
        super(file);
        this.isFakeIOException = false;
    }

    public UnreliableRepeatableFileInputStream(File file, boolean isFaileIOException) throws IOException {
        super(file);
        this.isFakeIOException = isFaileIOException;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read != -1) position++;
        triggerError();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        triggerError();
        int read = super.read(b, off, len);
        position += read;
        triggerError();
        return read;
    }

    private void triggerError() throws IOException {
        if (currNumberOfErrors >= maxNumberOfErrors) return;

        if (position >= 100) {
            currNumberOfErrors++;
            if (isFakeIOException)
                throw new FakeIOException("Faked IO error " + currNumberOfErrors
                        + " on UnreliableFileInputStream");
            else
                throw new IOException("Injected IO error " + currNumberOfErrors
                        + " on UnreliableFileInputStream");
        }
    }

    public int getNumberOfErrors() {
        return maxNumberOfErrors;
    }

    public void setNumberOfErrors(int numberOfErrors) {
        this.maxNumberOfErrors = numberOfErrors;
    }

    public UnreliableRepeatableFileInputStream withNumberOfErrors(int numberOfErrors) {
        this.maxNumberOfErrors = numberOfErrors;
        return this; 
    }

    public int getCurrNumberOfErrors() {
        return currNumberOfErrors;
    }
}