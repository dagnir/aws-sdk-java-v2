package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.SDKGlobalTime;

/**
 * Clock skew test
 */
public class ClockSkewIntegrationTest extends S3IntegrationTestBase {

	/**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkewS3() {
        SDKGlobalTime.setGlobalTimeOffset(3600);
        int offset = SDKGlobalTime.getGlobalTimeOffset();
        assertTrue("offset=" + offset, offset == 3600);
        s3.shutdown();
        s3 = new AmazonS3Client(credentials);
        s3.listBuckets();
        offset = SDKGlobalTime.getGlobalTimeOffset();
        assertTrue("offset=" + offset, offset < 3600);
        // subsequent changes to the global time offset won't affect existing client
        SDKGlobalTime.setGlobalTimeOffset(3600);
        s3.listBuckets();
        assertTrue(SDKGlobalTime.getGlobalTimeOffset() == 3600);
    }
}
