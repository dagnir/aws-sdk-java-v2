package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.ASCII_HIGH;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.ASCII_LOW;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class CryptoTestUtilsTest {
    @Test
    public void generateRandomAsciiFile() throws IOException {
        int size = 1000;
        File f = CryptoTestUtils.generateRandomAsciiFile(size);
        assertTrue("Unexpected file size: " + f.length(), f.length() == size);
        byte[] bytes = FileUtils.readFileToByteArray(f);
        for (byte b : bytes) {
            if (b < ASCII_LOW && b != '\n' || b > ASCII_HIGH)
                fail("Unexpected byte " + b);
        }
    }
}
