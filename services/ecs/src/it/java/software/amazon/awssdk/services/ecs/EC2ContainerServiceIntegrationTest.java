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

package software.amazon.awssdk.services.ecs;

import java.util.Arrays;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ecs.model.ContainerDefinition;
import software.amazon.awssdk.services.ecs.model.CreateClusterRequest;
import software.amazon.awssdk.services.ecs.model.CreateClusterResult;
import software.amazon.awssdk.services.ecs.model.DeleteClusterRequest;
import software.amazon.awssdk.services.ecs.model.DescribeClustersRequest;
import software.amazon.awssdk.services.ecs.model.ListClustersRequest;
import software.amazon.awssdk.services.ecs.model.ListTaskDefinitionsRequest;
import software.amazon.awssdk.services.ecs.model.PortMapping;
import software.amazon.awssdk.services.ecs.model.RegisterTaskDefinitionRequest;
import software.amazon.awssdk.services.ecs.model.RegisterTaskDefinitionResult;
import software.amazon.awssdk.test.AwsTestBase;

public class EC2ContainerServiceIntegrationTest extends AwsTestBase {

    private static final String CLUSTER_NAME =
            "java-sdk-test-cluster-" + System.currentTimeMillis();

    private static ECSClient client;
    private static String clusterArn;

    @BeforeClass
    public static void setup() throws Exception {
        //        BasicConfigurator.configure();

        setUpCredentials();

        client = ECSClient.builder().withCredentials(CREDENTIALS_PROVIDER_CHAIN).build();

        CreateClusterResult result = client.createCluster(
                new CreateClusterRequest()
                        .withClusterName(CLUSTER_NAME));

        Assert.assertEquals(CLUSTER_NAME, result.getCluster().getClusterName());
        Assert.assertNotNull(result.getCluster().getClusterArn());
        Assert.assertNotNull(result.getCluster().getStatus());

        clusterArn = result.getCluster().getClusterArn();

        while (!client.describeClusters(new DescribeClustersRequest()
                                                .withClusters(CLUSTER_NAME))
                      .getClusters()
                      .get(0)
                      .getStatus().equals("ACTIVE")) {

            Thread.sleep(1000);
        }
    }

    @AfterClass
    public static void cleanup() {
        if (client != null) {
            client.deleteCluster(new DeleteClusterRequest().withCluster(CLUSTER_NAME));
        }
    }

    @Test
    public void basicTest() {
        List<String> arns = client.listClusters(new ListClustersRequest()).getClusterArns();
        Assert.assertNotNull(arns);
        Assert.assertTrue(arns.contains(clusterArn));

        RegisterTaskDefinitionResult result =
                client.registerTaskDefinition(new RegisterTaskDefinitionRequest()

                                                      .withFamily("test")
                                                      .withContainerDefinitions(new ContainerDefinition()
                                                                                        .withCommand("command", "command", "command")
                                                                                        .withCpu(1)
                                                                                        .withEntryPoint("entryPoint", "entryPoint")
                                                                                        .withImage("image")
                                                                                        .withMemory(1)
                                                                                        .withName("test")
                                                                                        .withPortMappings(new PortMapping()
                                                                                                                  .withHostPort(12345)
                                                                                                                  .withContainerPort(6789)
                                                                                                         )
                                                                               )
                                             );

        Assert.assertEquals("test", result.getTaskDefinition().getFamily());
        Assert.assertNotNull(result.getTaskDefinition().getRevision());
        Assert.assertNotNull(result.getTaskDefinition().getTaskDefinitionArn());

        ContainerDefinition def = result.getTaskDefinition()
                                        .getContainerDefinitions()
                                        .get(0);

        Assert.assertEquals("image", def.getImage());
        Assert.assertEquals(
                Arrays.asList("entryPoint", "entryPoint"),
                def.getEntryPoint());

        Assert.assertEquals(
                Arrays.asList("command", "command", "command"),
                def.getCommand());

        // Can't deregister task definitions yet... :(

        List<String> taskArns = client.listTaskDefinitions(new ListTaskDefinitionsRequest())
                                      .getTaskDefinitionArns();

        Assert.assertNotNull(taskArns);
        Assert.assertFalse(taskArns.isEmpty());
    }
}
