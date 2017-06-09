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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Date;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;

public class RestoreObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String bucketName = "java-sdk-" + new Date().getTime();

    private static final String key = "key";

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();

        // Put a permanent S3 object
        s3.createBucket(bucketName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength("Hello S3 Java client world!!!".getBytes().length);
        s3.putObject(bucketName, key, new ByteArrayInputStream("Hello S3 Java client world!!!".getBytes()), metadata);
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(bucketName);
    }

    @Test
    public void testRestoreObject() {

        try {
            s3.restoreObject(new RestoreObjectRequest(bucketName, key, 1));
            fail();
        } catch (AmazonServiceException e) {
            assertTrue(e.getMessage().contains("Restore is not allowed, as object's storage class is not GLACIER"));
        }

        ObjectMetadata metadata = s3.getObjectMetadata(bucketName, key);
        assertNull(metadata.getOngoingRestore());
        assertNull(metadata.getRestoreExpirationTime());
    }
}
