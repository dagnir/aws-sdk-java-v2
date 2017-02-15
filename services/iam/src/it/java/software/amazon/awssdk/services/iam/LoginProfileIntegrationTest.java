package software.amazon.awssdk.services.iam;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.model.CreateLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateLoginProfileResult;
import software.amazon.awssdk.services.identitymanagement.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.identitymanagement.model.GetLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetLoginProfileResult;
import software.amazon.awssdk.services.identitymanagement.model.NoSuchEntityException;

/**
 * Integration tests of the login profile APIs of IAM.
 *
 * Adapted from jimfl@'s C# tests:
 *
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/
 * AWSCSharpSDKFactory
 * /mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/LoginProfileTests.cs
 *
 * @author zachmu
 */
public class LoginProfileIntegrationTest extends IntegrationTestBase {

    @Before
    public void TestSetup() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestCreateGetLoginProfile() throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            CreateLoginProfileResult createRes = iam
                    .createLoginProfile(new CreateLoginProfileRequest()
                                                .withUserName(username).withPassword(password));

            Thread.sleep(3 * 3600);

            assertEquals(username, createRes.getLoginProfile().getUserName());

            GetLoginProfileResult getRes = iam
                    .getLoginProfile(new GetLoginProfileRequest()
                                             .withUserName(username));

            assertEquals(username, getRes.getLoginProfile().getUserName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void TestCreateLoginProfileTwiceException()
            throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            iam.createLoginProfile(new CreateLoginProfileRequest()
                                           .withUserName(username).withPassword(password));
            Thread.sleep(3 * 3600);
            iam.createLoginProfile(new CreateLoginProfileRequest()
                                           .withUserName(username).withPassword(password));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteLoginProfile() throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            iam.createLoginProfile(new CreateLoginProfileRequest()
                                           .withUserName(username).withPassword(password));
            Thread.sleep(3 * 3600);
            iam.deleteLoginProfile(new DeleteLoginProfileRequest()
                                           .withUserName(username));
            Thread.sleep(3 * 3600);
            iam.getLoginProfile(new GetLoginProfileRequest()
                                        .withUserName(username));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

}
