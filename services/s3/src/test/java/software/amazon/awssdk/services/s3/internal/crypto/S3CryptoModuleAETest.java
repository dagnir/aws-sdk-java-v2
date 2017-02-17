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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_GCM;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.decodeBase64;
import static software.amazon.awssdk.services.s3.model.CryptoMode.AuthenticatedEncryption;

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

public class S3CryptoModuleAETest extends S3CryptoModuleTestBase {
    @Override
    protected S3CryptoModuleBase<?> createS3CryptoModule(S3Direct s3,
                                                         EncryptionMaterialsProvider provider,
                                                         CryptoConfiguration cryptoConfig) {
        return new S3CryptoModuleAE(s3, provider,
                                    cryptoConfig.clone()
                                                .withCryptoMode(AuthenticatedEncryption)
                                                .readOnly());
    }

    protected int getExpectedCipherTextByteLength(int plaintextByteLength) {
        return plaintextByteLength + 16;
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
                // plaintext 100 + 16 bytes tag
                assertTrue(100 + 16 == md.getContentLength());
                Map<String, String> userMD = md.getUserMetadata();
                String plaintextLen = userMD.get(Headers.UNENCRYPTED_CONTENT_LENGTH);
                assertEquals("100", plaintextLen);
                String b64cekWrapped = userMD.get(Headers.CRYPTO_KEY_V2);
                assertNotNull(b64cekWrapped);
                byte[] cekWrapped = decodeBase64(b64cekWrapped);
                assertTrue(40 == cekWrapped.length);
                String b64iv = userMD.get(Headers.CRYPTO_IV);
                assertNotNull(b64iv);
                byte[] iv = decodeBase64(b64iv);
                assertTrue(AES_GCM.getIvLengthInBytes() == iv.length);
                assertEquals(AES_GCM.getCipherAlgorithm(), userMD.get(Headers.CRYPTO_CEK_ALGORITHM));
                assertEquals(S3KeyWrapScheme.AESWrap, userMD.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
                assertEquals(String.valueOf(AES_GCM.getTagLengthInBits()), userMD.get(Headers.CRYPTO_TAG_LENGTH));
                return;
            }
        };
    }

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
                // hchar: not sure why the x-amz-unencrypted-content-length is
                // repeated in the user meta data of the instruction file
                // but that's how it is currently
                if (count == 1) {
                    // Verify the content S3 object
                    String plaintextLen = userMD.get(Headers.UNENCRYPTED_CONTENT_LENGTH);
                    assertEquals("100", plaintextLen);
                    // plaintext 100 + 16 bytes tag
                    assertTrue(100 + 16 == md.getContentLength());
                    assertTrue(userMD.size() == 1);
                    assertTrue(req.getKey().equals("key"));
                } else if (count == 2) {
                    // Verify the instruction file S3 object
                    assertTrue(md.getContentLength() > 0);
                    assertNotNull(userMD.get(Headers.CRYPTO_INSTRUCTION_FILE));
                    assertTrue(req.getKey().equals("key.instruction"));
                    assertTrue(userMD.size() == 1);
                    String json = IOUtils.toString(req.getInputStream(), "UTF-8");
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = Jackson.fromJsonString(json, Map.class);
                    String b64cekWrapped = map.get(Headers.CRYPTO_KEY_V2);
                    assertNotNull(b64cekWrapped);
                    byte[] cekWrapped = decodeBase64(b64cekWrapped);
                    assertTrue(40 == cekWrapped.length);
                    String b64iv = map.get(Headers.CRYPTO_IV);
                    assertNotNull(b64iv);
                    byte[] iv = decodeBase64(b64iv);
                    assertTrue(AES_GCM.getIvLengthInBytes() == iv.length);
                    assertEquals("{}", map.get(Headers.MATERIALS_DESCRIPTION));
                    assertEquals(AES_GCM.getCipherAlgorithm(),
                                 map.get(Headers.CRYPTO_CEK_ALGORITHM));
                    assertEquals(S3KeyWrapScheme.AESWrap,
                                 map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
                    assertEquals(String.valueOf(AES_GCM.getTagLengthInBits()),
                                 map.get(Headers.CRYPTO_TAG_LENGTH));
                } else {
                    fail();
                }
                return;
            }
        };
    }

    @Override
    protected void verifyEncryptionInstruction(
            ContentCryptoMaterial inst) {
        ContentCryptoScheme scheme = inst.getContentCryptoScheme();
        assertTrue(AES_GCM.getBlockSizeInBytes() == scheme.getBlockSizeInBytes());
        assertEquals(AES_GCM.getCipherAlgorithm(), scheme.getCipherAlgorithm());
        assertTrue(AES_GCM.getIvLengthInBytes() == scheme.getIvLengthInBytes());
        assertTrue(AES_GCM.getTagLengthInBits() == scheme.getTagLengthInBits());
        assertEquals(AES_GCM.getKeyGeneratorAlgorithm(), scheme.getKeyGeneratorAlgorithm());
        assertTrue(AES_GCM.getKeyLengthInBits() == scheme.getKeyLengthInBits());
        assertEquals(AES_GCM.getSpecificCipherProvider(), scheme.getSpecificCipherProvider());

        String kwa = inst.getKeyWrappingAlgorithm();
        assertEquals(S3KeyWrapScheme.AESWrap, kwa);
        byte[] encryptedCEK = inst.getEncryptedCek();
        assertTrue(encryptedCEK.length == 40);

        CipherLite cipher = inst.getCipherLite();
        assertEquals(scheme.getCipherAlgorithm(), cipher.getCipherAlgorithm());
        assertTrue(scheme.getBlockSizeInBytes() == cipher.getBlockSize());
        assertTrue(scheme.getIvLengthInBytes() == cipher.getIv().length);
        assertEquals(scheme.getSpecificCipherProvider(), cipher.getCipherProvider().getName());

        CipherLite w = inst.getCipherLite();
        assertEquals(scheme.getKeyGeneratorAlgorithm(), w.getSecretKeyAlgorithm());
    }
}
