package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.services.identitymanagement.model.CreateAccountAliasRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteAccountAliasRequest;

public class AccountAliasesIntegrationTest extends IntegrationTestBase {

	private static final String ACCOUNT_ALIAS = "java-sdk-alias-" + System.currentTimeMillis();


	/** Tests that we can create, list and delete account aliases. */
	@Test
	public void testAccountAliases() throws Exception {
		iam.createAccountAlias(new CreateAccountAliasRequest(ACCOUNT_ALIAS));

		List<String> accountAliases = iam.listAccountAliases().getAccountAliases();
		assertNotNull(accountAliases);
		assertEquals(1, accountAliases.size());
		assertEquals(ACCOUNT_ALIAS, accountAliases.get(0));

		iam.deleteAccountAlias(new DeleteAccountAliasRequest(ACCOUNT_ALIAS));
		assertTrue(iam.listAccountAliases().getAccountAliases().isEmpty());
	}
}
