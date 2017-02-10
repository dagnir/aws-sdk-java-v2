package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.newBouncyCastleProvider;

import java.security.Security;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CryptoRuntimeNegativeTest {
    @BeforeClass
    public static void touchBouncyCastle() {
        Security.removeProvider("BC");
        // Only necessary in unit test when the same class loader is used across
        // multiple unit tests, like during brazil-build.
        CryptoRuntime.recheck();
    }

    @AfterClass
    public static void after() throws Exception {
        CryptoRuntime.enableBouncyCastle();
        assertTrue(Security.addProvider(newBouncyCastleProvider()) == -1);
        // Only necessary in unit test when the same class loader is used across
        // multiple unit tests, like during brazil-build.
        CryptoRuntime.recheck();
        assertTrue(CryptoRuntime.isBouncyCastleAvailable());
        assertTrue(CryptoRuntime.isAesGcmAvailable());
    }

    @Test
    public void isAesGcmAvailable() {
        assertFalse(CryptoRuntime.isBouncyCastleAvailable());
        assertFalse(CryptoRuntime.isAesGcmAvailable());
        assertFalse(CryptoRuntime.isAesGcmAvailable());
    }

    @Test
    public void isRsaKeyWrappingAvailable() {
        assertFalse(CryptoRuntime.isBouncyCastleAvailable());
        assertFalse(CryptoRuntime.isRsaKeyWrapAvailable());
        assertFalse(CryptoRuntime.isRsaKeyWrapAvailable());
    }
}
