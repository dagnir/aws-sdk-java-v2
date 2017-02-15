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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.util.StringInputStream;

public class DoesObjectExistIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET_NAME = "does-object-exist-" + System.currentTimeMillis();

    @BeforeClass
    public static void setupFixture() {
        s3.createBucket(BUCKET_NAME);
    }

    @AfterClass
    public static void tearDownFixture() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
    }

    @Test
    public void doesObjectExist_WhenObjectExists_ReturnsTrue() throws Exception {
        final String objectName = "some-object";
        s3.putObject(BUCKET_NAME, objectName, new StringInputStream("content"), null);
        assertTrue(s3.doesObjectExist(BUCKET_NAME, objectName));
    }

    @Test
    public void doesObjectExist_WhenObjectDoesNotExist_ReturnsFalse() throws Exception {
        assertFalse(s3.doesObjectExist(BUCKET_NAME, "non-existent-object-name"));
    }

    @Test(expected = AmazonS3Exception.class)
    public void doesObjectExist_NonExistentBucket_ThrowsException() {
        // Should throw an access denied exception since we don't own the bucket
        s3.doesObjectExist("aws-java-sdk", "some-object");
    }
}
