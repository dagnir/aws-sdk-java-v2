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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.model.AssociateDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayResult;
import software.amazon.awssdk.services.ec2.model.CreateDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.CreateDhcpOptionsResult;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResult;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResult;
import software.amazon.awssdk.services.ec2.model.CreateVpnConnectionRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpnConnectionResult;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayResult;
import software.amazon.awssdk.services.ec2.model.CustomerGateway;
import software.amazon.awssdk.services.ec2.model.DeleteCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpnConnectionRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpnGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCustomerGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCustomerGatewaysResult;
import software.amazon.awssdk.services.ec2.model.DescribeDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeDhcpOptionsResult;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpnConnectionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpnConnectionsResult;
import software.amazon.awssdk.services.ec2.model.DescribeVpnGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpnGatewaysResult;
import software.amazon.awssdk.services.ec2.model.DhcpConfiguration;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.PurchaseReservedInstancesOfferingRequest;
import software.amazon.awssdk.services.ec2.model.PurchaseReservedInstancesOfferingResult;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpnConnection;
import software.amazon.awssdk.services.ec2.model.VpnGateway;
import software.amazon.awssdk.test.AwsTestBase;

@Deprecated
public class EC2TestHelper {

    /** Shared EC2 client for all tests to use. */
    public static EC2Client EC2;

    public static AwsCredentials CREDENTIALS;

