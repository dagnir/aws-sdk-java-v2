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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_CTR;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_GCM;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.MAX_GCM_BLOCKS;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.encodeBase64String;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.encodeHexString;
import static software.amazon.awssdk.util.StringUtils.UTF8;

import java.nio.ByteBuffer;
import java.util.Random;
import javax.crypto.Cipher;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.util.StringUtils;

public class ContentCryptoSchemeTest {
    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test
    public void testEncryptGcmDecryptCtr() throws Exception {
        CipherLite gcm_encrypter = CryptoTestUtils.createTestCipher(AES_GCM,
                                                                    AES_GCM.getIvLengthInBytes(), Cipher.ENCRYPT_MODE);
        String plaintext = "1234567890123456ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        byte[] pt = plaintext.getBytes(UTF8);
        // Encrypt with GCM
        byte[] ct_gcm = gcm_encrypter.doFinal(pt);
        String ct_gcm_b64 = encodeBase64String(ct_gcm);
        System.err.println("ct_gcm_b64: " + ct_gcm_b64);
        String expectedCipherText = "/5VzCXhWXFY+fvThiceoLjMiQI5y4G08mOi+wjhIet6MGOJ8GBy2Ixiy";
        String expectedTag = "FzYYyfQmuL9izA5Ez6T4bw==";
        assertEquals(expectedCipherText + expectedTag, ct_gcm_b64);
        System.out.println("pt.length=" + pt.length + ", ct_gcm.length="
                           + ct_gcm.length);
        assertTrue(ct_gcm.length == pt.length + 16);
        // Encrypt with CTR
        CipherLite ctr_encrypter = gcm_encrypter.createAuxiliary(0);
        byte[] ct_ctr = ctr_encrypter.doFinal(pt);
        String ct_ctr_b64 = encodeBase64String(ct_ctr);
        System.err.println("ct_ctr_b64: " + ct_ctr_b64);
        assertEquals(expectedCipherText, ct_ctr_b64);
        // Decrypt GCM ciphertext with CTR
        CipherLite gcm_decrypter = gcm_encrypter.createInverse();
        CipherLite ctr_decrypter = gcm_decrypter.createAuxiliary(0);
        byte[] decrypted = ctr_decrypter.doFinal(ct_gcm, 0, ct_gcm.length - 16);
        String decryptedStr = new String(decrypted, UTF8);
        System.out.println(decryptedStr);
        assertEquals(plaintext, decryptedStr);
        // Decrypt GCM ciphertext with CTR from 17th byte
        int offset = 16;
        ctr_decrypter = gcm_decrypter.createAuxiliary(offset);
        decrypted = ctr_decrypter.doFinal(ct_gcm, offset, ct_gcm.length - 16
                                                          - offset);
        decryptedStr = new String(decrypted, UTF8);
        System.out.println(decryptedStr);
        // "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        assertEquals(plaintext.substring(offset), decryptedStr);
        // Decrypt GCM ciphertext with CTR from 33th byte
        offset = 32;
        ctr_decrypter = gcm_decrypter.createAuxiliary(offset);
        decrypted = ctr_decrypter.doFinal(ct_gcm, offset, ct_gcm.length - 16
                                                          - offset);
        decryptedStr = new String(decrypted, UTF8);
        System.out.println(decryptedStr);
        // "QRSTUVWXYZ"
        assertEquals(plaintext.substring(offset), decryptedStr);
    }

