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
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
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
        for ( Vpc vpc : ec2.describeVpcs().getVpcs() ) {
            log.warn("Found vpc: " + vpc);

            deleteAllSubnets();
            deleteAllSecurityGroups(vpc);
            deleteAllNetworkAcls();
            deleteAllRouteTables();
            deleteAllPeeringConnections();

            try {
                ec2.deleteVpc(new DeleteVpcRequest().withVpcId(vpc.getVpcId()));
            } catch ( Exception e ) {
                log.warn("Couldn't delete vpc", e);
            }
        }
    }

    protected static void deleteAllRouteTables() {
        for ( RouteTable table : ec2.describeRouteTables().getRouteTables() ) {
            log.warn("Deleting route table " + table);
            for ( RouteTableAssociation ass : table.getAssociations() ) {
                try {
                    ec2.disassociateRouteTable(new DisassociateRouteTableRequest().withAssociationId(ass
                            .getRouteTableAssociationId()));
                } catch ( Exception e ) {
                    log.warn("Couldn't disassociate route table ", e);
                }
            }

            for ( Route rout : table.getRoutes() ) {
                try {
                    ec2.deleteRoute(new DeleteRouteRequest().withRouteTableId(table.getRouteTableId())
                            .withDestinationCidrBlock(rout.getDestinationCidrBlock()));
                } catch ( Exception e ) {
                    log.warn("Couldn't delete route ", e);
                }
            }

            try {
                ec2.deleteRouteTable(new DeleteRouteTableRequest().withRouteTableId(table.getRouteTableId()));
            } catch ( Exception e ) {
                log.warn("Couldn't delete route table ", e);
            }
        }
    }

    protected static void deleteAllSecurityGroups(Vpc vpc) {
        for ( SecurityGroup group : ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest())
                .getSecurityGroups() ) {
            if ( vpc.getVpcId().equals(group.getVpcId()) ) {
                try {
                    log.warn("Deleting group " + group);
                    ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest().withGroupId(group.getGroupId()));
                } catch ( Exception e ) {
                    log.warn("Couldn't delete group", e);
                }
            }
        }
    }

    protected static void deleteAllNetworkAcls() {
        for ( NetworkAcl acl : ec2.describeNetworkAcls().getNetworkAcls() ) {
            if ( !acl.getIsDefault() ) {
                log.warn("Deleting network ACL " + acl);
                ec2.deleteNetworkAcl(new DeleteNetworkAclRequest().withNetworkAclId(acl.getNetworkAclId()));
            }
        }
    }

    protected static void deleteAllSubnets() {
        for ( Subnet subnet : ec2.describeSubnets().getSubnets() ) {
            log.warn("Deleting subnet " + subnet);
            try {
                ec2.deleteSubnet(new DeleteSubnetRequest().withSubnetId(subnet.getSubnetId()));
            } catch (Exception e) {
                log.warn("Couldn't delete subnet", e);
            }
        }
    }

    protected static void deleteAllPeeringConnections() {
        for ( VpcPeeringConnection connection : ec2.describeVpcPeeringConnections().getVpcPeeringConnections() ) {
            try {
                log.warn("Deleting peering connection " + connection);
                ec2.deleteVpcPeeringConnection(
                        new DeleteVpcPeeringConnectionRequest()
                            .withVpcPeeringConnectionId(connection.getVpcPeeringConnectionId()));
            } catch (Exception e) {
                log.warn("Couldn't delete peering connection", e);
            }
           
        }
    }

}
