package software.amazon.awssdk.services.glacier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import software.amazon.awssdk.services.glacier.model.GetVaultNotificationsRequest;
import software.amazon.awssdk.services.glacier.model.SetVaultNotificationsRequest;
import software.amazon.awssdk.services.glacier.model.VaultNotificationConfig;

public class ConfigurationIntegrationTest extends GlacierIntegrationTestBase {
    /**
     * Tests the various configuration operations on Glacier (audit logging
     * configuration, permissions and notifications).
     */
    @Test
    public void testConfigurationOperation() throws Exception {
        initializeClient();

        String topic = "arn:aws:sns:us-east-1:311841313490:topic";
        // TODO: It would be nice to have enums in the Coral model for event types
        String event = "ArchiveRetrievalCompleted";
        glacier.setVaultNotifications(new SetVaultNotificationsRequest().withAccountId(accountId).withVaultName(vaultName)
                .withVaultNotificationConfig(new VaultNotificationConfig().withSNSTopic(topic).withEvents(event)));

        Thread.sleep(1000 * 5);

        VaultNotificationConfig config = glacier.getVaultNotifications(new GetVaultNotificationsRequest().withAccountId(accountId).withVaultName(vaultName)).getVaultNotificationConfig();
        assertTrue(1 == config.getEvents().size());
        assertEquals(event, config.getEvents().get(0));
        assertEquals(topic, config.getSNSTopic());
    }
 }