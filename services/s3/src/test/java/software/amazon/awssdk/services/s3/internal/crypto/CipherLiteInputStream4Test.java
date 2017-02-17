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
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.createTestCipherLite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.crypto.Cipher;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.test.util.ConstantInputStream;

public class CipherLiteInputStream4Test {

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test
    public void gcmMultiPart_NotLast() throws Exception {
        CipherLite aes_gcm = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        InputStream is = new CipherLiteInputStream(new ByteArrayInputStream(new byte[0]),
                                                   aes_gcm,
                                                   512, true, false);
        assertTrue(is.read(new byte[16]) == -1);
        assertTrue(is.read(new byte[16]) == -1);
        is.close();
    }

    @Test
    public void gcmMultiPart_Last() throws Exception {
        CipherLite aes_gcm = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        InputStream is = new CipherLiteInputStream(new ByteArrayInputStream(new byte[0]),
                                                   aes_gcm,
                                                   512, true, true);
        int len = is.read(new byte[16]);
        assertTrue(len == 16);
        assertTrue(is.read(new byte[16]) == -1);
        is.close();
    }

    @SuppressWarnings("resource")
    @Test(expected = IllegalArgumentException.class)
    public void invalidArgument() throws Exception {
        new CipherLiteInputStream(new ByteArrayInputStream(new byte[0]),
                                  null,
                                  512, false, true);
    }

    @Test
    public void gcmNonMultiPart() throws Exception {
        CipherLite aes_gcm = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        InputStream is = new CipherLiteInputStream(new ByteArrayInputStream(new byte[0]),
                                                   aes_gcm,
                                                   512, false, false);
        int len = is.read(new byte[16]);
        assertTrue(len == 16);
        assertTrue(is.read(new byte[16]) == -1);
        is.close();
    }

    @Test
    public void testInvalidBufferSize() {
        int[] bufsizes = {123, 0, -1};
        for (int size : bufsizes) {
            try {
                new CipherLiteInputStream(new SillyInputStream(), null, size,
                                          false, false);
                fail();
            } catch (IllegalArgumentException expected) {
                // Expected.
            }
        }
    }

    // Test the case when the underlying input stream always returns
    // zero length data
    @Test
    public void testZeroReturnLen() throws Exception {
        ContentCryptoScheme[] schemes = {
            ContentCryptoScheme.AES_CBC,
            ContentCryptoScheme.AES_GCM,
            ContentCryptoScheme.AES_CTR,
        };
        for (ContentCryptoScheme scheme : schemes) {
            try {
                readWithBuffer(scheme);
                fail();
            } catch (IOException ex) {
                assertTrue(ex.getMessage().contains("exceeded maximum number of attempts to read next chunk of data"));
            }
        }
    }

    // Test the case when the underlying input stream always returns
    // zero length data with single-byte reads
    @Test
    public void testZeroReturnLenNoBuffer() throws Exception {
        ContentCryptoScheme[] schemes = {
            ContentCryptoScheme.AES_CBC,
            ContentCryptoScheme.AES_GCM,
            ContentCryptoScheme.AES_CTR,
        };
        for (ContentCryptoScheme scheme : schemes) {
            try {
                readWithNoBuffer(scheme);
                fail();
            } catch (IOException ex) {
                assertTrue(ex.getMessage().contains("exceeded maximum number of attempts to read next chunk of data"));
            }
        }
    }

    @Test
    public void testNullCipherInputStream() throws Exception {
        CipherLiteInputStream input = new CipherLiteInputStream(new ConstantInputStream(100, (byte) 'Z'));
        String s = IOUtils.toString(input);
        assertTrue(100 == s.length());
        for (char c : s.toCharArray()) {
            assertTrue('Z' == c);
        }
    }

    private byte[] readWithBuffer(ContentCryptoScheme scheme) throws Exception {
        CipherLite cipherLite = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                     scheme);
        SillyInputStream cis = new SillyInputStream();
        InputStream is = new CipherLiteInputStream(cis, cipherLite);
        byte[] ret = IOUtils.toByteArray(is); // IOUtils invokes read with byte
        // buffer
        is.close();
        return ret;
    }

    private byte[] readWithNoBuffer(ContentCryptoScheme scheme) throws Exception {
        CipherLite cipherLite = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                     scheme);
        ConstantInputStream cis = new SillyInputStream();
        InputStream is = new CipherLiteInputStream(cis, cipherLite);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int b = is.read();
        while (b != -1) {
            os.write(b);
            b = is.read();
        }
        is.close();
        return os.toByteArray();
    }

    private static class SillyInputStream extends ConstantInputStream {
        SillyInputStream() {
            super(100, (byte) 'Z');
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return 0;
        }
    }
}
