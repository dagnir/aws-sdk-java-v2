package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_CTR;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_GCM;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.encodeHexString;
import static software.amazon.awssdk.util.StringUtils.UTF8;

import javax.crypto.Cipher;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.util.StringUtils;

public class EncryptTest {
    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test
    public void testCTREncryptWithOffset() throws Exception {
        String plaintext = "1234567890123456ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        byte[] pt = plaintext.getBytes(UTF8);
        String expectedCipherText = StringUtils.upperCase("ff95730978565c563e7ef4e189c7a82e3322408e72e06d3c98e8bec238487ade8c18e27c181cb62318b23846246853912029a28bead7125e0e0c6c91d8784f69a7bcf609cd20e17b219ad1a3c4d384e4f7d12d75");
        // Encrypt with GCM
        CipherLite gcm = CryptoTestUtils.createTestCipher(AES_GCM,
                AES_GCM.getIVLengthInBytes(), Cipher.ENCRYPT_MODE);
        byte[] ct_ctr = gcm.doFinal(pt);
        String ct_ctr_str = encodeHexString(ct_ctr);
        System.err.println("ct_ctr_str : " + ct_ctr_str);
        assertEquals(expectedCipherText, ct_ctr_str);
        {
            // 1st 16 bytes
            CipherLite ctr1 = CryptoTestUtils.createTestCipherWithStartingBytePos(AES_CTR,
                    AES_GCM.getIVLengthInBytes(), Cipher.ENCRYPT_MODE, 0);
    ////        Cipher cipher_ctr_with_offset = AES_GCM.createAuxillaryCipher(cek, iv, cipherMode, securityProvider, startingBytePos)
            byte[] ba1 = ctr1.doFinal(pt, 0, 16);
            String str1 = encodeHexString(ba1);
            System.err.println("ct_ctr_str1: " + str1);
            assertEquals(ct_ctr_str.substring(0, 16*2), str1);
        }
        {
            // 2nd 16 bytes
            CipherLite ctr2 = CryptoTestUtils.createTestCipherWithStartingBytePos(AES_CTR,
                    AES_GCM.getIVLengthInBytes(), Cipher.ENCRYPT_MODE, 16);
            byte[] ba2 = ctr2.doFinal(pt, 16, 16);
            String str2 = encodeHexString(ba2);
            System.err.println("ct_ctr_str2: " + str2);
            assertEquals(ct_ctr_str.substring(16*2, 16*4), str2);
        }
        {
            // 2nd 32 bytes
            CipherLite ctr2a = CryptoTestUtils.createTestCipherWithStartingBytePos(AES_CTR,
                    AES_GCM.getIVLengthInBytes(), Cipher.ENCRYPT_MODE, 16);
            byte[] ba2a = ctr2a.doFinal(pt, 16, 32);
            String str2a = encodeHexString(ba2a);
            System.err.println("      str2a: " + str2a);
            assertEquals(ct_ctr_str.substring(16*2, 16*6), str2a);
        }

        {
            // 2nd 16 bytes
            CipherLite ctr2 = gcm.createAuxiliary(16);
            byte[] ba2 = ctr2.update(pt, 16, 20);
            String str2 = encodeHexString(ba2);
            System.err.println("ct_ctr_str2: " + str2);
            assertEquals(ct_ctr_str.substring(16*2, 16*4), str2);
        }

        {
            // 2nd 32 bytes
            CipherLite ctr2a = gcm.createAuxiliary(16);
            byte[] ba2a = ctr2a.update(pt, 16, 40);
            String str2a = encodeHexString(ba2a);
            System.err.println("      str2a: " + str2a);
            assertEquals(ct_ctr_str.substring(16*2, 16*6), str2a);
        }
        {
            CipherLite gcm2 = CryptoTestUtils.createTestCipher(AES_GCM,
                    AES_GCM.getIVLengthInBytes(), Cipher.ENCRYPT_MODE);
            gcm2.mark();
            byte[] ba = gcm2.update(pt, 0, 16);
            String hex  = encodeHexString(ba);
            System.err.println("  hex[0-15]: " + hex);
            assertEquals(ct_ctr_str.substring(0, 32), hex);

            gcm2.reset();
            ba = gcm2.update(pt, 0, 16);
            hex  = encodeHexString(ba);
            System.err.println("  hex[0-15]: " + hex);
            assertEquals(ct_ctr_str.substring(0, 32), hex);

            // next 16-31 bytes
            int len = 16;
            gcm2.mark();
            ba = gcm2.update(pt, 16, len);
            hex  = encodeHexString(ba);
            System.err.println(" hex[16-31]: " + hex);
            assertEquals(ct_ctr_str.substring(32, 32+len*2), hex);

            gcm2.reset();
            ba = gcm2.update(pt, 16, len);
            hex  = encodeHexString(ba);
            System.err.println(" hex[16-31]: " + hex);
            assertEquals(ct_ctr_str.substring(32, 32+len*2), hex);

            // next 32-47 bytes
            gcm2.mark();
            ba = gcm2.update(pt, 32, len);
            hex  = encodeHexString(ba);
            System.err.println(" hex[32-47]: " + hex);
            assertEquals(ct_ctr_str.substring(64, 64+len*2), hex);

            gcm2.reset();
            ba = gcm2.update(pt, 32, len);
            hex  = encodeHexString(ba);
            System.err.println(" hex[32-47]: " + hex);
            assertEquals(ct_ctr_str.substring(64, 64+len*2), hex);
        }
    }
}
