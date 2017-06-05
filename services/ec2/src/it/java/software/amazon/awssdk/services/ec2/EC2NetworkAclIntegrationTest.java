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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.CreateNetworkAclEntryRequest;
import software.amazon.awssdk.services.ec2.model.CreateNetworkAclRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteNetworkAclEntryRequest;
import software.amazon.awssdk.services.ec2.model.DeleteNetworkAclRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkAclsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkAclsResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IcmpTypeCode;
import software.amazon.awssdk.services.ec2.model.NetworkAcl;
import software.amazon.awssdk.services.ec2.model.NetworkAclAssociation;
import software.amazon.awssdk.services.ec2.model.NetworkAclEntry;
import software.amazon.awssdk.services.ec2.model.PortRange;
import software.amazon.awssdk.services.ec2.model.ReplaceNetworkAclAssociationRequest;
import software.amazon.awssdk.services.ec2.model.ReplaceNetworkAclAssociationResult;
import software.amazon.awssdk.services.ec2.model.ReplaceNetworkAclEntryRequest;

/**
 * Tests the NetworksACL related APIs
 */
public class EC2NetworkAclIntegrationTest extends EC2IntegrationTestBase {

    private static final String VPC_CIDR_BLOCK = "10.0.0.0/23";
    private static final String SUBNET_CIDR_BLOCK = VPC_CIDR_BLOCK;
    private static final Integer DEFAULT_ACL_ENTRY_COUNT = 2; // one for egress and one for ingress
    /** The id of the VPC created by this test. */
    private static String vpcId;
    /** The id of the Subnet created by this test. */
    private static String subnetId;
    /** The id of the network ACL created by this test. */
    private static String networkAclId;

    @BeforeClass
    public static void setUp() {
        vpcId = createVPC();
        subnetId = createSubnet(vpcId);
        networkAclId = createNetworkAcl(vpcId);
    }

    /**
     * Deletes resources created by this test
     */
    @AfterClass
    public static void cleanUp() {
        if (subnetId != null) {
            ec2.deleteSubnet(DeleteSubnetRequest.builder().subnetId(subnetId).build());
        }
        if (networkAclId != null) {
            ec2.deleteNetworkAcl(DeleteNetworkAclRequest.builder()
                                                        .networkAclId(networkAclId).build());
        }
        if (vpcId != null) {
            ec2.deleteVpc(DeleteVpcRequest.builder().vpcId(vpcId).build());
        }
    }

    private static String createVPC() {
        return ec2.createVpc(CreateVpcRequest.builder()
                                             .cidrBlock(VPC_CIDR_BLOCK).build()
                            ).vpc().vpcId();
    }

    private static String createSubnet(String vpcId) {
        return ec2.createSubnet(CreateSubnetRequest.builder()
                                                   .vpcId(vpcId)
                                                   .cidrBlock(SUBNET_CIDR_BLOCK).build()
                               ).subnet().subnetId();
    }

    private static String createNetworkAcl(String vpcId) {

        NetworkAcl networkAcl = ec2.createNetworkAcl(
                CreateNetworkAclRequest.builder()
                                       .vpcId(vpcId).build()).networkAcl();
        String networkAclId = networkAcl.networkAclId();

        assertStringNotEmpty(networkAclId);
        assertEquals(vpcId, networkAcl.vpcId());
        assertEquals(DEFAULT_ACL_ENTRY_COUNT.intValue(), networkAcl.entries().size());
        assertFalse(networkAcl.entries().get(0).ruleNumber() == 0);
        assertNotNull(networkAcl.entries().get(0).protocol());
        assertEquals("deny", networkAcl.entries().get(0).ruleAction());
        assertEquals("0.0.0.0/0", networkAcl.entries().get(0).cidrBlock());

        return networkAclId;
    }

    private static NetworkAclEntry findEntry(NetworkAcl acl, Integer ruleNum, boolean egress) {
        for (NetworkAclEntry e : acl.entries()) {
            if (e.ruleNumber().equals(ruleNum) && e.egress() == egress) {
                return e;
            }
        }
        return null;
    }

