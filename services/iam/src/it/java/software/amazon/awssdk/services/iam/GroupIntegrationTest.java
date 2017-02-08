package software.amazon.awssdk.services.iam;

import static org.junit.Assert.*;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.model.AddUserToGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteConflictException;
import software.amazon.awssdk.services.identitymanagement.model.DeleteGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteUserRequest;
import software.amazon.awssdk.services.identitymanagement.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.identitymanagement.model.GetGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetGroupResult;
import software.amazon.awssdk.services.identitymanagement.model.Group;
import software.amazon.awssdk.services.identitymanagement.model.ListGroupsRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListGroupsResult;
import software.amazon.awssdk.services.identitymanagement.model.NoSuchEntityException;
import software.amazon.awssdk.services.identitymanagement.model.RemoveUserFromGroupRequest;
import software.amazon.awssdk.services.identitymanagement.model.User;

/**
 * Integration tests for group-related IAM interfaces.
 * 
 * Adapted from jimfl@'s C# tests here:
 * 
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/
 * AWSCSharpSDKFactory
 * /mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/GroupTests.cs
 */
public class GroupIntegrationTest extends IntegrationTestBase {

	@Before
	public void PreTestRun() {
		IAMUtil.deleteUsersAndGroupsInTestNameSpace();
	}

