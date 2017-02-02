package software.amazon.awssdk.services.sts;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.auth.policy.Action;
import software.amazon.awssdk.auth.policy.Policy;
import software.amazon.awssdk.auth.policy.Resource;
import software.amazon.awssdk.auth.policy.Statement;
import software.amazon.awssdk.auth.policy.Statement.Effect;
import software.amazon.awssdk.auth.policy.actions.SecurityTokenServiceActions;
import software.amazon.awssdk.auth.policy.conditions.IpAddressCondition;
import software.amazon.awssdk.services.identitymanagement.model.AccessKeyMetadata;
import software.amazon.awssdk.services.identitymanagement.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateAccessKeyResult;
import software.amazon.awssdk.services.identitymanagement.model.CreateRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListAccessKeysResult;
import software.amazon.awssdk.services.identitymanagement.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListUserPoliciesResult;
import software.amazon.awssdk.services.identitymanagement.model.PutUserPolicyRequest;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenService;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenServiceClient;
import software.amazon.awssdk.services.securitytoken.model.AssumeRoleRequest;
import software.amazon.awssdk.services.securitytoken.model.AssumeRoleResult;
import software.amazon.awssdk.services.securitytoken.model.DecodeAuthorizationMessageRequest;
import software.amazon.awssdk.services.securitytoken.model.GetFederationTokenRequest;
import software.amazon.awssdk.services.securitytoken.model.GetFederationTokenResult;
import software.amazon.awssdk.services.securitytoken.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.securitytoken.model.GetSessionTokenResult;

public class AssumeRoleIntegrationTest extends IntegrationTestBaseWithIAM {

    private static final int SESSION_DURATION = 60 * 60;
    private static final String ROLE_ARN = "arn:aws:iam::599169622985:role/java-test-role";
    private static final String USER_NAME  = "user-" + System.currentTimeMillis();

    @AfterClass
    public static void tearDown() {
        deleteUser(USER_NAME);
    }

    /** Tests that we can call assumeRole successfully. */
    @Test
    public void testAssumeRole() throws InterruptedException {
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
            .withDurationSeconds(SESSION_DURATION)
            .withRoleArn(ROLE_ARN)
            .withRoleSessionName("Name")
            .withPolicy(new Policy()
                .withStatements(new Statement(Effect.Allow)
                    .withActions(SecurityTokenServiceActions.AllSecurityTokenServiceActions)
                    .withResources(new Resource("*")))
                .toJson());

        AWSSecurityTokenService sts = getIamClient();
        Thread.sleep(1000 * 60);
        AssumeRoleResult assumeRoleResult = sts.assumeRole(assumeRoleRequest);
        assertNotNull(assumeRoleResult.getAssumedRoleUser());
        assertNotNull(assumeRoleResult.getAssumedRoleUser().getArn());
        assertNotNull(assumeRoleResult.getAssumedRoleUser().getAssumedRoleId());
        assertNotNull(assumeRoleResult.getCredentials());
        assertNotNull(assumeRoleResult.getPackedPolicySize());
    }

    private AWSSecurityTokenService getIamClient() {
          iam.createUser(new CreateUserRequest().withUserName(USER_NAME));

          String policyDoc = new Policy()
              .withStatements(new Statement(Effect.Allow)
                  .withActions(SecurityTokenServiceActions.AssumeRole)
                  .withResources(new Resource("*")))
              .toJson();

        iam.putUserPolicy(new PutUserPolicyRequest().withPolicyDocument(policyDoc).withUserName(USER_NAME).withPolicyName("assume-role"));
        CreateAccessKeyResult createAccessKeyResult = iam.createAccessKey(new CreateAccessKeyRequest().withUserName(USER_NAME));
        AWSCredentials credentials = new BasicAWSCredentials(createAccessKeyResult.getAccessKey().getAccessKeyId(), createAccessKeyResult.getAccessKey().getSecretAccessKey());
        return new AWSSecurityTokenServiceClient(credentials);
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

        }
        try {
            deleteUserPoliciesForUser(userName);
        } catch (Exception e) {

        }
        try {
            iam.deleteLoginProfile(new DeleteLoginProfileRequest()
                    .withUserName(userName));
        } catch (Exception e) {

        }
        try {
        iam.deleteUser(new DeleteUserRequest().withUserName(userName));
        } catch (Exception e) {

        }
    }
 }
