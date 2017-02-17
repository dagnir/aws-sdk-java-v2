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
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.flipRandomBit;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateRandomAsciiFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Goal: {@link CipherLiteInputStream} should behave similar to a
 * {@link CipherInputStream} BUT it should honor mac check failure when AEAD is
 * in use.
 */
public class CipherLiteInputStreamTest {
    private static final Random RAND = new Random();
    private static final boolean CLEAN_UP = true;

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test
    public void test() throws Exception {
        // Generate a file
        File file = generateRandomAsciiFile(1024 * 1024 + RAND.nextInt(100), CLEAN_UP);
        System.out.println("file: " + file + ", len=" + file.length());
        long pt_len = file.length();
        InputStream is = new FileInputStream(file);
        final byte[] pt = IOUtils.toByteArray(is);
        is.close();
        // Encrypt it
        CipherLite cipherWrapper = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                                        ContentCryptoScheme.AES_GCM);
        is = new CipherLiteInputStream(new FileInputStream(file),
                                       cipherWrapper);
        byte[] ct_aead = IOUtils.toByteArray(is);
        is.close();
        File encryptedFileAead = new File(
                "/tmp/S3CipherInputStreamTest-v2-ct.bin");
        FileUtils.writeByteArrayToFile(encryptedFileAead, ct_aead);
        cipherWrapper = createTestCipherLite(Cipher.ENCRYPT_MODE,
                                             ContentCryptoScheme.AES_CBC);
        is = new CipherLiteInputStream(new FileInputStream(file), cipherWrapper);
        byte[] ct_v1 = IOUtils.toByteArray(is);
        is.close();
        assertTrue(ct_aead.length - pt_len == 16);
        assertTrue(ct_v1.length >= pt_len);
        if (ct_aead.length == ct_v1.length) {
            assertFalse(Arrays.equals(ct_aead, ct_v1));
        }
        File encryptedFileV1 = new File(
                "/tmp/S3CipherInputStreamTest-v1-ct.bin");
        FileUtils.writeByteArrayToFile(encryptedFileV1, ct_v1);
        // Decrypt the right file with AEAD
        cipherWrapper = createTestCipherLite(Cipher.DECRYPT_MODE,
                                             ContentCryptoScheme.AES_GCM);
        is = new CipherLiteInputStream(new FileInputStream(encryptedFileAead), cipherWrapper);
        byte[] pt_aead = IOUtils.toByteArray(is);
        is.close();
        assertTrue(Arrays.equals(pt, pt_aead));
        // Decrypt the wrong file with AEAD using S3CipherInputStream
        cipherWrapper = createTestCipherLite(Cipher.DECRYPT_MODE,
                                             ContentCryptoScheme.AES_GCM);
        is = new CipherLiteInputStream(new FileInputStream(encryptedFileV1), cipherWrapper);
        try {
            IOUtils.toByteArray(is);
            fail("Should see something like mac check in GCM failed");
        } catch (SecurityException ex) {
            //            ex.printStackTrace();
        }
        // Decrypt the wrong file with AEAD using CipherInputStream
        cipherWrapper = createTestCipherLite(Cipher.DECRYPT_MODE,
                                             ContentCryptoScheme.AES_GCM);
        byte[] garbage = null;
        try {
            is = new CipherInputStream(new FileInputStream(encryptedFileV1), cipherWrapper.getCipher());
            garbage = IOUtils.toByteArray(is);
            assertFalse(Arrays.equals(pt, garbage));
        } catch (IOException ex) {
            // Some versions of Java 7 has changed behavior to fail correctly; so that's fine too
            assertTrue(ex.getMessage().contains("mac check in GCM failed"));
        }
        // Decrypt the right file with v1 crypto
        cipherWrapper = createTestCipherLite(Cipher.DECRYPT_MODE,
                                             ContentCryptoScheme.AES_CBC);
        is = new CipherLiteInputStream(new FileInputStream(encryptedFileV1), cipherWrapper);
        byte[] pt_v1 = IOUtils.toByteArray(is);
        is.close();
        assertTrue(Arrays.equals(pt, pt_v1));
        // Decrypt the wrong file with v1 crypto
        cipherWrapper = createTestCipherLite(Cipher.DECRYPT_MODE,
                                             ContentCryptoScheme.AES_CBC);
        is = new CipherLiteInputStream(new FileInputStream(encryptedFileAead), cipherWrapper);
        garbage = IOUtils.toByteArray(is);
        is.close();
        assertFalse(Arrays.equals(pt, garbage));

        // Let's flip a random bit in the AEAD cipher text and see if the
        // authentication would fail
        byte[] ct_corrupted = flipRandomBit(ct_aead);
        cipherWrapper = createTestCipherLite(Cipher.DECRYPT_MODE,
                                             ContentCryptoScheme.AES_GCM);
        is = new CipherLiteInputStream(new ByteArrayInputStream(ct_corrupted), cipherWrapper);
        try {
            IOUtils.toByteArray(is);
            fail("Should see something like mac check in GCM failed");
        } catch (SecurityException ex) {
            //            ex.printStackTrace();
        }
        // In contrast, CipherInputStreamm would just happily return garbage
        cipherWrapper = createTestCipherLite(Cipher.DECRYPT_MODE,
                                             ContentCryptoScheme.AES_GCM);
        try {
            is = new CipherInputStream(new ByteArrayInputStream(ct_corrupted), cipherWrapper.getCipher());
            garbage = IOUtils.toByteArray(is);
            assertFalse(Arrays.equals(pt, garbage));
        } catch (IOException ex) {
            // Some versions of Java 7 has changed behavior to fail correctly; so that's fine too
            assertTrue(ex.getMessage().contains("mac check in GCM failed"));
        }
        // Clean up
        if (CLEAN_UP) {
            encryptedFileAead.delete();
            encryptedFileV1.delete();
        }
    }

}
