package software.amazon.awssdk.services.s3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.model.Region;

public class AmazonS3ClientTest {
    private static AmazonS3Client s3;

    @Before
    public void setup() {
        s3 = new AmazonS3Client();
    }

    @After
    public void cleanup() {
        s3.shutdown();
    }

    /**
     * Test configureRegion for all regions. Note the special treatment of
     * US_EAST_1.
     */
    @Test
    public void configureRegion() {
        for (Regions region : Regions.values()) {
            s3.configureRegion(region);
            if (region == Regions.US_EAST_1) {
                Region s3region = s3.getRegion();
                assertSame(s3region, Region.US_Standard);
                assertNull(s3region.toString());

            } else {
                assertEquals(region.getName(), s3.getRegion().toString());
            }
        }
    }

    @Test
    public void tt0051346531() {
        String strRegion = "eu-central-1";
        software.amazon.awssdk.regions.Region region = RegionUtils.getRegion(strRegion);
        String endpoint = region.getServiceEndpoint(AmazonS3Client.S3_SERVICE_NAME);
        s3.setEndpoint(endpoint);
        s3.getRegion();
    }

    @Test
    public void getRegionName_ReturnsRegion_When_SetRegion() {
        s3.setRegion(RegionUtils.getRegion("us-east-1"));
        assertThat(s3.getRegionName(), equalTo("us-east-1"));
    }

    @Test
    public void getRegionName_ReturnsRegion_When_SetEndpoint() {
        s3.setEndpoint("s3-us-east-1.amazonaws.com");
        assertThat(s3.getRegionName(), equalTo("us-east-1"));
    }

    @Test
    public void getRegionName_ReturnsRegion_WhenSetEndpointStandardFormat() {
        s3.setEndpoint("s3.us-east-2.amazonaws.com");
        assertThat(s3.getRegionName(), equalTo("us-east-2"));
    }

    @Test (expected = IllegalStateException.class)
    public void getRegionName_ThrowsIllegalStateException_When_InvalidRegion() {
        s3.setEndpoint("s3-mordor.amazonaws.com");
        s3.getRegionName();
    }

    @Test (expected = IllegalStateException.class)
    public void getRegionName_ThrowsIllegalStateException_When_InvalidRegionWithStandardFormat() {
        s3.setEndpoint("s3.mordor.amazonaws.com");
        s3.getRegionName();
    }

    @Test(expected = IllegalStateException.class)
    public void deleteBucket_ThrowsIllegalStateException_When_RegionNullAndSystemPropertyProhibitsNullRegions() {
        try {
            System.setProperty(SDKGlobalConfiguration.DISABLE_S3_IMPLICIT_GLOBAL_CLIENTS_SYSTEM_PROPERTY, "true");
            s3.deleteBucket("test-bucket");
        }
        finally {
            System.clearProperty(SDKGlobalConfiguration.DISABLE_S3_IMPLICIT_GLOBAL_CLIENTS_SYSTEM_PROPERTY);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void createBucket_ThrowsIllegalStateException_When_RegionNullAndSystemPropertyProhibitsNullRegions() {
        try {
            System.setProperty(SDKGlobalConfiguration.DISABLE_S3_IMPLICIT_GLOBAL_CLIENTS_SYSTEM_PROPERTY, "true");
            s3.createBucket("test-bucket", Region.US_West);
        }
        finally {
            System.clearProperty(SDKGlobalConfiguration.DISABLE_S3_IMPLICIT_GLOBAL_CLIENTS_SYSTEM_PROPERTY);
        }
    }
}
