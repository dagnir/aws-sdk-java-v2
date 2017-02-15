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
import static software.amazon.awssdk.util.IOUtils.release;

import java.io.File;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.internal.S3Direct;
import software.amazon.awssdk.services.s3.model.AbstractPutObjectRequest;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StaticEncryptionMaterialsProvider;

public abstract class S3CryptoModuleTestBase {
    private S3Direct s3 = new S3DirectMock();

    private static SecretKey generateOneTimeUseSymmetricKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance(JceEncryptionConstants.SYMMETRIC_KEY_ALGORITHM);
            generator.init(JceEncryptionConstants.SYMMETRIC_KEY_LENGTH, new SecureRandom());
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new AmazonClientException(
                    "Unable to generate envelope symmetric key:" + e.getMessage(), e);
        }
    }

    /**
     * Tests the method
     * {@link S3CryptoModule#putObjectSecurely(PutObjectRequest).
     */
    @Test
    public void putObjectSecurely() throws Exception {
        // Generate a KEK
        SecretKey kek = generateOneTimeUseSymmetricKey();
        assertEquals("AES", kek.getAlgorithm());
        EncryptionMaterials materials = new EncryptionMaterials(kek);
        EncryptionMaterialsProvider provider = new StaticEncryptionMaterialsProvider(
                materials);
        S3CryptoModule<?> module = createS3CryptoModule(
                mockS3Direct_putObjectSecurely(), provider,
                new CryptoConfiguration());
        int plaintextLen = 100;
        // Generate some 100 random ASCII content
        File file = CryptoTestUtils.generateRandomAsciiFile(plaintextLen);
        PutObjectRequest putObjectRequest = new PutObjectRequest("bucketname",
                                                                 "key", file);
        // This call will pass the fully constructed put request to the
        // mocked S3Direct which can then verify the results
        module.putObjectSecurely(putObjectRequest);
    }

    @Test
    public void putObjectSecurelyViaInstructionFile() throws Exception {
        // Generate a KEK
        SecretKey kek = generateOneTimeUseSymmetricKey();
        assertEquals("AES", kek.getAlgorithm());
        EncryptionMaterials materials = new EncryptionMaterials(kek);
        EncryptionMaterialsProvider provider = new StaticEncryptionMaterialsProvider(
                materials);
        S3CryptoModule<?> module = createS3CryptoModule(
                mockS3Direct_putObjectSecurelyViaInstructionFile(),
                provider,
                new CryptoConfiguration()
                        .withStorageMode(CryptoStorageMode.InstructionFile));
        int plaintextLen = 100;
        // Generate some 100 random ASCII content
        File file = CryptoTestUtils.generateRandomAsciiFile(plaintextLen);
        PutObjectRequest putObjectRequest = new PutObjectRequest("bucketname",
                                                                 "key", file);
        // This call will pass the fully constructed put request to the
        // mocked S3Direct which can then verify the results
        module.putObjectSecurely(putObjectRequest);
    }

    /**
     * Returns the mocked {@link S3Direct} that is used for testing the method
     * {@link S3CryptoModule#putObjectSecurely(PutObjectRequest).
     */
    protected abstract S3Direct mockS3Direct_putObjectSecurely();

    protected abstract S3Direct mockS3Direct_putObjectSecurelyViaInstructionFile();

    /**
     * Tests the method {@link S3CryptoModuleBase#wrapWithCipher(AbstractPutObjectRequest,
     * ContentCryptoMaterial)}
     */
    @Test
    public void wrapWithCipher() throws Exception {
        // Generate a KEK
        SecretKey kek = generateOneTimeUseSymmetricKey();
        assertEquals("AES", kek.getAlgorithm());
        EncryptionMaterials materials = new EncryptionMaterials(kek);
        EncryptionMaterialsProvider provider = new StaticEncryptionMaterialsProvider(
                materials);
        S3CryptoModuleBase<?> module = createS3CryptoModule(s3, provider,
                                                            new CryptoConfiguration());
        int plaintextLen = 100;
        // Generate some 100 random ASCII content
        File file = CryptoTestUtils.generateRandomAsciiFile(plaintextLen);
        PutObjectRequest putObjectRequest = new PutObjectRequest("bucketname",
                                                                 "key", file);
        ContentCryptoMaterial cekMaterial = module.createContentCryptoMaterial(putObjectRequest);
        verifyEncryptionInstruction(cekMaterial);
        PutObjectRequest request = module.wrapWithCipher(putObjectRequest,
                                                         cekMaterial);
        // At this point the request is now wrapped with a cipher.
        // Let's retrieve the cipher text.
        InputStream is = request.getInputStream();
        byte[] ct = IOUtils.toByteArray(is);
        is.close();
        // Check the ciphertext length
        int expectedCtLength = getExpectedCipherTextByteLength(plaintextLen);
        assertTrue("Unexpected ciphertext length: " + ct.length
                   + ", expected: " + expectedCtLength,
                   ct.length == expectedCtLength);
        // Decrypt and check compare to the original plaintext
        CipherLite decrypter = cekMaterial.getCipherLite().createInverse();
        byte[] pt = decrypter.doFinal(ct);
        assertEquals(new String(pt, "UTF-8"), FileUtils.readFileToString(file));
        release(request.getInputStream(), null);
    }

    protected abstract S3CryptoModuleBase<?> createS3CryptoModule(S3Direct s3,
                                                                  EncryptionMaterialsProvider provider,
                                                                  CryptoConfiguration cryptoConfig);

    protected abstract int getExpectedCipherTextByteLength(
            int plaintextByteLength);

    protected abstract void verifyEncryptionInstruction(
            ContentCryptoMaterial encryptionInstructionV2);
}
