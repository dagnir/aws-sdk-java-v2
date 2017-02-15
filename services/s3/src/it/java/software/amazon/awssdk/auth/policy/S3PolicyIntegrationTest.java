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

package software.amazon.awssdk.auth.policy;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.auth.policy.actions.S3Actions;
import software.amazon.awssdk.auth.policy.conditions.ConditionFactory;
import software.amazon.awssdk.auth.policy.conditions.S3ConditionFactory;
import software.amazon.awssdk.auth.policy.conditions.StringCondition;
import software.amazon.awssdk.auth.policy.conditions.StringCondition.StringComparisonType;
import software.amazon.awssdk.auth.policy.resources.S3BucketResource;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;

/**
 * Integration tests for the service specific access control policy code
 * provided by the S3 client.
 */
public class S3PolicyIntegrationTest extends S3IntegrationTestBase {

    /** Name of the bucket created by this test */
    private final String bucketName = "java-custom-bucket-policy-integ-test-" + System.currentTimeMillis();

    /** Release all allocated resources */
    @After
    public void tearDown() {
        s3.deleteBucket(bucketName);
    }

    /**
     * Tests that the S3 specific access control policy code works as expected.
     */
    @Test
    public void testPolicies() throws Exception {
        s3.createBucket(bucketName);

        Policy policy = new Policy().withStatements(
                new Statement(Statement.Effect.Allow)
                        .withActions(S3Actions.AllS3Actions)
                        .withPrincipals(Principal.AllUsers)
                        .withResources(new S3BucketResource(bucketName))
                        .withConditions(S3ConditionFactory.newCannedACLCondition(CannedAccessControlList.Private)));
        s3.setBucketPolicy(bucketName, policy.toJson());

        policy = new Policy().withStatements(
                new Statement(Statement.Effect.Allow)
                        .withActions(S3Actions.AllS3Actions)
                        .withPrincipals(Principal.AllUsers)
                        .withResources(new S3BucketResource(bucketName))
                        .withConditions(
                                S3ConditionFactory.newCannedACLCondition(CannedAccessControlList.AuthenticatedRead),
                                new StringCondition(StringComparisonType.StringEquals,
                                                    ConditionFactory.USER_AGENT_CONDITION_KEY,
                                                    "foo*")),
                new Statement(Statement.Effect.Allow)
                        .withActions(S3Actions.ListObjectVersions)
                        .withResources(new S3BucketResource(bucketName))
                        .withPrincipals(Principal.AllUsers));
        s3.setBucketPolicy(bucketName, policy.toJson());
    }

}
