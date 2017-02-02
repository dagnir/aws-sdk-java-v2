package software.amazon.awssdk.services.glacier;

import org.junit.Test;

import software.amazon.awssdk.services.glacier.model.InitiateJobRequest;
import software.amazon.awssdk.services.glacier.model.JobParameters;

public class InventoryIntegrationTest extends GlacierIntegrationTestBase {
    /** Tests that we can request an inventory for our vault. */
    @Test
    public void testInventory() throws Exception {
        initializeClient();

        glacier.initiateJob(new InitiateJobRequest()
            .withAccountId(accountId)
            .withVaultName(vaultName)
            .withJobParameters(new JobParameters()
                    .withType("inventory-retrieval")));
    }
 }