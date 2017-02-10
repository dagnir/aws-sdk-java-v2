package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.util.StringUtils.UTF8;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.runtime.io.ResettableInputStream;

public class GCMCipherLiteTest {
    private static final boolean debug = false;

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    /**
     * single doFinal
     */
    @Test
    public void singleDoFinal() throws Exception {
        // Encryption
        CipherLite encrypter = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        assertTrue(encrypter instanceof GCMCipherLite);
        File f = CryptoTestUtils.generateRandomAsciiFile(100);
        String ptstr = IOUtils.toString(new FileInputStream(f));
        System.err.println(ptstr);
        GCMCipherLite e = (GCMCipherLite) encrypter;
        byte[] pt = ptstr.getBytes(UTF8);
        byte[] ct = e.doFinal(pt);
        byte[] tag = e.getTag();
        assertNotNull(tag);
        assertTrue(tag.length == 16);
        assertTrue(0 == e.getMarkedCount());
        assertTrue(0 == e.getCurrentCount());
        assertTrue("encrypted outputByteCount=" + e.getOutputByteCount(),
                100 == e.getOutputByteCount());
        e.reset();
        byte[] ct2 = e.doFinal(pt);
        assertNotSame(ct, ct2);
        assertTrue(Arrays.equals(ct, ct2));
        byte[] tag2 = e.getTag();
        assertTrue(Arrays.equals(tag, tag2));
        // Decryption
        CipherLite decrypter = encrypter.createInverse();
        assertTrue(decrypter instanceof GCMCipherLite);
        GCMCipherLite d = (GCMCipherLite) decrypter;
        byte[] dpt = d.doFinal(ct);
        assertNull(d.getTag());
        assertTrue(0 == d.getMarkedCount());
        assertTrue(0 == d.getCurrentCount());
        assertTrue("decrypted outputByteCount=" + e.getOutputByteCount(),
                100 == e.getOutputByteCount());
        e.reset();
        byte[] dpt2 = d.doFinal(ct);
        assertNotSame(dpt2, dpt);
        assertTrue(Arrays.equals(dpt2, dpt));
        assertNull(d.getTag());
    }

    /**
     * update-doFinal
     */
    @Test
    public void updateFinal() throws Exception {
        // Encryption
        CipherLite encrypter = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        File f = CryptoTestUtils.generateRandomAsciiFile(100);
        String ptstr = IOUtils.toString(new FileInputStream(f));
        System.err.println(ptstr);
        GCMCipherLite e = (GCMCipherLite) encrypter;
        byte[] pt = ptstr.getBytes(UTF8);
        byte[] ct = e.update(pt, 0, pt.length);
        assertTrue("encrypted outputByteCount=" + e.getOutputByteCount(),
                96 == e.getOutputByteCount());
        byte[] final0 = e.doFinal();
        byte[] final1 = e.getFinalBytes();
        assertTrue(Arrays.equals(final0, final1));
        assertTrue(final0.length >= 16);
        assertTrue(0 == e.getMarkedCount());
        assertTrue(0 == e.getCurrentCount());
        assertTrue(100 == e.getOutputByteCount());
        e.reset();
        byte[] ct2 = e.update(pt, 0, pt.length);
        byte[] final2 = e.doFinal();
        assertNotSame(ct, ct2);
        assertTrue(Arrays.equals(ct, ct2));
        byte[] final3 = e.getFinalBytes();
        assertTrue(Arrays.equals(final0, final2));
        assertTrue(Arrays.equals(final0, final3));
        // Decryption
        CipherLite decrypter = encrypter.createInverse();
        GCMCipherLite d = (GCMCipherLite) decrypter;
        byte[] ct_all = Arrays.copyOf(ct, ct.length + final0.length);
        System.arraycopy(final0, 0, ct_all, ct.length, final0.length);
        byte[] dpt = d.update(ct_all, 0, ct_all.length);
        assertTrue("decrypted outputByteCount=" + d.getOutputByteCount(),
                96 == d.getOutputByteCount());
        byte[] dfinal0 = d.doFinal();
        byte[] dfinal1 = d.getFinalBytes();
        assertTrue(Arrays.equals(dfinal0, dfinal1));
        assertTrue(0 == d.getMarkedCount());
        assertTrue(0 == d.getCurrentCount());
        assertTrue(100 == d.getOutputByteCount());
        d.reset();
        byte[] dpt2 = d.update(ct_all, 0, ct_all.length);
        byte[] dfinal2 = d.doFinal();
        assertNotSame(dpt, dpt2);
        assertTrue(Arrays.equals(dpt, dpt2));
        byte[] dfinal3 = d.getFinalBytes();
        assertTrue(Arrays.equals(dfinal0, dfinal2));
        assertTrue(Arrays.equals(dfinal0, dfinal3));

    }

