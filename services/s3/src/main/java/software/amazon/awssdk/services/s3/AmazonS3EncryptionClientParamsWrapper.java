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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.services.kms.KMSClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;

@Immutable
@SdkInternalApi
public final class AmazonS3EncryptionClientParamsWrapper extends AmazonS3EncryptionClientParams {
    private final EncryptionMaterialsProvider encryptionMaterials;
    private final CryptoConfiguration cryptoConfiguration;
    private final KMSClient kms;
    private final AwsSyncClientParams getClientParams;
    private final S3ClientOptions getS3ClientOptions;

    AmazonS3EncryptionClientParamsWrapper(AwsSyncClientParams getClientParams,
                                          S3ClientOptions getS3ClientOptions,
                                          EncryptionMaterialsProvider encryptionMaterials,
                                          CryptoConfiguration cryptoConfiguration,
                                          KMSClient kms) {
        this.encryptionMaterials = encryptionMaterials;
        this.cryptoConfiguration = cryptoConfiguration;
        this.kms = kms;
        this.getClientParams = getClientParams;
        this.getS3ClientOptions = getS3ClientOptions;
    }

    @Override
    EncryptionMaterialsProvider getEncryptionMaterials() {
        return encryptionMaterials;
    }

    @Override
    CryptoConfiguration getCryptoConfiguration() {
        return cryptoConfiguration;
    }

    @Override
    KMSClient getKmsClient() {
        return kms;
    }

    @Override
    public AwsSyncClientParams getClientParams() {
        return getClientParams;
    }

    @Override
    public S3ClientOptions getS3ClientOptions() {
        return getS3ClientOptions;
    }
}
