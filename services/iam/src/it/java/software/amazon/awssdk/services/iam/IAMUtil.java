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

package software.amazon.awssdk.services.iam;

import java.util.UUID;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagement;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.services.identitymanagement.model.AccessKeyMetadata;
import software.amazon.awssdk.services.identitymanagement.model.CreateUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteSigningCertificateRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetGroupResult;
import software.amazon.awssdk.services.identitymanagement.model.Group;
import software.amazon.awssdk.services.identitymanagement.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListAccessKeysResult;
import software.amazon.awssdk.services.identitymanagement.model.ListGroupsRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListGroupsResult;
import software.amazon.awssdk.services.identitymanagement.model.ListSigningCertificatesRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListSigningCertificatesResult;
import software.amazon.awssdk.services.identitymanagement.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListUserPoliciesResult;
import software.amazon.awssdk.services.identitymanagement.model.ListUsersRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListUsersResult;
import software.amazon.awssdk.services.identitymanagement.model.RemoveUserFromGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.SigningCertificate;
import software.amazon.awssdk.services.identitymanagement.model.User;


/**
 * Shamelessly stolen from:
 *
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/AWSCSharpSDKFactory/mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/Util.cs
 */
public class IAMUtil {
    private static final AmazonIdentityManagement client;
    public static String TEST_PATH = "/IntegrationTests/IAM/";

    static {
        try {
            IntegrationTestBase.setUpCredentials();
            client = new AmazonIdentityManagementClient(IntegrationTestBase.credentials);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String makePath(String... elements) {
        String path = TEST_PATH;
        for (String s : elements) {
            path = String.format("%s%s/", path, s);
        }

        return path;
    }

    public static void deleteUsersAndGroupsInTestNameSpace() {
        ListGroupsResult lgRes = client.listGroups(new ListGroupsRequest()
                                                           .withPathPrefix(TEST_PATH));
        for (Group g : lgRes.getGroups()) {
            GetGroupResult ggRes = client.getGroup(new GetGroupRequest()
                                                           .withGroupName(g.getGroupName()));
            for (User u : ggRes.getUsers()) {
                client.removeUserFromGroup(new RemoveUserFromGroupRequest()
                                                   .withGroupName(g.getGroupName()).withUserName(
                                u.getUserName()));
            }
            client.deleteGroup(new DeleteGroupRequest().withGroupName(g
                                                                              .getGroupName()));
        }

        ListUsersResult luRes = client.listUsers(new ListUsersRequest()
                                                         .withPathPrefix(TEST_PATH));
        for (User u : luRes.getUsers()) {
            deleteTestUsers(u.getUserName());
        }
    }

    public static void deleteAccessKeysForUser(String username) {
        ListAccessKeysResult response = client
                .listAccessKeys(new ListAccessKeysRequest()
                                        .withUserName(username));
        for (AccessKeyMetadata akm : response.getAccessKeyMetadata()) {
            client.deleteAccessKey(new DeleteAccessKeyRequest().withUserName(
                    username).withAccessKeyId(akm.getAccessKeyId()));
        }
    }

    public static void deleteUserPoliciesForUser(String username) {
        ListUserPoliciesResult response = client
                .listUserPolicies(new ListUserPoliciesRequest()
                                          .withUserName(username));
        for (String pName : response.getPolicyNames()) {
            client.deleteUserPolicy(new DeleteUserPolicyRequest().withUserName(
                    username).withPolicyName(pName));
        }
    }

    public static void deleteCertificatesForUser(String username) {
        ListSigningCertificatesResult response = client
                .listSigningCertificates(new ListSigningCertificatesRequest()
                                                 .withUserName(username));
        for (SigningCertificate cert : response.getCertificates()) {
            client.deleteSigningCertificate(new DeleteSigningCertificateRequest()
                                                    .withUserName(username).withCertificateId(
                            cert.getCertificateId()));
        }
    }

    public static String createTestUser() {
        String username = uniqueName();
        client.createUser(new CreateUserRequest().withUserName(username)
                                                 .withPath(IAMUtil.TEST_PATH));
        return username;
    }

    public static String uniqueName() {
        return "IamIntegrationTests" + UUID.randomUUID().toString().replace('-', '0');
    }

    public static void deleteTestUsers(String... usernames) {
        for (String s : usernames) {
            deleteAccessKeysForUser(s);
            deleteUserPoliciesForUser(s);
            deleteCertificatesForUser(s);
            try {
                client.deleteLoginProfile(new DeleteLoginProfileRequest()
                                                  .withUserName(s));
            } catch (Exception e) {
                /* Nobody cares. */
            }
            client.deleteUser(new DeleteUserRequest().withUserName(s));
        }
    }
}