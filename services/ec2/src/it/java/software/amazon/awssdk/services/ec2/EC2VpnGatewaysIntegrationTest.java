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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpnGatewaysResult;
import software.amazon.awssdk.services.ec2.model.VpnGateway;

public class EC2VpnGatewaysIntegrationTest extends EC2IntegrationTestBase {

    private VpnGateway vpnGateway;

    /** Release resources used by tests. */
    @After
    public void tearDown() {
        if (vpnGateway != null) {
            EC2TestHelper.deleteVpnGateway(vpnGateway.vpnGatewayId());
        }
    }

    /**
     * Tests that we can create, describe and delete a VpnGateway.
     */
    @Test
    public void testVpnGatewayOperations() {
        // Create VpnGateway
        try {
            CreateVpnGatewayResult createResult =
                    EC2TestHelper.createVpnGateway("ipsec.1");
            vpnGateway = createResult.vpnGateway();
            tagResource(vpnGateway.vpnGatewayId(), TAGS);
        } catch (AmazonServiceException ase) {
            if (!ase.getErrorCode().equals("VpnGatewayLimitExceeded")) {
                throw ase;
            }

            System.err.println("Unable to run " + getClass().getName() + ": "
                               + ase.getMessage());
            return;
        }

        assertNotNull(vpnGateway);
        assertTrue(vpnGateway.type().equals("ipsec.1"));

        // Describe VpnGateway
        DescribeVpnGatewaysResult describeResult =
                EC2TestHelper.describeVpnGateway(vpnGateway.vpnGatewayId());

        assertEquals(1, describeResult.vpnGateways().size());
        assertEquals(vpnGateway.vpnGatewayId(),
                     describeResult.vpnGateways().get(0).vpnGatewayId());
        assertEqualUnorderedTagLists(TAGS, describeResult.vpnGateways().get(0).tags());

        // Delete VpnGateway
        EC2TestHelper.deleteVpnGateway(vpnGateway.vpnGatewayId());
        vpnGateway = null;
    }

}