    /**
     * update-update-doFinal
     */
    @Test
    public void update2Final() throws Exception {
        // Encryption
        CipherLite encrypter = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        File f = CryptoTestUtils.generateRandomAsciiFile(100);
        String ptstr = IOUtils.toString(new FileInputStream(f));
        System.err.println(ptstr);
        GCMCipherLite e = (GCMCipherLite) encrypter;
        byte[] pt = ptstr.getBytes(UTF8);
        byte[] ct1 = e.update(pt, 0, pt.length / 2);
        assertTrue("encrypted outputByteCount=" + e.getOutputByteCount(),
                48 == e.getOutputByteCount());
        byte[] ct2 = e.update(pt, pt.length / 2, pt.length - pt.length / 2);
        assertTrue("encrypted outputByteCount=" + e.getOutputByteCount(),
                96 == e.getOutputByteCount());
        byte[] final0 = e.doFinal();
        byte[] final1 = e.getFinalBytes();
        assertTrue(Arrays.equals(final0, final1));
        assertTrue(final0.length >= 16);
        assertTrue(0 == e.getMarkedCount());
        assertTrue(0 == e.getCurrentCount());
        assertTrue(100 == e.getOutputByteCount());
        e.reset();
        byte[] ct3 = e.update(pt, 0, pt.length / 2);
        byte[] ct4 = e.update(pt, pt.length / 2, pt.length - pt.length / 2);
        byte[] final2 = e.doFinal();
        assertNotSame(ct1, ct3);
        assertTrue(Arrays.equals(ct1, ct3));
        assertNotSame(ct2, ct4);
        assertTrue(Arrays.equals(ct2, ct4));
        byte[] final3 = e.getFinalBytes();
        assertTrue(Arrays.equals(final0, final2));
        assertTrue(Arrays.equals(final0, final3));

        // Decryption
        CipherLite decrypter = encrypter.createInverse();
        GCMCipherLite d = (GCMCipherLite) decrypter;
        byte[] ct = decrypter.createInverse().doFinal(pt);
        byte[] dpt1 = d.update(ct, 0, ct.length / 2);
        assertTrue("decrypted outputByteCount=" + d.getOutputByteCount(),
                d.getOutputByteCount() <= ct.length / 2);
        byte[] dpt2 = d.update(ct, ct.length / 2, ct.length - ct.length / 2);
        assertTrue("encrypted outputByteCount=" + d.getOutputByteCount(),
                d.getOutputByteCount() <= 100);
        byte[] dfinal0 = d.doFinal();
        byte[] dfinal1 = d.getFinalBytes();
        byte[] dpt_all = Arrays.copyOf(dpt1, 100);
        System.arraycopy(dpt2, 0, dpt_all, dpt1.length, dpt2.length);
        System.arraycopy(dfinal0, 0, dpt_all, dpt1.length + dpt2.length,
                dfinal0.length);
        assertTrue(Arrays.equals(dfinal0, dfinal1));
        assertTrue(0 == d.getMarkedCount());
        assertTrue(0 == d.getCurrentCount());
        assertTrue(100 == d.getOutputByteCount());
        d.reset();
        byte[] dpt3 = d.update(ct, 0, ct.length / 2);
        byte[] dpt4 = d.update(ct, ct.length / 2, ct.length - ct.length / 2);
        byte[] dfinal2 = d.doFinal();
        assertTrue(dpt3.length + dpt4.length + dfinal2.length == 100);
        byte[] dpt_all2 = Arrays.copyOf(dpt3, 100);
        System.arraycopy(dpt4, 0, dpt_all2, dpt3.length, dpt4.length);
        System.arraycopy(dfinal2, 0, dpt_all2, dpt3.length + dpt4.length,
                dfinal2.length);
        assertTrue(Arrays.equals(dpt_all, dpt_all2));
    }

