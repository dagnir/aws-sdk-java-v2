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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;


public class TT0031998440Test {
    @Test
    public void test() throws Exception {
        // Generate a key pair
        KeyPair pair = CryptoTestUtils.generateKeyPair("RSA", 1024);
        // Generate a secret key
        SecretKey k = CryptoTestUtils.generateSecretKey("AES", 256);
        Cipher c = Cipher.getInstance(pair.getPublic().getAlgorithm());
        c.init(Cipher.ENCRYPT_MODE, pair.getPublic());
        // Encrypt the secret key with the public key
        byte[] ct = c.doFinal(k.getEncoded());
        Cipher d = Cipher.getInstance(pair.getPrivate().getAlgorithm());
        d.init(Cipher.DECRYPT_MODE, pair.getPrivate());
        // Decrypt the encrypted secret key with the private key
        byte[] pt = d.doFinal(ct);
        assertTrue(Arrays.equals(k.getEncoded(), pt));

        // Generate another key pair
        KeyPair pair2 = CryptoTestUtils.generateKeyPair("RSA", 1024);
        Cipher d2 = Cipher.getInstance(pair2.getPrivate().getAlgorithm());
        d2.init(Cipher.DECRYPT_MODE, pair2.getPrivate());
        try {
            // Decrypt the encrypted secret key with a key pair different than the one used to encrypt it 
            byte[] pt2 = d2.doFinal(ct);
            fail(); // should never succeeds
        } catch (BadPaddingException ex) {
            // Expected failure
            ex.printStackTrace(System.err);
        }
    }
}
