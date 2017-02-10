package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class S3CryptoModuleBaseTest {
    @Test
    public void getAdjustedCryptoRange_Invalid() {
        long[] range = { 1, 0 };
        long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
        assertNull(result);
    }

    @Test
    public void getAdjustedCryptoRange_MaxLong() {
        long[] range = { 0, Long.MAX_VALUE };
        long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
        assertTrue(0 == result[0]);
        assertTrue(String.valueOf(result[1]), Long.MAX_VALUE == result[1]);
    }

    @Test
    public void getAdjustedCryptoRange_0To15() {
        for (int i = 0; i < 16; i++) {
            long[] range = { 0, i };
            long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
            assertTrue(0 == result[0]);
            assertTrue(String.valueOf(result[1]), 32 == result[1]);
        }
    }

    @Test
    public void getAdjustedCryptoRange_1To15() {
        for (int i = 0; i < 16; i++) {
            long[] range = { i, i };
            long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
            assertTrue(0 == result[0]);
            assertTrue(String.valueOf(result[1]), 32 == result[1]);
        }
    }

    @Test
    public void getAdjustedCryptoRange_16() {
        long[] range = { 0, 16 };
        long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
        assertTrue(0 == result[0]);
        assertTrue(String.valueOf(result[1]), 48 == result[1]);
    }
}
