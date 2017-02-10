package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.runtime.io.ResettableInputStream;
import software.amazon.awssdk.services.s3.categories.S3Categories;

@Category(S3Categories.Slow.class)
public class CipherLiteInputStream1Test {
    private static final boolean debug = false;
    private static Random rand = new Random();

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidBuffSize() throws Exception {
        CipherLite w1 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        File f1 = CryptoTestUtils.generateRandomAsciiFile(10);
        new CipherLiteInputStream(new ResettableInputStream(f1), w1, 11)
                .close();
    }

    @Test
    public void markAndReset() throws Exception {
        int[] bufsizes = { 512, 1024, 1024 * 4, 1024 * 8, 1024 * 1024 };
        int filesize = 10 * rand.nextInt(1 << 20);
        for (int i = 0; i < 16; i++) {
            int fileSize = filesize + i;
            for (int bufsize : bufsizes) {
                final long start = System.nanoTime();
                testMarkAndReset(fileSize, bufsize);
                System.err.println("took "
                        + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()
                                - start) + " ms");
            }
        }
    }

    public void testMarkAndReset(int fileSize, int bufSize) throws Exception {
        System.err.print("Testing fileSize=" + fileSize + ", bufSize="
                + bufSize + "\t");
        File f1 = CryptoTestUtils.generateRandomAsciiFile(fileSize);
        File f2 = new File(f1.getPath());
        CipherLite w1 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        CipherLite w2 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);

        CipherLiteInputStream testStream = new CipherLiteInputStream(
                new ResettableInputStream(f1), w1, bufSize);
        CipherLiteInputStream refStream = new CipherLiteInputStream(
                new ResettableInputStream(f2), w2, bufSize);

        byte[] buf1 = new byte[bufSize];
        byte[] buf2 = new byte[bufSize];
        while (true) {
            testStream.mark(buf1.length);
            int read1 = testStream.read(buf1);
            int read2 = refStream.read(buf2);
            assertTrue(read1 == read2);
            assertTrue(Arrays.equals(buf1, buf2));
            if (read1 == -1)
                break;
            testStream.reset();
            read1 = testStream.read(buf1);
            if (debug)
                System.err.println("read1=" + read1 + ", read2=" + read2);
            assertTrue(read1 == read2);
            assertTrue(Arrays.equals(buf1, buf2));
        }
        testStream.close();
        refStream.close();
    }
}
