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

    private static String customerGatewayId;

    private static final String IP_ADDRESS = "1.1.1.1";
    private static final Integer BGP_ASN = 65534;

    /**
     * Make sure we delete the resource even if the test method fails with
     * exception.
     */
    @AfterClass
    public static void tearDown() {
        if (customerGatewayId != null )
            ec2.deleteCustomerGateway(new DeleteCustomerGatewayRequest()
                    .withCustomerGatewayId(customerGatewayId)
                    );
    }

    /**
     * Tests that we can create, describe and delete CustomerGateways.
     */
    @Test
    public void testCustomerGatewayOperations() {

        // Create CustomerGateway
        CustomerGateway customerGateway = ec2.createCustomerGateway(
                new CreateCustomerGatewayRequest()
                    .withPublicIp(IP_ADDRESS)
                    .withBgpAsn(BGP_ASN)
                    .withType(GatewayType.Ipsec1)
                ).getCustomerGateway();

        customerGatewayId = customerGateway.getCustomerGatewayId();

        assertEquals(IP_ADDRESS, customerGateway.getIpAddress());
        assertEquals(GatewayType.Ipsec1.toString(), customerGateway.getType());
        assertEquals(Integer.toString(BGP_ASN), customerGateway.getBgpAsn());

        // Describe CustomerGateway
        List<CustomerGateway> gateways = ec2.describeCustomerGateways(
                new DescribeCustomerGatewaysRequest()
                    .withCustomerGatewayIds(customerGatewayId)
                ).getCustomerGateways();
        assertEquals(1, gateways.size());
        assertEquals(customerGatewayId, gateways.get(0).getCustomerGatewayId());

        // Delete CustomerGateway
        ec2.deleteCustomerGateway(new DeleteCustomerGatewayRequest(customerGatewayId));

        // Check the "deleted" state
        gateways = ec2.describeCustomerGateways(
                new DescribeCustomerGatewaysRequest()
                    .withCustomerGatewayIds(customerGatewayId)
                ).getCustomerGateways();
        assertEquals(1, gateways.size());
        assertEquals("deleted", gateways.get(0).getState());

        customerGatewayId = null;
    }
}
