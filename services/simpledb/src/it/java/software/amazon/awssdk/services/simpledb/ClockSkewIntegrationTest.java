package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import software.amazon.awssdk.SDKGlobalTime;

public class ClockSkewIntegrationTest extends IntegrationTestBase {

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkewSDB() {
        SDKGlobalTime.setGlobalTimeOffset(3600);
        AmazonSimpleDBClient clockSkewClient = new AmazonSimpleDBClient(credentials);
        clockSkewClient.listDomains();
        assertTrue(SDKGlobalTime.getGlobalTimeOffset() < 60);
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkewAsync() {
        SDKGlobalTime.setGlobalTimeOffset(3600);
        AmazonSimpleDBAsyncClient clockSkewClient = new AmazonSimpleDBAsyncClient(credentials);
        clockSkewClient.listDomains();
        assertTrue(SDKGlobalTime.getGlobalTimeOffset() < 60);
    }
}
