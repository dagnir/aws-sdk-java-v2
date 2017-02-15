package software.amazon.awssdk.services.gamelift;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.gamelift.model.Alias;
import software.amazon.awssdk.services.gamelift.model.CreateAliasRequest;
import software.amazon.awssdk.services.gamelift.model.CreateAliasResult;
import software.amazon.awssdk.services.gamelift.model.DeleteAliasRequest;
import software.amazon.awssdk.services.gamelift.model.DescribeAliasRequest;
import software.amazon.awssdk.services.gamelift.model.DescribeAliasResult;
import software.amazon.awssdk.services.gamelift.model.RoutingStrategy;
import software.amazon.awssdk.services.gamelift.model.RoutingStrategyType;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class ServiceIntegrationTest extends AWSIntegrationTestBase {

    private static AmazonGameLift gameLift;

    private static String aliasId = null;

    @BeforeClass
    public static void setUp() throws IOException {
        gameLift = new AmazonGameLiftClient(getCredentials());
    }

    @AfterClass
    public static void cleanUp() {
        if (aliasId != null) {
            gameLift.deleteAlias(new DeleteAliasRequest().withAliasId(aliasId));
        }
    }

    @Test
    public void aliasOperations() {
        String aliasName = "alias-foo";
        String fleetId = "fleet-foo";

        CreateAliasResult createAliasResult = gameLift
                .createAlias(new CreateAliasRequest().withName(aliasName).withRoutingStrategy(
                        new RoutingStrategy().withType(RoutingStrategyType.SIMPLE).withFleetId(fleetId)));

        Alias createdAlias = createAliasResult.getAlias();
        aliasId = createdAlias.getAliasId();
        RoutingStrategy strategy = createdAlias.getRoutingStrategy();

        Assert.assertNotNull(createAliasResult);
        Assert.assertNotNull(createAliasResult.getAlias());
        Assert.assertEquals(createdAlias.getName(), aliasName);
        Assert.assertEquals(strategy.getType(), RoutingStrategyType.SIMPLE.toString());
        Assert.assertEquals(strategy.getFleetId(), fleetId);

        DescribeAliasResult describeAliasResult = gameLift
                .describeAlias(new DescribeAliasRequest().withAliasId(aliasId));
        Assert.assertNotNull(describeAliasResult);
        Alias describedAlias = describeAliasResult.getAlias();
        Assert.assertEquals(createdAlias, describedAlias);
    }

}
