package software.amazon.awssdk.services.s3.region;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.services.s3.model.Region;

/**
 * The Region enum originally had a null pointer exception since the US
 * region's value is null. This simple test just makes sure that never comes
 * back.
 */
public class RegionTest {

    @Test
    public void testRegionEnumeration() {
        assertEquals(Region.EU_Ireland, Region.fromValue(Region.EU_Ireland.toString()));
        assertEquals(Region.US_Standard, Region.fromValue(Region.US_Standard.toString()));
    }


    /**
     * Tests if US Standard Enum is returned in cases where empty string or "US"
     * is passed to fromValue method.
     */
    @Test
    public void testFromValueForUSStandard() {
        assertEquals(Region.US_Standard, Region.fromValue(null));
        assertEquals(Region.US_Standard, Region.fromValue("US"));
    }

    /**
     * Tests if correct enums are returned when the location constraints are
     * passed to fromValue method.
     */
    @Test
    public void testFromValueForOthers(){
        assertEquals(Region.EU_Ireland, Region.fromValue("eu-west-1"));
        assertEquals(Region.EU_Ireland, Region.fromValue("EU"));
        assertEquals(Region.US_GovCloud, Region.fromValue("us-gov-west-1"));
    }

}
