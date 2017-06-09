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

import static software.amazon.awssdk.services.s3.internal.crypto.KmsSecuredCek.isKmsKeyWrapped;
import static software.amazon.awssdk.services.s3.model.ExtraMaterialsDescription.NONE;
import static software.amazon.awssdk.util.Throwables.failure;
import static software.amazon.awssdk.utils.BinaryUtils.copyAllBytesFrom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.services.kms.KMSClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.KeyWrapException;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsAccessor;
import software.amazon.awssdk.services.s3.model.ExtraMaterialsDescription;
import software.amazon.awssdk.services.s3.model.KmsEncryptionMaterials;
import software.amazon.awssdk.services.s3.model.MaterialsDescriptionProvider;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.util.json.Jackson;
import software.amazon.awssdk.utils.Base64Utils;

/**
 * Cryptographic material used for client-side content encrypt/decryption in S3.
 * This includes the randomly generated one-time secured CEK
 * (content-encryption-key) and the respective key wrapping algorithm, if any,
 * and the cryptographic scheme in use.
 */
final class ContentCryptoMaterial {
    // null if cek is not secured via key wrapping
    private final String keyWrappingAlgorithm;
    private final CipherLite cipherLite;

    private final Map<String, String> kekMaterialsDescription;
    private final byte[] encryptedCek;

    ContentCryptoMaterial(Map<String, String> kekMaterialsDescription,
                          byte[] encryptedCek,
                          String keyWrappingAlgorithm,
                          CipherLite cipherLite) {
        this.cipherLite = cipherLite;
        this.keyWrappingAlgorithm = keyWrappingAlgorithm;
        this.encryptedCek = encryptedCek.clone();
        this.kekMaterialsDescription = kekMaterialsDescription;
    }

    /**
     * Returns the corresponding kek material description from the given json;
     * or null if the input is null.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> matdescFromJson(String json) {
        Map<String, String> map = Jackson.fromJsonString(json, Map.class);
        return map == null ? null : Collections.unmodifiableMap(map);
    }

    /**
     * Returns the content encrypting key unwrapped or decrypted.  Note if KMS
     * is used for key protection, a remote call will be made to KMS to decrypt
     * the ciphertext blob.
     *
     * @param cekSecured
     *            the content encrypting key in wrapped or encrypted form; must
     *            not be null
     * @param keyWrapAlgo
     *            key wrapping algorithm; or null if direct encryption instead
     *            of key wrapping is used
     * @param materials
     *            the client key encrypting key material for the content
     *            encrypting key
     * @param securityProvider
     *            security provider or null if the default security provider of
     *            the JCE is used
     */
    private static SecretKey cek(byte[] cekSecured, String keyWrapAlgo,
                                 EncryptionMaterials materials, Provider securityProvider,
                                 ContentCryptoScheme contentCryptoScheme, KMSClient kms) {
        if (isKmsKeyWrapped(keyWrapAlgo)) {
            return cekByKms(cekSecured, keyWrapAlgo, materials, contentCryptoScheme, kms);
        }
        Key kek;
        if (materials.getKeyPair() != null) {
            // Do envelope decryption with private key from key pair
            kek = materials.getKeyPair().getPrivate();
            if (kek == null) {
                throw new SdkClientException("Key encrypting key not available");
            }
        } else {
            // Do envelope decryption with symmetric key
            kek = materials.getSymmetricKey();
            if (kek == null) {
                throw new SdkClientException("Key encrypting key not available");
            }
        }

        try {
            if (keyWrapAlgo != null) {
                // Key wrapping specified
                Cipher cipher = securityProvider == null ? Cipher
                        .getInstance(keyWrapAlgo) : Cipher.getInstance(
                        keyWrapAlgo, securityProvider);
                cipher.init(Cipher.UNWRAP_MODE, kek);
                return (SecretKey) cipher.unwrap(cekSecured, keyWrapAlgo,
                                                 Cipher.SECRET_KEY);
            }
            // fall back to the Encryption Only (EO) key decrypting method
            Cipher cipher;
            if (securityProvider != null) {
                cipher = Cipher.getInstance(kek.getAlgorithm(),
                                            securityProvider);
            } else {
                cipher = Cipher.getInstance(kek.getAlgorithm());
            }
            cipher.init(Cipher.DECRYPT_MODE, kek);
            byte[] decryptedSymmetricKeyBytes = cipher.doFinal(cekSecured);
            return new SecretKeySpec(decryptedSymmetricKeyBytes,
                                     JceEncryptionConstants.SYMMETRIC_KEY_ALGORITHM);
        } catch (Exception e) {
            throw failure(e, "Unable to decrypt symmetric key from object metadata");
        }
    }

