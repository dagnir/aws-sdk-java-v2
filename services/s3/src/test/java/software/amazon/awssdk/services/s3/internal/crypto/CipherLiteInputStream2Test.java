/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.createTestCipherLite;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateRandomAsciiFile;

import java.io.File;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.runtime.io.ResettableInputStream;

/**
 * @author hchar
 */
public class CipherLiteInputStream2Test {
    private static final Random rand = new Random();
    private static final boolean CLEAN_UP = true;
    private static final int MAX_TEST_SECONDS = 10;

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    // Test exhaustively reseting from the offset of every multiple of 512 bytes
    // to the end of a file with size of between 1M and 1M + 99 bytes.
    @Test
    public void test() throws Exception {
        // Generate a file
        File file = generateRandomAsciiFile(1024 * 1024 + rand.nextInt(100),
                                            CLEAN_UP);
        System.out.println("file: " + file + ", len=" + file.length());
        final long pt_len = file.length();
        // Encrypt it
        CipherLite gcm1 = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                               ContentCryptoScheme.AES_GCM);
        // Compute the MD5 of the ciphertext
        byte[] buf1 = new byte[1024 * 1024];
        CipherLiteInputStream s3is1 = new CipherLiteInputStream(new ResettableInputStream(file), gcm1);
        int read1 = s3is1.read(buf1);
        MessageDigest md1 = MessageDigest.getInstance("MD5");
        while (read1 != -1) {
            md1.update(buf1, 0, read1);
            read1 = s3is1.read(buf1);
        }
        final byte[] md5a = md1.digest();
        s3is1.close();

        byte[] buf2 = new byte[512];
        long startNano = System.nanoTime();
        for (int count = 0; count < pt_len; count += 512) {
            long endNano = System.nanoTime();
            if (TimeUnit.NANOSECONDS.toSeconds(endNano - startNano) > MAX_TEST_SECONDS) {
                System.out.println("Time's up");
                return;
            }
            MessageDigest md2 = MessageDigest.getInstance("MD5");
            CipherLite gcm2 = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                   ContentCryptoScheme.AES_GCM);
            CipherLiteInputStream s3is2 = new CipherLiteInputStream(new ResettableInputStream(file), gcm2);
            int offset = 0;
            boolean marked = false;
            if (offset == count) {
                s3is2.mark(0);
                marked = true;
                System.out.println("marked at " + offset);
            }
            int read2 = s3is2.read(buf2);
            while (read2 != -1) {
                if (!marked) {
                    // update the MD5 up to before marking
                    md2.update(buf2, 0, read2);
                }
                offset += read2;
                if (offset == count) {
                    s3is2.mark(0);
                    marked = true;
                    System.out.println("marked at " + offset);
                }
                read2 = s3is2.read(buf2);
            }
            System.out.println("Resetting");
            s3is2.reset();
            read2 = s3is2.read(buf2);
            while (read2 != -1) {
                // update the MD5 from the point after reset
                md2.update(buf2, 0, read2);
                read2 = s3is2.read(buf2);
            }
            s3is2.close();
            byte[] md5b = md2.digest();
            boolean b = Arrays.equals(md5a, md5b);
            assertTrue("count=" + count, b);
        }
    }

    // Test resetting arbitrary parts of a 10M file
    @Test
    public void test2() throws Exception {
        // Generate a file
        File file = generateRandomAsciiFile(10 * 1024 * 1024 + rand.nextInt(100),
                                            CLEAN_UP);
        System.out.println("file: " + file + ", len=" + file.length());
        final long pt_len = file.length();
        // Encrypt it
        CipherLite gcm1 = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                               ContentCryptoScheme.AES_GCM);
        // Compute the MD5 of the ciphertext
        byte[] buf1 = new byte[1024 * 1024];
        CipherLiteInputStream s3is1 = new CipherLiteInputStream(new ResettableInputStream(file), gcm1);
        int read1 = s3is1.read(buf1);
        MessageDigest md1 = MessageDigest.getInstance("MD5");
        while (read1 != -1) {
            md1.update(buf1, 0, read1);
            read1 = s3is1.read(buf1);
        }
        final byte[] md5a = md1.digest();
        s3is1.close();

        byte[] buf2 = new byte[512];
        int delta = (rand.nextInt(100) + 201);
        System.out.println("delta=" + delta);
        long startNano = System.nanoTime();
        for (int count = 0; count < pt_len; count += 512 * delta) {
            long endNano = System.nanoTime();
            if (TimeUnit.NANOSECONDS.toSeconds(endNano - startNano) > MAX_TEST_SECONDS) {
                System.out.println("Time's up");
                return;
            }
            MessageDigest md2 = MessageDigest.getInstance("MD5");
            CipherLite gcm2 = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                   ContentCryptoScheme.AES_GCM);
            CipherLiteInputStream s3is2 = new CipherLiteInputStream(new ResettableInputStream(file), gcm2);
            int offset = 0;
            boolean marked = false;
            if (offset == count) {
                s3is2.mark(0);
                marked = true;
                System.out.println("marked at " + offset);
            }
            int read2 = s3is2.read(buf2);
            int resetOffset = -1;
            while (read2 != -1) {
                if (!marked) {
                    // update the MD5 up to before marking
                    md2.update(buf2, 0, read2);
                }
                offset += read2;
                if (offset == resetOffset) {
                    break;
                }
                if (offset == count) {
                    s3is2.mark(0);
                    int remainingBlocks = (int) (pt_len - offset) / 512;
                    if (remainingBlocks > 0) {
                        resetOffset = offset + rand.nextInt(remainingBlocks) * 512;
                    }
                    marked = true;
                    System.out.println("marked at " + offset + " with resetOffset set to " + resetOffset);
                }
                read2 = s3is2.read(buf2);
            }
            System.out.println("Resetting");
            s3is2.reset();
            read2 = s3is2.read(buf2);
            while (read2 != -1) {
                // update the MD5 from the point after reset
                md2.update(buf2, 0, read2);
                read2 = s3is2.read(buf2);
            }
            s3is2.close();
            byte[] md5b = md2.digest();
            assertTrue(Arrays.equals(md5a, md5b));
        }
    }
}
