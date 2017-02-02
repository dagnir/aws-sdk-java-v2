/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableResult;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResult;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResult;
import software.amazon.awssdk.services.ec2.model.DeleteNetworkAclRequest;
import software.amazon.awssdk.services.ec2.model.DeleteRouteRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResult;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkAclsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResult;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DisassociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.NetworkAcl;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.ReplaceRouteRequest;
import software.amazon.awssdk.services.ec2.model.ReplaceRouteTableAssociationRequest;
import software.amazon.awssdk.services.ec2.model.ReplaceRouteTableAssociationResult;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResult;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;

/**
 * Tests of the RouteTable and Route apis.
 */
public class EC2RouteTableIntegrationTest extends EC2VPCIntegrationTestBase {

    private static final String VPC_CIDR_BLOCK = "10.0.0.0/23";
    protected static final Log log = LogFactory.getLog(EC2RouteTableIntegrationTest.class);

    private static final String AVAILABILITY_ZONE = "us-east-1b";
    private static final String USER_DATA = "foobarbazbar";
    private static final String INSTANCE_TYPE = "c1.medium";

    private static Vpc vpc;
    private static Subnet subnet;
    private static RouteTable table;
    private static SecurityGroup vpcGroup;

    @BeforeClass
    public static void setUp() {
        cleanUp();
        createVPC();
        createVPCSecurityGroup();
        createSubnet();
        createRouteTable();
    }

    /**
     * Deletes all groups used by this test (this run or past runs), and any
     * VPCs that we can safely delete.
     */
    @AfterClass
    public static void cleanUp() {

        deleteAllSubnets();

        // Delete the network ACL before the vpc
        for ( NetworkAcl acl : ec2.describeNetworkAcls().getNetworkAcls() ) {
            if ( !acl.getIsDefault() )
                ec2.deleteNetworkAcl(new DeleteNetworkAclRequest().withNetworkAclId(acl.getNetworkAclId()));
        }

        // Assert there are no non-default ACLs left
        assertEquals(
                0,
                ec2.describeNetworkAcls(
                        new DescribeNetworkAclsRequest().withFilters(new Filter().withName("default").withValues(
                                "false"))).getNetworkAcls().size());

        deleteAllVpcs();
    }

    private static void createVPC() {
        CreateVpcResult result = ec2.createVpc(new CreateVpcRequest()
                .withCidrBlock(VPC_CIDR_BLOCK));
        vpc = result.getVpc();
    }

    private static void createRouteTable() {
        CreateRouteTableRequest createRequest = new CreateRouteTableRequest().withVpcId(vpc.getVpcId());
        CreateRouteTableResult createResult = ec2.createRouteTable(createRequest);
        assertNotNull(createResult.getRouteTable().getRouteTableId());
        table = createResult.getRouteTable();

        assertNotNull(table.getVpcId());
        assertEquals(vpc.getVpcId(), table.getVpcId());

        assertEquals(1, table.getRoutes().size());
        assertEquals(VPC_CIDR_BLOCK, table.getRoutes().get(0).getDestinationCidrBlock());
        assertEquals("local", table.getRoutes().get(0).getGatewayId());
        assertEquals("active", table.getRoutes().get(0).getState());

        assertNotNull(table.getTags());
        assertEquals(0, table.getTags().size());
        assertNotNull(table.getAssociations());
        assertEquals(0, table.getAssociations().size());
    }

    protected static void createSubnet() {
        subnet = ec2.createSubnet(new CreateSubnetRequest().withVpcId(vpc
                .getVpcId()).withCidrBlock(VPC_CIDR_BLOCK)
                .withAvailabilityZone(AVAILABILITY_ZONE))
                .getSubnet();
    }

    private static void createVPCSecurityGroup() {
        String groupName = "group" + System.currentTimeMillis();
        String description = "Test group";
        String vpcGroupId = ec2.createSecurityGroup(
                new CreateSecurityGroupRequest().withGroupName(groupName).withDescription(description)
                        .withVpcId(vpc.getVpcId())).getGroupId();
        vpcGroup = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupIds(vpcGroupId))
                .getSecurityGroups().get(0);

