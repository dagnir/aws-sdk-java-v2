package software.amazon.awssdk.services.iam;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.model.CreateGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteGroupPolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetGroupPolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetGroupPolicyResult;
import software.amazon.awssdk.services.identitymanagement.model.GetUserPolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetUserPolicyResult;
import software.amazon.awssdk.services.identitymanagement.model.ListGroupPoliciesRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListGroupPoliciesResult;
import software.amazon.awssdk.services.identitymanagement.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListUserPoliciesResult;
import software.amazon.awssdk.services.identitymanagement.model.MalformedPolicyDocumentException;
import software.amazon.awssdk.services.identitymanagement.model.NoSuchEntityException;
import software.amazon.awssdk.services.identitymanagement.model.PutGroupPolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.PutUserPolicyRequest;

/**
 * Integration tests of the policy APIs of IAM.
 *
 * Adapted from jimfl@'s C# tests:
 *
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/AWSCSharpSDKFactory/mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/PolicyTests.cs
 */
public class PolicyIntegrationTest extends IntegrationTestBase {

    public static final String TEST_ALLOW_POLICY = "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}";
    public static final String TEST_DENY_POLICY = "{\"Statement\":[{\"Effect\":\"Deny\",\"Action\":\"*\",\"Resource\":\"*\"}]}";

