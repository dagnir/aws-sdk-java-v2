package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.encodeHexString;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.runtime.io.ResettableInputStream;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.MD5DigestCalculatingInputStream;
/*
    Sample Run:

    Generating random ASCII file with size: 4296015870 at /var/folders/z2/cbbxj1lj5zd300z6zp7j7d7xw9jdgp/T/CryptoTestUtils4872336324615163588.txt
    took: 55 secs
    MD5: 7a8521118c5e894560f697cc3b71d710
    took: 134 secs
    markPoint=4294967806, resetPoint=4295018494
    Marked after reading 4294975488 bytes
    Reset after reading 4295024640 bytes
    total: 4296015886, marked: true, reset: true, marked at: 4294975488
    MD5: 7a8521118c5e894560f697cc3b71d710
    took: 152 secs
 */
@Category(S3Categories.ReallySlow.class)
public class BigFileGCMIntegrationTest {
    private static final int BUFSIZE = 1024*8;
    private static final boolean GENERATE_ENCRYPTED_FILE = false;

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test
    public void test() throws Exception {
//        if (!CryptoTestUtils.runTimeConsumingTests()) {
//            System.out.println("Please set the environment variable, export RUN_TIME_CONSUMING_TESTS=true, to run the test");
//            return;
//        }
        long start = System.nanoTime();
        File file = generateHugeFile();
        start = took(start);
        String md5a = encrypt(file);
        start = took(start);
        String md5b = encryptWithMarkAndReset(file);
        start = took(start);
        assertEquals("md5a="+md5a+", md5b="+md5b, md5a, md5b);
    }

    private File generateHugeFile() throws Exception {
        long fileSize = ((long) Integer.MAX_VALUE) * 2 + 1024L * 1024L;
        return CryptoTestUtils.generateRandomAsciiFile(fileSize);
    }

    private String encrypt(File infile) throws Exception {
        assertTrue(infile.length() == 4296015870L);
        ContentCryptoScheme scheme = ContentCryptoScheme.AES_GCM;
        MD5DigestCalculatingInputStream md5in = new MD5DigestCalculatingInputStream(
                new CipherLiteInputStream(new ResettableInputStream(
                        infile), scheme.createCipherLite(
                CryptoTestUtils.getTestSecretKey(scheme),
                new byte[scheme.getIVLengthInBytes()],
                Cipher.ENCRYPT_MODE),
            BUFSIZE));
        File outfile = null;
        FileOutputStream out = null;
        if (GENERATE_ENCRYPTED_FILE) {
            outfile = new File(infile.getAbsolutePath() + "_encrypted");
            out = new FileOutputStream(outfile);
        }
        byte[] buf = new byte[BUFSIZE];
        int len = md5in.read(buf);
        while (len > -1) {
            if (out != null)
                out.write(buf, 0, len);
            len = md5in.read(buf);
        }
        if (out != null)
            out.close();
        md5in.close();
        String md5sum = encodeHexString(md5in.getMd5Digest());
        System.out.println("MD5: " + md5sum);
        if (outfile != null)
            System.out.println("outfile size: " + outfile.length());
        return md5sum;
    }


    private String encryptWithMarkAndReset(File infile) throws Exception {
        assertTrue(infile.length() == 4296015870L);
        ContentCryptoScheme scheme = ContentCryptoScheme.AES_GCM;
        CipherLiteInputStream in = new CipherLiteInputStream(new ResettableInputStream(infile),
            scheme.createCipherLite(
                CryptoTestUtils.getTestSecretKey(scheme),
                new byte[scheme.getIVLengthInBytes()],
                Cipher.ENCRYPT_MODE),
            BUFSIZE);
        boolean marked = false;
        long markedAt = -1;
        boolean reset = false;
        byte[] buf = new byte[BUFSIZE];
        long total = 0;
        int len = in.read(buf);
        final long markPoint = (((long)Integer.MAX_VALUE)*2+512L);
        final long resetPoint = (((long)Integer.MAX_VALUE)*2 + 100*512L);
        System.out.println("markPoint=" + markPoint + ", resetPoint=" + resetPoint);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        while (len > -1) {
            total += len;
            if (!marked || reset) {
                md5.update(buf, 0, len);
            }
            if (total > markPoint) {
                if (!marked) {
                    in.mark(-1);
                    System.out.println("Marked after reading " + total + " bytes");
                    marked = true;
                    markedAt = total;
                } else if (total > resetPoint) {
                    if (!reset) {
                        in.reset();
                        System.out.println("Reset after reading " + total + " bytes");
                        reset = true;
                        total = markedAt;
                    }
                }
            }
            len = in.read(buf);
        }
        in.close();
        String md5sum = encodeHexString(md5.digest());
        System.out.println("total: " + total + ", marked: " + marked + ", reset: " + reset +", marked at: " + markedAt);
        System.out.println("MD5: " + md5sum);
        return md5sum;
        /*
            Output Sample:
            markPoint=4294967806, resetPoint=4295018494
            Marked after reading 4294975488 bytes
            Reset after reading 4295024640 bytes
            total: 4296015886, marked: true, reset: true, marked at: 4294975488
            MD5: 7cafb7c500ce371f33fc7a1a7bcb5461, expected: 7cafb7c500ce371f33fc7a1a7bcb5461
            took: 141
         */
    }

    private long took(long start) {
        long end = System.nanoTime();
        System.out.println("took: " + TimeUnit.NANOSECONDS.toSeconds(end-start) + " secs");
        return end;
    }
}
