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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResult;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.UserIdGroupPair;
import software.amazon.awssdk.services.ec2.model.Vpc;

/**
 * Integration tests for the EC2 Security Group operations.
 *
 * @author fulghum@amazon.com
 */
public class EC2SecurityGroupsIntegrationTest extends EC2VPCIntegrationTestBase {

    protected static final Log log = LogFactory.getLog(EC2SecurityGroupsIntegrationTest.class);
    private static final String GROUP_NAME_PREFIX = EC2SecurityGroupsIntegrationTest.class.getName().replace('.', '_');
    /*
     * Test Data Constants
     */
    private static final int TO_PORT = 200;
    private static final int FROM_PORT = 100;
    private static final String CIDR_IP_RANGE = "205.192.0.0/16";
    private static final String VPC_CIDR_BLOCK = "10.0.0.0/23";
    private static final String TCP_PROTOCOL = "tcp";
    private static final String GROUP_DESCRIPTION = "FooBar Group Description";

    /**
     * The main security group created, deleted, modified, described, etc, by
     * each test method.
     */
    private static SecurityGroup testGroup;

    /**
     * Second test security group for testing user group security group permissions.
     */
    private static SecurityGroup sourceGroup;

    /**
     * VPC security group
     */
    private static SecurityGroup vpcGroup;

    private static Vpc vpc;

    @BeforeClass
    public static void setUp() {
        cleanUp();
        createSecurityGroup();
        createVPCSecurityGroup();
    }

    /**
     * Deletes all groups used by this test (this run or past runs), and any
     * VPCs that we can safely delete.
     */
    @AfterClass
    public static void cleanUp() {
        // Do a two-pass through the groups to delete them all. The first time
        // through, we might get unlucky and try to delete one before we delete
        // one that references it, so we ignore errors on the first pass and
        // throw them on the second.
        for (SecurityGroup group : ec2.describeSecurityGroups(
                DescribeSecurityGroupsRequest.builder().build()).securityGroups()) {
            log.info("Found group " + group);
            if (group.groupName().startsWith(GROUP_NAME_PREFIX)) {
                log.warn("Deleting group " + group);
                try {
                    ec2.deleteSecurityGroup(DeleteSecurityGroupRequest.builder()
                                                    .groupId(group.groupId()).build());
                } catch (Exception ignored) {
                    // Ignored or expected.
                }
            }
        }
        for (SecurityGroup group : ec2.describeSecurityGroups(
                DescribeSecurityGroupsRequest.builder().build()).securityGroups()) {
            if (group.groupName().startsWith(GROUP_NAME_PREFIX)) {
                log.warn("Deleting group " + group);
                ec2.deleteSecurityGroup(DeleteSecurityGroupRequest.builder()
                                                .groupId(group.groupId()).build());
            }
        }

        deleteAllVpcs();
    }

    private static void createSecurityGroup() {
        String groupName = createUniqueGroupName();
        assertFalse(doesSecurityGroupExist(groupName));

        testGroup = createGroup(groupName, GROUP_DESCRIPTION);

        assertEquals(groupName, testGroup.groupName());
        assertEquals(GROUP_DESCRIPTION, testGroup.description());
        assertTrue(doesSecurityGroupExist(groupName));
    }

    private static void createVPCSecurityGroup() {
        CreateVpcResult result = ec2.createVpc(CreateVpcRequest.builder()
                                                       .cidrBlock(VPC_CIDR_BLOCK).build());
        vpc = result.vpc();

        String groupName = createUniqueGroupName();
        String description = "Test group";
        String vpcGroupId = ec2.createSecurityGroup(
                CreateSecurityGroupRequest.builder().groupName(
                        groupName).description(
                        description).vpcId(vpc.vpcId()).build()).groupId();
        vpcGroup = ec2.describeSecurityGroups(
                DescribeSecurityGroupsRequest.builder().groupIds(vpcGroupId).build())
                      .securityGroups().get(0);

        assertEquals(groupName, vpcGroup.groupName());
        assertEquals(vpc.vpcId(), vpcGroup.vpcId());
        assertEquals(description, vpcGroup.description());
    }

