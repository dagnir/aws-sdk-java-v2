package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesResult;

/**
 * Integration tests for changing the EC2 client's endpoint.
 *
 * @author fulghum@amazon.com
 */
public class EC2EndpointIntegrationTest {

    /**
     * Tests that the EC2 client can correctly change its endpoint and talk to a
     * different EC2 region.
     */
    @Test
    public void testEc2Regions() {

        AmazonEC2Client ec2 = new AmazonEC2Client();

        ec2.setEndpoint("https://ec2.us-east-1.amazonaws.com");
        DescribeAvailabilityZonesResult result = ec2.describeAvailabilityZones();
        AvailabilityZone zone = result.getAvailabilityZones().get(0);
        assertEquals("us-east-1", zone.getRegionName());

        ec2.setEndpoint("https://ec2.us-west-1.amazonaws.com");
        result = ec2.describeAvailabilityZones();
        zone = result.getAvailabilityZones().get(0);
        assertEquals("us-west-1", zone.getRegionName());
    }
}
