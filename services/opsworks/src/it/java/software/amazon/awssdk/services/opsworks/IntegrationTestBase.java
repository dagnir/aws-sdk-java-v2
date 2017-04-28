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

package software.amazon.awssdk.services.opsworks;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.Listener;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class IntegrationTestBase extends AwsIntegrationTestBase {

    /** Protocol value used in LB requests. */
    private static final String PROTOCOL = "HTTP";
    /** AZs used for LB. */
    private static final String AVAILABILITY_ZONE = "us-east-1c";
    /** Shared Charlie client for all tests to use. */
    protected static OpsWorksClient opsWorks;
    /** The ELB client used in these tests. */
    protected static ElasticLoadBalancingClient elb;
    protected static String loadBalancerName;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        opsWorks = OpsWorksClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        elb = ElasticLoadBalancingClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        loadBalancerName = "integ-test-" + System.currentTimeMillis();
        Listener expectedListener = new Listener().withInstancePort(8080)
                                                  .withLoadBalancerPort(80)
                                                  .withProtocol(PROTOCOL);

        // Create a load balancer
        elb.createLoadBalancer(
                new CreateLoadBalancerRequest()
                        .withLoadBalancerName(loadBalancerName)
                        .withAvailabilityZones(AVAILABILITY_ZONE)
                        .withListeners(expectedListener));
    }
}