    static {
        try {
            if (CREDENTIALS == null) {
                try {
                    CREDENTIALS = AwsTestBase.CREDENTIALS_PROVIDER_CHAIN.getCredentialsOrThrow();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            EC2 = EC2Client.builder().credentialsProvider(new StaticCredentialsProvider(CREDENTIALS)).build();
        } catch (Exception exception) {
            // Ignored or expected.
        }
    }

    /**
     * Deletes all customer gateways
     */
    public static void deleteAllCustomerGateways() {
        DescribeCustomerGatewaysRequest request = new DescribeCustomerGatewaysRequest();
        DescribeCustomerGatewaysResult result = EC2.describeCustomerGateways(request);

        for (CustomerGateway gateway : result.getCustomerGateways()) {
            deleteCustomerGateway(gateway.getCustomerGatewayId());

        }
    }

    /**
     * Deletes all Vpn Connections
     */
    public static void deleteAllVpnConnections() {
        DescribeVpnConnectionsRequest request = new DescribeVpnConnectionsRequest();
        DescribeVpnConnectionsResult result = EC2.describeVpnConnections(request);

        for (VpnConnection connection : result.getVpnConnections()) {
            DeleteVpnConnectionRequest r = new DeleteVpnConnectionRequest();
            EC2.deleteVpnConnection(r.vpnConnectionId(connection
                                                                  .getVpnConnectionId()));
        }
    }

    /**
     * Delete customer gateway by gateway id
     *
     * @param customerGatewayIds
     *            variable list of customer gateway ids
     */
    public static void deleteCustomerGateway(String... customerGatewayIds) {
        for (String customerGatewayId : customerGatewayIds) {
            DeleteCustomerGatewayRequest request = new DeleteCustomerGatewayRequest()
                    .customerGatewayId(customerGatewayId);
            EC2.deleteCustomerGateway(request);
        }
    }

    /**
     * Create customer gateway
     *
     * @param ipAddress
     *            IP address of the gateway
     * @param bgpAsn
     *            The customer gateway's Border Gateway Protocol (BGP)
     *            Autonomous System Number (ASN).
     * @param type
     *            The type of VPN connection this customer gateway supports.
     * @return CreateCustomerGatewayResult
     */
    public static CreateCustomerGatewayResult createCustomerGateway(
            String ipAddress, Integer bgpAsn, String type) {

        CreateCustomerGatewayRequest request =
                new CreateCustomerGatewayRequest()
                        .publicIp(ipAddress)
                        .bgpAsn(bgpAsn)
                        .type(type);

        return EC2.createCustomerGateway(request);
    }

    /**
     * Describe customer gateway by customer gateway Id
     *
     * @param customerGatewayIds
     *            variable list of customer gateway ids
     * @return DescribeCustomerGatewaysResult
     */
    public static DescribeCustomerGatewaysResult describeCustomerGateway(
            List<String> customerGatewayIds) {

        DescribeCustomerGatewaysRequest request = new DescribeCustomerGatewaysRequest();
        request.setCustomerGatewayIds(customerGatewayIds);

        return EC2.describeCustomerGateways(request);

    }

    /**
     * Describe customer gateway by customer gateway Id
     *
     * @param customerGatewayId
     *            customer gateway id
     * @return DescribeCustomerGatewaysResult
     */
    public static DescribeCustomerGatewaysResult describeCustomerGateway(
            String customerGatewayId) {

        List<String> ids = new ArrayList<String>();

        ids.add(customerGatewayId);

        return describeCustomerGateway(ids);

    }

    public static CreateDhcpOptionsResult createDhcpOptions(String optionKey,
                                                            String... optionValue) {

        DhcpConfiguration configurationOne = new DhcpConfiguration()
                .key(optionKey).values(optionValue);

        CreateDhcpOptionsRequest request = new CreateDhcpOptionsRequest()
                .dhcpConfigurations(configurationOne);

        return EC2.createDhcpOptions(request);
    }

    /**
     * Describe dhcp options by list of option ids
     *
     * @param dhcpOptionsIds
     *            list of option ids
     * @return DescribeDhcpOptionsResult
     */
    public static DescribeDhcpOptionsResult describeDhcpOptions(
            List<String> dhcpOptionsIds) {

        DescribeDhcpOptionsRequest request = new DescribeDhcpOptionsRequest();
        request.setDhcpOptionsIds(dhcpOptionsIds);

        return EC2.describeDhcpOptions(request);
    }

    /**
     * Describe dhcp options by option id
     *
     * @param dhcpOptionsId
     *            A DHCP options set ID.
     */
    public static DescribeDhcpOptionsResult describeDhcpOptions(String dhcpOptionsId) {
        List<String> ids = new ArrayList<String>();
        ids.add(dhcpOptionsId);

        return describeDhcpOptions(ids);
    }

    /**
     * Associate dhcp options
     *
     * @param dhcpOptionsId
     *            The ID of the DHCP options you want to associate with the VPC,
     *            or "default" if you want to associate the default DHCP options
     *            with the VPC.
     *
     * @param vpcId
     *            The ID of the VPC you want to associate the DHCP options with.
     */
    public static void associateDhcpOptions(String dhcpOptionsId, String vpcId) {
        AssociateDhcpOptionsRequest request = new AssociateDhcpOptionsRequest()
                .dhcpOptionsId(dhcpOptionsId).vpcId(vpcId);
        EC2.associateDhcpOptions(request);
    }

    /**
     * Delete dhcp options
     *
     * @param dhcpOptionsIds
     *            variable list of options ids
     */
    public static void deleteDhcpOptions(String... dhcpOptionsIds) {
        for (String dhcpOptionsId : dhcpOptionsIds) {
            DeleteDhcpOptionsRequest request = new DeleteDhcpOptionsRequest()
                    .dhcpOptionsId(dhcpOptionsId);
            EC2.deleteDhcpOptions(request);
        }
    }

    /**
     * Create vpn gateway
     *
     * @param type
     *            The type of VPN connection this VPN gateway supports.
     * @param availabilityZone
     *            The Availability Zone where you want the VPN gateway.
     * @return CreateVpnGatewayResult
     */
    public static CreateVpnGatewayResult createVpnGateway(String type,
                                                          String availabilityZone) {
        CreateVpnGatewayRequest request = new CreateVpnGatewayRequest()
                .type(type)
                .availabilityZone(availabilityZone);

        return EC2.createVpnGateway(request);
    }

    /**
     * Create vpn gateway
     *
     * @param type
     *            The type of VPN connection this VPN gateway supports.
     * @return CreateVpnGatewayResult
     */
    public static CreateVpnGatewayResult createVpnGateway(String type) {
        return createVpnGateway(type, null);
    }

    /**
     * Deletes VPN gateway
     *
     * @param vpnGatewayId
     *            id of the gateway to delete
     */
    public static void deleteVpnGateway(String vpnGatewayId) {
        DeleteVpnGatewayRequest request = new DeleteVpnGatewayRequest()
                .vpnGatewayId(vpnGatewayId);
        EC2.deleteVpnGateway(request);
    }

    /**
     * Describe vpn gatways
     *
     * @param vpnGatewayIds
     *            list of vpn gateway ids
     * @return DescribeVpnGatewaysResult
     */
    public static DescribeVpnGatewaysResult describeVpnGateways(List<String> vpnGatewayIds) {
        DescribeVpnGatewaysRequest request = new DescribeVpnGatewaysRequest();
        request.setVpnGatewayIds(vpnGatewayIds);

        return EC2.describeVpnGateways(request);
    }

    /**
     * Describe vpn gatways
     *
     * @param vpnGatewayId
     *            gateway id
     * @return DescribeVpnGatewaysResult
     */
    public static DescribeVpnGatewaysResult describeVpnGateway(String vpnGatewayId) {
        List<String> ids = new ArrayList<String>();
        ids.add(vpnGatewayId);

        return describeVpnGateways(ids);
    }

    /**
     * Deletes VPN gateway
     *
     * @param vpnGatewayId
     *            id of the gateway to delete
     */
    public static void deletVpnGateway(String vpnGatewayId) {
        DeleteVpnGatewayRequest request = new DeleteVpnGatewayRequest()
                .vpnGatewayId(vpnGatewayId);
        EC2.deleteVpnGateway(request);
    }

    /**
     * Deletes VPN connection
     *
     * @param vpnConnectionId
     *            vpn connection id
     */
    public static void deleteVpnConnection(String vpnConnectionId) {
        DeleteVpnConnectionRequest request = new DeleteVpnConnectionRequest()
                .vpnConnectionId(vpnConnectionId);
        EC2.deleteVpnConnection(request);
    }

    /**
     * Deletes VPN gateway
     *
     */
    public static void deleteAllVpnGateways() {
        DescribeVpnGatewaysResult describeResult = describeVpnGateways(null);

        for (VpnGateway vpnGateway : describeResult.getVpnGateways()) {
            deletVpnGateway(vpnGateway.getVpnGatewayId());
        }
    }

    /**
     * Creates VPN connection
     *
     * @param type
     *            The type of VPN connection.
     * @param customerGatewayId
     *            The ID of the customer gateway
     * @param vpnGatewayId
     *            The ID of the customer gateway.
     */
    public static CreateVpnConnectionResult createVpnConnection(String type,
                                                                String customerGatewayId, String vpnGatewayId) {

        CreateVpnConnectionRequest request = new CreateVpnConnectionRequest()
                .type(type)
                .vpnGatewayId(vpnGatewayId)
                .customerGatewayId(customerGatewayId);

        return EC2.createVpnConnection(request);
    }

    /**
     * Deletes VPC by VPC Id
     *
     * @param vpcId
     *            VPC id
     */
    public static void deleteVpc(String vpcId) {
        DeleteVpcRequest request = new DeleteVpcRequest()
                .vpcId(vpcId);
        EC2.deleteVpc(request);
    }

    /**
     * Creates Vpc
     *
     * @param cidrBlock
     *            A valid CIDR block.
     * @return CreateVpcResult
     */
    public static CreateVpcResult createVpc(String cidrBlock) {
        CreateVpcRequest request = new CreateVpcRequest()
                .cidrBlock(cidrBlock);
        return EC2.createVpc(request);
    }

    /**
     * Describe VPC by VPC Id
     *
     * @param vpcId
     *            VPC Id
     * @return DescribeVpcsResult
     */
    public static DescribeVpcsResult describeVpc(String vpcId) {
        List<String> ids = new ArrayList<String>();
        ids.add(vpcId);

        return describeVpcs(ids);
    }

    /**
     * Describe VPC by list of VPC Ids
     *
     * @param ids
     *            list of VPC ids
     * @return DescribeVpcsResult
     */
    public static DescribeVpcsResult describeVpcs(List<String> ids) {
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        request.setVpcIds(ids);

        return EC2.describeVpcs(request);
    }

    public static DescribeVpcAttributeResult describeVpcAttribute(String vpcId, boolean enableDnsHostnames,
                                                                  boolean enableDnsSupport) {
        DescribeVpcAttributeRequest describeVpcAttributeRequest = new DescribeVpcAttributeRequest().vpcId(vpcId);
        if (enableDnsHostnames == true) {
            describeVpcAttributeRequest.setAttribute("enableDnsHostnames");
        }
        if (enableDnsSupport == true) {
            describeVpcAttributeRequest.setAttribute("enableDnsSupport");
        }
        return EC2.describeVpcAttribute(describeVpcAttributeRequest);
    }

    public static void modifyVpcAttribute(String vpcId) {
        EC2.modifyVpcAttribute(new ModifyVpcAttributeRequest().vpcId(vpcId).enableDnsSupport(true));
    }

    /**
     * Deletes ALL Vpc
     */
    public static void deleteAllVpcs() {
        DescribeVpcsResult describeResult = describeVpcs(null);
        for (Vpc vpc : describeResult.getVpcs()) {
            deleteVpc(vpc.getVpcId());
        }
    }

    /**
     * Deletes subnet
     *
     * @param subnetId
     *            subnet id
     */
    public static void deleteSubnet(String subnetId) {
        DeleteSubnetRequest request = new DeleteSubnetRequest()
                .subnetId(subnetId);
        EC2.deleteSubnet(request);
    }

    /**
     * Creates Subnet
     *
     * @param vpcId
     *            The ID of the VPC where you want to create the subnet.
     * @param cidrBlock
     *            The CIDR block you want the subnet to cover.
     * @return CreateSubnetResult
     */
    public static CreateSubnetResult createSubnet(String vpcId, String cidrBlock) {
        CreateSubnetRequest request = new CreateSubnetRequest()
                .vpcId(vpcId).cidrBlock(cidrBlock);
        return EC2.createSubnet(request);
    }

    /**
     * Describes subnets
     *
     * @param subnetId
     *            subnet id
     * @return DescribeSubnetsResult
     */
    public static DescribeSubnetsResult describeSubnet(String subnetId) {
        List<String> ids = new ArrayList<String>();
        ids.add(subnetId);

        return describeSubnets(ids);
    }

    /**
     * Describes subnets given the list of subnet ids
     *
     * @param subnetIds
     *            subnet ids
     * @return DescribeSubnetsResult
     */
    public static DescribeSubnetsResult describeSubnets(List<String> subnetIds) {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        request.setSubnetIds(subnetIds);

        return EC2.describeSubnets(request);
    }

    /**
     * Purchase reserved instance. Careful with this! Use test
     * offering ids.
     *
     * @param offeringId offering id
     * @param instanceCount how many instances to reserve
     */
    public static PurchaseReservedInstancesOfferingResult purchaseReservedInstancesOffering(
            String offeringId, int instanceCount) {
        PurchaseReservedInstancesOfferingRequest request = new PurchaseReservedInstancesOfferingRequest()
                .instanceCount(instanceCount)
                .reservedInstancesOfferingId(offeringId);

        return EC2.purchaseReservedInstancesOffering(request);
    }

}
