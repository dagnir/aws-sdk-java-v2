package software.amazon.awssdk.services.s3.internal.crypto;

import java.io.InputStream;
import javax.crypto.Cipher;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.test.util.ConstantInputStream;

public class GCMIntegrityTest {

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test(expected=SecurityException.class)
    public void negative() throws Exception {
        InputStream is = new CipherLiteInputStream(new ConstantInputStream(100,
                (byte) 'F'), CryptoTestUtils.createTestCipherLite(
                Cipher.DECRYPT_MODE, ContentCryptoScheme.AES_GCM));
        IOUtils.toString(is);
    }

}
