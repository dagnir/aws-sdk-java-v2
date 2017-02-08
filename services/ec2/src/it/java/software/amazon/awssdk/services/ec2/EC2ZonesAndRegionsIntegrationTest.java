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
        DescribeAvailabilityZonesResult result = ec2.describeAvailabilityZones();
        List<AvailabilityZone> zones = result.getAvailabilityZones();
        assertTrue(zones.size() > 1);
        assertZonesNotEmpty(zones);

        // explicit zones
        DescribeAvailabilityZonesRequest request = new DescribeAvailabilityZonesRequest();
        request.withZoneNames(zones.get(0).getZoneName());
        result = ec2.describeAvailabilityZones(request);
        zones = result.getAvailabilityZones();
        assertTrue(zones.size() == 1);
        assertZonesNotEmpty(zones);
        
        // filters
        request = new DescribeAvailabilityZonesRequest();
        request.withFilters(new Filter("zone-name", null).withValues(zones.get(0).getZoneName()));
        result = ec2.describeAvailabilityZones(request);
        zones = result.getAvailabilityZones();
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
        DescribeRegionsResult result = ec2.describeRegions();
        List<Region> regions = result.getRegions();
        assertTrue(regions.size() > 1);
        assertRegionsNotEmpty(regions);

        // explicit region names
        DescribeRegionsRequest request = new DescribeRegionsRequest();
        request.withRegionNames(regions.get(0).getRegionName());
        result = ec2.describeRegions(request);
        regions = result.getRegions();
        assertTrue(regions.size() == 1);
        assertRegionsNotEmpty(regions);
        
        // filters
        request = new DescribeRegionsRequest();
        request.withFilters(new Filter("region-name", null).withValues(regions.get(0).getRegionName()));
        result = ec2.describeRegions(request);
        regions = result.getRegions();
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
            assertThat(zone.getRegionName(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(zone.getZoneName(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(zone.getState(), Matchers.not(Matchers.isEmptyOrNullString()));
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
            assertThat(region.getEndpoint(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(region.getRegionName(), Matchers.not(Matchers.isEmptyOrNullString()));
        }
    }
    
}
