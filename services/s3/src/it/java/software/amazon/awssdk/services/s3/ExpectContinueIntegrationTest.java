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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Date;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.test.util.RandomInputStream;

/**
 * Integration tests for object too large which utilizes the "expect continue" header.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class ExpectContinueIntegrationTest extends S3IntegrationTestBase {

    private String bucketName = "expect-continue-integ-test-" + new Date().getTime();
    private String key = "key";

    /** Releases all resources created by this test */
    @After
    public void tearDown() {
        try {
            s3.deleteObject(bucketName, key);
        } catch (Exception e) {
        }
        try {
            s3.deleteBucket(bucketName);
        } catch (Exception e) {
        }
    }

    /**
     * Tests that users are prevented from uploading
     */
    @Test
    public void testObjectTooLarge() {
        s3.createBucket(bucketName);

        // Current S3 object limit is 5GB
        long contentLength = 6 * 1024L * 1024L * 1024L;
        InputStream input = new RandomInputStream(10);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);

        try {
            s3.putObject(bucketName, key, input, metadata);
            fail("Expected a service exception, but wasn't thrown");
        } catch (AmazonServiceException ase) {
            assertEquals("EntityTooLarge", ase.getErrorCode());
        }
    }
}
