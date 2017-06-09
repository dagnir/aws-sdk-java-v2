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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;

/**
 * Integration tests for exception handling in the Amazon S3 Java client.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class ExceptionHandlingIntegrationTest extends S3IntegrationTestBase {

    /** A non-existent bucket name for tests to use. */
    private String nonExistentBucket = "non-existent-bucket-" + new Date().getTime();

    /** A non-existent object key for tests to use. */
    private String nonExistentKey = "non-existent-key-" + new Date().getTime();

    /** An existing bucket name, not owned by us, for tests to use. */
    private String existingBucket = "amazon";


    /**
     * Tests that trying to create a bucket with an invalid bucket name throws a
     * correctly populated AmazonServiceException.
     */
    @Test
    public void testInvalidBucketName() {
        try {
            s3.createBucket("!!!");
            fail("Expected exception, IllegalArgumentException, wasn't thrown");
        } catch (IllegalArgumentException ase) {
            // Ignored or expected.
        }
    }

    /**
     * Tests that trying to delete a non-existent object throws a correctly
     * populated AmazonServiceException.
     */
    @Test
    public void testNoSuchBucket() {
        try {
            s3.deleteObject(nonExistentBucket, nonExistentKey);
            fail("Expected exception");
        } catch (AmazonS3Exception ase) {
            assertTrue(ase.getStatusCode() > 0);
            assertEquals("NoSuchBucket", ase.getErrorCode());
            assertTrue(ase.getMessage().length() > 1);
            assertTrue(ase.getRequestId().length() > 1);
            assertTrue(ase.getExtendedRequestId().length() > 1);
            assertEquals(ErrorType.Client, ase.getErrorType());
        }
    }

    /**
     * Tests that trying to get the ACL for a bucket not owned by our test
     * account throws a correctly populated AmazonServiceException.
     */
    @Test
    public void testAccessDenied() {
        try {
            s3.getBucketAcl(existingBucket);
            fail("Expected exception");
        } catch (AmazonS3Exception ase) {
            assertTrue(ase.getStatusCode() > 0);
            assertEquals("AccessDenied", ase.getErrorCode());
            assertTrue(ase.getMessage().length() > 1);
            assertTrue(ase.getRequestId().length() > 1);
            assertTrue(ase.getExtendedRequestId().length() > 1);
            assertEquals(ErrorType.Client, ase.getErrorType());
        }
    }

    /**
     * Tests that trying to HEAD the object metadata for a non-existent object
     * throws a correctly populated AmazonServiceException.
     *
     * This test is important, because a HEAD response can't contain a response
     * body, so we're testing that our error response handler pulls as much data
     * as it can out of the response headers.
     */
    @Test
    public void testGetObjectMetadata() {
        try {
            s3.getObjectMetadata(nonExistentBucket, nonExistentKey);
            fail("Expected exception");
        } catch (AmazonS3Exception ase) {
            assertTrue(ase.getStatusCode() > 0);
            assertTrue(ase.getMessage().length() > 1);
            assertTrue(ase.getRequestId().length() > 1);
            assertTrue(ase.getExtendedRequestId().length() > 1);
            assertEquals(ErrorType.Client, ase.getErrorType());
        }
    }

    /**
     * We expected IllegalArgumentExceptions when null values are specified for
     * requried parameters.
     */
    @Test
    public void testIllegalArgumentExceptions() {
        try {
            s3.getObject((String) null, (String) null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Ignored or expected.
        }
    }
}
