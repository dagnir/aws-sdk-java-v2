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

import static software.amazon.awssdk.services.s3.model.CryptoMode.StrictAuthenticatedEncryption;

import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.services.kms.KMSClient;
import software.amazon.awssdk.services.s3.internal.S3Direct;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;

/**
 * Strict Authenticated encryption (AE) cryptographic module for the S3
 * encryption client.
 */
class S3CryptoModuleAeStrict extends S3CryptoModuleAe {
    /**
     * @param cryptoConfig a read-only copy of the crypto configuration.
     */
    S3CryptoModuleAEStrict(KMSClient kms, S3Direct s3,
                           AwsCredentialsProvider credentialsProvider,
                           EncryptionMaterialsProvider encryptionMaterialsProvider,
                           CryptoConfiguration cryptoConfig) {
        super(kms, s3, credentialsProvider, encryptionMaterialsProvider,
              cryptoConfig);
        if (cryptoConfig.getCryptoMode() != StrictAuthenticatedEncryption) {
            throw new IllegalArgumentException();
        }
    }

    protected final boolean isStrict() {
        return true;
    }

    protected void securityCheck(ContentCryptoMaterial cekMaterial,
                                 S3ObjectWrapper retrieved) {
        if (!ContentCryptoScheme.AES_GCM.equals(cekMaterial.getContentCryptoScheme())) {
            throw new SecurityException("S3 object [bucket: "
                                        + retrieved.getBucketName() + ", key: "
                                        + retrieved.getKey()
                                        + "] not encrypted using authenticated encryption");
        }
    }
}
