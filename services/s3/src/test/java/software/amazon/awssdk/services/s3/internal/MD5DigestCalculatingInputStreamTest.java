package software.amazon.awssdk.services.s3.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import software.amazon.awssdk.util.Md5Utils;

public class MD5DigestCalculatingInputStreamTest {
    private static final boolean DEBUG = true;
    @Test
    public void test() throws Exception {
        byte[] data = "0123456789abcdefghijklmnopqrstuvwxyz!@#$%^&*()_+-="
                .getBytes();
        byte[] md5Expected = Md5Utils.computeMD5Hash(data);
        byte[] baExpected;
        {
            MD5DigestCalculatingInputStream is = new MD5DigestCalculatingInputStream(
                    new ByteArrayInputStream(data));
            baExpected = IOUtils.toByteArray(is);
            byte[] md5 = is.getMd5Digest();
            assertTrue(Arrays.equals(md5Expected, md5));
        }
        {
            MD5DigestCalculatingInputStream is = new MD5DigestCalculatingInputStream(
                    new ByteArrayInputStream(data));
            is.mark(-1);
            baExpected = IOUtils.toByteArray(is);
            byte[] md5 = is.getMd5Digest();
            assertTrue(Arrays.equals(md5Expected, md5));

            is.reset();
            byte[] ba = IOUtils.toByteArray(is);
            byte[] md5_2 = is.getMd5Digest();
            assertTrue(Arrays.equals(md5Expected, md5_2));
            assertTrue(Arrays.equals(baExpected, ba));
        }
        {
            MD5DigestCalculatingInputStream is = new MD5DigestCalculatingInputStream(
                    new ByteArrayInputStream(data));
            is.mark(-1);
            is.read(new byte[10]);
            is.reset();
            byte[] ba = IOUtils.toByteArray(is);
            if (DEBUG)
                System.out.println("ba.length: " + ba.length);
            assertTrue(Arrays.equals(baExpected, ba));
            byte[] md5 = is.getMd5Digest();
            assertTrue(Arrays.equals(md5Expected, md5));
        }
        {
            MD5DigestCalculatingInputStream is = new MD5DigestCalculatingInputStream(
                    new ByteArrayInputStream(data));
            is.mark(-1);
            is.read(new byte[10]);
            is.mark(-1);
            is.read(new byte[10]);
            is.reset();
            byte[] ba = IOUtils.toByteArray(is);
            if (DEBUG)
                System.out.println("ba.length: " + ba.length);
            assertFalse(Arrays.equals(baExpected, ba));
            byte[] md5 = is.getMd5Digest();
            assertTrue(Arrays.equals(md5Expected, md5));
        }
        {
            MD5DigestCalculatingInputStream is = new MD5DigestCalculatingInputStream(
                    new ByteArrayInputStream(data));
            is.read(new byte[10]);
            is.mark(-1);
            is.read(new byte[10]);
            is.reset();
            is.read(new byte[10]);
            is.mark(-1);
            is.read(new byte[10]);
            is.reset();
            byte[] ba = IOUtils.toByteArray(is);
            if (DEBUG)
                System.out.println("ba.length: " + ba.length);
            assertFalse(Arrays.equals(baExpected, ba));
            byte[] md5 = is.getMd5Digest();
            assertTrue(Arrays.equals(md5Expected, md5));
        }
    }

    @Test
    public void testResetWithoutMark() throws IOException {
        byte[] data = new byte[10];
        MD5DigestCalculatingInputStream is = new MD5DigestCalculatingInputStream(
                new ByteArrayInputStream(data));
        IOUtils.toByteArray(is);
        byte[] digestExpected = is.getMd5Digest();
        
        is = new MD5DigestCalculatingInputStream(
                new ByteArrayInputStream(data));
        byte[] buf = new byte[data.length];
        is.read(buf);
        is.reset();
        IOUtils.toByteArray(is);
        byte[] digest = is.getMd5Digest();
        assertTrue(Arrays.equals(digest, digestExpected));
    }
}
