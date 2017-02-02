/*
 * Copyright 2016-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CreateBucketLocationConstraintIntegrationTest extends
        S3IntegrationTestBase{

    private static final String BUCKET_NAME = "loc-constraint-" + System
            .currentTimeMillis();

    @BeforeClass
    public static void setup() {
        s3.setEndpoint("s3-external-1.amazonaws.com");
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
    }

    @Test
    public void create_bucket_returns_success_on_external_1_endpoint() {
        s3.createBucket(BUCKET_NAME);
    }
}
