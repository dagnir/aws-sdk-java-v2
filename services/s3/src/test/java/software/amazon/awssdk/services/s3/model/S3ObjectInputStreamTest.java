package software.amazon.awssdk.services.s3.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import software.amazon.awssdk.util.IOUtils;

public class S3ObjectInputStreamTest {

    /**
     * Mock {@link S3ObjectInputStream} that overrides abort to call close.
     */
    private final class MockS3ObjectInputStream extends S3ObjectInputStream {
        private MockS3ObjectInputStream(InputStream in) {
            super(in, null);
        }

        @Override
        public void abort() {
            try {
                close();
            } catch (IOException e) {
                fail();
            }
        }
    }

    /**
     * Input stream that can be closed
     */
    private final class MockClosableInputStream extends InputStream {

        private boolean isClosed = false;

        @Override
        public int read() throws IOException {
            if (isClosed) {
                return -1;
            }
            return 1;
        }

        @Override
        public void close() throws IOException {
            isClosed = true;
        }
    }

    /**
     * Previously the {@link S3ObjectInputStream#close()} method would call
     * {@link S3ObjectInputStream#abort()}. If abort was overriden to call close then it would
     * result in a stack overflow exception as they continue to invoke one another.
     */
    @Test
    public void overrideAbortToClose_DoesNotCauseStackOverflow() throws Exception {
        S3ObjectInputStream mockInputStream = new MockS3ObjectInputStream(new MockClosableInputStream());

        // Abort should call close
        assertEquals(1, mockInputStream.read());
        mockInputStream.abort();
        assertEquals(-1, mockInputStream.read());

        IOUtils.closeQuietly(mockInputStream, null);
    }
}
