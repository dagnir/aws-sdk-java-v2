package software.amazon.awssdk.services.iam;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.model.CreateUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateUserResult;
import software.amazon.awssdk.services.identitymanagement.model.DeleteUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.identitymanagement.model.GetUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetUserResult;
import software.amazon.awssdk.services.identitymanagement.model.ListUsersRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListUsersResult;
import software.amazon.awssdk.services.identitymanagement.model.NoSuchEntityException;
import software.amazon.awssdk.services.identitymanagement.model.UpdateUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.User;

/**
 * Integration tests of the user APIs of IAM.
 *
 * Adapted from jimfl@'s C# tests:
 *
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/AWSCSharpSDKFactory/mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/UserTests.cs
 */
public class UserIntegrationTest extends IntegrationTestBase {

    @Before
    public void PreTestRun() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestGetUserImplicit() {
        GetUserRequest request = new GetUserRequest();

        GetUserResult Result = iam.getUser(request);

        assertEquals("arn:aws:iam::599169622985:root", Result.getUser()
                                                             .getArn());
        assertEquals("599169622985", Result.getUser().getUserId());
    }

    @Test
    public void TestCreateGetUser() {
        String username = IAMUtil.uniqueName();

        try {
            CreateUserRequest request = new CreateUserRequest().withUserName(
                    username).withPath(IAMUtil.TEST_PATH);
            CreateUserResult result = iam.createUser(request);
            assertEquals(username, result.getUser().getUserName());
            GetUserResult getResult = iam.getUser(new GetUserRequest()
                                                          .withUserName(username));
            assertEquals(username, getResult.getUser().getUserName());
        } finally {
            iam.deleteUser(new DeleteUserRequest().withUserName(username));
        }
    }

    @Test
    public void TestListUsers() {
        String username1 = IAMUtil.createTestUser();
        String username2 = IAMUtil.createTestUser();
        String username3 = IAMUtil.createTestUser();
        try {
            ListUsersResult Result = iam.listUsers(new ListUsersRequest()
                                                           .withPathPrefix(IAMUtil.TEST_PATH));

            assertEquals(3, Result.getUsers().size());

            int matches = 0;
            for (User user : Result.getUsers()) {
                if (user.getUserName().equals(username1)) {
                    matches |= 1;
                }
                if (user.getUserName().equals(username2)) {
                    matches |= 2;
                }
                if (user.getUserName().equals(username3)) {
                    matches |= 4;
                }
            }
            assertEquals(7, matches);
        } finally {
            IAMUtil.deleteTestUsers(username1, username2, username3);
        }
    }

