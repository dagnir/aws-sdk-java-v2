package software.amazon.awssdk.services.cloudhsm;

import org.junit.Test;

public class ServiceIntegrationTest extends IntegrationTestBase {

    /**
     * Simple smoke test to make sure we fix the empty JSON payload issue.
     */
    @Test
    public void testOperations() {
        client.listHsms().getHsmList();
        client.listAvailableZones();
        client.listHapgs();
        client.listLunaClients();
    }
}
