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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.createTestCipherLite;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.test.util.ConstantInputStream;

/**
 * Test various edge cases when zero or null buffer is used for reading, and
 * when zero length data is sometimes returned during a read operation from the
 * underlying input stream.
 */
public class CipherLiteInputStream3Test {
    private static final int DATA_SIZE = 1024 * 1024;
    private static final boolean RANDOMLY_RETURN_ZERO = true;
    private static final boolean RANDOM_ZERO_LEN_BUFFER = true;
    private static final boolean RANDOM_NULL_BUFFER = true;
    private static final Random RAND = new Random();

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    // Test the case when the underlying input stream sometimes returns
    // zero length data
    @Test
    public void testZeroReturnLen() throws Exception {
        doTestZeroReturnLen(ContentCryptoScheme.AES_CBC);
        doTestZeroReturnLen(ContentCryptoScheme.AES_GCM);
        doTestZeroReturnLen(ContentCryptoScheme.AES_CTR);
    }

    // Test the case when a null buffer is invalidly passed causing NPE
    @Test
    public void testNullBuffer() throws Exception {
        ContentCryptoScheme[] schemes = {ContentCryptoScheme.AES_CBC,
                                         ContentCryptoScheme.AES_GCM, ContentCryptoScheme.AES_CTR};
        for (ContentCryptoScheme scheme : schemes) {
            try {
                doTestNullBuffer(scheme);
                fail();
            } catch (NullPointerException expected) {
                // Expected.
            }
        }
    }

    // Test the case when a zero length non-null buffer is sometimes used
    // for reading
    @Test
    public void testZeroLenBuffer() throws Exception {
        doTestZeroLenBuffer(ContentCryptoScheme.AES_CBC);
        doTestZeroLenBuffer(ContentCryptoScheme.AES_GCM);
        doTestZeroLenBuffer(ContentCryptoScheme.AES_CTR);
    }

    // Test the case when a zero length null buffer is sometimes used
    // for reading
    @Test
    public void testZeroLenNullBuffer() throws Exception {
        doTestZeroLenNullBuffer(ContentCryptoScheme.AES_CBC);
        doTestZeroLenNullBuffer(ContentCryptoScheme.AES_GCM);
        doTestZeroLenNullBuffer(ContentCryptoScheme.AES_CTR);
    }

    public void doTestZeroReturnLen(ContentCryptoScheme scheme)
            throws Exception {
        final byte[] ct1 = readWithBuffer(scheme, RANDOMLY_RETURN_ZERO);
        final byte[] ct2 = readWithBuffer(scheme, !RANDOMLY_RETURN_ZERO);
        assertTrue(Arrays.equals(ct1, ct2));
        final byte[] ct3 = readWithNoBuffer(scheme, RANDOMLY_RETURN_ZERO);
        assertTrue(Arrays.equals(ct1, ct3));
        final byte[] ct4 = readWithNoBuffer(scheme, !RANDOMLY_RETURN_ZERO);
        assertTrue(Arrays.equals(ct1, ct4));
    }

