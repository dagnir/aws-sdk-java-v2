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

package software.amazon.awssdk.services.sts;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.auth.policy.Policy;
import software.amazon.awssdk.auth.policy.Resource;
import software.amazon.awssdk.auth.policy.Statement;
import software.amazon.awssdk.auth.policy.Statement.Effect;
import software.amazon.awssdk.auth.policy.actions.SecurityTokenServiceActions;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyResult;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResult;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesResult;
import software.amazon.awssdk.services.iam.model.PutUserPolicyRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResult;

public class AssumeRoleIntegrationTest extends IntegrationTestBaseWithIAM {

    private static final int SESSION_DURATION = 60 * 60;
    private static final String ROLE_ARN = "arn:aws:iam::599169622985:role/java-test-role";
    private static final String USER_NAME = "user-" + System.currentTimeMillis();

    @AfterClass
    public static void tearDown() {
        deleteUser(USER_NAME);
    }

    private static void deleteAccessKeysForUser(String userName) {
        ListAccessKeysResult response = iam.listAccessKeys(new ListAccessKeysRequest().withUserName(userName));
        for (AccessKeyMetadata akm : response.getAccessKeyMetadata()) {
            iam.deleteAccessKey(new DeleteAccessKeyRequest().withUserName(userName).withAccessKeyId(akm.getAccessKeyId()));
        }
    }

    private static void deleteUserPoliciesForUser(String userName) {
        ListUserPoliciesResult response = iam.listUserPolicies(new ListUserPoliciesRequest().withUserName(userName));
        for (String pName : response.getPolicyNames()) {
            iam.deleteUserPolicy(new DeleteUserPolicyRequest().withUserName(userName).withPolicyName(pName));
        }
    }

    private static void deleteUser(String userName) {
        try {
            deleteAccessKeysForUser(userName);
        } catch (Exception e) {
            // Ignore.
        }
        try {
            deleteUserPoliciesForUser(userName);
        } catch (Exception e) {
            // Ignore.
        }
        try {
            iam.deleteLoginProfile(new DeleteLoginProfileRequest()
                                           .withUserName(userName));
        } catch (Exception e) {
            // Ignore.
        }
        try {
            iam.deleteUser(new DeleteUserRequest().withUserName(userName));
        } catch (Exception e) {
            // Ignore.
        }
    }

    /** Tests that we can call assumeRole successfully. */
    @Test
    public void testAssumeRole() throws InterruptedException {
        Statement statement = new Statement(Effect.Allow)
                .withActions(SecurityTokenServiceActions.AllSecurityTokenServiceActions)
                .withResources(new Resource("*"));
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(SESSION_DURATION)
                .withRoleArn(ROLE_ARN)
                .withRoleSessionName("Name")
                .withPolicy(new Policy().withStatements(statement)
                                        .toJson());

        STSClient sts = getStsClient();
        Thread.sleep(1000 * 60);
        AssumeRoleResult assumeRoleResult = sts.assumeRole(assumeRoleRequest);
        assertNotNull(assumeRoleResult.getAssumedRoleUser());
        assertNotNull(assumeRoleResult.getAssumedRoleUser().getArn());
        assertNotNull(assumeRoleResult.getAssumedRoleUser().getAssumedRoleId());
        assertNotNull(assumeRoleResult.getCredentials());
        assertNotNull(assumeRoleResult.getPackedPolicySize());
    }

    private STSClient getStsClient() {
        iam.createUser(new CreateUserRequest().withUserName(USER_NAME));

        String policyDoc = new Policy()
                .withStatements(new Statement(Effect.Allow)
                                        .withActions(SecurityTokenServiceActions.AssumeRole)
                                        .withResources(new Resource("*")))
                .toJson();

        iam.putUserPolicy(new PutUserPolicyRequest().withPolicyDocument(policyDoc)
                                                    .withUserName(USER_NAME).withPolicyName("assume-role"));
        CreateAccessKeyResult createAccessKeyResult = iam.createAccessKey(new CreateAccessKeyRequest().withUserName(USER_NAME));
        AwsCredentials credentials = new BasicAwsCredentials(createAccessKeyResult.getAccessKey().getAccessKeyId(),
                                                             createAccessKeyResult.getAccessKey().getSecretAccessKey());
        return STSClient.builder().credentialsProvider(new AwsStaticCredentialsProvider(credentials)).build();
    }
}
