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

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.util.Base64;

// https://github.com/aws/aws-sdk-android/issues/15
public class AndriodSdkIssue15dTestLocal {
    public static void enableBouncyCastle() throws Exception {
        @SuppressWarnings("unchecked")
        Class<Provider> c = (Class<Provider>) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
        Provider provider = c.newInstance();
        Security.addProvider(provider);
    }

    @Test
    public void test() throws Exception {
        System.out.println(System.getProperties());
        PublicKey publicKey = loadPublicKey(null);
        PrivateKey privatekey = loadPrivateKey(null);

        byte b;
        for (int i = 0; i < 256; i++) {
            b = (byte) i;
            byte[] bytes = new byte[32];
            bytes[0] = b;
            bytes[1] = 1;
            encryptDecrypt(publicKey, privatekey, new SecretKeySpec(bytes, "AES"), null);
        }
    }

    @Test
    public void testBC() throws Exception {
        enableBouncyCastle();
        PublicKey publicKey = loadPublicKey("BC");
        PrivateKey privatekey = loadPrivateKey("BC");
        byte b;
        for (int i = 0; i < 256; i++) {
            b = (byte) i;
            byte[] bytes = new byte[32];
            bytes[1] = 1;
            bytes[0] = b;
            encryptDecrypt(publicKey, privatekey, new SecretKeySpec(bytes, "AES"), "BC");
        }
    }

    private PublicKey loadPublicKey(String provider) throws Exception {
        PublicKey publicKey;
        InputStream is = getClass().getResourceAsStream("raw/publickey");
        byte[] bytes = IOUtils.toByteArray(is);
        is.close();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        System.out.println(spec.getFormat());
        KeyFactory kf = provider == null ? KeyFactory.getInstance("RSA")
                                         : KeyFactory.getInstance("RSA", provider);
        publicKey = kf.generatePublic(spec);
        System.err.println("publicKey.getEncoded().length: " + publicKey.getEncoded().length);
        return publicKey;
    }

    private PrivateKey loadPrivateKey(String provider) throws Exception {
        PrivateKey privatekey;
        InputStream is = getClass().getResourceAsStream("raw/privatekey");
        byte[] bytes = IOUtils.toByteArray(is);
        is.close();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        System.out.println(spec.getFormat());
        KeyFactory kf = provider == null ? KeyFactory.getInstance("RSA")
                                         : KeyFactory.getInstance("RSA", provider);
        privatekey = kf.generatePrivate(spec);
        System.err.println("privatekey.getEncoded().length: " + privatekey.getEncoded().length);
        return privatekey;
    }

    private void encryptDecrypt(PublicKey publicKey, PrivateKey privatekey,
                                SecretKey secretKey, String provider) throws Exception {
        String algo = publicKey.getAlgorithm();
        Assert.assertEquals("RSA", algo);
        Cipher cipher = provider == null ? Cipher.getInstance(algo) : Cipher.getInstance(algo, provider);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] origtext = secretKey.getEncoded();
        Assert.assertTrue(origtext.length == 32);
        byte[] ciphertext = cipher.doFinal(origtext);
        System.out.println("ciphertext: " + Base64.encodeAsString(ciphertext));
        Assert.assertTrue(ciphertext.length > 32);
        Assert.assertFalse(Arrays.equals(origtext, ciphertext));
        algo = privatekey.getAlgorithm();
        Assert.assertEquals("RSA", algo);
        cipher = provider == null ? Cipher.getInstance(algo) : Cipher.getInstance(algo, provider);
        cipher.init(Cipher.DECRYPT_MODE, privatekey);
        byte[] plaintext = cipher.doFinal(ciphertext);
        if ("BC".equals(provider) && secretKey.getEncoded()[0] == 0) {
            // Buggy BC implementation for decryption, leading to only 31 bytes!
            Assert.assertTrue(plaintext.length == 31);
        } else {
            Assert.assertTrue(plaintext.length == 32);
            Assert.assertTrue(Arrays.equals(origtext, plaintext));
        }
    }
}