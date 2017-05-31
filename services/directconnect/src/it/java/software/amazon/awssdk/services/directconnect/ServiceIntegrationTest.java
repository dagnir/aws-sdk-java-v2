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

package software.amazon.awssdk.services.directconnect;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
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
                                                                    .withConnectionName(CONNECTION_NAME)
                                                                    .withBandwidth("1Gbps")
                                                                    .withLocation("EqSV5"));
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
        DescribeConnectionsResult describeConnectionsResult = dc.describeConnections(new DescribeConnectionsRequest());
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
        SdkGlobalTime.setGlobalTimeOffset(3600);
        DirectConnectClient clockSkewClient = DirectConnectClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(credentials))
                .build();

        clockSkewClient.describeConnections(new DescribeConnectionsRequest());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }

}