    /**
     * Returns true if the specified security group exists.
     *
     * @param groupName
     *            The name of the security group to check.
     *
     * @return True if the specified group exists, otherwise false.
     */
    private static boolean doesSecurityGroupExist(String groupName) {
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder().groupNames(groupName).build();
        try {
            DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request);
            return result.securityGroups().size() > 0;
        } catch (AmazonServiceException ase) {
            if (ase.getErrorCode().equals("InvalidGroup.NotFound")) {
                return false;
            }
            throw ase;
        }
    }

    /**
     * Creates a new security group, describes it, and returns the security
     * group object.
     *
     * @param name
     *            The name for the new group.
     * @param description
     *            The description for the new group.
     *
     * @return The details of the new security group.
     */
    private static SecurityGroup createGroup(String name, String description) {
        CreateSecurityGroupRequest.Builder createGroupRequest = CreateSecurityGroupRequest.builder();
        createGroupRequest.groupName(name);
        createGroupRequest.description(description);
        ec2.createSecurityGroup(createGroupRequest.build());

        DescribeSecurityGroupsRequest.Builder describeGroupRequest = DescribeSecurityGroupsRequest.builder();
        describeGroupRequest.groupNames(name);
        return ec2.describeSecurityGroups(describeGroupRequest.build()).securityGroups().get(0);
    }

    /**
     * Creates a unique security group name, using a timestamp suffix.
     *
     * @return A unique security group name.
     */
    private static String createUniqueGroupName() {
        return GROUP_NAME_PREFIX + "-" + System.currentTimeMillis();
    }

    /**
     * Tests that describeSecurityGroups correctly returns our security groups.
     */
    @Test
    public void testDescribeSecurityGroups() {
        // no-required-args method form
        DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(DescribeSecurityGroupsRequest.builder().build());
        List<SecurityGroup> groups = result.securityGroups();
        Map<String, SecurityGroup> securityGroupsByName = convertSecurityGroupListToMap(groups);

        SecurityGroup group = securityGroupsByName.get(testGroup.groupName());
        assertNotNull(group);
        assertEquals(GROUP_DESCRIPTION, group.description());

        group = securityGroupsByName.get(vpcGroup.groupName());
        assertNotNull(group);
        assertEquals(vpc.vpcId(), group.vpcId());

        // filters
        DescribeSecurityGroupsRequest.Builder request = DescribeSecurityGroupsRequest.builder();
        request.filters(Filter.builder().name("group-name").values(testGroup.groupName()).build());
        List<SecurityGroup> securityGroups = ec2.describeSecurityGroups(request.build()).securityGroups();
        assertEquals(1, securityGroups.size());
        assertEquals(testGroup.groupName(), securityGroups.get(0).groupName());
    }

    /**
     * Tests that we can authorize an IP permission for the security group we
     * previously created, using the new IP permission list query parameters.
     */
    @Test
    public void testAuthorizeIPSecurityGroupIngress() {
        assertFalse(doesIpPermissionExist(testGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));

        AuthorizeSecurityGroupIngressRequest.Builder request = AuthorizeSecurityGroupIngressRequest.builder();
        request.groupName(testGroup.groupName());
        request.ipProtocol(TCP_PROTOCOL);
        request.cidrIp(CIDR_IP_RANGE);
        request.fromPort(FROM_PORT);
        request.toPort(TO_PORT);
        ec2.authorizeSecurityGroupIngress(request.build());

        // Revoke our permissions to clean up
        assertTrue(doesIpPermissionExist(testGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
        RevokeSecurityGroupIngressRequest.Builder revokeRequest = RevokeSecurityGroupIngressRequest.builder();
        revokeRequest.groupName(testGroup.groupName());
        revokeRequest.ipProtocol(TCP_PROTOCOL);
        revokeRequest.cidrIp(CIDR_IP_RANGE);
        revokeRequest.fromPort(FROM_PORT);
        revokeRequest.toPort(TO_PORT);
        ec2.revokeSecurityGroupIngress(revokeRequest.build());

        assertFalse(doesIpPermissionExist(testGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
    }

    /**
     * Tests that we can authorize an IP permission for the VPC security group we
     * previously created using its group id
     */
    @Test
    public void testAuthorizeIPSecurityGroupIngressVPC() {
        assertFalse(doesIpPermissionExist(vpcGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));

        AuthorizeSecurityGroupIngressRequest.Builder request = AuthorizeSecurityGroupIngressRequest.builder();
        request.groupName(vpcGroup.groupName());
        request.ipPermissions(IpPermission.builder()
                                          .ipProtocol(TCP_PROTOCOL)
                                          .ipv4Ranges(IpRange.builder().cidrIp(CIDR_IP_RANGE).build())
                                          .fromPort(FROM_PORT)
                                          .toPort(TO_PORT).build());
        try {
            ec2.authorizeSecurityGroupIngress(request.build());
            fail("Expected exception: group ID required");
        } catch (Exception expected) {
            // Ignored or expected.
        }

        request.groupName(null).groupId(vpcGroup.groupId());
        ec2.authorizeSecurityGroupIngress(request.build());

        assertTrue(doesIpPermissionExist(vpcGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));

        // Now revoke the permission
        RevokeSecurityGroupIngressRequest.Builder revoke = RevokeSecurityGroupIngressRequest.builder();
        revoke.groupName(vpcGroup.groupName());
        revoke.ipPermissions(IpPermission.builder()
                                         .ipProtocol(TCP_PROTOCOL)
                                         .ipv4Ranges(IpRange.builder().cidrIp(CIDR_IP_RANGE).build())
                                         .fromPort(FROM_PORT)
                                         .toPort(TO_PORT).build());
        try {
            ec2.revokeSecurityGroupIngress(revoke.build());
            fail("Expected exception: group ID required");
        } catch (Exception expected) {
            // Ignored or expected.
        }

        revoke.groupName(null).groupId(vpcGroup.groupId());
        ec2.revokeSecurityGroupIngress(revoke.build());

        assertFalse(doesIpPermissionExist(vpcGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
    }

    /**
     * Tests that we can authorize a user group permission for the security
     * group we previously created, using the new IP permission query parameters.
     */
    @Test
    public void testAuthorizeUserGroupSecurityGroupIngress() {
        sourceGroup = createGroup(createUniqueGroupName(), GROUP_DESCRIPTION);
        assertFalse(doesUserGroupPermissionExist(testGroup.groupId(), sourceGroup.groupId(), sourceGroup.ownerId()));

        AuthorizeSecurityGroupIngressRequest.Builder request = AuthorizeSecurityGroupIngressRequest.builder();
        request.groupName(testGroup.groupName());
        request.ipPermissions(IpPermission.builder()
                                          .ipProtocol(TCP_PROTOCOL)
                                          .fromPort(FROM_PORT)
                                          .toPort(TO_PORT)
                                          .userIdGroupPairs(UserIdGroupPair.builder()
                                                                           .groupName(sourceGroup.groupName())
                                                                           .userId(sourceGroup.ownerId()).build())
                                          .build());
        ec2.authorizeSecurityGroupIngress(request.build());

        // Revoke our permission to clean up
        assertTrue(doesUserGroupPermissionExist(testGroup.groupId(), sourceGroup.groupId(), sourceGroup.ownerId()));

        RevokeSecurityGroupIngressRequest.Builder revokeRequest = RevokeSecurityGroupIngressRequest.builder();
        revokeRequest.groupName(testGroup.groupName());
        revokeRequest.ipPermissions(IpPermission.builder()
                                                .userIdGroupPairs(UserIdGroupPair.builder()
                                                                              .groupName(sourceGroup.groupName())
                                                                              .userId(sourceGroup.ownerId()).build())
                                                .ipProtocol(TCP_PROTOCOL)
                                                .fromPort(FROM_PORT)
                                                .toPort(TO_PORT)
                                                .build());
        ec2.revokeSecurityGroupIngress(revokeRequest.build());

        assertFalse(doesUserGroupPermissionExist(testGroup.groupId(), sourceGroup.groupId(), sourceGroup.ownerId()));
    }

    /**
     * Tests that we can authorize a user group permission for the security
     * group we previously created, using the legacy, deprecated query parameters.
     */
    @Test
    public void testLegacyAuthorizeUserGroupSecurityGroupIngress() {
        sourceGroup = createGroup(createUniqueGroupName(), GROUP_DESCRIPTION);
        assertFalse(doesUserGroupPermissionExist(testGroup.groupId(), sourceGroup.groupId(), sourceGroup.ownerId()));

        AuthorizeSecurityGroupIngressRequest.Builder request = AuthorizeSecurityGroupIngressRequest.builder();
        request.groupName(testGroup.groupName());
        request.sourceSecurityGroupName(sourceGroup.groupName());
        request.sourceSecurityGroupOwnerId(sourceGroup.ownerId());

        ec2.authorizeSecurityGroupIngress(request.build());

        assertTrue(doesUserGroupPermissionExist(testGroup.groupId(), sourceGroup.groupId(), sourceGroup.ownerId()));
    }

    /**
     * Tests authorizing and revoking vpc security group egress
     */
    @Test
    public void testAuthorizeSecurityGroupEgress() {
        assertFalse(doesIpEgressPermissionExist(vpcGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));

        AuthorizeSecurityGroupEgressRequest.Builder request = AuthorizeSecurityGroupEgressRequest.builder();
        request.groupId(vpcGroup.groupId());
        request.ipPermissions(IpPermission.builder()
                                          .ipProtocol(TCP_PROTOCOL)
                                          .ipv4Ranges(IpRange.builder().cidrIp(CIDR_IP_RANGE).build())
                                          .fromPort(FROM_PORT)
                                          .toPort(TO_PORT).build());
        ec2.authorizeSecurityGroupEgress(request.build());

        assertTrue(doesIpEgressPermissionExist(vpcGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));

        // Now revoke the permission
        RevokeSecurityGroupEgressRequest.Builder revoke = RevokeSecurityGroupEgressRequest.builder();
        revoke.groupId(vpcGroup.groupId());
        revoke.ipPermissions(IpPermission.builder()
                                         .ipProtocol(TCP_PROTOCOL)
                                         .ipv4Ranges(IpRange.builder().cidrIp(CIDR_IP_RANGE).build())
                                         .fromPort(FROM_PORT)
                                         .toPort(TO_PORT).build());
        ec2.revokeSecurityGroupEgress(revoke.build());

        assertFalse(doesIpEgressPermissionExist(vpcGroup.groupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
    }

    /**
     * Converts a list of security groups, into a map of security groups, keyed
     * by name.
     *
     * @param groups
     *            The list of security groups to convert.
     *
     * @return A map of the specified security groups, keyed by name.
     */
    private Map<String, SecurityGroup> convertSecurityGroupListToMap(List<SecurityGroup> groups) {
        Map<String, SecurityGroup> map = new HashMap<String, SecurityGroup>();

        for (SecurityGroup group : groups) {
            map.put(group.groupName(), group);
        }

        return map;
    }

    /**
     * Returns true if the specified security group contains an IP permission
     * matching the specified parameters.
     *
     * @param groupId
     *            The id of the security group to check.
     * @param protocol
     *            The expected protocol to find in an IP permission.
     * @param cidrIp
     *            The expected CIDR IP range to find in an IP permission.
     * @param fromPort
     *            The expected lower port limit to find in an IP permission.
     * @param toPort
     *            The expected upper port limit to find in an IP permission.
     *
     * @return True if the specified security group contains an IP permission
     *         matching the specified parameters, otherwise false.
     */
    private boolean doesIpPermissionExist(String groupId, String protocol, String cidrIp, int fromPort, int toPort) {
        Set<String> acceptableProtocols = new HashSet<String>();
        acceptableProtocols.add(protocol);

        SecurityGroup group = getSecurityGroup(groupId);
        if (group.vpcId() != null && "tcp".equals(protocol)) {
            acceptableProtocols.add("6");
        }

        for (IpPermission permission : group.ipPermissions()) {
            if (permission.fromPort().equals(fromPort)
                && permission.toPort().equals(toPort)
                && acceptableProtocols.contains(permission.ipProtocol())) {

                for (IpRange range : permission.ipv4Ranges()) {
                    if (range.cidrIp().equals(cidrIp)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns whether the given group has the given IP egress permission.
     *
     * @see #doesIpPermissionExist(String, String, String, int, int)
     */
    private boolean doesIpEgressPermissionExist(String groupId,
                                                String protocol, String cidrIpRange, int fromPort, int toPort) {
        Set<String> acceptableProtocols = new HashSet<String>();
        acceptableProtocols.add(protocol);

        SecurityGroup group = getSecurityGroup(groupId);
        if (group.vpcId() != null && "tcp".equals(protocol)) {
            acceptableProtocols.add("6");
        }

        for (IpPermission permission : group.ipPermissionsEgress()) {
            if (permission.fromPort() != null && permission.fromPort().equals(fromPort)
                && permission.toPort() != null && permission.toPort().equals(toPort)
                && acceptableProtocols.contains(permission.ipProtocol())) {

                for (IpRange range : permission.ipv4Ranges()) {
                    if (range.cidrIp().equals(cidrIpRange)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Return True if the specified security group contains an IP permission for
     * the specified source group and owner.
     *
     * @param groupId
     *            The name of the security group to check.
     * @param sourceGroupId
     *            The expected source group id in the IP permission.
     * @param sourceGroupOwnerId
     *            The expected source group owner in the IP permission.
     *
     * @return True if the specified security group contains an IP permission
     *         for the specified source group and owner, otherwise false.
     */
    private boolean doesUserGroupPermissionExist(String groupId, String sourceGroupId, String sourceGroupOwnerId) {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            // Ignored or expected.
        }
        SecurityGroup group = getSecurityGroup(groupId);
        for (IpPermission permission : group.ipPermissions()) {
            for (UserIdGroupPair pair : permission.userIdGroupPairs()) {
                if (pair.groupId().equals(sourceGroupId)
                    && pair.userId().equals(sourceGroupOwnerId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Queries Amazon EC2 for the specified security group and returns it.
     *
     * @param id
     *            The security group id to look up.
     *
     * @return The details on the specified security group.
     */
    private SecurityGroup getSecurityGroup(String id) {
        DescribeSecurityGroupsRequest.Builder request = DescribeSecurityGroupsRequest.builder();
        DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request.groupIds(id).build());

        List<SecurityGroup> groups = result.securityGroups();
        assertEquals(1, groups.size());

        return groups.get(0);
    }

}
