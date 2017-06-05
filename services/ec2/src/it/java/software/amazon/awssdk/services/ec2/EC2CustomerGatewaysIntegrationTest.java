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

import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CustomerGateway;
import software.amazon.awssdk.services.ec2.model.DeleteCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCustomerGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.GatewayType;

public class EC2CustomerGatewaysIntegrationTest extends EC2IntegrationTestBase {

    private static final String IP_ADDRESS = "1.1.1.1";
    private static final Integer BGP_ASN = 65534;
    private static String customerGatewayId;

    /**
     * Make sure we delete the resource even if the test method fails with
     * exception.
     */
    @AfterClass
    public static void tearDown() {
        if (customerGatewayId != null) {
            ec2.deleteCustomerGateway(DeleteCustomerGatewayRequest.builder().customerGatewayId(customerGatewayId).build());
        }
    }

    /**
     * Tests that we can create, describe and delete CustomerGateways.
     */
    @Test
    public void testCustomerGatewayOperations() {

        // Create CustomerGateway
        CustomerGateway customerGateway = ec2.createCustomerGateway(
                CreateCustomerGatewayRequest.builder()
                                            .publicIp(IP_ADDRESS)
                                            .bgpAsn(BGP_ASN)
                                            .type(GatewayType.Ipsec1).build()).customerGateway();

        customerGatewayId = customerGateway.customerGatewayId();

        assertEquals(IP_ADDRESS, customerGateway.ipAddress());
        assertEquals(GatewayType.Ipsec1.toString(), customerGateway.type());
        assertEquals(Integer.toString(BGP_ASN), customerGateway.bgpAsn());

        // Describe CustomerGateway
        List<CustomerGateway> gateways = ec2.describeCustomerGateways(
                DescribeCustomerGatewaysRequest.builder()
                                               .customerGatewayIds(customerGatewayId).build()).customerGateways();
        assertEquals(1, gateways.size());
        assertEquals(customerGatewayId, gateways.get(0).customerGatewayId());

        // Delete CustomerGateway
        ec2.deleteCustomerGateway(DeleteCustomerGatewayRequest.builder().customerGatewayId(customerGatewayId).build());

        // Check the "deleted" state
        gateways = ec2.describeCustomerGateways(
                DescribeCustomerGatewaysRequest.builder()
                                               .customerGatewayIds(customerGatewayId).build()).customerGateways();
        assertEquals(1, gateways.size());
        assertEquals("deleted", gateways.get(0).state());

        customerGatewayId = null;
    }
}