    /**
     * Decrypts the secured CEK via KMS; involves network calls.
     *
     * @return the CEK (in plaintext).
     */
    private static SecretKey cekByKms(byte[] cekSecured, String keyWrapAlgo,
                                      EncryptionMaterials materials,
                                      ContentCryptoScheme contentCryptoScheme, KMSClient kms) {
        DecryptRequest kmsreq = DecryptRequest.builder()
                .encryptionContext(materials.getMaterialsDescription())
                .ciphertextBlob(ByteBuffer.wrap(cekSecured)).build();
        DecryptResponse result = kms.decrypt(kmsreq);
        return new SecretKeySpec(copyAllBytesFrom(result.plaintext()),
                                 contentCryptoScheme.getKeyGeneratorAlgorithm());
    }

    /**
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromObjectMetadata(
            ObjectMetadata metadata,
            EncryptionMaterialsAccessor kekMaterialAccessor,
            Provider securityProvider,
            boolean keyWrapExpected,
            KMSClient kms) {
        return fromObjectMetadata0(metadata, kekMaterialAccessor,
                                   securityProvider, null, NONE, keyWrapExpected, kms);
    }

    /**
     * Factory method to return the content crypto material from the S3 object
     * meta data, using the specified key encrypting key material accessor and
     * an optional security provider.
     *
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromObjectMetadata(
            ObjectMetadata metadata,
            EncryptionMaterialsAccessor kekMaterialAccessor,
            Provider securityProvider,
            long[] range,
            ExtraMaterialsDescription extra,
            boolean keyWrapExpected,
            KMSClient kms) {
        return fromObjectMetadata0(metadata, kekMaterialAccessor,
                                   securityProvider, range, extra, keyWrapExpected, kms);
    }

    /**
     * @return a non-null content crypto material.
     */
    private static ContentCryptoMaterial fromObjectMetadata0(
            ObjectMetadata metadata,
            EncryptionMaterialsAccessor kekMaterialAccessor,
            Provider securityProvider,
            long[] range,
            ExtraMaterialsDescription extra,
            boolean keyWrapExpected,
            KMSClient kms) {
        // CEK and IV
        Map<String, String> userMeta = metadata.getUserMetadata();
        String b64key = userMeta.get(Headers.CRYPTO_KEY_V2);
        if (b64key == null) {
            b64key = userMeta.get(Headers.CRYPTO_KEY);
            if (b64key == null) {
                throw new SdkClientException(
                        "Content encrypting key not found.");
            }
        }
        byte[] cekWrapped = Base64Utils.decode(b64key);
        byte[] iv = Base64Utils.decode(userMeta.get(Headers.CRYPTO_IV));
        if (cekWrapped == null || iv == null) {
            throw new SdkClientException(
                    "Content encrypting key or IV not found.");
        }
        // Material description
        String matdescStr = userMeta.get(Headers.MATERIALS_DESCRIPTION);
        final String keyWrapAlgo = userMeta.get(Headers.CRYPTO_KEYWRAP_ALGORITHM);
        final boolean isKms = isKmsKeyWrapped(keyWrapAlgo);
        final Map<String, String> core = matdescFromJson(matdescStr);
        final Map<String, String> merged = isKms || extra == null
                                           ? core : extra.mergeInto(core);
        final EncryptionMaterials materials;
        if (isKms) {
            materials = new KmsEncryptionMaterials(
                    core.get(KmsEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
            materials.addDescriptions(core);
        } else {
            materials = kekMaterialAccessor == null
                        ? null
                        : kekMaterialAccessor.getEncryptionMaterials(merged)
            ;
            if (materials == null) {
                throw new SdkClientException(
                        "Unable to retrieve the client encryption materials");
            }
        }
        // CEK algorithm
        String cekAlgo = userMeta.get(Headers.CRYPTO_CEK_ALGORITHM);
        boolean isRangeGet = range != null;
        // The content crypto scheme may vary depending on whether
        // it is a range get operation
        ContentCryptoScheme contentCryptoScheme =
                ContentCryptoScheme.fromCekAlgo(cekAlgo, isRangeGet);
        if (isRangeGet) {
            // Adjust the IV as needed
            iv = contentCryptoScheme.adjustIv(iv, range[0]);
        } else {
            // Validate the tag length supported
            int tagLenExpected = contentCryptoScheme.getTagLengthInBits();
            if (tagLenExpected > 0) {
                String s = userMeta.get(Headers.CRYPTO_TAG_LENGTH);
                int tagLenActual = Integer.parseInt(s);
                if (tagLenExpected != tagLenActual) {
                    throw new SdkClientException("Unsupported tag length: "
                                                 + tagLenActual + ", expected: " + tagLenExpected);
                }
            }
        }
        // Unwrap or decrypt the CEK
        if (keyWrapExpected && keyWrapAlgo == null) {
            throw newKeyWrapException();
        }
        SecretKey cek = cek(cekWrapped, keyWrapAlgo, materials,
                            securityProvider, contentCryptoScheme, kms);
        return new ContentCryptoMaterial(merged, cekWrapped, keyWrapAlgo,
                                         contentCryptoScheme.createCipherLite(cek, iv,
                                                                              Cipher.DECRYPT_MODE, securityProvider));
    }

    private static KeyWrapException newKeyWrapException() {
        return new KeyWrapException(
                "Missing key-wrap for the content-encrypting-key");
    }

    /**
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromInstructionFile(
            Map<String, String> instFile,
            EncryptionMaterialsAccessor kekMaterialAccessor,
            Provider securityProvider,
            boolean keyWrapExpected,
            KMSClient kms) {
        return fromInstructionFile0(instFile, kekMaterialAccessor,
                                    securityProvider, null, NONE, keyWrapExpected, kms);
    }

    /**
     * Factory method to return the content crypto material from the S3
     * instruction file, using the specified key encrypting key material
     * accessor and an optional security provider.
     *
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromInstructionFile(
            Map<String, String> instFile,
            EncryptionMaterialsAccessor kekMaterialAccessor,
            Provider securityProvider,
            long[] range,
            ExtraMaterialsDescription extra,
            boolean keyWrapExpected,
            KMSClient kms) {
        return fromInstructionFile0(instFile, kekMaterialAccessor,
                                    securityProvider, range, extra, keyWrapExpected, kms);
    }

    /**
     * @return a non-null content crypto material.
     */
    private static ContentCryptoMaterial fromInstructionFile0(
            Map<String, String> instFile,
            EncryptionMaterialsAccessor kekMaterialAccessor,
            Provider securityProvider,
            long[] range,
            ExtraMaterialsDescription extra,
            boolean keyWrapExpected,
            KMSClient kms) {
        // CEK and IV
        String b64key = instFile.get(Headers.CRYPTO_KEY_V2);
        if (b64key == null) {
            b64key = instFile.get(Headers.CRYPTO_KEY);
            if (b64key == null) {
                throw new SdkClientException(
                        "Content encrypting key not found.");
            }
        }
        byte[] cekWrapped = Base64Utils.decode(b64key);
        byte[] iv = Base64Utils.decode(instFile.get(Headers.CRYPTO_IV));
        if (cekWrapped == null || iv == null) {
            throw new SdkClientException(
                    "Necessary encryption info not found in the instruction file "
                    + instFile);
        }
        final String keyWrapAlgo = instFile.get(Headers.CRYPTO_KEYWRAP_ALGORITHM);
        final boolean isKms = isKmsKeyWrapped(keyWrapAlgo);
        // Material description
        String matdescStr = instFile.get(Headers.MATERIALS_DESCRIPTION);
        final Map<String, String> core = matdescFromJson(matdescStr);
        final Map<String, String> merged = extra == null || isKms
                                           ? core : extra.mergeInto(core);
        EncryptionMaterials materials;
        if (isKms) {
            materials = new KmsEncryptionMaterials(
                    core.get(KmsEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
            materials.addDescriptions(core);
        } else {
            materials = kekMaterialAccessor == null
                        ? null
                        : kekMaterialAccessor.getEncryptionMaterials(merged);
            if (materials == null) {
                throw new SdkClientException(
                        "Unable to retrieve the encryption materials that originally "
                        + "encrypted object corresponding to instruction file "
                        + instFile);
            }
        }
        // CEK algorithm
        final String cekAlgo = instFile.get(Headers.CRYPTO_CEK_ALGORITHM);
        final boolean isRangeGet = range != null;
        // The content crypto scheme may vary depending on whether
        // it is a range get operation
        ContentCryptoScheme contentCryptoScheme = ContentCryptoScheme
                .fromCekAlgo(cekAlgo, isRangeGet);
        if (isRangeGet) {
            // Adjust the IV as needed
            iv = contentCryptoScheme.adjustIv(iv, range[0]);
        } else {
            // Validate the tag length supported
            int tagLenExpected = contentCryptoScheme.getTagLengthInBits();
            if (tagLenExpected > 0) {
                String s = instFile.get(Headers.CRYPTO_TAG_LENGTH);
                int tagLenActual = Integer.parseInt(s);
                if (tagLenExpected != tagLenActual) {
                    throw new SdkClientException("Unsupported tag length: "
                                                 + tagLenActual + ", expected: " + tagLenExpected);
                }
            }
        }
        // Unwrap or decrypt the CEK
        if (keyWrapExpected && keyWrapAlgo == null) {
            throw newKeyWrapException();
        }
        SecretKey cek = cek(cekWrapped, keyWrapAlgo, materials,
                            securityProvider, contentCryptoScheme, kms);
        return new ContentCryptoMaterial(merged, cekWrapped, keyWrapAlgo,
                                         contentCryptoScheme.createCipherLite(cek, iv,
                                                                              Cipher.DECRYPT_MODE, securityProvider));
    }

    /**
     * Parses instruction data retrieved from S3 and returns a JSON string
     * representing the instruction. Made for testing purposes.
     */
    static String parseInstructionFile(S3Object instructionFile) {
        try {
            return convertStreamToString(instructionFile.getObjectContent());
        } catch (Exception e) {
            throw failure(e, "Error parsing JSON instruction file");
        }
    }

    /**
     * Converts the contents of an input stream to a String
     */
    private static String convertStreamToString(InputStream inputStream)
            throws IOException {
        if (inputStream == null) {
            return "";
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(inputStream,
                                                                 StringUtils.UTF8));
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            } finally {
                inputStream.close();
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code> for the
     * input parameters using the specified content crypto scheme, and the key
     * wrapping and secure randomness specified of the specified s3 crypto
     * scheme.
     *
     * Note network calls are involved if the CEK is to be protected by KMS.
     *
     * @param cek
     *            content encrypting key; must not be null.
     * @param iv
     *            initialization vector; must not be null.
     * @param contentCryptoScheme
     *            content crypto scheme to be used
     * @param targetScheme
     *            the target s3 crypto scheme to be used for recreating the
     *            content crypto material by providing the key wrapping scheme
     *            and mechanism for secure randomness
     * @param provider
     *            optional security provider
     */
    static ContentCryptoMaterial create(SecretKey cek, byte[] iv,
                                        EncryptionMaterials kekMaterials,
                                        ContentCryptoScheme contentCryptoScheme,
                                        S3CryptoScheme targetScheme,
                                        Provider provider, KMSClient kms,
                                        AmazonWebServiceRequest req) {
        return doCreate(cek, iv, kekMaterials, contentCryptoScheme,
                        targetScheme, provider, kms, req);
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code>
     * for the input parameters using the specified s3 crypto scheme.
     * Note network calls are involved if the CEK is to be protected by KMS.
     *
     * @param cek content encrypting key
     * @param iv initialization vector
     * @param kekMaterials kek encryption material used to secure the CEK;
     *     can be KMS enabled.
     * @param scheme
     *            s3 crypto scheme to be used for the content crypto material by
     *            providing the content crypto scheme, key wrapping scheme and
     *            mechanism for secure randomness
     * @param provider optional security provider
     * @param kms reference to the KMS client
     * @param req originating service request
     */
    static ContentCryptoMaterial create(SecretKey cek, byte[] iv,
                                        EncryptionMaterials kekMaterials,
                                        S3CryptoScheme scheme,
                                        Provider provider, KMSClient kms,
                                        AmazonWebServiceRequest req) {
        return doCreate(cek, iv, kekMaterials, scheme.getContentCryptoScheme(),
                        scheme, provider, kms, req);
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code> for the
     * given input parameters by using the specified content crypto scheme, and
     * S3 crypto scheme.
     *
     * Note network calls are involved if the CEK is to be protected by KMS.
     *
     * @param cek
     *            content encrypting key
     * @param iv
     *            initialization vector
     * @param kekMaterials
     *            kek encryption material used to secure the CEK; can be KMS
     *            enabled.
     * @param contentCryptoScheme
     *            content crypto scheme to be used, which can differ from the
     *            one of <code>targetS3CryptoScheme</code>
     * @param targetS3CryptoScheme
     *            the target s3 crypto scheme to be used for providing the key
     *            wrapping scheme and mechanism for secure randomness
     * @param provider
     *            security provider
     * @param kms
     *            reference to the KMS client
     * @param req
     *            the originating AWS service request
     */
    private static ContentCryptoMaterial doCreate(SecretKey cek, byte[] iv,
                                                  EncryptionMaterials kekMaterials,
                                                  ContentCryptoScheme contentCryptoScheme,
                                                  S3CryptoScheme targetS3CryptoScheme,
                                                  Provider provider,
                                                  KMSClient kms,
                                                  AmazonWebServiceRequest req) {
        // Secure the envelope symmetric key either by encryption, key wrapping
        // or KMS.
        SecuredCek cekSecured = secureCek(cek, kekMaterials,
                                          targetS3CryptoScheme.getKeyWrapScheme(),
                                          targetS3CryptoScheme.getSecureRandom(),
                                          provider, kms, req);
        return wrap(cek, iv, contentCryptoScheme, provider, cekSecured);
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code> by wrapping
     * the input parameters, including the already secured CEK. No network calls
     * are involved.
     */
    public static ContentCryptoMaterial wrap(
            SecretKey cek, byte[] iv,
            ContentCryptoScheme contentCryptoScheme,
            Provider provider,
            SecuredCek cekSecured) {
        return new ContentCryptoMaterial(
                cekSecured.getMaterialDescription(),
                cekSecured.getEncrypted(),
                cekSecured.getKeyWrapAlgorithm(),
                contentCryptoScheme.createCipherLite(cek, iv, Cipher.ENCRYPT_MODE, provider));
    }

    /**
     * Secure the given CEK.  Note network calls are involved if the CEK is to
     * be protected by KMS.
     *
     * @param cek content encrypting key to be secured
     * @param materials used to provide the key-encryption-key (KEK); or if
     *     it is KMS-enabled, the customer master key id and material description.
     * @param kwScheme the content crypto scheme
     * @param p optional security provider; can be null if the default is used.
     * @return a secured CEK in the form of ciphertext or ciphertext blob.
     */
    private static SecuredCek secureCek(SecretKey cek,
                                        EncryptionMaterials materials, S3KeyWrapScheme kwScheme,
                                        SecureRandom srand, Provider p, KMSClient kms,
                                        AmazonWebServiceRequest req) {
        final Map<String, String> matdesc;

        if (materials.isKmsEnabled()) {
            matdesc = mergeMaterialDescriptions(materials, req);
            EncryptRequest encryptRequest = EncryptRequest.builder()
                    .encryptionContext(matdesc)
                    .keyId(materials.getCustomerMasterKeyId())
                    .plaintext(ByteBuffer.wrap(cek.getEncoded()))
                    .build();
            encryptRequest
                    .withGeneralProgressListener(req.getGeneralProgressListener())
                    .withRequestMetricCollector(req.getRequestMetricCollector())
            ;
            EncryptResponse encryptResult = kms.encrypt(encryptRequest);
            byte[] keyBlob = copyAllBytesFrom(encryptResult.ciphertextBlob());
            return new KmsSecuredCek(keyBlob, matdesc);
        } else {
            matdesc = materials.getMaterialsDescription();
        }
        Key kek;
        if (materials.getKeyPair() != null) {
            // Do envelope encryption with public key from key pair
            kek = materials.getKeyPair().getPublic();
        } else {
            // Do envelope encryption with symmetric key
            kek = materials.getSymmetricKey();
        }
        String keyWrapAlgo = kwScheme.getKeyWrapAlgorithm(kek);
        try {
            if (keyWrapAlgo != null) {
                Cipher cipher = p == null ? Cipher
                        .getInstance(keyWrapAlgo) : Cipher.getInstance(
                        keyWrapAlgo, p);
                cipher.init(Cipher.WRAP_MODE, kek, srand);
                return new SecuredCek(cipher.wrap(cek), keyWrapAlgo, matdesc);
            }
            // fall back to the Encryption Only (EO) key encrypting method
            Cipher cipher;
            byte[] toBeEncryptedBytes = cek.getEncoded();
            String algo = kek.getAlgorithm();
            if (p != null) {
                cipher = Cipher.getInstance(algo, p);
            } else {
                cipher = Cipher.getInstance(algo); // Use default JCE Provider
            }
            cipher.init(Cipher.ENCRYPT_MODE, kek);
            return new SecuredCek(cipher.doFinal(toBeEncryptedBytes), null, matdesc);
        } catch (Exception e) {
            throw failure(e, "Unable to encrypt symmetric key");
        }
    }

    static Map<String, String> mergeMaterialDescriptions(
            EncryptionMaterials materials,
            AmazonWebServiceRequest req) {
        Map<String, String> matdesc = materials.getMaterialsDescription();
        if (req instanceof MaterialsDescriptionProvider) {
            MaterialsDescriptionProvider mdp = (MaterialsDescriptionProvider) req;
            Map<String, String> materialsDescription = mdp.getMaterialsDescription();
            if (materialsDescription != null) {
                matdesc = new TreeMap<String, String>(matdesc);
                matdesc.putAll(materialsDescription);    // request takes precedence
            }
        }
        return matdesc;
    }

    /**
     * Returns the key wrapping algorithm, or null if the content key is not
     * secured via a key wrapping algorithm.
     * <p>
     * Note if the returned value is
     * {@value KmsSecuredCek#KEY_PROTECTION_MECHANISM}, it means the key is
     * protected via KMS rather than a direct key-wrapping algorithm.
     */
    String getKeyWrappingAlgorithm() {
        return keyWrappingAlgorithm;
    }

    /**
     * Returns true if KMS is used for the content key protection; false
     * otherwise.
     */
    private boolean usesKmsKey() {
        return isKmsKeyWrapped(keyWrappingAlgorithm);
    }

    /**
     * Returns the content crypto scheme.
     */
    ContentCryptoScheme getContentCryptoScheme() {
        return cipherLite.getContentCryptoScheme();
    }

    /**
     * Returns the given metadata updated with this content crypto material.
     */
    ObjectMetadata toObjectMetadata(ObjectMetadata metadata, CryptoMode mode) {
        return mode == CryptoMode.EncryptionOnly && !usesKmsKey()
               ? toObjectMetadataEo(metadata)
               : toObjectMetadata(metadata);
    }

    /**
     * Returns the metadata in the latest format.
     */
    private ObjectMetadata toObjectMetadata(ObjectMetadata metadata) {
        // If we generated a symmetric key to encrypt the data, store it in the
        // object metadata.
        byte[] encryptedCek = getEncryptedCek();
        metadata.addUserMetadata(Headers.CRYPTO_KEY_V2,
                                 Base64Utils.encodeAsString(encryptedCek));
        // Put the cipher initialization vector (IV) into the object metadata
        byte[] iv = cipherLite.getIv();
        metadata.addUserMetadata(Headers.CRYPTO_IV, Base64Utils.encodeAsString(iv));
        // Put the materials description into the object metadata as JSON
        metadata.addUserMetadata(Headers.MATERIALS_DESCRIPTION,
                                 kekMaterialDescAsJson());
        // The CRYPTO_CEK_ALGORITHM, CRYPTO_TAG_LENGTH and
        // CRYPTO_KEYWRAP_ALGORITHM were not available in the Encryption Only
        // (EO) implementation
        ContentCryptoScheme scheme = getContentCryptoScheme();
        metadata.addUserMetadata(Headers.CRYPTO_CEK_ALGORITHM,
                                 scheme.getCipherAlgorithm());
        int tagLen = scheme.getTagLengthInBits();
        if (tagLen > 0) {
            metadata.addUserMetadata(Headers.CRYPTO_TAG_LENGTH,
                                     String.valueOf(tagLen));
        }
        String keyWrapAlgo = getKeyWrappingAlgorithm();
        if (keyWrapAlgo != null) {
            metadata.addUserMetadata(Headers.CRYPTO_KEYWRAP_ALGORITHM,
                                     keyWrapAlgo);
        }
        return metadata;
    }

    /**
     * Returns the metadata in backward compatibility (old) format, so it can be
     * read by older version of the AWS SDK.
     */
    private ObjectMetadata toObjectMetadataEo(ObjectMetadata metadata) {
        // If we generated a symmetric key to encrypt the data, store it in the
        // object metadata.
        byte[] encryptedCek = getEncryptedCek();
        metadata.addUserMetadata(Headers.CRYPTO_KEY,
                                 Base64Utils.encodeAsString(encryptedCek));
        // Put the cipher initialization vector (IV) into the object metadata
        byte[] iv = cipherLite.getIv();
        metadata.addUserMetadata(Headers.CRYPTO_IV, Base64Utils.encodeAsString(iv));
        // Put the materials description into the object metadata as JSON
        metadata.addUserMetadata(Headers.MATERIALS_DESCRIPTION,
                                 kekMaterialDescAsJson());
        return metadata;
    }

    /**
     * Returns the json string in backward compatibility (old) format, so it can
     * be read by older version of the AWS SDK.
     */
    String toJsonString(CryptoMode mode) {
        return mode == CryptoMode.EncryptionOnly && !usesKmsKey()
               ? toJsonStringEo() : toJsonString();
    }

    /**
     * Returns the json string in the latest format.
     */
    private String toJsonString() {
        Map<String, String> map = new HashMap<String, String>();
        byte[] encryptedCek = getEncryptedCek();
        map.put(Headers.CRYPTO_KEY_V2, Base64Utils.encodeAsString(encryptedCek));
        byte[] iv = cipherLite.getIv();
        map.put(Headers.CRYPTO_IV, Base64Utils.encodeAsString(iv));
        map.put(Headers.MATERIALS_DESCRIPTION, kekMaterialDescAsJson());
        // The CRYPTO_CEK_ALGORITHM, CRYPTO_TAG_LENGTH and
        // CRYPTO_KEYWRAP_ALGORITHM were not available in the Encryption Only
        // (EO) implementation
        ContentCryptoScheme scheme = getContentCryptoScheme();
        map.put(Headers.CRYPTO_CEK_ALGORITHM, scheme.getCipherAlgorithm());
        int tagLen = scheme.getTagLengthInBits();
        if (tagLen > 0) {
            map.put(Headers.CRYPTO_TAG_LENGTH, String.valueOf(tagLen));
        }
        String keyWrapAlgo = getKeyWrappingAlgorithm();
        if (keyWrapAlgo != null) {
            map.put(Headers.CRYPTO_KEYWRAP_ALGORITHM, keyWrapAlgo);
        }
        return Jackson.toJsonString(map);
    }

    private String toJsonStringEo() {
        Map<String, String> map = new HashMap<String, String>();
        byte[] encryptedCek = getEncryptedCek();
        map.put(Headers.CRYPTO_KEY, Base64Utils.encodeAsString(encryptedCek));
        byte[] iv = cipherLite.getIv();
        map.put(Headers.CRYPTO_IV, Base64Utils.encodeAsString(iv));
        map.put(Headers.MATERIALS_DESCRIPTION, kekMaterialDescAsJson());
        return Jackson.toJsonString(map);
    }

    /**
     * Returns the key-encrypting-key material description as a non-null json
     * string;
     */
    private String kekMaterialDescAsJson() {
        Map<String, String> kekMaterialDesc = getKekMaterialsDescription();
        if (kekMaterialDesc == null) {
            kekMaterialDesc = Collections.emptyMap();
        }
        return Jackson.toJsonString(kekMaterialDesc);
    }

    /**
     * Return the cipher lite used for content encryption/decryption purposes.
     */
    CipherLite getCipherLite() {
        return cipherLite;
    }

    /**
     * Returns the description of the kek materials that were used to encrypt
     * the cek.
     */
    Map<String, String> getKekMaterialsDescription() {
        return this.kekMaterialsDescription;
    }

    /**
     * Returns an array of bytes representing the encrypted envelope symmetric
     * key.
     *
     * @return an array of bytes representing the encrypted envelope symmetric
     *         key.
     */
    byte[] getEncryptedCek() {
        return this.encryptedCek.clone();
    }

    /**
     * Recreates a new content crypto material from the current material given a
     * new KEK material-descriptions. The purpose is to re-encrypt the CEK under
     * a different KEK.
     *
     * Note network calls are involved if the CEK has been or is to be protected
     * by KMS.
     *
     * @param newKekMatDesc
     *            material descriptions for the new KEK; never null
     * @param accessor
     *            used to retrieve the KEK given the corresponding material
     *            description
     * @param targetScheme
     *            the target crypto scheme to be used for key wrapping, etc.
     * @param p
     *            optional security provider; null means to use the default.
     * @throws SecurityException
     *             if the old and new material description are the same; or if
     *             the old and new KEK are the same
     */
    ContentCryptoMaterial recreate(Map<String, String> newKekMatDesc,
                                   EncryptionMaterialsAccessor accessor, S3CryptoScheme targetScheme,
                                   Provider p, KMSClient kms, AmazonWebServiceRequest req) {
        if (!usesKmsKey() && newKekMatDesc.equals(kekMaterialsDescription)) {
            throw new SecurityException(
                    "Material description of the new KEK must differ from the current one");
        }
        final EncryptionMaterials origKek;
        if (usesKmsKey()) {
            origKek = new KmsEncryptionMaterials(kekMaterialsDescription.get(
                    KmsEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
        } else {
            origKek = accessor.getEncryptionMaterials(kekMaterialsDescription);
        }
        EncryptionMaterials newKek = accessor.getEncryptionMaterials(newKekMatDesc);
        if (newKek == null) {
            throw new SdkClientException(
                    "No material available with the description "
                    + newKekMatDesc
                    + " from the encryption material provider");
        }
        SecretKey cek = cek(encryptedCek, keyWrappingAlgorithm, origKek, p,
                            getContentCryptoScheme(), kms);
        ContentCryptoMaterial output = create(cek, cipherLite.getIv(), newKek,
                                              getContentCryptoScheme(),  // must use same content crypto scheme
                                              targetScheme,
                                              p, kms, req);
        if (Arrays.equals(output.encryptedCek, encryptedCek)) {
            throw new SecurityException(
                    "The new KEK must differ from the original");
        }
        return output;
    }

    /**
     * Recreates a new content crypto material from the current material given a
     * new KEK encryption materials. The purpose is to re-encrypt the CEK under
     * the new KEK.
     *
     * Note network calls are involved if the CEK has been or is to be protected
     * by KMS.
     *
     * @param newKek
     *            encryption materials for the new KEK; must not be null
     * @param accessor
     *            used to retrieve the original KEK given the corresponding
     *            material description
     * @param targetScheme
     *            the target crypto scheme to use for recreating the content
     *            crypto material
     * @param p
     *            optional security provider; null means to use the default.
     * @throws SecurityException
     *             if the old and new material description are the same; or if
     *             the old and new KEK are the same
     */
    ContentCryptoMaterial recreate(EncryptionMaterials newKek,
                                   EncryptionMaterialsAccessor accessor, S3CryptoScheme targetScheme,
                                   Provider p, KMSClient kms, AmazonWebServiceRequest req) {
        if (!usesKmsKey()
            && newKek.getMaterialsDescription().equals(kekMaterialsDescription)) {
            throw new SecurityException(
                    "Material description of the new KEK must differ from the current one");
        }
        final EncryptionMaterials origKek;
        if (usesKmsKey()) {
            origKek = new KmsEncryptionMaterials(kekMaterialsDescription.get(
                    KmsEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
        } else {
            origKek = accessor.getEncryptionMaterials(kekMaterialsDescription);
        }
        SecretKey cek = cek(encryptedCek, keyWrappingAlgorithm, origKek, p,
                            getContentCryptoScheme(), kms);
        ContentCryptoMaterial output =
                create(cek, cipherLite.getIv(), newKek,
                       getContentCryptoScheme(),  // must use same content crypto scheme
                       targetScheme, // target scheme used to recreate the content crypto material
                       p, kms, req);
        if (Arrays.equals(output.encryptedCek, encryptedCek)) {
            throw new SecurityException(
                    "The new KEK must differ from the original");
        }
        return output;
    }
}
