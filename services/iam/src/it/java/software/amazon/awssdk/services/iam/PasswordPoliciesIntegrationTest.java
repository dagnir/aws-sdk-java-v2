package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.model.GetAccountPasswordPolicyResult;
import software.amazon.awssdk.services.identitymanagement.model.NoSuchEntityException;
import software.amazon.awssdk.services.identitymanagement.model.UpdateAccountPasswordPolicyRequest;

public class PasswordPoliciesIntegrationTest extends IntegrationTestBase {

    /** Tests that we can create, list and delete account aliases. */
    @Test
    public void testAccountAliases() throws Exception {
        int minimumPasswordLength = 8;
        iam.updateAccountPasswordPolicy(new UpdateAccountPasswordPolicyRequest()
                                                .withMinimumPasswordLength(minimumPasswordLength)
                                                .withRequireLowercaseCharacters(true)
                                                .withRequireNumbers(true)
                                                .withRequireSymbols(true)
                                                .withRequireUppercaseCharacters(true));

        GetAccountPasswordPolicyResult accountPasswordPolicy = iam.getAccountPasswordPolicy();
        assertEquals(minimumPasswordLength, accountPasswordPolicy.getPasswordPolicy().getMinimumPasswordLength().intValue());
        assertTrue(accountPasswordPolicy.getPasswordPolicy().getRequireLowercaseCharacters());
        assertTrue(accountPasswordPolicy.getPasswordPolicy().getRequireNumbers());
        assertTrue(accountPasswordPolicy.getPasswordPolicy().getRequireSymbols());
        assertTrue(accountPasswordPolicy.getPasswordPolicy().getRequireUppercaseCharacters());

        minimumPasswordLength = 6;
        iam.updateAccountPasswordPolicy(new UpdateAccountPasswordPolicyRequest()
                                                .withMinimumPasswordLength(minimumPasswordLength)
                                                .withRequireLowercaseCharacters(false)
                                                .withRequireNumbers(false)
                                                .withRequireSymbols(false)
                                                .withRequireUppercaseCharacters(false));

        accountPasswordPolicy = iam.getAccountPasswordPolicy();
        assertEquals(minimumPasswordLength, accountPasswordPolicy.getPasswordPolicy().getMinimumPasswordLength().intValue());
        assertFalse(accountPasswordPolicy.getPasswordPolicy().getRequireLowercaseCharacters());
        assertFalse(accountPasswordPolicy.getPasswordPolicy().getRequireNumbers());
        assertFalse(accountPasswordPolicy.getPasswordPolicy().getRequireSymbols());
        assertFalse(accountPasswordPolicy.getPasswordPolicy().getRequireUppercaseCharacters());

        iam.deleteAccountPasswordPolicy();
        try {
            iam.getAccountPasswordPolicy().getPasswordPolicy();
            fail("Should have thrown an exception for a missing policy");
        } catch (NoSuchEntityException e) {
        }
    }
}
