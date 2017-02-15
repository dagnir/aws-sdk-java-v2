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

import software.amazon.awssdk.services.s3.internal.S3Direct;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;

public class S3CryptoModuleAEStrictTest extends S3CryptoModuleAETest {
    @Override
    protected S3CryptoModuleBase<?> createS3CryptoModule(S3Direct s3,
                                                         EncryptionMaterialsProvider provider,
                                                         CryptoConfiguration cryptoConfig) {
        return new S3CryptoModuleAE(s3, provider,
                                    cryptoConfig.clone()
                                                .withCryptoMode(StrictAuthenticatedEncryption)
                                                .readOnly());
    }
}
