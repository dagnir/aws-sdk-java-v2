/*
 * Copyright 2015-2017 Amazon Technologies, Inc.
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
    /** The id of the VPC created by this test */
    private static String vpcId;
    /** The id of the Subnet created by this test */
    private static String subnetId;
    /** The id of the network ACL created by this test */
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
            ec2.deleteSubnet(new DeleteSubnetRequest(subnetId));
        }
        if (networkAclId != null) {
            ec2.deleteNetworkAcl(new DeleteNetworkAclRequest()
                                         .withNetworkAclId(networkAclId));
        }
        if (vpcId != null) {
            ec2.deleteVpc(new DeleteVpcRequest(vpcId));
        }
    }

    private static String createVPC() {
        return ec2.createVpc(new CreateVpcRequest()
                                     .withCidrBlock(VPC_CIDR_BLOCK)
                            ).getVpc().getVpcId();
    }

    private static String createSubnet(String vpcId) {
        return ec2.createSubnet(new CreateSubnetRequest()
                                        .withVpcId(vpcId)
                                        .withCidrBlock(SUBNET_CIDR_BLOCK)
                               ).getSubnet().getSubnetId();
    }

    private static String createNetworkAcl(String vpcId) {

        NetworkAcl networkAcl = ec2.createNetworkAcl(
                new CreateNetworkAclRequest()
                        .withVpcId(vpcId)
                                                    ).getNetworkAcl();
        String networkAclId = networkAcl.getNetworkAclId();

        assertStringNotEmpty(networkAclId);
        assertEquals(vpcId, networkAcl.getVpcId());
        assertEquals(DEFAULT_ACL_ENTRY_COUNT.intValue(), networkAcl.getEntries().size());
        assertFalse(networkAcl.getEntries().get(0).getRuleNumber() == 0);
        assertNotNull(networkAcl.getEntries().get(0).getProtocol());
        assertEquals("deny", networkAcl.getEntries().get(0).getRuleAction());
        assertEquals("0.0.0.0/0", networkAcl.getEntries().get(0).getCidrBlock());

        return networkAclId;
    }

    private static NetworkAclEntry findEntry(NetworkAcl acl, Integer ruleNum, boolean egress) {
        for (NetworkAclEntry e : acl.getEntries()) {
            if (e.getRuleNumber().equals(ruleNum) && e.getEgress() == egress) {
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
                new DescribeNetworkAclsRequest()
                        .withNetworkAclIds(networkAclId));
        assertEquals(1, result.getNetworkAcls().size());
        NetworkAcl acl = result.getNetworkAcls().get(0);
        assertEquals(networkAclId, acl.getNetworkAclId());
        assertEqualUnorderedTagLists(TAGS, acl.getTags());

        // Test filter by ACL id
        result = ec2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                                                 .withFilters(new Filter()
                                                                      .withName("network-acl-id")
                                                                      .withValues(networkAclId)));
        assertEquals(1, result.getNetworkAcls().size());
        assertEquals(networkAclId, acl.getNetworkAclId());
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
        for (NetworkAcl acl : ec2.describeNetworkAcls().getNetworkAcls()) {
            for (NetworkAclAssociation ass : acl.getAssociations()) {
                if (subnetId.equals(ass.getSubnetId())) {
                    existingAssociationId = ass.getNetworkAclAssociationId();
                }
            }
        }

        // Replace the association with our custom network-acl
        ReplaceNetworkAclAssociationResult replacementResult =
                ec2.replaceNetworkAclAssociation(
                        new ReplaceNetworkAclAssociationRequest()
                                .withAssociationId(existingAssociationId)
                                .withNetworkAclId(networkAclId));
        assertNotNull(replacementResult.getNewAssociationId());

        // Check the association via DescribeNetworkAcls
        String newAssociationId = null;
        for (NetworkAcl acl : ec2.describeNetworkAcls().getNetworkAcls()) {
            for (NetworkAclAssociation ass : acl.getAssociations()) {
                if (subnetId.equals(ass.getSubnetId())) {
                    newAssociationId = ass.getNetworkAclAssociationId();
                }
            }
        }
        assertEquals(replacementResult.getNewAssociationId(), newAssociationId);
    }

    /**
     * Tests creating, describing, and deleting ACL entries
     */
    @Test
    public void testNetworkAclEntries() {

        // Test create. We need to use icmp as the protocol to be able to tests
        // the icmp fields
        CreateNetworkAclEntryRequest createEntryRequest = new CreateNetworkAclEntryRequest()
                .withCidrBlock("0.0.0.0/16")
                .withEgress(true)
                .withIcmpTypeCode(new IcmpTypeCode()
                                          .withCode(-1)
                                          .withType(-1))
                .withNetworkAclId(networkAclId)
                .withProtocol("1")
                .withRuleAction("allow")
                .withRuleNumber(42);
        ec2.createNetworkAclEntry(createEntryRequest);

        NetworkAcl acl = ec2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                                                         .withNetworkAclIds(networkAclId)
                                                ).getNetworkAcls().get(0);

        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 1, acl.getEntries().size());

        NetworkAclEntry entry = findEntry(acl,
                                          createEntryRequest.getRuleNumber(), createEntryRequest.isEgress());
        assertNotNull(entry);
        assertEquals(createEntryRequest.getCidrBlock(), entry.getCidrBlock());
        assertEquals(createEntryRequest.getEgress(), entry.getEgress());
        assertEquals(createEntryRequest.getIcmpTypeCode().getCode(), entry.getIcmpTypeCode().getCode());
        assertEquals(createEntryRequest.getIcmpTypeCode().getType(), entry.getIcmpTypeCode().getType());
        assertEquals(createEntryRequest.getProtocol(), entry.getProtocol());
        assertEquals(createEntryRequest.getRuleAction(), entry.getRuleAction());

        // Test adding an ingress rule with the same number
        createEntryRequest.setEgress(false);
        ec2.createNetworkAclEntry(createEntryRequest);
        acl = ec2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                                              .withNetworkAclIds(acl.getNetworkAclId())
                                     ).getNetworkAcls().get(0);

        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 2, acl.getEntries().size());
        entry = findEntry(acl, createEntryRequest.getRuleNumber(), createEntryRequest.getEgress());
        assertEquals(createEntryRequest.getEgress(), entry.getEgress());

        // Test replace. Not all protocols allow a port range to be specified.
        ReplaceNetworkAclEntryRequest replaceRequest = new ReplaceNetworkAclEntryRequest()
                .withCidrBlock("0.0.0.0/16")
                .withEgress(true)
                .withPortRange(new PortRange()
                                       .withFrom(1)
                                       .withTo(100))
                .withNetworkAclId(networkAclId)
                .withProtocol("17")
                .withRuleAction("deny")
                .withRuleNumber(42);
        ec2.replaceNetworkAclEntry(replaceRequest);

        acl = ec2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                                              .withNetworkAclIds(networkAclId)
                                     ).getNetworkAcls().get(0);
        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 2, acl.getEntries().size());

        entry = findEntry(acl, replaceRequest.getRuleNumber(), replaceRequest.getEgress());
        assertNotNull(entry);
        assertEquals(replaceRequest.getCidrBlock(), entry.getCidrBlock());
        assertEquals(replaceRequest.getEgress(), entry.getEgress());
        assertEquals(replaceRequest.getPortRange().getFrom(), entry.getPortRange().getFrom());
        assertEquals(replaceRequest.getPortRange().getTo(), entry.getPortRange().getTo());
        assertEquals(replaceRequest.getProtocol(), entry.getProtocol());
        assertEquals(replaceRequest.getRuleAction(), entry.getRuleAction());

        // Test delete
        DeleteNetworkAclEntryRequest deleteRequest = new DeleteNetworkAclEntryRequest()
                .withNetworkAclId(networkAclId)
                .withEgress(true)
                .withRuleNumber(replaceRequest.getRuleNumber());
        ec2.deleteNetworkAclEntry(deleteRequest);
        acl = ec2.describeNetworkAcls(new DescribeNetworkAclsRequest()
                                              .withNetworkAclIds(acl.getNetworkAclId())
                                     ).getNetworkAcls().get(0);
        assertEquals(DEFAULT_ACL_ENTRY_COUNT + 1, acl.getEntries().size());
    }
}
