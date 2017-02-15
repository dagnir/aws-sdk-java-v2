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
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_GCM;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.encodeHexString;
import static software.amazon.awssdk.util.StringUtils.UTF8;

import javax.crypto.Cipher;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.util.StringUtils;

public class Encrypt20Test {
    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test
    public void testCTREncryptWithOffset() throws Exception {
        String plaintext = "1234567890123456" + // 16
                           "ABCDEFGHIJKLMNOP" + // 32
                           "QRSTUVWXYZabcdef" + // 48
                           //                            "ghijklmnopqrstuv" +
                           "wxyz";                 // 52
        System.out.println("plaintext.length()=" + plaintext.length());
        byte[] pt = plaintext.getBytes(UTF8);
        String expectedCipherText = StringUtils.upperCase("ff95730978565c563e7ef4e189c7a82e3322408e72e06d3c98e8bec238487ade8c18e27c181cb62318b23846246853913039b29bb20e2199942ab8ac7c3abc1814d5ffe6");
        //        String expectedCipherText = "ff95730978565c563e7ef4e189c7a82e3322408e72e06d3c98e8bec238487ade8c18e27c181cb62318b23846246853912029a28bead7125e0e0c6c91d8784f69a7bcf609cd20e17b219ad1a3c4d384e4f7d12d75";
        // Encrypt with GCM
        CipherLite gcm = CryptoTestUtils.createTestCipher(AES_GCM,
                                                          AES_GCM.getIVLengthInBytes(), Cipher.ENCRYPT_MODE);
        byte[] ct_ctr = gcm.doFinal(pt);
        String ct_ctr_str = encodeHexString(ct_ctr);
        System.err.println("ct_ctr_str : " + ct_ctr_str);
        assertEquals(expectedCipherText, ct_ctr_str);
        {
            GCMCipherLite gcm2 = (GCMCipherLite) CryptoTestUtils.createTestCipher(AES_GCM,
                                                                                  AES_GCM.getIVLengthInBytes(), Cipher.ENCRYPT_MODE);
            // ff95730978565c563e7ef4e189c7a82e
            long marked = gcm2.mark();
            byte[] ba = gcm2.update(pt, 0, 20);
            String hex = encodeHexString(ba);
            System.err.println("  hex[0-15]: " + hex);
            assertEquals(ct_ctr_str.substring(0, 32), hex);

            gcm2.reset();
            ba = gcm2.update(pt, 0, 20);
            hex = encodeHexString(ba);
            System.err.println("  hex[0-15]: " + hex);
            assertEquals(ct_ctr_str.substring(0, 32), hex);

            // next 16-31 bytes
            // 3322408e72e06d3c98e8bec238487ade
            int len = 20;
            marked = gcm2.mark();
            ba = gcm2.update(pt, 20, len);
            hex = encodeHexString(ba);
            System.err.println(" hex[16-31]: " + hex);
            assertEquals(ct_ctr_str.substring(32, 32 + 16 * 2), hex);

            gcm2.reset();
            ba = gcm2.update(pt, (int) marked, len);
            hex = encodeHexString(ba);
            System.err.println(" hex[16-31]: " + hex);
            assertEquals(ct_ctr_str.substring(32, 32 + 16 * 2), hex);

            // next 32-47 bytes
            marked = gcm2.mark();
            ba = gcm2.update(pt, 40, 12);
            hex = encodeHexString(ba);
            System.err.println(" hex[32-47]: " + hex);
            assertEquals(ct_ctr_str.substring(64, 64 + 16 * 2), hex);

            gcm2.reset();
            ba = gcm2.update(pt, (int) marked, 16);
            hex = encodeHexString(ba);
            System.err.println(" hex[32-47]: " + hex);
            assertEquals(ct_ctr_str.substring(64, 64 + 16 * 2), hex);

            // next 48-52 bytes
            marked = gcm2.mark();
            ba = gcm2.doFinal();
            hex = encodeHexString(ba);
            System.err.println(" hex[48-52]: " + hex);
            assertEquals(ct_ctr_str.substring(48 * 2), hex);

            gcm2.reset();
            ba = gcm2.doFinal(pt, (int) marked, 4);
            hex = encodeHexString(ba);
            System.err.println(" hex[48-52]: " + hex);
            assertEquals(ct_ctr_str.substring(48 * 2), hex);
        }
    }
}
