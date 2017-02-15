package software.amazon.awssdk.services.applicationautoscaling;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class ServiceIntegrationTest extends AWSIntegrationTestBase {

    private static AWSApplicationAutoScaling autoscaling;

    @BeforeClass
    public static void setUp() {
        autoscaling = new AWSApplicationAutoScalingClient(getCredentials());
    }

    @Test
    public void testScalingPolicy() {
        DescribeScalingPoliciesResult res = autoscaling.describeScalingPolicies(new DescribeScalingPoliciesRequest()
                                                                                        .withServiceNamespace(ServiceNamespace.Ecs));
        Assert.assertNotNull(res);
        Assert.assertNotNull(res.getScalingPolicies());
    }

}
