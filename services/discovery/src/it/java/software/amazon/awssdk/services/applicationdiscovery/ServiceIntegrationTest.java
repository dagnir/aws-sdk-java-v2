package software.amazon.awssdk.services.applicationdiscovery;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.applicationdiscovery.model.ConfigurationItemType;
import software.amazon.awssdk.services.applicationdiscovery.model.ListConfigurationsRequest;
import software.amazon.awssdk.services.applicationdiscovery.model.ListConfigurationsResult;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class ServiceIntegrationTest extends AWSIntegrationTestBase {

    private static AWSApplicationDiscovery discoveryService;

    @BeforeClass
    public static void setUp() {
        discoveryService = new AWSApplicationDiscoveryClient(getCredentials());
    }

    @Test
    public void testListOperation() {
        ListConfigurationsResult listResult = discoveryService.listConfigurations(
                new ListConfigurationsRequest().withConfigurationType(ConfigurationItemType.PROCESS));
        Assert.assertNotNull(listResult);
        Assert.assertNotNull(listResult.getConfigurations());
    }

}
