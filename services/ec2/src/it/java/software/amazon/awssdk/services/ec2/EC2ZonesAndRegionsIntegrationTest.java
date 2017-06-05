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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesResult;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Region;

/**
 * Integration tests for the EC2 Availability Zones and Regions operations.
 *
 * @author fulghum@amazon.com
 */
public class EC2ZonesAndRegionsIntegrationTest extends EC2IntegrationTestBase {

    /**
     * Tests that the describe availability zones operation correctly returns
     * zones, both for the no-arg method form, and when we specify zones as
     * parameters.
     */
    @Test
    public void testDescribeAvailabilityZones() {
        // no-required-args method form
        DescribeAvailabilityZonesResult result = ec2.describeAvailabilityZones(DescribeAvailabilityZonesRequest.builder().build());
        List<AvailabilityZone> zones = result.availabilityZones();
        assertTrue(zones.size() > 1);
        assertZonesNotEmpty(zones);

        // explicit zones
        DescribeAvailabilityZonesRequest.Builder request = DescribeAvailabilityZonesRequest.builder();
        request.zoneNames(zones.get(0).zoneName());
        result = ec2.describeAvailabilityZones(request.build());
        zones = result.availabilityZones();
        assertTrue(zones.size() == 1);
        assertZonesNotEmpty(zones);

        // filters
        request = DescribeAvailabilityZonesRequest.builder();
        request.filters(Filter.builder().name("zone-name").values(zones.get(0).zoneName()).build());
        result = ec2.describeAvailabilityZones(request.build());
        zones = result.availabilityZones();
        assertTrue(zones.size() == 1);
        assertZonesNotEmpty(zones);
    }

    /**
     * Tests that the describe regions operation correctly returns regions, both
     * for the no-arg method form, and when we specify regions as parameters.
     */
    @Test
    public void testDescribeRegions() {
        // no-requred-args method form
        DescribeRegionsResult result = ec2.describeRegions(DescribeRegionsRequest.builder().build());
        List<Region> regions = result.regions();
        assertTrue(regions.size() > 1);
        assertRegionsNotEmpty(regions);

        // explicit region names
        DescribeRegionsRequest.Builder request = DescribeRegionsRequest.builder();
        request.regionNames(regions.get(0).regionName());
        result = ec2.describeRegions(request.build());
        regions = result.regions();
        assertTrue(regions.size() == 1);
        assertRegionsNotEmpty(regions);

        // filters
        request = DescribeRegionsRequest.builder();
        request.filters(Filter.builder().name("region-name").values(regions.get(0).regionName()).build());
        result = ec2.describeRegions(request.build());
        regions = result.regions();
        assertTrue(regions.size() == 1);
        assertRegionsNotEmpty(regions);
    }
    

    /*
     * Test Helper Methods
     */

    /**
     * Asserts that the specified list of availability zones are all populated
     * with data.
     *
     * @param zones
     *            The list of zones to test.
     */
    private void assertZonesNotEmpty(List<AvailabilityZone> zones) {
        for (AvailabilityZone zone : zones) {
            assertThat(zone.regionName(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(zone.zoneName(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(zone.state(), Matchers.not(Matchers.isEmptyOrNullString()));
        }
    }

    /**
     * Asserts that the specified list of regions are all populated with data.
     *
     * @param regions
     *            The list of regions to test.
     */
    private void assertRegionsNotEmpty(List<Region> regions) {
        for (Region region : regions) {
            assertThat(region.endpoint(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(region.regionName(), Matchers.not(Matchers.isEmptyOrNullString()));
        }
    }

}
