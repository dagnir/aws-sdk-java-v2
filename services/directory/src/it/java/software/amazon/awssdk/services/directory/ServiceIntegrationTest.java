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

package software.amazon.awssdk.services.directory;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.directory.model.CreateDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DeleteDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DescribeDirectoriesRequest;
import software.amazon.awssdk.services.directory.model.DirectorySize;
import software.amazon.awssdk.services.directory.model.DirectoryVpcSettings;
import software.amazon.awssdk.services.directory.model.InvalidNextTokenException;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;

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
        List<Vpc> vpcs = ec2Client.describeVpcs(new DescribeVpcsRequest()).getVpcs();
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
