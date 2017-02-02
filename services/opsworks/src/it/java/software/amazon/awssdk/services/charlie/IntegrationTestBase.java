package software.amazon.awssdk.services.charlie;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;

import software.amazon.awssdk.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import software.amazon.awssdk.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.Listener;
import software.amazon.awssdk.services.opsworks.AWSOpsWorksClient;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class IntegrationTestBase extends AWSIntegrationTestBase {

    /** Shared Charlie client for all tests to use */
    protected static AWSOpsWorksClient opsWorks;

    /** The ELB client used in these tests */
    protected static AmazonElasticLoadBalancing elb;

    protected static String  loadBalancerName;

    /** Protocol value used in LB requests */
    private static final String PROTOCOL = "HTTP";

    /** AZs used for LB */
    private static final String AVAILABILITY_ZONE = "us-east-1c";

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        opsWorks = new AWSOpsWorksClient(getCredentials());
        elb = new AmazonElasticLoadBalancingClient(getCredentials());

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