    @Test
    public void testIncrementBlocks() {
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            int start = rand.nextInt(Integer.MAX_VALUE);
            int delta = rand.nextInt(Integer.MAX_VALUE);
            ByteBuffer bb = ByteBuffer.allocate(16);
            bb.putLong(0).putLong(start);
            ContentCryptoScheme.incrementBlocks(bb.array(), delta);
            bb.rewind();
            assertTrue("Leftmost 8 bytes remain as zeros", 0 == bb.getLong());
            long sum = (long) start + (long) delta;
            // System.out.println("start=" + start + ", delta=" + delta +
            // ", sum=" + sum);
            assertTrue("Rightmost 4 bytes have been incremented by delta",
                       sum == bb.getLong());
        }
    }

    @Test
    public void testLimit_incrementBlocks() {
        long delta = MAX_GCM_BLOCKS;
        ByteBuffer bb = ByteBuffer.allocate(16);
        ContentCryptoScheme.incrementBlocks(bb.array(), delta);
        bb.rewind();
        assertTrue(0 == bb.getLong());
        assertTrue(MAX_GCM_BLOCKS == bb.getLong());
        bb.rewind();
        bb.putLong(0);
        bb.putLong(1);
        try {
            ContentCryptoScheme.incrementBlocks(bb.array(), delta);
            fail("max increment exceeded");
        } catch (IllegalStateException expected) {
            // Ignored or expected.
        }
    }

    @Test
    public void testLimit2_incrementBlocks() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        long rlong = 0xFF00000000L;
        System.out.println("         rlong: " + hexOf(rlong));
        System.out.println("MAX_GCM_BLOCKS: " + hexOf(MAX_GCM_BLOCKS));
        long expected = rlong + MAX_GCM_BLOCKS;
        System.out.println("      expected: " + hexOf(expected));
        assertTrue("original bits in rlong must be intact",
                   rlong == (expected & rlong));
        bb = ByteBuffer.allocate(16);
        bb.putLong(0).putLong(rlong);
        ContentCryptoScheme.incrementBlocks(bb.array(), MAX_GCM_BLOCKS);
        bb.rewind();
        long result = bb.asLongBuffer().get(1);
        assertTrue(expected == result);
        // increment by zero is fine
        ContentCryptoScheme.incrementBlocks(bb.array(), 0);
        bb.rewind();
        result = bb.asLongBuffer().get(1);
        assertTrue(expected == result);
        // increment by 1 would exceed the max
        try {
            ContentCryptoScheme.incrementBlocks(bb.array(), 1);
            fail("max increment exceeded");
        } catch (IllegalStateException good) {
            // Ignored or expected.
        }
    }

    @Test
    public void testDeltaLimit_incrementBlocks() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        try {
            ContentCryptoScheme.incrementBlocks(bb.array(), MAX_GCM_BLOCKS + 1);
            fail("max delta exceeded");
        } catch (IllegalStateException good) {
            // Ignored or expected.
        }
    }

    private String hexOf(long n) {
        return encodeHexString(ByteBuffer.allocate(8).putLong(n).array());
    }

    @Test
    public void testCtrEncryptWithOffset() throws Exception {
        String plaintext = "1234567890123456ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        byte[] pt = plaintext.getBytes(UTF8);
        String expectedCipherText =
                StringUtils.upperCase("ff95730978565c563e7ef4e189c7a82e3322408e72e06d3c98e8bec238487ade8c18e27c181cb62318b2");
        // Encrypt with CTR
        CipherLite cipher_ctr = CryptoTestUtils.createTestCipher(AES_CTR,
                                                                 AES_GCM.getIvLengthInBytes(), Cipher.ENCRYPT_MODE);
        byte[] ct_ctr = cipher_ctr.doFinal(pt);
        String ct_ctr_str = encodeHexString(ct_ctr);
        System.err.println("ct_ctr_str: " + ct_ctr_str);
        assertEquals(expectedCipherText, ct_ctr_str);

        CipherLite cipher_ctr_with_offset = CryptoTestUtils
                .createTestCipherWithStartingBytePos(AES_CTR,
                                                     AES_GCM.getIvLengthInBytes(), Cipher.ENCRYPT_MODE, 16);
        // Cipher cipher_ctr_with_offset = AES_GCM.createAuxillaryCipher(cek,
        // iv, cipherMode, securityProvider, startingBytePos)
        byte[] ct_ctr_offset = cipher_ctr_with_offset.doFinal(pt, 16,
                                                              pt.length - 16);
        String ct_ctr_offset_str = encodeHexString(ct_ctr_offset);
        System.err.println("ct_ctr_offset_str: " + ct_ctr_offset_str);
        assertEquals(ct_ctr_str.substring(16 * 2), ct_ctr_offset_str);
    }
}