	@Test
	public void TestCreateGetGroup() {
		String groupname = UUID.randomUUID().toString().replace('-', '0');

		try {
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname));
			GetGroupResult response = iam.getGroup(new GetGroupRequest()
					.withGroupName(groupname));
			assertEquals(0, response.getUsers().size());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {		
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
		}
	}

	@Test
	public void TestGroupWithUsers() {
		String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
				.uniqueName(), username3 = IAMUtil.uniqueName(), groupname = IAMUtil
				.uniqueName();

		try {
			iam.createUser(new CreateUserRequest().withUserName(username1)
					.withPath(IAMUtil.TEST_PATH));
			iam.createUser(new CreateUserRequest().withUserName(username2)
					.withPath(IAMUtil.TEST_PATH));
			iam.createUser(new CreateUserRequest().withUserName(username3)
					.withPath(IAMUtil.TEST_PATH));

			iam.createGroup(new CreateGroupRequest().withGroupName(groupname)
					.withPath(IAMUtil.TEST_PATH));

			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username1));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username2));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username3));

			GetGroupResult response = iam.getGroup(new GetGroupRequest()
					.withGroupName(groupname));

			assertEquals(3, response.getUsers().size());
			assertFalse(response.isTruncated());

			int matches = 0;

			for (User u : response.getUsers()) {
				if (u.getUserName().equals(username1))
					matches |= 1;
				if (u.getUserName().equals(username2))
					matches |= 2;
				if (u.getUserName().equals(username3))
					matches |= 4;
			}

			assertEquals(7, matches);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username1));
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username2));
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username3));
			iam.deleteUser(new DeleteUserRequest().withUserName(username1));
			iam.deleteUser(new DeleteUserRequest().withUserName(username2));
			iam.deleteUser(new DeleteUserRequest().withUserName(username3));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
		}
	}

	@Test
	public void TestRemoveUsersFromGroup() {
		String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
				.uniqueName(), username3 = IAMUtil.uniqueName(), groupname = IAMUtil
				.uniqueName();

		try {
			iam.createUser(new CreateUserRequest().withUserName(username1)
					.withPath(IAMUtil.TEST_PATH));
			iam.createUser(new CreateUserRequest().withUserName(username2)
					.withPath(IAMUtil.TEST_PATH));
			iam.createUser(new CreateUserRequest().withUserName(username3)
					.withPath(IAMUtil.TEST_PATH));

			iam.createGroup(new CreateGroupRequest().withGroupName(groupname)
					.withPath(IAMUtil.TEST_PATH));

			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username1));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username2));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username3));

			GetGroupResult response = iam.getGroup(new GetGroupRequest()
					.withGroupName(groupname));

			assertEquals(3, response.getUsers().size());

			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username2));

			response = iam.getGroup(new GetGroupRequest()
					.withGroupName(groupname));

			assertEquals(2, response.getUsers().size());

			int matches = 0;

			for (User u : response.getUsers()) {
				if (u.getUserName().equals(username1))
					matches |= 1;
				if (u.getUserName().equals(username2))
					fail();
				if (u.getUserName().equals(username3))
					matches |= 4;
			}

			assertEquals(5, matches);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username1));
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username3));
			iam.deleteUser(new DeleteUserRequest().withUserName(username1));
			iam.deleteUser(new DeleteUserRequest().withUserName(username2));
			iam.deleteUser(new DeleteUserRequest().withUserName(username3));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
		}
	}

	@Test
	public void TestGroupPaging() {
		String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
				.uniqueName(), username3 = IAMUtil.uniqueName(), username4 = IAMUtil
				.uniqueName(), groupname = IAMUtil.uniqueName();

		try {
			iam.createUser(new CreateUserRequest().withUserName(username1)
					.withPath(IAMUtil.TEST_PATH));
			iam.createUser(new CreateUserRequest().withUserName(username2)
					.withPath(IAMUtil.TEST_PATH));
			iam.createUser(new CreateUserRequest().withUserName(username3)
					.withPath(IAMUtil.TEST_PATH));
			iam.createUser(new CreateUserRequest().withUserName(username4)
					.withPath(IAMUtil.TEST_PATH));

			iam.createGroup(new CreateGroupRequest().withGroupName(groupname)
					.withPath(IAMUtil.TEST_PATH));

			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username1));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username2));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username3));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					groupname).withUserName(username4));

			GetGroupResult response = iam.getGroup(new GetGroupRequest()
					.withGroupName(groupname).withMaxItems(2));

			assertEquals(2, response.getUsers().size());
			assertTrue(response.isTruncated());

			String marker = response.getMarker();

			int matches = 0;

			for (User u : response.getUsers()) {
				if (u.getUserName().equals(username1))
					matches |= 1;
				if (u.getUserName().equals(username2))
					matches |= 2;
				if (u.getUserName().equals(username3))
					matches |= 4;
				if (u.getUserName().equals(username4))
					matches |= 8;
			}

			response = iam.getGroup(new GetGroupRequest().withMarker(marker)
					.withGroupName(groupname));

			assertEquals(2, response.getUsers().size());
			assertFalse(response.isTruncated());

			for (User u : response.getUsers()) {
				if (u.getUserName().equals(username1))
					matches |= 1;
				if (u.getUserName().equals(username2))
					matches |= 2;
				if (u.getUserName().equals(username3))
					matches |= 4;
				if (u.getUserName().equals(username4))
					matches |= 8;
			}

			assertEquals(15, matches);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username1));
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username2));
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username3));
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(groupname).withUserName(username4));
			iam.deleteUser(new DeleteUserRequest().withUserName(username1));
			iam.deleteUser(new DeleteUserRequest().withUserName(username2));
			iam.deleteUser(new DeleteUserRequest().withUserName(username3));
			iam.deleteUser(new DeleteUserRequest().withUserName(username4));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
		}
	}

	@Test
	public void TestListGroupWithPaths() {
		String groupname1 = IAMUtil.uniqueName(), groupname2 = IAMUtil
				.uniqueName(), groupname3 = IAMUtil.uniqueName(), groupname4 = IAMUtil
				.uniqueName();

		String pathA = IAMUtil.makePath("A"), pathB = IAMUtil.makePath("B");

		try {
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname1)
					.withPath(pathA));
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname2)
					.withPath(pathA));
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname3)
					.withPath(pathB));
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname4)
					.withPath(pathB));

			ListGroupsResult response = iam.listGroups(new ListGroupsRequest()
					.withPathPrefix(pathA));

			assertEquals(2, response.getGroups().size());

			int matches = 0;

			for (Group g : response.getGroups()) {
				if (g.getGroupName().equals(groupname1))
					matches |= 1;
				if (g.getGroupName().equals(groupname2))
					matches |= 2;
				if (g.getGroupName().equals(groupname3))
					fail();
				if (g.getGroupName().equals(groupname4))
					fail();
			}

			response = iam.listGroups(new ListGroupsRequest()
					.withPathPrefix(pathB));

			assertEquals(2, response.getGroups().size());

			for (Group g : response.getGroups()) {
				if (g.getGroupName().equals(groupname1))
					fail();
				if (g.getGroupName().equals(groupname2))
					fail();
				if (g.getGroupName().equals(groupname3))
					matches |= 4;
				if (g.getGroupName().equals(groupname4))
					matches |= 8;
			}

			assertEquals(15, matches);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname1));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname2));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname3));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname4));
		}
	}

	@Test
	public void TestListGroupsPaging() {
		String groupname1 = IAMUtil.uniqueName(), groupname2 = IAMUtil
				.uniqueName(), groupname3 = IAMUtil.uniqueName(), groupname4 = IAMUtil
				.uniqueName();

		try {
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname1)
					.withPath(IAMUtil.TEST_PATH));
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname2)
					.withPath(IAMUtil.TEST_PATH));
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname3)
					.withPath(IAMUtil.TEST_PATH));
			iam.createGroup(new CreateGroupRequest().withGroupName(groupname4)
					.withPath(IAMUtil.TEST_PATH));

			ListGroupsResult response = iam.listGroups(new ListGroupsRequest()
					.withMaxItems(2).withPathPrefix(IAMUtil.TEST_PATH));

			assertEquals(2, response.getGroups().size());
			assertTrue(response.isTruncated());

			String marker = response.getMarker();

			int matches = 0;

			for (Group g : response.getGroups()) {
				if (g.getGroupName().equals(groupname1))
					matches |= 1;
				if (g.getGroupName().equals(groupname2))
					matches |= 2;
				if (g.getGroupName().equals(groupname3))
					matches |= 4;
				if (g.getGroupName().equals(groupname4))
					matches |= 8;
			}

			response = iam.listGroups(new ListGroupsRequest()
					.withMarker(marker).withPathPrefix(IAMUtil.TEST_PATH));

			assertEquals(2, response.getGroups().size());
			assertFalse(response.isTruncated());

			for (Group g : response.getGroups()) {
				if (g.getGroupName().equals(groupname1))
					matches |= 1;
				if (g.getGroupName().equals(groupname2))
					matches |= 2;
				if (g.getGroupName().equals(groupname3))
					matches |= 4;
				if (g.getGroupName().equals(groupname4))
					matches |= 8;
			}

			assertEquals(15, matches);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname1));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname2));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname3));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname4));
		}

	}

	@Test(expected = NoSuchEntityException.class)
	public void AddUserToNonExistentGroup() {
		String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();
		try {
			iam.createUser(new CreateUserRequest().withUserName(username)
					.withPath(IAMUtil.TEST_PATH));
			iam.addUserToGroup(new AddUserToGroupRequest().withGroupName(
					grpname).withUserName(username));
		} finally {
			iam.deleteUser(new DeleteUserRequest().withUserName(username));
		}
	}

	@Test(expected = EntityAlreadyExistsException.class)
	public void TestDoubleCreation() {
		String grpname = IAMUtil.uniqueName();

		try {
			iam.createGroup(new CreateGroupRequest().withGroupName(grpname)
					.withPath(IAMUtil.TEST_PATH));
			iam.createGroup(new CreateGroupRequest().withGroupName(grpname)
					.withPath(IAMUtil.TEST_PATH));
		} finally {
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(grpname));
		}
	}

	@Test(expected = DeleteConflictException.class)
	public void TestDeleteUserInGroupThrowsException() {
		String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();

		try {
			iam.createUser(new CreateUserRequest().withUserName(username)
					.withPath(IAMUtil.TEST_PATH));
			iam.createGroup(new CreateGroupRequest().withGroupName(grpname)
					.withPath(IAMUtil.TEST_PATH));
			iam.addUserToGroup(new AddUserToGroupRequest().withUserName(
					username).withGroupName(grpname));

			iam.deleteUser(new DeleteUserRequest().withUserName(username));
		} finally {
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(grpname).withUserName(username));
			iam.deleteUser(new DeleteUserRequest().withUserName(username));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(grpname));
		}
	}

	@Test(expected = DeleteConflictException.class)
	public void TestDeleteGroupWithUsersThrowsException() {
		String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();

		try {
			iam.createUser(new CreateUserRequest().withUserName(username)
					.withPath(IAMUtil.TEST_PATH));
			iam.createGroup(new CreateGroupRequest().withGroupName(grpname)
					.withPath(IAMUtil.TEST_PATH));
			iam.addUserToGroup(new AddUserToGroupRequest().withUserName(
					username).withGroupName(grpname));

			iam.deleteGroup(new DeleteGroupRequest().withGroupName(grpname));
		} finally {
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest()
					.withGroupName(grpname).withUserName(username));
			iam.deleteUser(new DeleteUserRequest().withUserName(username));
			iam.deleteGroup(new DeleteGroupRequest().withGroupName(grpname));
		}
	}
}
