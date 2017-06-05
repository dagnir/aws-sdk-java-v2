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

package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;
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

        EC2Client ec2 = EC2Client.builder()
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(URI.create("https://ec2.us-east-1.amazonaws.com"))
                                 .build();

        DescribeAvailabilityZonesResult result =
                ec2.describeAvailabilityZones(DescribeAvailabilityZonesRequest.builder().build());
        AvailabilityZone zone = result.availabilityZones().get(0);
        assertEquals("us-east-1", zone.regionName());

        ec2 = EC2Client.builder()
                       .region(Region.US_WEST_1)
                       .endpointOverride(URI.create("https://ec2.us-west-1.amazonaws.com"))
                       .build();
        result = ec2.describeAvailabilityZones(DescribeAvailabilityZonesRequest.builder().build());
        zone = result.availabilityZones().get(0);
        assertEquals("us-west-1", zone.regionName());
    }
}
