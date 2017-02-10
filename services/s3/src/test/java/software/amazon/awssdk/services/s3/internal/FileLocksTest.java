package software.amazon.awssdk.services.s3.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.UUID;
import org.junit.Test;

public class FileLocksTest {

    @Test
    public void test() {
        File file = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        assertFalse(file.exists());
        file.deleteOnExit();

        FileLocks.lock(file);
        assertTrue(file.exists());

        assertTrue(FileLocks.isFileLocked(file));

        FileLocks.unlock(file);
        assertFalse(FileLocks.isFileLocked(file));

        FileLocks.lock(file);
        assertTrue(FileLocks.isFileLocked(file));
        FileLocks.unlock(file);
    }
}