    @Before
    public void TestSetup() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestPutGetUserPolicy() throws UnsupportedEncodingException {
        String username = IAMUtil.createTestUser();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.putUserPolicy(new PutUserPolicyRequest().withUserName(username)
                                                        .withPolicyName(policyName)
                                                        .withPolicyDocument(TEST_ALLOW_POLICY));

            GetUserPolicyResult response = iam
                    .getUserPolicy(new GetUserPolicyRequest().withUserName(
                            username).withPolicyName(policyName));

            assertEquals(username, response.getUserName());
            assertEquals(policyName, response.getPolicyName());
            assertEquals(TEST_ALLOW_POLICY,
                         URLDecoder.decode(response.getPolicyDocument(), "UTF-8"));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestPutGetGroupPolicy() throws UnsupportedEncodingException {
        String groupname = IAMUtil.uniqueName();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.createGroup(new CreateGroupRequest().withGroupName(groupname)
                                                    .withPath(IAMUtil.TEST_PATH));

            iam.putGroupPolicy(new PutGroupPolicyRequest()
                                       .withGroupName(groupname).withPolicyName(policyName)
                                       .withPolicyDocument(TEST_ALLOW_POLICY));

            GetGroupPolicyResult response = iam
                    .getGroupPolicy(new GetGroupPolicyRequest().withGroupName(
                            groupname).withPolicyName(policyName));

            assertEquals(groupname, response.getGroupName());
            assertEquals(policyName, response.getPolicyName());
            assertEquals(TEST_ALLOW_POLICY,
                         URLDecoder.decode(response.getPolicyDocument(), "UTF-8"));
        } finally {
            iam.deleteGroupPolicy(new DeleteGroupPolicyRequest().withGroupName(
                    groupname).withPolicyName(policyName));
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestGetNonExistantPolicy() {
        String username = IAMUtil.createTestUser();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.getUserPolicy(new GetUserPolicyRequest().withUserName(username)
                                                        .withPolicyName(policyName));
        } finally {
            IAMUtil.deleteTestUsers();
        }
    }

    @Test
    public void TestListUserPolicies() {
        String username = IAMUtil.createTestUser();
        String[] policyNames = new String[3];
        int nPolicies = 3;

        try {
            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putUserPolicy(new PutUserPolicyRequest()
                                          .withUserName(username).withPolicyName(policyNames[i])
                                          .withPolicyDocument(TEST_ALLOW_POLICY));
            }

            ListUserPoliciesResult response = iam
                    .listUserPolicies(new ListUserPoliciesRequest()
                                              .withUserName(username));

            assertEquals(nPolicies, response.getPolicyNames().size());

            int matches = 0;
            for (String name : response.getPolicyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }
            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestListGroupPolicies() {
        String grpname = IAMUtil.uniqueName();
        String[] policyNames = new String[3];
        int nPolicies = 3;

        try {
            iam.createGroup(new CreateGroupRequest().withGroupName(grpname)
                                                    .withPath(IAMUtil.TEST_PATH));

            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putGroupPolicy(new PutGroupPolicyRequest()
                                           .withGroupName(grpname).withPolicyName(policyNames[i])
                                           .withPolicyDocument(TEST_ALLOW_POLICY));
            }

            ListGroupPoliciesResult response = iam
                    .listGroupPolicies(new ListGroupPoliciesRequest()
                                               .withGroupName(grpname));

            assertEquals(nPolicies,
                         response.getPolicyNames().size());

            int matches = 0;
            for (String name : response.getPolicyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }
            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            for (int i = 0; i < nPolicies; i++) {
                iam.deleteGroupPolicy(new DeleteGroupPolicyRequest()
                                              .withGroupName(grpname).withPolicyName(policyNames[i]));
            }

            iam.deleteGroup(new DeleteGroupRequest().withGroupName(grpname));
        }
    }

    @Test
    public void TestListUserPoliciesPaging() {
        String username = IAMUtil.createTestUser();
        int nPolicies = 4;
        String[] policyNames = new String[nPolicies];

        try {
            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putUserPolicy(new PutUserPolicyRequest()
                                          .withUserName(username).withPolicyName(policyNames[i])
                                          .withPolicyDocument(TEST_ALLOW_POLICY));
            }

            ListUserPoliciesResult response = iam
                    .listUserPolicies(new ListUserPoliciesRequest()
                                              .withUserName(username).withMaxItems(2));

            assertEquals(2, response.getPolicyNames().size());
            assertTrue(response.isTruncated());
            String marker = response.getMarker();

            int matches = 0;
            for (String name : response.getPolicyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            response = iam.listUserPolicies(new ListUserPoliciesRequest()
                                                    .withUserName(username).withMarker(marker));

            assertEquals(nPolicies - 2,
                         response.getPolicyNames().size());
            assertFalse(response.isTruncated());

            for (String name : response.getPolicyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestListGroupPoliciesPaging() {
        String grpname = IAMUtil.uniqueName();
        int nPolicies = 3;
        String[] policyNames = new String[nPolicies];

        try {
            iam.createGroup(new CreateGroupRequest().withGroupName(grpname)
                                                    .withPath(IAMUtil.TEST_PATH));

            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putGroupPolicy(new PutGroupPolicyRequest()
                                           .withGroupName(grpname).withPolicyName(policyNames[i])
                                           .withPolicyDocument(TEST_ALLOW_POLICY));
            }

            ListGroupPoliciesResult response = iam
                    .listGroupPolicies(new ListGroupPoliciesRequest()
                                               .withGroupName(grpname).withMaxItems(2));

            assertEquals(2,
                         response.getPolicyNames().size());
            assertTrue(response.isTruncated());
            String marker = response.getMarker();

            int matches = 0;
            for (String name : response.getPolicyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            response = iam.listGroupPolicies(new ListGroupPoliciesRequest()
                                                     .withGroupName(grpname).withMarker(marker));

            assertEquals(nPolicies - 2,
                         response.getPolicyNames().size());
            assertFalse(response.isTruncated());

            for (String name : response.getPolicyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            for (int i = 0; i < nPolicies; i++) {
                iam.deleteGroupPolicy(new DeleteGroupPolicyRequest()
                                              .withGroupName(grpname).withPolicyName(policyNames[i]));
            }

            iam.deleteGroup(new DeleteGroupRequest().withGroupName(grpname));
        }
    }

    @Test
    public void TestDeleteUserPolicy() {
        String username = IAMUtil.createTestUser();
        String pName = IAMUtil.uniqueName();

        try {
            iam.putUserPolicy(new PutUserPolicyRequest().withUserName(username)
                                                        .withPolicyName(pName)
                                                        .withPolicyDocument(TEST_ALLOW_POLICY));

            ListUserPoliciesResult response = iam
                    .listUserPolicies(new ListUserPoliciesRequest()
                                              .withUserName(username));

            assertEquals(1, response.getPolicyNames().size());

            iam.deleteUserPolicy(new DeleteUserPolicyRequest().withUserName(
                    username).withPolicyName(pName));

            response = iam.listUserPolicies(new ListUserPoliciesRequest()
                                                    .withUserName(username));

            assertEquals(0, response.getPolicyNames().size());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestDeleteGroupPolicy() {
        String groupname = IAMUtil.uniqueName();
        String pName = IAMUtil.uniqueName();

        try {
            iam.createGroup(new CreateGroupRequest().withGroupName(groupname)
                                                    .withPath(IAMUtil.TEST_PATH));

            iam.putGroupPolicy(new PutGroupPolicyRequest()
                                       .withGroupName(groupname).withPolicyName(pName)
                                       .withPolicyDocument(TEST_ALLOW_POLICY));

            ListGroupPoliciesResult response = iam
                    .listGroupPolicies(new ListGroupPoliciesRequest()
                                               .withGroupName(groupname));

            assertEquals(1,
                         response.getPolicyNames().size());

            iam.deleteGroupPolicy(new DeleteGroupPolicyRequest().withGroupName(
                    groupname).withPolicyName(pName));

            response = iam.listGroupPolicies(new ListGroupPoliciesRequest()
                                                     .withGroupName(groupname));

            assertEquals(0,
                         response.getPolicyNames().size());
        } finally {
            iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteNonExistentGroupPolicyException() {
        String groupname = IAMUtil.uniqueName();

        try {
            iam.createGroup(new CreateGroupRequest().withGroupName(groupname)
                                                    .withPath(IAMUtil.TEST_PATH));
            iam.deleteGroupPolicy(new DeleteGroupPolicyRequest().withGroupName(
                    groupname).withPolicyName(IAMUtil.uniqueName()));
        } finally {
            iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestGetNonExistentGroupPolicyException() {
        String groupname = IAMUtil.uniqueName();

        try {
            iam.createGroup(new CreateGroupRequest().withGroupName(groupname)
                                                    .withPath(IAMUtil.TEST_PATH));
            iam.getGroupPolicy(new GetGroupPolicyRequest().withGroupName(
                    groupname).withPolicyName(IAMUtil.uniqueName()));
        } finally {
            iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteNonExistentUserPolicyException() {
        String username = IAMUtil.createTestUser();

        try {
            iam.deleteUserPolicy(new DeleteUserPolicyRequest().withUserName(
                    username).withPolicyName(IAMUtil.uniqueName()));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestGetNonExistentUserPolicyException() {
        String username = IAMUtil.createTestUser();

        try {
            iam.getUserPolicy(new GetUserPolicyRequest().withUserName(username)
                                                        .withPolicyName(IAMUtil.uniqueName()));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = MalformedPolicyDocumentException.class)
    public void TestPutUserPolicyMalformedPolicyDocumentException() {
        String username = IAMUtil.createTestUser();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.putUserPolicy(new PutUserPolicyRequest().withUserName(username)
                                                        .withPolicyName(policyName).withPolicyDocument("["));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

}
