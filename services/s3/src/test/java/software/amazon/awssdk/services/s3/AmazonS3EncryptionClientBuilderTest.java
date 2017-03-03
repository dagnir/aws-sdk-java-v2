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

import static org.mockito.Mockito.mock;

import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;

public class AmazonS3EncryptionClientBuilderTest {

    @Test
    public void canCreateAnS3EncryptionClient() {
        CryptoConfiguration cryptoConfig = new CryptoConfiguration();
        EncryptionMaterialsProvider encryptionMaterials = mock(EncryptionMaterialsProvider.class);
        AWSKMSClient kmsClient = mock(AWSKMSClient.class);
        ClientConfiguration clientConfig = new ClientConfiguration();

        AmazonS3EncryptionClientBuilder.standard()
                                       .withCryptoConfiguration(cryptoConfig)
                                       .withRegion("us-west-1")
                                       .withEncryptionMaterials(encryptionMaterials)
                                       .withKmsClient(kmsClient)
                                       .withPathStyleAccessEnabled(true)
                                       .withClientConfiguration(clientConfig)
                                       .build();
    }

}
