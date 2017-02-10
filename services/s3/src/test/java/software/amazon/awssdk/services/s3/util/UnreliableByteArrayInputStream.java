package software.amazon.awssdk.services.s3.util;

import software.amazon.awssdk.util.FakeIOException;

public class UnreliableByteArrayInputStream extends TestByteArrayInputStream {
    /**
     * Max number of errors that can be triggered.
     */
    private int maxNumberOfErrors = 1;
    /**
     * Current number of errors that have been triggered.
     */
    private int currNumberOfErrors;
    private int position;
    // True to throw a FakeIOException; false to throw a RuntimeException
    private final boolean isFakeIOException;

    public UnreliableByteArrayInputStream(byte buf[]) {
        super(buf);
        this.isFakeIOException = false;
    }

    public UnreliableByteArrayInputStream(byte buf[], boolean isFakeIOException) {
        super(buf);
        this.isFakeIOException = isFakeIOException;
    }

    @Override
    public int read() throws FakeIOException {
        int read = super.read();
        if (read != -1) position++;
        triggerError();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws FakeIOException {
        triggerError();
        int read = super.read(b, off, len);
        position += read;
        triggerError();
        return read;
    }

    private void triggerError() throws FakeIOException {
        if (currNumberOfErrors >= maxNumberOfErrors) return;

        if (position >= 100) {
            currNumberOfErrors++;
            if (isFakeIOException)
                throw new FakeIOException("Fake IO error " + currNumberOfErrors
                    + " on UnreliableFileInputStream");
            else
                throw new RuntimeException("Injected runtime error " + currNumberOfErrors
                        + " on UnreliableFileInputStream");
        }
    }

    public int getNumberOfErrors() {
        return maxNumberOfErrors;
    }

    public void setNumberOfErrors(int numberOfErrors) {
        this.maxNumberOfErrors = numberOfErrors;
    }

    public UnreliableByteArrayInputStream withNumberOfErrors(int numberOfErrors) {
        this.maxNumberOfErrors = numberOfErrors;
        return this; 
    }

    public int getCurrNumberOfErrors() {
        return currNumberOfErrors;
    }
}