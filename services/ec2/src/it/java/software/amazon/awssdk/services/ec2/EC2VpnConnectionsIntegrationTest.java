/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayResult;
import software.amazon.awssdk.services.ec2.model.CreateVpnConnectionRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpnConnectionResult;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayResult;
import software.amazon.awssdk.services.ec2.model.CustomerGateway;
import software.amazon.awssdk.services.ec2.model.DeleteCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpnConnectionRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpnGatewayRequest;
import software.amazon.awssdk.services.ec2.model.GatewayType;
import software.amazon.awssdk.services.ec2.model.VpnConnection;
import software.amazon.awssdk.services.ec2.model.VpnGateway;

public class EC2VpnConnectionsIntegrationTest extends EC2IntegrationTestBase {

    private static VpnConnection vpnConnection;
    private static CustomerGateway customerGateway;
    private static VpnGateway vpnGateway;

    @BeforeClass
    public static void setUp() {

        final String publicIp = "1.1.1.1";
        final String type = "ipsec.1";
        final int bgpAsn = 65534;

        CreateCustomerGatewayResult result = ec2.createCustomerGateway(
                new CreateCustomerGatewayRequest()
                        .withPublicIp(publicIp)
                        .withBgpAsn(bgpAsn)
                        .withType(GatewayType.Ipsec1));

        customerGateway = result.getCustomerGateway();

        Assert.assertNotNull(customerGateway);
        Assert.assertNotNull(customerGateway.getCustomerGatewayId());
        Assert.assertEquals(publicIp, customerGateway.getIpAddress());
        Assert.assertEquals(String.valueOf(bgpAsn), customerGateway.getBgpAsn());
        Assert.assertEquals(GatewayType.Ipsec1.toString(), customerGateway.getType());

        CreateVpnGatewayResult createVpnGatewayResult = ec2.createVpnGateway
                (new CreateVpnGatewayRequest().withType
                        (GatewayType.Ipsec1));

        vpnGateway = createVpnGatewayResult.getVpnGateway();

        Assert.assertNotNull(vpnGateway);
        Assert.assertNotNull(vpnGateway.getVpnGatewayId());
    }

    @AfterClass
    public static void tearDown() {

        if (vpnConnection != null) {
            ec2.deleteVpnConnection(new DeleteVpnConnectionRequest()
                                            .withVpnConnectionId(vpnConnection.getVpnConnectionId()));
        }
        if (vpnGateway != null) {
            ec2.deleteVpnGateway(new DeleteVpnGatewayRequest()
                                         .withVpnGatewayId(vpnGateway.getVpnGatewayId()));
        }
        if (customerGateway != null) {
            ec2.deleteCustomerGateway(new DeleteCustomerGatewayRequest()
                                              .withCustomerGatewayId(customerGateway.getCustomerGatewayId()));
        }
    }

    @Test
    public void testCreateVpcConnection() {

        CreateVpnConnectionResult createVpnConnectionResult = ec2
                .createVpnConnection(new CreateVpnConnectionRequest()
                                             .withVpnGatewayId(vpnGateway.getVpnGatewayId())
                                             .withCustomerGatewayId(customerGateway.getCustomerGatewayId())
                                             .withType(GatewayType.Ipsec1.toString()));
        vpnConnection = createVpnConnectionResult.getVpnConnection();

        Assert.assertNotNull(vpnConnection);
        Assert.assertNotNull(vpnConnection.getVpnConnectionId());
        Assert.assertEquals(customerGateway.getCustomerGatewayId(),
                            vpnConnection.getCustomerGatewayId());
        Assert.assertEquals(vpnGateway.getVpnGatewayId(), vpnConnection.getVpnGatewayId());
    }
}