        assertEquals(groupName, vpcGroup.getGroupName());
        assertEquals(vpc.getVpcId(), vpcGroup.getVpcId());
        assertEquals(description, vpcGroup.getDescription());
    }

    /**
     * Tests describing the route tables.
     */
    @Test
    public void testDescribeRouteTables() {
        tagResource(table.getRouteTableId(), TAGS);

        DescribeRouteTablesRequest describeRequest = new DescribeRouteTablesRequest().withRouteTableIds(table
                .getRouteTableId());
        DescribeRouteTablesResult describeResult = ec2.describeRouteTables(describeRequest);

        assertEquals(1, describeResult.getRouteTables().size());
        table = describeResult.getRouteTables().get(0);

        assertNotNull(table.getVpcId());
        assertEquals(vpc.getVpcId(), table.getVpcId());
        assertEquals(1, table.getRoutes().size());
        assertEquals(VPC_CIDR_BLOCK, table.getRoutes().get(0).getDestinationCidrBlock());
        assertEquals("local", table.getRoutes().get(0).getGatewayId());
        assertEquals("active", table.getRoutes().get(0).getState());

        assertEqualUnorderedTagLists(TAGS, table.getTags());
    }

    /**
     * Tests associating and disassociating the route table with a subnet.
     */
    @Test
    public void testAssociateAndDisassociate() {
        AssociateRouteTableRequest associateRequest = new AssociateRouteTableRequest().withRouteTableId(
                table.getRouteTableId()).withSubnetId(subnet.getSubnetId());

        AssociateRouteTableResult associateResult = ec2.associateRouteTable(associateRequest);
        assertNotNull(associateResult.getAssociationId());

        String associateId = associateResult.getAssociationId();

        ReplaceRouteTableAssociationRequest replaceRequest = new ReplaceRouteTableAssociationRequest()
                .withAssociationId(associateId).withRouteTableId(table.getRouteTableId());
        ReplaceRouteTableAssociationResult replaceResult = ec2.replaceRouteTableAssociation(replaceRequest);
        String associationid = replaceResult.getNewAssociationId();

        table = describeTable();

        assertNotNull(table.getAssociations());
        assertEquals(1, table.getAssociations().size());
        assertEquals(associationid, table.getAssociations().get(0).getRouteTableAssociationId());
        assertEquals(table.getRouteTableId(), table.getAssociations().get(0).getRouteTableId());
        assertEquals(subnet.getSubnetId(), table.getAssociations().get(0).getSubnetId());

        DisassociateRouteTableRequest disassociateRequest = new DisassociateRouteTableRequest()
                .withAssociationId(associationid);
        ec2.disassociateRouteTable(disassociateRequest);

        table = describeTable();

        assertNotNull(table.getAssociations());
        assertEquals(0, table.getAssociations().size());
    }

    /**
     * Tests creating and deleting routes in the table
     */
    @Test
    public void testCreateAndDeleteRoutes() throws Exception {
        String instanceId = createInstanceWithSecurityGroup();
        try {
            waitForInstanceToTransitionToState(instanceId, InstanceStateName.Running);

            CreateRouteRequest createRouteRequest = new CreateRouteRequest().withDestinationCidrBlock("0.0.0.0/0")
                    .withInstanceId(instanceId).withRouteTableId(table.getRouteTableId());

            ec2.createRoute(createRouteRequest);

            table = describeTable();
            assertEquals(2, table.getRoutes().size());

            ReplaceRouteRequest replaceRouteRequest = new ReplaceRouteRequest().withDestinationCidrBlock("0.0.0.0/0")
                    .withInstanceId(instanceId).withRouteTableId(table.getRouteTableId());
            ec2.replaceRoute(replaceRouteRequest);

            table = describeTable();
            assertEquals(2, table.getRoutes().size());

            DeleteRouteRequest deleteRoute = new DeleteRouteRequest().withRouteTableId(table.getRouteTableId())
                    .withDestinationCidrBlock("0.0.0.0/0");
            ec2.deleteRoute(deleteRoute);

            table = describeTable();
            assertEquals(1, table.getRoutes().size());

        } finally {
            super.terminateInstance(instanceId);
            super.waitForInstanceToTransitionToState(instanceId, InstanceStateName.Terminated);
        }

    }

    private String createInstanceWithSecurityGroup() {
        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs(new DescribeKeyPairsRequest()).getKeyPairs();
        assertTrue("No existing key pairs to test with", keyPairs.size() > 0);
        String existingKeyPairName = keyPairs.get(0).getKeyName();

        RunInstancesRequest request = new RunInstancesRequest();

        request.withImageId(AMI_ID);
        request.withMinCount(1);
        request.withMaxCount(1);
        request.withInstanceType(INSTANCE_TYPE);
        request.withMonitoring(true);
        request.withUserData(USER_DATA);
        request.withKernelId(KERNEL_ID);
        request.withRamdiskId(RAMDISK_ID);
        request.withKeyName(existingKeyPairName);
        request.withSecurityGroupIds(vpcGroup.getGroupId());
        request.withClientToken("MyClientToken" + System.currentTimeMillis());
        request.withSubnetId(subnet.getSubnetId());

        BlockDeviceMapping mapping = new BlockDeviceMapping();
        mapping.withDeviceName("/dev/sdh").withVirtualName("ephemeral0");
        request.withBlockDeviceMappings(mapping);

        request.withPlacement(new Placement().withAvailabilityZone(AVAILABILITY_ZONE));
        RunInstancesResult result = ec2.runInstances(request);
        assertEquals(1, result.getReservation().getInstances().size());
        assertEquals(1, result.getReservation().getInstances().get(0).getSecurityGroups().size());
        assertEquals(vpcGroup.getGroupId(), result.getReservation().getInstances().get(0).getSecurityGroups().get(0)
                .getGroupId());

        DescribeInstancesRequest describeRequest = new DescribeInstancesRequest();
        describeRequest.withInstanceIds(result.getReservation().getInstances().get(0).getInstanceId());
        DescribeInstancesResult desResult = ec2.describeInstances(describeRequest);
        assertEquals(1, desResult.getReservations().get(0).getInstances().size());
        Instance inst = desResult.getReservations().get(0).getInstances().get(0);

        return inst.getInstanceId();
    }

    private RouteTable describeTable() {
        return ec2.describeRouteTables(new DescribeRouteTablesRequest().withRouteTableIds(table.getRouteTableId()))
                .getRouteTables().get(0);
    }

}
