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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.services.ec2.model.DeleteNetworkAclRequest;
import software.amazon.awssdk.services.ec2.model.DeleteRouteRequest;
import software.amazon.awssdk.services.ec2.model.DeleteRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcPeeringConnectionRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkAclsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcPeeringConnectionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DisassociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.NetworkAcl;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.RouteTableAssociation;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcPeeringConnection;

/**
 * Base class for EC2 integration tests; responsible for loading AWS account
 * info for running the tests, and instantiating EC2 clients for tests to use.
 *
 * @author fulghum@amazon.com
 */
public abstract class EC2VPCIntegrationTestBase extends EC2IntegrationTestBase {
    protected static final Log log = LogFactory.getLog(EC2VPCIntegrationTestBase.class);

    /**
     * Deletes all VPCs, including deleting all groups and subnets hanging off
     * of them. These things have a stupid number of dependencies, and the error
     * message on a failed delete is useless in determining which dependency you
     * forgot.
     */
    protected static void deleteAllVpcs() {
        for (Vpc vpc : ec2.describeVpcs(DescribeVpcsRequest.builder().build()).vpcs()) {
            log.warn("Found vpc: " + vpc);

            deleteAllSubnets();
            deleteAllSecurityGroups(vpc);
            deleteAllNetworkAcls();
            deleteAllRouteTables();
            deleteAllPeeringConnections();

            try {
                ec2.deleteVpc(DeleteVpcRequest.builder().vpcId(vpc.vpcId()).build());
            } catch (Exception e) {
                log.warn("Couldn't delete vpc", e);
            }
        }
    }

    protected static void deleteAllRouteTables() {
        for (RouteTable table : ec2.describeRouteTables(DescribeRouteTablesRequest.builder().build()).routeTables()) {
            log.warn("Deleting route table " + table);
            for (RouteTableAssociation ass : table.associations()) {
                try {
                    ec2.disassociateRouteTable(
                            DisassociateRouteTableRequest.builder().associationId(ass.routeTableAssociationId()).build());
                } catch (Exception e) {
                    log.warn("Couldn't disassociate route table ", e);
                }
            }

            for (Route rout : table.routes()) {
                try {
                    ec2.deleteRoute(DeleteRouteRequest.builder().routeTableId(table.routeTableId())
                                                      .destinationCidrBlock(rout.destinationCidrBlock()).build());
                } catch (Exception e) {
                    log.warn("Couldn't delete route ", e);
                }
            }

            try {
                ec2.deleteRouteTable(DeleteRouteTableRequest.builder().routeTableId(table.routeTableId()).build());
            } catch (Exception e) {
                log.warn("Couldn't delete route table ", e);
            }
        }
    }

    protected static void deleteAllSecurityGroups(Vpc vpc) {
        for (SecurityGroup group : ec2.describeSecurityGroups(DescribeSecurityGroupsRequest.builder().build())
                                      .securityGroups()) {
            if (vpc.vpcId().equals(group.vpcId())) {
                try {
                    log.warn("Deleting group " + group);
                    ec2.deleteSecurityGroup(DeleteSecurityGroupRequest.builder().groupId(group.groupId()).build());
                } catch (Exception e) {
                    log.warn("Couldn't delete group", e);
                }
            }
        }
    }

    protected static void deleteAllNetworkAcls() {
        for (NetworkAcl acl : ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder().build()).networkAcls()) {
            if (!acl.isDefault()) {
                log.warn("Deleting network ACL " + acl);
                ec2.deleteNetworkAcl(DeleteNetworkAclRequest.builder().networkAclId(acl.networkAclId()).build());
            }
        }
    }

    protected static void deleteAllSubnets() {
        for (Subnet subnet : ec2.describeSubnets(DescribeSubnetsRequest.builder().build()).subnets()) {
            log.warn("Deleting subnet " + subnet);
            try {
                ec2.deleteSubnet(DeleteSubnetRequest.builder().subnetId(subnet.subnetId()).build());
            } catch (Exception e) {
                log.warn("Couldn't delete subnet", e);
            }
        }
    }

    protected static void deleteAllPeeringConnections() {
        for (VpcPeeringConnection connection : ec2
                .describeVpcPeeringConnections(DescribeVpcPeeringConnectionsRequest.builder().build())
                .vpcPeeringConnections()) {
            try {
                log.warn("Deleting peering connection " + connection);
                ec2.deleteVpcPeeringConnection(
                        DeleteVpcPeeringConnectionRequest.builder()
                                                         .vpcPeeringConnectionId(connection.vpcPeeringConnectionId()).build());
            } catch (Exception e) {
                log.warn("Couldn't delete peering connection", e);
            }

        }
    }

}
