package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.identitymanagement.model.AccessKeyMetadata;
import software.amazon.awssdk.services.identitymanagement.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateAccessKeyResult;
import software.amazon.awssdk.services.identitymanagement.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.identitymanagement.model.LimitExceededException;
import software.amazon.awssdk.services.identitymanagement.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListAccessKeysResult;
import software.amazon.awssdk.services.identitymanagement.model.NoSuchEntityException;

/**
 * Integration tests for access key methods in IAM.
 * 
 * Converted from jimfl@'s C# code here:
 * 
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/
 * AWSCSharpSDKFactory
 * /mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/AccessKeyTests.cs
 * 
 */
public class AccessKeyIntegrationTest extends IntegrationTestBase {

	private static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

	@Before
	public void TestSetup() {
		IAMUtil.deleteUsersAndGroupsInTestNameSpace();
	}

	@Test
	public void TestCreateAccessKey() {
		String username = IAMUtil.createTestUser();
		String keyId = null;
		try {
			CreateAccessKeyResult response = iam
					.createAccessKey(new CreateAccessKeyRequest()
							.withUserName(username));
			keyId = response.getAccessKey().getAccessKeyId();
			assertEquals(System.currentTimeMillis() / MILLISECONDS_IN_DAY,
					response.getAccessKey().getCreateDate().getTime()
							/ MILLISECONDS_IN_DAY);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (keyId != null)
				iam.deleteAccessKey(new DeleteAccessKeyRequest().withUserName(
						username).withAccessKeyId(keyId));

			IAMUtil.deleteTestUsers(username);
		}
	}

	@Test(expected = NoSuchEntityException.class)
	public void TestCreateAccessKeyNonExistentUserException() {
		String username = IAMUtil.uniqueName();
		iam.createAccessKey(new CreateAccessKeyRequest().withUserName(username));
	}

	@Test
	public void TestListAccessKeys() {
		String username = IAMUtil.createTestUser();
		String[] keyIds = new String[2];
		try {
			for (int i = 0; i < 2; i++) {
				CreateAccessKeyResult response = iam
						.createAccessKey(new CreateAccessKeyRequest()
								.withUserName(username));

				keyIds[i] = response.getAccessKey().getAccessKeyId();
			}

			ListAccessKeysResult listRes = iam
					.listAccessKeys(new ListAccessKeysRequest()
							.withUserName(username));

			int matches = 0;
			for (AccessKeyMetadata akm : listRes.getAccessKeyMetadata()) {
				if (akm.getAccessKeyId().equals(keyIds[0]))
					matches |= 1;
				if (akm.getAccessKeyId().equals(keyIds[1]))
					matches |= 2;
			}
			assertEquals(3, matches);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			IAMUtil.deleteTestUsers(username);
		}
	}

	// There is a limit of 2 access keys per user
	@Test(expected = LimitExceededException.class)
	public void TestLimitExceedException() {
		String username = IAMUtil.createTestUser();

		try {
			for (int i = 0; i < 3; i++) {
				iam.createAccessKey(new CreateAccessKeyRequest()
						.withUserName(username));
			}
		} finally {
			IAMUtil.deleteTestUsers(username);
		}
	}

	@Test
	public void TestDeleteAccessKey() {
		String username = IAMUtil.createTestUser();
		String[] keyIds = new String[2];
		try {
			for (int i = 0; i < 2; i++) {
				CreateAccessKeyResult response = iam
						.createAccessKey(new CreateAccessKeyRequest()
								.withUserName(username));

				keyIds[i] = response.getAccessKey().getAccessKeyId();
			}

			ListAccessKeysResult lakRes = iam
					.listAccessKeys(new ListAccessKeysRequest()
							.withUserName(username));

			assertEquals(2, lakRes.getAccessKeyMetadata().size());

			iam.deleteAccessKey(new DeleteAccessKeyRequest().withUserName(
					username).withAccessKeyId(keyIds[0]));

			lakRes = iam.listAccessKeys(new ListAccessKeysRequest()
					.withUserName(username));

			assertEquals(1, lakRes.getAccessKeyMetadata().size());
			assertEquals(keyIds[1], lakRes.getAccessKeyMetadata().get(0)
					.getAccessKeyId());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			IAMUtil.deleteTestUsers(username);
		}
	}

	@Test(expected = NoSuchEntityException.class)
	public void TestDeleteNonExistentAccessKeyException() {
		String username = IAMUtil.createTestUser();
		try {
			CreateAccessKeyResult response = iam
					.createAccessKey(new CreateAccessKeyRequest()
							.withUserName(username));

			String keyId = response.getAccessKey().getAccessKeyId();

			iam.deleteAccessKey(new DeleteAccessKeyRequest().withUserName(
					username).withAccessKeyId(keyId));
			iam.deleteAccessKey(new DeleteAccessKeyRequest().withUserName(
					username).withAccessKeyId(keyId));
		} finally {
			IAMUtil.deleteTestUsers(username);
		}
	}

	/**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SDKGlobalConfiguration.setGlobalTimeOffset(3600);
        iam.listAccessKeys();
        assertTrue("Clockskew is fixed!", SDKGlobalConfiguration.getGlobalTimeOffset() < 60);
    }
}
