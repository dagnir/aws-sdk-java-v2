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

    protected static final Log log = LogFactory.getLog(EC2RouteTableIntegrationTest.class);
    private static final String VPC_CIDR_BLOCK = "10.0.0.0/23";
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
        for (NetworkAcl acl : ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder().build()).networkAcls()) {
            if (!acl.isDefault()) {
                ec2.deleteNetworkAcl(DeleteNetworkAclRequest.builder().networkAclId(acl.networkAclId()).build());
            }
        }

        // Assert there are no non-default ACLs left
        assertEquals(
                0,
                ec2.describeNetworkAcls(
                        DescribeNetworkAclsRequest.builder()
                                                  .filters(Filter.builder().name("default").values("false").build())
                                                  .build()).networkAcls().size());

        deleteAllVpcs();
    }

    private static void createVPC() {
        CreateVpcResult result = ec2.createVpc(CreateVpcRequest.builder()
                                                               .cidrBlock(VPC_CIDR_BLOCK).build());
        vpc = result.vpc();
    }

    private static void createRouteTable() {
        CreateRouteTableRequest createRequest = CreateRouteTableRequest.builder().vpcId(vpc.vpcId()).build();
        CreateRouteTableResult createResult = ec2.createRouteTable(createRequest);
        assertNotNull(createResult.routeTable().routeTableId());
        table = createResult.routeTable();

        assertNotNull(table.vpcId());
        assertEquals(vpc.vpcId(), table.vpcId());

        assertEquals(1, table.routes().size());
        assertEquals(VPC_CIDR_BLOCK, table.routes().get(0).destinationCidrBlock());
        assertEquals("local", table.routes().get(0).gatewayId());
        assertEquals("active", table.routes().get(0).state());

        assertNotNull(table.tags());
        assertEquals(0, table.tags().size());
        assertNotNull(table.associations());
        assertEquals(0, table.associations().size());
    }

    protected static void createSubnet() {
        subnet = ec2.createSubnet(CreateSubnetRequest.builder().vpcId(vpc.vpcId()).cidrBlock(VPC_CIDR_BLOCK)
                                                     .availabilityZone(AVAILABILITY_ZONE).build())
                    .subnet();
    }

    private static void createVPCSecurityGroup() {
        String groupName = "group" + System.currentTimeMillis();
        String description = "Test group";
        String vpcGroupId = ec2.createSecurityGroup(
                CreateSecurityGroupRequest.builder().groupName(groupName).description(description)
                                          .vpcId(vpc.vpcId()).build()).groupId();
        vpcGroup = ec2.describeSecurityGroups(DescribeSecurityGroupsRequest.builder().groupIds(vpcGroupId).build())
                      .securityGroups().get(0);

        assertEquals(groupName, vpcGroup.groupName());
        assertEquals(vpc.vpcId(), vpcGroup.vpcId());
        assertEquals(description, vpcGroup.description());
    }

    /**
     * Tests describing the route tables.
     */
    @Test
    public void testDescribeRouteTables() {
        tagResource(table.routeTableId(), TAGS);

        DescribeRouteTablesRequest describeRequest =
                DescribeRouteTablesRequest.builder().routeTableIds(table.routeTableId()).build();
        DescribeRouteTablesResult describeResult = ec2.describeRouteTables(describeRequest);

        assertEquals(1, describeResult.routeTables().size());
        table = describeResult.routeTables().get(0);

        assertNotNull(table.vpcId());
        assertEquals(vpc.vpcId(), table.vpcId());
        assertEquals(1, table.routes().size());
        assertEquals(VPC_CIDR_BLOCK, table.routes().get(0).destinationCidrBlock());
        assertEquals("local", table.routes().get(0).gatewayId());
        assertEquals("active", table.routes().get(0).state());

        assertEqualUnorderedTagLists(TAGS, table.tags());
    }

    /**
     * Tests associating and disassociating the route table with a subnet.
     */
    @Test
    public void testAssociateAndDisassociate() {
        AssociateRouteTableRequest associateRequest = AssociateRouteTableRequest.builder().routeTableId(
                table.routeTableId()).subnetId(subnet.subnetId()).build();

        AssociateRouteTableResult associateResult = ec2.associateRouteTable(associateRequest);
        assertNotNull(associateResult.associationId());

        String associateId = associateResult.associationId();

        ReplaceRouteTableAssociationRequest replaceRequest = ReplaceRouteTableAssociationRequest.builder()
                                                                                                .associationId(associateId)
                                                                                                .routeTableId(
                                                                                                        table.routeTableId())
                                                                                                .build();
        ReplaceRouteTableAssociationResult replaceResult = ec2.replaceRouteTableAssociation(replaceRequest);
        String associationid = replaceResult.newAssociationId();

        table = describeTable();

        assertNotNull(table.associations());
        assertEquals(1, table.associations().size());
        assertEquals(associationid, table.associations().get(0).routeTableAssociationId());
        assertEquals(table.routeTableId(), table.associations().get(0).routeTableId());
        assertEquals(subnet.subnetId(), table.associations().get(0).subnetId());

        DisassociateRouteTableRequest disassociateRequest = DisassociateRouteTableRequest.builder()
                                                                                         .associationId(associationid).build();
        ec2.disassociateRouteTable(disassociateRequest);

        table = describeTable();

        assertNotNull(table.associations());
        assertEquals(0, table.associations().size());
    }

    /**
     * Tests creating and deleting routes in the table
     */
    @Test
    public void testCreateAndDeleteRoutes() throws Exception {
        String instanceId = createInstanceWithSecurityGroup();
        try {
            waitForInstanceToTransitionToState(instanceId, InstanceStateName.Running);

            CreateRouteRequest createRouteRequest = CreateRouteRequest.builder().destinationCidrBlock("0.0.0.0/0")
                                                                      .instanceId(instanceId).routeTableId(table.routeTableId())
                                                                      .build();

            ec2.createRoute(createRouteRequest);

            table = describeTable();
            assertEquals(2, table.routes().size());

            ReplaceRouteRequest replaceRouteRequest = ReplaceRouteRequest.builder().destinationCidrBlock("0.0.0.0/0")
                                                                         .instanceId(instanceId)
                                                                         .routeTableId(table.routeTableId()).build();
            ec2.replaceRoute(replaceRouteRequest);

            table = describeTable();
            assertEquals(2, table.routes().size());

            DeleteRouteRequest deleteRoute = DeleteRouteRequest.builder().routeTableId(table.routeTableId())
                                                               .destinationCidrBlock("0.0.0.0/0").build();
            ec2.deleteRoute(deleteRoute);

            table = describeTable();
            assertEquals(1, table.routes().size());

        } finally {
            super.terminateInstance(instanceId);
            super.waitForInstanceToTransitionToState(instanceId, InstanceStateName.Terminated);
        }

    }

    private String createInstanceWithSecurityGroup() {
        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs(DescribeKeyPairsRequest.builder().build()).keyPairs();
        assertTrue("No existing key pairs to test with", keyPairs.size() > 0);
        String existingKeyPairName = keyPairs.get(0).keyName();

        RunInstancesRequest.Builder request = RunInstancesRequest.builder();

        request.imageId(AMI_ID);
        request.minCount(1);
        request.maxCount(1);
        request.instanceType(INSTANCE_TYPE);
        request.monitoring(true);
        request.userData(USER_DATA);
        request.kernelId(KERNEL_ID);
        request.ramdiskId(RAMDISK_ID);
        request.keyName(existingKeyPairName);
        request.securityGroupIds(vpcGroup.groupId());
        request.clientToken("MyClientToken" + System.currentTimeMillis());
        request.subnetId(subnet.subnetId());

        BlockDeviceMapping.Builder mapping = BlockDeviceMapping.builder();
        mapping.deviceName("/dev/sdh").virtualName("ephemeral0");
        request.blockDeviceMappings(mapping.build());

        request.placement(Placement.builder().availabilityZone(AVAILABILITY_ZONE).build());
        RunInstancesResult result = ec2.runInstances(request.build());
        assertEquals(1, result.reservation().instances().size());
        assertEquals(1, result.reservation().instances().get(0).securityGroups().size());
        assertEquals(vpcGroup.groupId(), result.reservation().instances().get(0).securityGroups().get(0)
                                               .groupId());

        DescribeInstancesRequest.Builder describeRequest = DescribeInstancesRequest.builder();
        describeRequest.instanceIds(result.reservation().instances().get(0).instanceId());
        DescribeInstancesResult desResult = ec2.describeInstances(describeRequest.build());
        assertEquals(1, desResult.reservations().get(0).instances().size());
        Instance inst = desResult.reservations().get(0).instances().get(0);

        return inst.instanceId();
    }

    private RouteTable describeTable() {
        return ec2.describeRouteTables(DescribeRouteTablesRequest.builder().routeTableIds(table.routeTableId()).build())
                  .routeTables().get(0);
    }

}
