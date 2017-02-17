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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_CBC;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.decodeBase64;
import static software.amazon.awssdk.services.s3.model.CryptoMode.EncryptionOnly;

import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.internal.S3Direct;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.util.json.Jackson;

public class S3CryptoModuleEOTest extends S3CryptoModuleTestBase {

    @Override
    protected S3CryptoModuleBase<?> createS3CryptoModule(S3Direct s3,
                                                         EncryptionMaterialsProvider provider,
                                                         CryptoConfiguration cryptoConfig) {
        return new S3CryptoModuleEO(s3, provider,
                                    cryptoConfig.clone()
                                                .withCryptoMode(EncryptionOnly)
                                                .readOnly());
    }

    protected S3Direct mockS3Direct_putObjectSecurely() {
        // A mocked S3Direct object to verify the put request
        return new S3DirectMock() {
            @Override
            public PutObjectResult putObject(PutObjectRequest req) {
                verifyRequest(req);
                return null;
            }

            private void verifyRequest(PutObjectRequest req) {
                ObjectMetadata md = req.getMetadata();
                // plaintext 100
                assertTrue(getExpectedCipherTextByteLength(100) == md.getContentLength());
                Map<String, String> userMD = md.getUserMetadata();
                String plaintextLen = userMD.get(Headers.UNENCRYPTED_CONTENT_LENGTH);
                assertEquals("100", plaintextLen);
                String b64cekEncrypted = userMD.get(Headers.CRYPTO_KEY);
                assertNotNull(b64cekEncrypted);
                byte[] cekWrapped = decodeBase64(b64cekEncrypted);
                assertTrue(48 == cekWrapped.length);
                String b64iv = userMD.get(Headers.CRYPTO_IV);
                assertNotNull(b64iv);
                byte[] iv = decodeBase64(b64iv);
                assertTrue(AES_CBC.getIvLengthInBytes() == iv.length);
                assertNull(userMD.get(Headers.CRYPTO_CEK_ALGORITHM));
                assertNull(userMD.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
                assertNull(userMD.get(Headers.CRYPTO_TAG_LENGTH));
                return;
            }
        };
    }

    @Override
    protected S3Direct mockS3Direct_putObjectSecurelyViaInstructionFile() {
        // A mocked S3Direct object to verify the put request
        return new S3DirectMock() {
            private int count;

            @Override
            public PutObjectResult putObject(PutObjectRequest req) {
                try {
                    verifyRequest(req);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    fail();
                }
                return null;
            }

            private void verifyRequest(PutObjectRequest req) throws IOException {
                count++;
                System.out.println("count: " + count);
                ObjectMetadata md = req.getMetadata();
                Map<String, String> userMD = md.getUserMetadata();
                // The x-amz-unencrypted-content-length seems erroneously
                // repeated in the user meta data of the instruction file.
                // This is now removed from the V2 client.
                String plaintextLen = userMD.get(Headers.UNENCRYPTED_CONTENT_LENGTH);
                //                assertEquals("100", plaintextLen);
                //                assertNull(plaintextLen);
                if (count == 1) {
                    // Verify the content S3 object
                    // plaintext 100
                    assertTrue(getExpectedCipherTextByteLength(100) == md.getContentLength());
                    assertTrue(userMD.size() == 1);
                    assertTrue(req.getKey().equals("key"));
                } else if (count == 2) {
                    // Verify the instruction file S3 object
                    assertTrue(md.getContentLength() > 0);
                    assertNotNull(userMD.get(Headers.CRYPTO_INSTRUCTION_FILE));
                    assertTrue(req.getKey().equals("key.instruction"));
                    //                    assertTrue(userMD.size() == 2);
                    assertTrue(userMD.size() == 1);
                    String json = IOUtils.toString(req.getInputStream(), "UTF-8");
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = Jackson.fromJsonString(json, Map.class);
                    String b64cekWrapped = map.get(Headers.CRYPTO_KEY);
                    assertNotNull(b64cekWrapped);
                    byte[] cekWrapped = decodeBase64(b64cekWrapped);
                    assertTrue(48 == cekWrapped.length);
                    String b64iv = map.get(Headers.CRYPTO_IV);
                    assertNotNull(b64iv);
                    byte[] iv = decodeBase64(b64iv);
                    assertTrue(AES_CBC.getIvLengthInBytes() == iv.length);
                    assertEquals("{}", map.get(Headers.MATERIALS_DESCRIPTION));
                    assertNull(map.get(Headers.CRYPTO_CEK_ALGORITHM));
                    assertNull(map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
                    assertNull(map.get(Headers.CRYPTO_TAG_LENGTH));
                } else {
                    fail();
                }
                return;
            }
        };
    }

    protected int getExpectedCipherTextByteLength(int plaintextByteLength) {
        int leftOver = plaintextByteLength - plaintextByteLength / 16 * 16;
        int padding = 16 - leftOver;
        return plaintextByteLength + padding;
    }

    @Override
    protected void verifyEncryptionInstruction(
            ContentCryptoMaterial inst) {
        // verify the crypto scheme
        ContentCryptoScheme scheme = inst.getContentCryptoScheme();
        assertTrue(AES_CBC.getBlockSizeInBytes() == scheme.getBlockSizeInBytes());
        assertEquals(AES_CBC.getCipherAlgorithm(), scheme.getCipherAlgorithm());
        assertTrue(AES_CBC.getIvLengthInBytes() == scheme.getIvLengthInBytes());
        assertTrue(AES_CBC.getTagLengthInBits() == scheme.getTagLengthInBits());
        assertEquals(AES_CBC.getKeyGeneratorAlgorithm(), scheme.getKeyGeneratorAlgorithm());
        assertTrue(AES_CBC.getKeyLengthInBits() == scheme.getKeyLengthInBits());
        assertTrue(AES_CBC.getSpecificCipherProvider() == scheme.getSpecificCipherProvider());
        assertNull(AES_CBC.getSpecificCipherProvider());

        // verify the key wrapping scheme
        String kwa = inst.getKeyWrappingAlgorithm();
        //        assertEquals(S3KeyWrapScheme.AESWrap, kwa);
        assertNull(kwa);

        // verify the key wrapped
        byte[] encryptedCEK = inst.getEncryptedCek();
        //        assertTrue(""+encryptedCEK.length, encryptedCEK.length == 40);
        assertTrue("" + encryptedCEK.length, encryptedCEK.length == 48);

        // verify the cipher
        CipherLite cipher = inst.getCipherLite();
        assertEquals(scheme.getCipherAlgorithm(), cipher.getCipherAlgorithm());
        assertTrue(scheme.getBlockSizeInBytes() == cipher.getBlockSize());
        assertTrue(scheme.getIvLengthInBytes() == cipher.getIv().length);
        // default implementation of the JDK
        assertEquals("SunJCE", cipher.getCipherProvider().getName());

        // verify the CEK
        CipherLite w = inst.getCipherLite();
        assertEquals(scheme.getKeyGeneratorAlgorithm(), w.getSecretKeyAlgorithm());
    }
}
