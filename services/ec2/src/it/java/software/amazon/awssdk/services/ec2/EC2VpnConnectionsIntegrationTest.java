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
                CreateCustomerGatewayRequest.builder()
                                            .publicIp(publicIp)
                                            .bgpAsn(bgpAsn)
                                            .type(GatewayType.Ipsec1).build());

        customerGateway = result.customerGateway();

        Assert.assertNotNull(customerGateway);
        Assert.assertNotNull(customerGateway.customerGatewayId());
        Assert.assertEquals(publicIp, customerGateway.ipAddress());
        Assert.assertEquals(String.valueOf(bgpAsn), customerGateway.bgpAsn());
        Assert.assertEquals(GatewayType.Ipsec1.toString(), customerGateway.type());

        CreateVpnGatewayResult createVpnGatewayResult =
                ec2.createVpnGateway(CreateVpnGatewayRequest.builder().type(GatewayType.Ipsec1).build());

        vpnGateway = createVpnGatewayResult.vpnGateway();

        Assert.assertNotNull(vpnGateway);
        Assert.assertNotNull(vpnGateway.vpnGatewayId());
    }

    @AfterClass
    public static void tearDown() {

        if (vpnConnection != null) {
            ec2.deleteVpnConnection(DeleteVpnConnectionRequest.builder()
                                                              .vpnConnectionId(vpnConnection.vpnConnectionId()).build());
        }
        if (vpnGateway != null) {
            ec2.deleteVpnGateway(DeleteVpnGatewayRequest.builder()
                                                        .vpnGatewayId(vpnGateway.vpnGatewayId()).build());
        }
        if (customerGateway != null) {
            ec2.deleteCustomerGateway(DeleteCustomerGatewayRequest.builder()
                                                                  .customerGatewayId(customerGateway.customerGatewayId())
                                                                  .build());
        }
    }

    @Test
    public void testCreateVpcConnection() {

        CreateVpnConnectionResult createVpnConnectionResult = ec2
                .createVpnConnection(CreateVpnConnectionRequest.builder()
                                                               .vpnGatewayId(vpnGateway.vpnGatewayId())
                                                               .customerGatewayId(customerGateway.customerGatewayId())
                                                               .type(GatewayType.Ipsec1.toString()).build());
        vpnConnection = createVpnConnectionResult.vpnConnection();

        Assert.assertNotNull(vpnConnection);
        Assert.assertNotNull(vpnConnection.vpnConnectionId());
        Assert.assertEquals(customerGateway.customerGatewayId(),
                            vpnConnection.customerGatewayId());
        Assert.assertEquals(vpnGateway.vpnGatewayId(), vpnConnection.vpnGatewayId());
    }
}
