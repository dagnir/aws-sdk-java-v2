/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private static GameLiftClient gameLift;

    private static String aliasId = null;

    @BeforeClass
    public static void setUp() throws IOException {
        gameLift = GameLiftClient.builder().withCredentials(CREDENTIALS_PROVIDER_CHAIN).build();
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