    private byte[] readWithBuffer(ContentCryptoScheme scheme,
                                  boolean randomlyReturnZero) throws Exception {
        CipherLite cipherLite = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                     scheme);
        ConstantInputStream cis = new ConstantInputStream(DATA_SIZE,
                                                          (byte) 'Z', randomlyReturnZero);
        CipherLiteInputStream is = new CipherLiteInputStream(cis, cipherLite);
        assertFalse(is.markSupported());
        byte[] ret = IOUtils.toByteArray(is); // IOUtils invokes read with byte
        // buffer
        is.close();
        if (randomlyReturnZero) {
            assertTrue(cis.getRandomZerosCount() > 0);
        } else {
            assertTrue(cis.getRandomZerosCount() == 0);
        }
        return ret;
    }

    private byte[] readWithNoBuffer(ContentCryptoScheme scheme,
                                    boolean randomlyReturnZero) throws Exception {
        CipherLite cipherLite = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                     scheme);
        ConstantInputStream cis = new ConstantInputStream(DATA_SIZE,
                                                          (byte) 'Z', randomlyReturnZero);
        InputStream is = new CipherLiteInputStream(cis, cipherLite);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int b = is.read();
        while (b != -1) {
            os.write(b);
            b = is.read();
        }
        is.close();
        if (randomlyReturnZero) {
            assertTrue(cis.getRandomZerosCount() > 0);
        } else {
            assertTrue(cis.getRandomZerosCount() == 0);
        }
        return os.toByteArray();
    }

    private byte[] doTestNullBuffer(ContentCryptoScheme scheme)
            throws Exception {
        CipherLite cipherLite = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                     scheme);
        ConstantInputStream cis = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        CipherLiteInputStream is = new CipherLiteInputStream(cis, cipherLite);
        is.read(null); // expect to throw NPE here
        is.close();
        return null;
    }

    public void doTestZeroLenNullBuffer(ContentCryptoScheme scheme)
            throws Exception {
        byte[] a = readWithZeroLenNullBuffer(scheme, RANDOM_NULL_BUFFER);
        byte[] b = readWithZeroLenNullBuffer(scheme, !RANDOM_NULL_BUFFER);
        assertTrue(a.length >= DATA_SIZE);
        assertTrue(Arrays.equals(a, b));
    }

    private byte[] readWithZeroLenNullBuffer(ContentCryptoScheme scheme,
                                             boolean useZeroLenNullBufferRandomly) throws Exception {
        CipherLite cipherLite = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                     scheme);
        ConstantInputStream cis = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        InputStream is = new CipherLiteInputStream(cis, cipherLite);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[128];
        int len;
        int countNullBufferUse = 0;
        if (useZeroLenNullBufferRandomly && RAND.nextBoolean()) {
            len = is.read(null, 0, 0);
            countNullBufferUse++;
        } else {
            len = is.read(buf);
        }
        while (len != -1) {
            bos.write(buf, 0, len);
            if (useZeroLenNullBufferRandomly && RAND.nextBoolean()) {
                len = is.read(null, 0, 0);
                countNullBufferUse++;
            } else {
                len = is.read(buf);
            }
        }
        is.close();
        if (useZeroLenNullBufferRandomly) {
            assertTrue(countNullBufferUse > 0);
        } else {
            assertTrue(countNullBufferUse == 0);
        }
        return bos.toByteArray();
    }

    public void doTestZeroLenBuffer(ContentCryptoScheme scheme)
            throws Exception {
        byte[] a = readWithZeroLenBuffer(scheme, RANDOM_ZERO_LEN_BUFFER);
        byte[] b = readWithZeroLenBuffer(scheme, !RANDOM_ZERO_LEN_BUFFER);
        assertTrue(a.length >= DATA_SIZE);
        assertTrue(Arrays.equals(a, b));
    }

    private byte[] readWithZeroLenBuffer(ContentCryptoScheme scheme,
                                         boolean useZeroLenBufferRandomly) throws Exception {
        CipherLite cipherLite = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                     scheme);
        ConstantInputStream cis = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        InputStream is = new CipherLiteInputStream(cis, cipherLite);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[128];
        int len;
        int countZeroBufferUse = 0;
        if (useZeroLenBufferRandomly && RAND.nextBoolean()) {
            len = is.read(new byte[0]);
            countZeroBufferUse++;
        } else {
            len = is.read(buf);
        }
        while (len != -1) {
            bos.write(buf, 0, len);
            if (useZeroLenBufferRandomly && RAND.nextBoolean()) {
                len = is.read(new byte[0]);
                countZeroBufferUse++;
            } else {
                len = is.read(buf);
            }
        }
        is.close();
        if (useZeroLenBufferRandomly) {
            assertTrue(countZeroBufferUse > 0);
        } else {
            assertTrue(countZeroBufferUse == 0);
        }
        return bos.toByteArray();
    }
}
