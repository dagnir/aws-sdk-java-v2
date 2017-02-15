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

import org.junit.experimental.categories.Category;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.CryptoMode;

/**
 * Integration tests for the Amazon S3 V2 Encryption Client running in V1 mode.
 */
@Category(S3Categories.Slow.class)
public class S3EOIntegrationTest extends S3CryptoIntegrationTestBase {
    @Override
    protected final CryptoMode cryptoMode() {
        return CryptoMode.EncryptionOnly;
    }
}