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
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.encodeHexString;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import javax.crypto.Cipher;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class GCMEncrypterSmallTest {
    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }


    @Test
    public void testSmall() throws Exception {
        int[] lens = {16, 32, 48, 64};
        for (int len : lens) {
            testSmall(len);
        }
    }

    private void testSmall(int inputLen) throws Exception {
        CipherLite w1 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        CipherLite w2 = ContentCryptoScheme.AES_GCM.createCipherLite(
                CryptoTestUtils.getTestSecretKey(), new byte[12],
                Cipher.ENCRYPT_MODE);
        GcmCipherLite e1 = (GcmCipherLite) w1;
        GcmCipherLite e2 = (GcmCipherLite) w2;
        File file = CryptoTestUtils.generateRandomAsciiFile(100);
        byte[] input = IOUtils.toByteArray(new FileInputStream(file));
        //        byte[] input = "1234567890123456ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes(UTF8);
        int remaining = input.length;
        System.err.println("Plaintext of length=" + input.length);
        int inputOffset = 0;
        while (remaining > 0) {
            System.err.println("====================");
            long marked = e1.mark();
            System.err.println("After mark: remaining: " + remaining
                               + ", inputOffset=" + inputOffset + ", inputLen=" + inputLen
                               + ", markedCount=" + e1.getMarkedCount()
                               + ", currentCount=" + e1.getCurrentCount()
                               + ", encryptedCount=" + e1.getOutputByteCount());

            byte[] ct1;
            int len = Math.min(inputLen, remaining);
            try {
                ct1 = e1.update(input, inputOffset, len);
                if (ct1 == null) {
                    byte[] ct2 = e2.update(input, inputOffset, len);
                    assertTrue(ct1 == ct2);
                    break;
                }
            } catch (RuntimeException ex) {
                System.err.println(ex);
                throw ex;
            }
            System.out.println("ct1: " + encodeHexString(ct1));
            System.err.println("After update: markedCount="
                               + e1.getMarkedCount() + ", currentCount="
                               + e1.getCurrentCount() + ", encryptedCount="
                               + e1.getOutputByteCount());
            System.err.println("ct1.length=" + ct1.length);
            byte[] ct2 = e2.update(input, inputOffset, len);
            assertTrue(Arrays.equals(ct1, ct2));
            // Reset and re-encrypt on e1
            e1.reset();
            System.err.println("After reset: markedCount="
                               + e1.getMarkedCount() + ", currentCount="
                               + e1.getCurrentCount() + ", encryptedCount="
                               + e1.getOutputByteCount());
            int inputLenAdjusted = (int) (len + (inputOffset - marked));
            byte[] ct3 = e1.update(input, (int) marked, inputLenAdjusted);
            System.out.println("ct3: " + encodeHexString(ct3));
            System.err.println("After update: markedCount="
                               + e1.getMarkedCount() + ", currentCount="
                               + e1.getCurrentCount() + ", encryptedCount="
                               + e1.getOutputByteCount());
            assertTrue(Arrays.equals(ct1, ct3));

            inputOffset += ct1.length;
            remaining -= ct1.length;
        }
        e1.mark();
        byte[] final1 = e1.doFinal();
        byte[] final2 = e2.doFinal();
        assertTrue(Arrays.equals(final1, final2));

        // Reset and re-final on e1
        e1.reset();
        byte[] final3 = e1.doFinal();
        assertTrue(Arrays.equals(final1, final3));
    }
}
