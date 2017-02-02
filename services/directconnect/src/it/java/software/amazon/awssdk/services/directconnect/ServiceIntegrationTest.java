package software.amazon.awssdk.services.directconnect;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.SDKGlobalTime;
import software.amazon.awssdk.services.directconnect.model.CreateConnectionRequest;
import software.amazon.awssdk.services.directconnect.model.CreateConnectionResult;
import software.amazon.awssdk.services.directconnect.model.DeleteConnectionRequest;
import software.amazon.awssdk.services.directconnect.model.DescribeConnectionsRequest;
import software.amazon.awssdk.services.directconnect.model.DescribeConnectionsResult;
import software.amazon.awssdk.services.directconnect.model.DescribeLocationsRequest;
import software.amazon.awssdk.services.directconnect.model.DescribeLocationsResult;
import software.amazon.awssdk.services.directconnect.model.Location;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String CONNECTION_NAME = "test-connection-name";
    private static final String EXPECTED_CONNECTION_STATUS = "requested";

    private static String connectionId;

    @BeforeClass
    public static void setup() {
        CreateConnectionResult result = dc.createConnection(new CreateConnectionRequest()
                .withConnectionName(CONNECTION_NAME).withBandwidth("1Gbps").withLocation("EqSV5"));
        connectionId = result.getConnectionId();
    }

    @AfterClass
    public static void tearDown() {
        dc.deleteConnection(new DeleteConnectionRequest().withConnectionId(connectionId));
    }

    @Test
    public void describeLocations_ReturnsNonEmptyList() {
        DescribeLocationsResult describeLocations = dc.describeLocations(new DescribeLocationsRequest());
        assertTrue(describeLocations.getLocations().size() > 0);
        for (Location location : describeLocations.getLocations()) {
            assertNotNull(location.getLocationCode());
            assertNotNull(location.getLocationName());
        }
    }

    @Test
    public void describeConnections_ReturnsNonEmptyList() {
        DescribeConnectionsResult describeConnectionsResult = dc.describeConnections();
        assertTrue(describeConnectionsResult.getConnections().size() > 0);
        assertNotNull(describeConnectionsResult.getConnections().get(0).getConnectionId());
        assertNotNull(describeConnectionsResult.getConnections().get(0).getConnectionName());
        assertNotNull(describeConnectionsResult.getConnections().get(0).getConnectionState());
        assertNotNull(describeConnectionsResult.getConnections().get(0).getLocation());
        assertNotNull(describeConnectionsResult.getConnections().get(0).getRegion());
    }

    @Test
    public void describeConnections_FilteredByCollectionId_ReturnsOnlyOneConnection() {
        DescribeConnectionsResult describeConnectionsResult = dc.describeConnections(new DescribeConnectionsRequest()
                .withConnectionId(connectionId));
        assertThat(describeConnectionsResult.getConnections(), hasSize(1));
        assertEquals(connectionId, describeConnectionsResult.getConnections().get(0).getConnectionId());
        assertEquals(EXPECTED_CONNECTION_STATUS, describeConnectionsResult.getConnections().get(0).getConnectionState());
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SDKGlobalTime.setGlobalTimeOffset(3600);
        AmazonDirectConnectClient clockSkewClient = new AmazonDirectConnectClient(credentials);
        clockSkewClient.describeConnections();
        assertTrue(SDKGlobalTime.getGlobalTimeOffset() < 60);
    }

}