    /**
     * Tests the describeNetworkACLs operation
     */
    @Test
    public void testDescribe() throws Exception {

        // Test limiting by ACL id + tagging
        tagResource(networkAclId, TAGS);
        DescribeNetworkAclsResult result = ec2.describeNetworkAcls(
                DescribeNetworkAclsRequest.builder()
                                          .networkAclIds(networkAclId).build());
        assertEquals(1, result.networkAcls().size());
        NetworkAcl acl = result.networkAcls().get(0);
        assertEquals(networkAclId, acl.networkAclId());
        assertEqualUnorderedTagLists(TAGS, acl.tags());

        // Test filter by ACL id
        result = ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder()
                                                                   .filters(Filter.builder()
                                                                                  .name("network-acl-id")
                                                                                  .values(networkAclId).build()).build());
        assertEquals(1, result.networkAcls().size());
        assertEquals(networkAclId, acl.networkAclId());
    }

    /**
     * Tests replacing a subnet association
     */
    @Test
    public void testReplaceAssociation() {

        // The subnet created by the test is automatically associated with the
        // default network-acl of the VPC. Find the id of the association
        // between the two.
        String existingAssociationId = null;
        for (NetworkAcl acl : ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder().build()).networkAcls()) {
            for (NetworkAclAssociation ass : acl.associations()) {
                if (subnetId.equals(ass.subnetId())) {
                    existingAssociationId = ass.networkAclAssociationId();
                }
            }
        }

        // Replace the association with our custom network-acl
        ReplaceNetworkAclAssociationResult replacementResult =
                ec2.replaceNetworkAclAssociation(
                        ReplaceNetworkAclAssociationRequest.builder()
                                                           .associationId(existingAssociationId)
                                                           .networkAclId(networkAclId).build());
        assertNotNull(replacementResult.newAssociationId());

        // Check the association via DescribeNetworkAcls
        String newAssociationId = null;
        for (NetworkAcl acl : ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder().build()).networkAcls()) {
            for (NetworkAclAssociation ass : acl.associations()) {
                if (subnetId.equals(ass.subnetId())) {
                    newAssociationId = ass.networkAclAssociationId();
                }
            }
        }
        assertEquals(replacementResult.newAssociationId(), newAssociationId);
    }

    /**
     * Tests creating, describing, and deleting ACL entries
     */
    @Test
    public void testNetworkAclEntries() {

        // Test create. We need to use icmp as the protocol to be able to tests
        // the icmp fields
        CreateNetworkAclEntryRequest createEntryRequest = CreateNetworkAclEntryRequest.builder()
                                                                                      .cidrBlock("0.0.0.0/16")
                                                                                      .egress(true)
                                                                                      .icmpTypeCode(IcmpTypeCode.builder()
                                                                                                                .code(-1)
                                                                                                                .type(-1).build())
                                                                                      .networkAclId(networkAclId)
                                                                                      .protocol("1")
                                                                                      .ruleAction("allow")
                                                                                      .ruleNumber(42).build();
        ec2.createNetworkAclEntry(createEntryRequest);

        NetworkAcl acl = ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder()
                                                                           .networkAclIds(networkAclId).build()
                                                ).networkAcls().get(0);

        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 1, acl.entries().size());

        NetworkAclEntry entry = findEntry(acl,
                                          createEntryRequest.ruleNumber(), createEntryRequest.egress());
        assertNotNull(entry);
        assertEquals(createEntryRequest.cidrBlock(), entry.cidrBlock());
        assertEquals(createEntryRequest.egress(), entry.egress());
        assertEquals(createEntryRequest.icmpTypeCode().code(), entry.icmpTypeCode().code());
        assertEquals(createEntryRequest.icmpTypeCode().type(), entry.icmpTypeCode().type());
        assertEquals(createEntryRequest.protocol(), entry.protocol());
        assertEquals(createEntryRequest.ruleAction(), entry.ruleAction());

        // Test adding an ingress rule with the same number
        createEntryRequest = createEntryRequest.toBuilder().egress(false).build();
        ec2.createNetworkAclEntry(createEntryRequest);
        acl = ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder()
                                                                .networkAclIds(acl.networkAclId()).build()
                                     ).networkAcls().get(0);

        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 2, acl.entries().size());
        entry = findEntry(acl, createEntryRequest.ruleNumber(), createEntryRequest.egress());
        assertEquals(createEntryRequest.egress(), entry.egress());

        // Test replace. Not all protocols allow a port range to be specified.
        ReplaceNetworkAclEntryRequest replaceRequest = ReplaceNetworkAclEntryRequest.builder()
                                                                                    .cidrBlock("0.0.0.0/16")
                                                                                    .egress(true)
                                                                                    .portRange(PortRange.builder()
                                                                                                        .from(1)
                                                                                                        .to(100).build())
                                                                                    .networkAclId(networkAclId)
                                                                                    .protocol("17")
                                                                                    .ruleAction("deny")
                                                                                    .ruleNumber(42).build();
        ec2.replaceNetworkAclEntry(replaceRequest);

        acl = ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder()
                                                                .networkAclIds(networkAclId).build()
                                     ).networkAcls().get(0);
        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 2, acl.entries().size());

        entry = findEntry(acl, replaceRequest.ruleNumber(), replaceRequest.egress());
        assertNotNull(entry);
        assertEquals(replaceRequest.cidrBlock(), entry.cidrBlock());
        assertEquals(replaceRequest.egress(), entry.egress());
        assertEquals(replaceRequest.portRange().from(), entry.portRange().from());
        assertEquals(replaceRequest.portRange().to(), entry.portRange().to());
        assertEquals(replaceRequest.protocol(), entry.protocol());
        assertEquals(replaceRequest.ruleAction(), entry.ruleAction());

        // Test delete
        DeleteNetworkAclEntryRequest deleteRequest = DeleteNetworkAclEntryRequest.builder()
                                                                                 .networkAclId(networkAclId)
                                                                                 .egress(true)
                                                                                 .ruleNumber(replaceRequest.ruleNumber()).build();
        ec2.deleteNetworkAclEntry(deleteRequest);
        acl = ec2.describeNetworkAcls(DescribeNetworkAclsRequest.builder()
                                                                .networkAclIds(acl.networkAclId()).build()
                                     ).networkAcls().get(0);
        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 1, acl.entries().size());
    }
}
