package software.amazon.awssdk.services.directory;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.services.directory.model.CreateDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DeleteDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DescribeDirectoriesRequest;
import software.amazon.awssdk.services.directory.model.DirectorySize;
import software.amazon.awssdk.services.directory.model.DirectoryVpcSettings;
import software.amazon.awssdk.services.directory.model.InvalidNextTokenException;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;

import junit.framework.Assert;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String US_EAST_1A = "us-east-1a";
    private static final String US_EAST_1B = "us-east-1b";

    @Test
    public void testDirectories() {

        String vpcId = getVpcId();
        // Creating a directory requires at least two subnets located in
        // different availability zones
        String subnetId_0 = getSubnetIdInVpc(vpcId, US_EAST_1A);
        String subnetId_1 = getSubnetIdInVpc(vpcId, US_EAST_1B);

        String dsId = dsClient
                .createDirectory(new CreateDirectoryRequest().withDescription("This is my directory!")
                        .withName("AWS.Java.SDK.Directory").withShortName("md").withPassword("My.Awesome.Password.2015")
                        .withSize(DirectorySize.Small).withVpcSettings(
                                new DirectoryVpcSettings().withVpcId(vpcId).withSubnetIds(subnetId_0, subnetId_1)))
                .getDirectoryId();

        dsClient.deleteDirectory(new DeleteDirectoryRequest().withDirectoryId(dsId));
    }

    private String getVpcId() {
        List<Vpc> vpcs = ec2Client.describeVpcs().getVpcs();
        if (vpcs.isEmpty()) {
            Assert.fail("No VPC found in this account.");
        }
        return vpcs.get(0).getVpcId();
    }

    private String getSubnetIdInVpc(String vpcId, String az) {
        List<Subnet> subnets = ec2Client.describeSubnets(new DescribeSubnetsRequest()
                .withFilters(new Filter("vpc-id").withValues(vpcId), new Filter("availabilityZone").withValues(az)))
                .getSubnets();
        if (subnets.isEmpty()) {
            Assert.fail("No Subnet found in VPC " + vpcId + " AvailabilityZone: " + az);
        }
        return subnets.get(0).getSubnetId();
    }

    /**
     * Tests that an exception with a member in it is serialized properly. See TT0064111680
     */
    @Test
    public void describeDirectories_InvalidNextToken_ThrowsExceptionWithRequestIdPresent() {
        try {
            dsClient.describeDirectories(new DescribeDirectoriesRequest().withNextToken("invalid"));
        } catch (InvalidNextTokenException e) {
            assertNotNull(e.getRequestId());
        }
    }
}