    /**
     * update-update-doFinal
     */
    @Test
    public void updateMarkUpdateFinal() throws Exception {
        // Encryption
        CipherLite encrypter = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        File f = CryptoTestUtils.generateRandomAsciiFile(100);
        String ptstr = IOUtils.toString(new FileInputStream(f));
        System.err.println(ptstr);
        GCMCipherLite e = (GCMCipherLite) encrypter;
        byte[] pt = ptstr.getBytes(UTF8);
        // encrypt the 1st half
        e.update(pt, 0, 64);
        e.mark();
        // encrypt the 2nd half
        byte[] ct2 = e.update(pt, 64, pt.length - 64);
        assertTrue("encrypted OutputByteCount=" + e.getOutputByteCount(),
                96 == e.getOutputByteCount());
        byte[] final0 = e.doFinal();
        byte[] final1 = e.getFinalBytes();
        assertTrue(Arrays.equals(final0, final1));
        assertTrue("final0.length=" + final0.length, final0.length >= 16);
        assertTrue(64 == e.getMarkedCount());
        assertTrue(0 == e.getCurrentCount());
        assertTrue("encrypted OutputByteCount=" + e.getOutputByteCount(),
                100 == e.getOutputByteCount());
        e.reset();
        assertTrue(64 == e.getMarkedCount());
        assertTrue(e.getMarkedCount() == e.getCurrentCount());

        byte[] ct4 = e.update(pt, 64, pt.length - 64);

        assertTrue(96 == e.getCurrentCount());
        byte[] final2 = e.doFinal();
        assertNotSame(ct2, ct4);

        assertTrue(Arrays.equals(ct2, ct4));

        byte[] final3 = e.getFinalBytes();
        assertTrue(Arrays.equals(final0, final2));
        assertTrue(Arrays.equals(final0, final3));

        // Decryption
        CipherLite decrypter = encrypter.createInverse();
        byte[] ct = decrypter.createInverse().doFinal(pt);
        GCMCipherLite d = (GCMCipherLite) decrypter;
        d.update(ct, 0, 64);
        d.mark();
        // decrypt the 2nd half
        byte[] dpt2 = d.update(ct, 64, ct.length - 64);
        assertTrue("decrypted OutputByteCount=" + d.getOutputByteCount(),
                d.getOutputByteCount() <= 100);
        byte[] dfinal0 = d.doFinal();
        byte[] dfinal1 = d.getFinalBytes();
        assertTrue(Arrays.equals(dfinal0, dfinal1));
        assertTrue(d.getMarkedCount() <= 64);
        assertTrue(0 == d.getCurrentCount());
        assertTrue("decrypted OutputByteCount=" + d.getOutputByteCount(),
                100 == d.getOutputByteCount());
        d.reset();
        assertTrue(d.getMarkedCount() <= 64);
        assertTrue(d.getMarkedCount() == d.getCurrentCount());

        byte[] dpt4 = d.update(ct, 64, ct.length - 64);

        assertTrue(d.getCurrentCount() <= 100);
        byte[] dfinal2 = d.doFinal();
        assertNotSame(dpt2, dpt4);

        assertTrue(Arrays.equals(ct2, ct4));

        byte[] dfinal3 = d.getFinalBytes();
        assertTrue(Arrays.equals(dfinal0, dfinal2));
        assertTrue(Arrays.equals(dfinal0, dfinal3));

    }

