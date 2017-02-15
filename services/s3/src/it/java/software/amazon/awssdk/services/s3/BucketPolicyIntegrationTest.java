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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;

/**
 * Integration tests for S3 bucket policy operations.
 */
public class BucketPolicyIntegrationTest extends S3IntegrationTestBase {

    /** Path to the sample policy for this test */
    private static final String POLICY_FILE = "/software/amazon/awssdk/services/s3/samplePolicy.json";
    /** Name of the bucket created by this test */
    private final String bucketName = "java-bucket-policy-integ-test-" + System.currentTimeMillis();

    /** Release all allocated resources */
    @After
    public void tearDown() {
        super.deleteBucketAndAllContents(bucketName);
    }

    /** Tests that we can get/put/delete bucket policies. */
    @Test
    public void testBucketPolicies() throws Exception {
        String policyText = IOUtils.toString(getClass().getResourceAsStream(POLICY_FILE));
        policyText = replace(policyText, "@BUCKET_NAME@", bucketName);

        s3.createBucket(bucketName);

        // Verify that no policy exists yet
        assertNull(s3.getBucketPolicy(bucketName).getPolicyText());

        // Upload a new bucket policy
        s3.setBucketPolicy(bucketName, policyText);

        // Try to retrieve it - then set what we get back to make sure
        // we correctly parsed the policy text
        String retrievedPolicyText = s3.getBucketPolicy(bucketName).getPolicyText();
        assertTrue(retrievedPolicyText.indexOf(bucketName) != -1);
        s3.setBucketPolicy(bucketName, retrievedPolicyText);

        // Delete it - and verify it's gone
        s3.deleteBucketPolicy(bucketName);
        assertNull(s3.getBucketPolicy(bucketName).getPolicyText());

        // Try to get the policy for a bucket we don't own to test error handling
        try {
            s3.getBucketPolicy("bucket");
            fail("Expected AmazonServiceException");
        } catch (AmazonServiceException ase) {
            assertNotNull(ase.getRequestId());
            assertNotNull(ase.getMessage());
        }
    }

    protected String replace(String source, String token, String value) {
        int startIndexOfToken = source.indexOf(token);
        int endIndexOfToken = startIndexOfToken + token.length();

        return source.substring(0, startIndexOfToken) + value + source.substring(endIndexOfToken);
    }
}