    @Test
    public void TestUserWithPath() {
        String username = IAMUtil.uniqueName();
        String path = IAMUtil.makePath("one", "two", "three");
        try {
            iam.createUser(new CreateUserRequest().withPath(path).withUserName(
                    username));
            GetUserResult Result = iam.getUser(new GetUserRequest()
                                                       .withUserName(username));
            assertEquals(username, Result.getUser().getUserName());
            assertEquals(path, Result.getUser().getPath());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestListUsersByPath() {
        String username1 = IAMUtil.uniqueName();
        String username2 = IAMUtil.uniqueName();
        String username3 = IAMUtil.uniqueName();
        String username4 = IAMUtil.uniqueName();

        String pathA = IAMUtil.makePath("A");
        String pathB = IAMUtil.makePath("B");

        try {
            iam.createUser(new CreateUserRequest().withUserName(username1)
                                                  .withPath(pathA));
            iam.createUser(new CreateUserRequest().withUserName(username2)
                                                  .withPath(pathA));
            iam.createUser(new CreateUserRequest().withUserName(username3)
                                                  .withPath(pathB));
            iam.createUser(new CreateUserRequest().withUserName(username4)
                                                  .withPath(pathA));

            ListUsersResult Result = iam.listUsers(new ListUsersRequest()
                                                           .withPathPrefix(pathA));

            assertEquals(3, Result.getUsers().size());

            int matches = 0;

            for (User u : Result.getUsers()) {
                if (u.getUserName().equals(username1)) {
                    matches |= 1;
                }
                if (u.getUserName().equals(username2)) {
                    matches |= 2;
                }
                if (u.getUserName().equals(username4)) {
                    matches |= 4;
                }
                if (u.getUserName().equals(username3)) {
                    fail();
                }
            }
            assertEquals(7, matches);

            Result = iam
                    .listUsers(new ListUsersRequest().withPathPrefix(pathB));

            assertEquals(1, Result.getUsers().size());

            matches = 0;

            for (User u : Result.getUsers()) {
                if (u.getUserName().equals(username1)) {
                    fail();
                }
                if (u.getUserName().equals(username2)) {
                    fail();
                }
                if (u.getUserName().equals(username4)) {
                    fail();
                }
                if (u.getUserName().equals(username3)) {
                    matches = 1;
                }
            }
            assertEquals(1, matches);

            Result = iam.listUsers(new ListUsersRequest()
                                           .withPathPrefix(IAMUtil.TEST_PATH));
            assertEquals(4, Result.getUsers().size());

        } finally {
            IAMUtil.deleteTestUsers(username1, username2, username3, username4);
        }
    }

    @Test
    public void TestListUsersMaxResults() {
        String username1 = IAMUtil.createTestUser();
        String username2 = IAMUtil.createTestUser();
        String username3 = IAMUtil.createTestUser();
        String username4 = IAMUtil.createTestUser();

        try {
            ListUsersResult Result = iam.listUsers(new ListUsersRequest()
                                                           .withMaxItems(2).withPathPrefix(IAMUtil.TEST_PATH));

            assertEquals(2, Result.getUsers().size());
            assertEquals(true, Result.isTruncated());

            int matches = 0;

            for (User u : Result.getUsers()) {
                if (u.getUserName().equals(username1)) {
                    matches |= 1;
                }
                if (u.getUserName().equals(username2)) {
                    matches |= 2;
                }
                if (u.getUserName().equals(username4)) {
                    matches |= 3;
                }
                if (u.getUserName().equals(username3)) {
                    matches |= 4;
                }
            }

            String marker = Result.getMarker();

            Result = iam.listUsers(new ListUsersRequest().withPathPrefix(
                    IAMUtil.TEST_PATH).withMarker(marker));

            assertEquals(2, Result.getUsers().size());
            assertEquals(false, Result.isTruncated());

            for (User u : Result.getUsers()) {
                if (u.getUserName().equals(username1)) {
                    matches |= 1;
                }
                if (u.getUserName().equals(username2)) {
                    matches |= 2;
                }
                if (u.getUserName().equals(username4)) {
                    matches |= 3;
                }
                if (u.getUserName().equals(username3)) {
                    matches |= 4;
                }
            }

            assertEquals(7, matches);
        } finally {
            IAMUtil.deleteTestUsers(username1, username2, username3, username4);
        }
    }

    @Test
    public void TestUpdateUser() {
        String username = IAMUtil.uniqueName(), newusername = IAMUtil
                .uniqueName();
        String firstPath = IAMUtil.makePath("first"), secondPath = IAMUtil
                .makePath("second");

        try {
            iam.createUser(new CreateUserRequest().withUserName(username)
                                                  .withPath(firstPath));

            GetUserResult Result = iam.getUser(new GetUserRequest()
                                                       .withUserName(username));
            assertEquals(firstPath, Result.getUser().getPath());

            String id = Result.getUser().getUserId();

            iam.updateUser(new UpdateUserRequest().withUserName(username)
                                                  .withNewPath(secondPath).withNewUserName(newusername));

            Result = iam
                    .getUser(new GetUserRequest().withUserName(newusername));

            assertEquals(newusername, Result.getUser().getUserName());
            assertEquals(secondPath, Result.getUser().getPath());
            assertEquals(id, Result.getUser().getUserId());
        } finally {
            iam.deleteUser(new DeleteUserRequest().withUserName(newusername));
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteUser() {
        String username = IAMUtil.uniqueName();

        iam.createUser(new CreateUserRequest().withUserName(username).withPath(
                IAMUtil.TEST_PATH));

        GetUserResult Result = iam.getUser(new GetUserRequest()
                                                   .withUserName(username));
        assertEquals(username, Result.getUser().getUserName());

        iam.deleteUser(new DeleteUserRequest().withUserName(username));

        iam.getUser(new GetUserRequest().withUserName(username));
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void TestDoubleCreateUser() {
        String username = IAMUtil.uniqueName();

        try {
            iam.createUser(new CreateUserRequest().withUserName(username)
                                                  .withPath(IAMUtil.TEST_PATH));
            iam.createUser(new CreateUserRequest().withUserName(username)
                                                  .withPath(IAMUtil.TEST_PATH));
        } finally {
            iam.deleteUser(new DeleteUserRequest().withUserName(username));
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestUpdateNonexistantUser() {
        String username = IAMUtil.uniqueName();

        iam.updateUser(new UpdateUserRequest().withUserName(username)
                                              .withNewPath("/lala/"));
    }

}