    private static Random rand = new Random();

    @Test
    public void testGCMEncryption() throws Exception {
        int size = 10 * rand.nextInt(1 << 20);
        for (int i = 0; i < 16; i++) {
            int fileSize = size + i;
            testEncryptMarkAndReset(fileSize);
        }
    }

    /**
     * This peculiar but important test covers an edge case where the internal
     * field "invisiblyEncrypted" plays a critical role.
     */
    @Test
    public void test3382280Encrypt() throws Exception {
        // True story: this peculiar file size caused an unexpected unit test
        // failure on April's fool day (4/1/2014) ! Kudo to random generator.
        /*
         * ... remaining: 1032, inputLen=512 remaining: 520, inputLen=512
         * remaining: 8
         */
        testEncryptMarkAndReset(3382280);
        testEncryptMarkAndReset(8);
    }

    public void testEncryptMarkAndReset(int fileSize) throws Exception {
        File file = CryptoTestUtils.generateRandomAsciiFile(fileSize);
        CipherLite w1 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        CipherLite w2 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        GCMCipherLite e1 = (GCMCipherLite) w1;
        GCMCipherLite e2 = (GCMCipherLite) w2;
        byte[] input = IOUtils.toByteArray(new FileInputStream(file));
        System.err.println("Testing encrypt and re-encryption with input size "
                + input.length);
        int remaining = input.length;
        int inputOffset = 0;
        while (remaining >= 512) {
            int inputLen = 512;
            if (debug)
                System.err.println("remaining: " + remaining + ", inputLen="
                        + inputLen);
            long marked = e1.mark();
            assertTrue(marked == inputOffset);
            byte[] ct1 = e1.update(input, inputOffset, inputLen);
            byte[] ct2 = e2.update(input, inputOffset, inputLen);
            assertTrue(Arrays.equals(ct1, ct2));
            // Reset and re-encrypt on e1
            e1.reset();
            byte[] ct3 = e1.update(input, inputOffset, inputLen);
            assertTrue(Arrays.equals(ct1, ct3));

            inputOffset += 512;
            remaining -= 512;
        }
        if (remaining > 0) {
            if (debug)
                System.err.println("remaining: " + remaining);
            long marked = e1.mark();
            assertTrue(marked == inputOffset);
            byte[] ct1 = e1.update(input, inputOffset, remaining);
            byte[] ct2 = e2.update(input, inputOffset, remaining);
            assertTrue(Arrays.equals(ct1, ct2));
            // Reset and re-encrypt on e1
            e1.reset();
            byte[] ct3 = e1.update(input, inputOffset, remaining);
            assertTrue(Arrays.equals(ct1, ct3));
        }
        long marked = e1.mark();
        if (debug)
            System.err.println("input.length - marked="
                    + (input.length - marked));
        assertTrue(input.length - marked <= 512);
        byte[] final1 = e1.doFinal();
        byte[] final2 = e2.doFinal();
        assertTrue(Arrays.equals(final1, final2));

        // Reset and re-final on e1
        e1.reset();
        byte[] final3 = e1.doFinal();
        assertTrue(Arrays.equals(final1, final3));
    }

    @Test
    public void test7155765Decrypt() throws Exception {
        testDecryptMarkAndReset(7155765);
        testDecryptMarkAndReset(3382280);
        testDecryptMarkAndReset(8);
    }

    @Test
    public void testGCMDecryption() throws Exception {
        int size = 10 * rand.nextInt(1 << 20);
        for (int i = 0; i < 16; i++) {
            int fileSize = size + i;
            testDecryptMarkAndReset(fileSize);
        }
    }

