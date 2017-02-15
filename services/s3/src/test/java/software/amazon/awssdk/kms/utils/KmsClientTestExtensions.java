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

package software.amazon.awssdk.kms.utils;

import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.kms.model.CreateAliasRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResult;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.NotFoundException;

/**
 * Useful extensions to the KMS client for S3 Integration tests
 */
public class KmsClientTestExtensions extends AWSKMSClient {

    private static final String NON_DEFAULT_KEY_ALIAS = "alias/s3/integration-tests";

    public KmsClientTestExtensions(AWSCredentials awsCredentials) {
        super(awsCredentials);
    }

    /**
     * Find the S3 service default KMS key. I.E. the key with alias 'alias/aws/s3'
     *
     * @return KMS Key ID of S3 default key
     */
    public String getDefaultS3KeyId() {
        return findKeyIdByAlias("alias/aws/s3");
    }

    /**
     * Find the key to use for S3 Integration tests if it exists. If it doesn't exist create it.
     * Aliases are used to signify which key should be used for the tests so we don't have to rely
     * on hardcoded key ids
     *
     * @return Key ID of Non-Default KMS key to use for integration tests.
     */
    public String getNonDefaultKeyId() {
        String keyId = findKeyIdByAlias(NON_DEFAULT_KEY_ALIAS);
        if (keyId == null) {
            CreateKeyResult result = createKey(new CreateKeyRequest()
                                                       .withDescription("KMS key used for S3 Integration tests"));
            createAlias(new CreateAliasRequest().withTargetKeyId(result.getKeyMetadata().getKeyId()).withAliasName(
                    NON_DEFAULT_KEY_ALIAS));
            return result.getKeyMetadata().getKeyId();
        } else {
            return keyId;
        }
    }

    /**
     * Retrieve the ID of a KMS key by its alias.
     *
     * @param keyAlias
     *            Alias of key to lookup ID for
     * @return KMS Key ID of key mapped to alias or null if no such key exists
     */
    public String findKeyIdByAlias(String keyAlias) {
        try {
            return describeKey(new DescribeKeyRequest().withKeyId(keyAlias)).getKeyMetadata().getKeyId();
        } catch (NotFoundException e) {
            return null;
        }
    }
}
