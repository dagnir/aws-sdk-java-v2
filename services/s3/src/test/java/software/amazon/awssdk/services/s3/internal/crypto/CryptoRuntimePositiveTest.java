package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.newBouncyCastleProvider;

import java.security.Security;
import org.junit.BeforeClass;
import org.junit.Test;

public class CryptoRuntimePositiveTest {

    @BeforeClass
    public static void touchBouncyCastle() throws Exception {
        CryptoRuntime.enableBouncyCastle();
        assertTrue(Security.addProvider(newBouncyCastleProvider()) == -1);
        // Only necessary in unit test when the same class loader is used across
        // multiple unit tests, like during brazil-build.
        CryptoRuntime.recheck();
    }

    @Test
    public void isAesGcmAvailable() {
        assertTrue(CryptoRuntime.isBouncyCastleAvailable());
        assertTrue(CryptoRuntime.isAesGcmAvailable());
        assertTrue(CryptoRuntime.isAesGcmAvailable());
    }

    @Test
    public void isRsaKeyWrappingAvailable() {
        assertTrue(CryptoRuntime.isBouncyCastleAvailable());
        assertTrue(CryptoRuntime.isRsaKeyWrapAvailable());
        assertTrue(CryptoRuntime.isRsaKeyWrapAvailable());
    }
}