    public void testDecryptMarkAndReset(int fileSize) throws Exception {
        File file = CryptoTestUtils
                .generateRandomGCMEncryptedAsciiFile(fileSize);
        CipherLite w1 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.DECRYPT_MODE);
        CipherLite w2 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.DECRYPT_MODE);
        GCMCipherLite d1 = (GCMCipherLite) w1;
        GCMCipherLite d2 = (GCMCipherLite) w2;
        byte[] input = IOUtils.toByteArray(new FileInputStream(file));
        System.err.println("Testing decrypt and re-decryption with input size "
                + input.length);
        int remaining = input.length;
        int inputOffset = 0;
        int chunk = 512;
        while (remaining >= chunk) {
            int inputLen = chunk;
            if (debug)
                System.err.println("remaining: " + remaining + ", inputLen="
                        + inputLen);
            int marked = (int) d1.mark();
            assertTrue(marked <= inputOffset);
            byte[] ct1 = d1.update(input, inputOffset, inputLen);
            byte[] ct2 = d2.update(input, inputOffset, inputLen);
            assertTrue(Arrays.equals(ct1, ct2));
            // Reset and re-encrypt on e1
            d1.reset();
            byte[] ct3 = d1.update(input, marked, ct1.length);
            assertTrue(Arrays.equals(ct1, ct3));

            inputOffset += chunk;
            remaining -= chunk;
        }
        if (remaining > 0) {
            if (debug)
                System.err.println("remaining: " + remaining);
            int marked = (int) d1.mark();
            assertTrue(marked <= inputOffset);
            byte[] pt1 = d1.update(input, inputOffset, remaining);
            byte[] pt2 = d2.update(input, inputOffset, remaining);
            assertTrue(Arrays.equals(pt1, pt2));
            // Reset and re-encrypt on e1
            d1.reset();
            byte[] pt3 = d1.update(input, marked, pt1 == null ? 0 : pt1.length);
            byte[] lhs = pt1 == null || pt1.length == 0 ? null : pt1;
            byte[] rhs = pt3 == null || pt3.length == 0 ? null : pt3;
            assertTrue(Arrays.equals(lhs, rhs));
        }
        long marked = d1.mark();
        if (debug)
            System.err.println("input.length - marked="
                    + (input.length - marked));
        assertTrue(input.length - marked <= 512);
        byte[] final1 = d1.doFinal();
        byte[] final2 = d2.doFinal();
        assertTrue(Arrays.equals(final1, final2));

        // Reset and re-final on e1
        d1.reset();
        byte[] final3 = d1.doFinal();
        assertTrue(Arrays.equals(final1, final3));
    }
    
    @Test
    public void markAndReset() throws Exception {
        File f = CryptoTestUtils.generateRandomAsciiFile(100);
        CipherLite cl = CryptoTestUtils.createTestCipherLite(Cipher.ENCRYPT_MODE, ContentCryptoScheme.AES_GCM);
        CipherLiteInputStream clis = new CipherLiteInputStream(new ResettableInputStream(f), cl);
        assertTrue(clis.markSupported());
        clis.mark(-1);
        byte[] ba1 = IOUtils.toByteArray(clis);
        assertTrue(ba1.length == 116);
        clis.reset();
        byte[] ba2 = IOUtils.toByteArray(clis);
        assertTrue(Arrays.equals(ba1, ba2));
        assertTrue(0 == clis.skip(10));
        clis.close();
    }

    @Test
    public void skip() throws Exception {
        File f = CryptoTestUtils.generateRandomAsciiFile(100);
        CipherLite cl = CryptoTestUtils.createTestCipherLite(Cipher.ENCRYPT_MODE, ContentCryptoScheme.AES_GCM);
        CipherLiteInputStream clis = new CipherLiteInputStream(new ResettableInputStream(f), cl);
        int b = clis.read();
        assertFalse(b == -1);
        assertTrue(9 == clis.skip(9));
        int n = (int)clis.skip(90);
        System.out.println("n=" + n);
        assertTrue(n <= 90);
        for (int i=0; i < 116-(n+10); i++) {
            System.out.println("i=" + i);
            assertFalse(clis.read() == -1);
        }
        assertTrue(clis.read() == -1);
        clis.close();
    }
}
