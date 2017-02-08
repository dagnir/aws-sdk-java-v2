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
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.ResponseMetadata;
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

    private static final String GROUP_NAME_PREFIX = EC2SecurityGroupsIntegrationTest.class.getName().replace('.', '_');
    protected static final Log log = LogFactory.getLog(EC2SecurityGroupsIntegrationTest.class);
    
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
				new DescribeSecurityGroupsRequest()).getSecurityGroups()) {
			log.info("Found group " + group);
			if (group.getGroupName().startsWith(GROUP_NAME_PREFIX)) {
				log.warn("Deleting group " + group);
				try {
					ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest()
							.withGroupId(group.getGroupId()));
				} catch (Exception ignored) {
				}
			}
		}		
		for (SecurityGroup group : ec2.describeSecurityGroups(
				new DescribeSecurityGroupsRequest()).getSecurityGroups()) {
			if (group.getGroupName().startsWith(GROUP_NAME_PREFIX)) {
				log.warn("Deleting group " + group);
				ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest()
						.withGroupId(group.getGroupId()));
			}
		}

		deleteAllVpcs();
	}
    
    private static void createSecurityGroup() {
        String groupName = createUniqueGroupName();
        assertFalse(doesSecurityGroupExist(groupName));

        testGroup = createGroup(groupName, GROUP_DESCRIPTION);

        assertEquals(groupName, testGroup.getGroupName());
        assertEquals(GROUP_DESCRIPTION, testGroup.getDescription());
        assertTrue(doesSecurityGroupExist(groupName));
    }
    
	private static void createVPCSecurityGroup() {
		CreateVpcResult result = ec2.createVpc(new CreateVpcRequest()
				.withCidrBlock(VPC_CIDR_BLOCK));
		vpc = result.getVpc();

		String groupName = createUniqueGroupName();
		String description = "Test group";
		String vpcGroupId = ec2.createSecurityGroup(
				new CreateSecurityGroupRequest().withGroupName(
						groupName).withDescription(
						description).withVpcId(vpc.getVpcId())).getGroupId();
		vpcGroup = ec2.describeSecurityGroups(
				new DescribeSecurityGroupsRequest().withGroupIds(vpcGroupId))
				.getSecurityGroups().get(0);
		
		assertEquals(groupName, vpcGroup.getGroupName());
		assertEquals(vpc.getVpcId(), vpcGroup.getVpcId());
		assertEquals(description, vpcGroup.getDescription());
	}
        
    /**
     * Tests that describeSecurityGroups correctly returns our security groups.
     */
    @Test
    public void testDescribeSecurityGroups() {
        // no-required-args method form
        DescribeSecurityGroupsResult result = ec2.describeSecurityGroups();
        List<SecurityGroup> groups = result.getSecurityGroups();
        Map<String, SecurityGroup> securityGroupsByName = convertSecurityGroupListToMap(groups);

        SecurityGroup group = securityGroupsByName.get(testGroup.getGroupName());
        assertNotNull(group);
        assertEquals(GROUP_DESCRIPTION, group.getDescription());
        
        group = securityGroupsByName.get(vpcGroup.getGroupName());
        assertNotNull(group);
        assertEquals(vpc.getVpcId(), group.getVpcId());
        
        // filters
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        request.withFilters(new Filter("group-name", null).withValues(testGroup.getGroupName()));
        List<SecurityGroup> securityGroups = ec2.describeSecurityGroups(request).getSecurityGroups();
        assertEquals(1, securityGroups.size());
        assertEquals(testGroup.getGroupName(), securityGroups.get(0).getGroupName());
    }

	/**
	 * Tests that we can authorize an IP permission for the security group we
	 * previously created, using the new IP permission list query parameters.
	 */
    @Test
    public void testAuthorizeIPSecurityGroupIngress() {
        assertFalse(doesIpPermissionExist(testGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));

        AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
        request.setGroupName(testGroup.getGroupName());
        request.setIpProtocol(TCP_PROTOCOL);
        request.setCidrIp(CIDR_IP_RANGE);
        request.setFromPort(FROM_PORT);
        request.setToPort(TO_PORT);
        ec2.authorizeSecurityGroupIngress(request);

        // Revoke our permissions to clean up
        assertTrue(doesIpPermissionExist(testGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
	    RevokeSecurityGroupIngressRequest revokeRequest = new RevokeSecurityGroupIngressRequest();
	    revokeRequest.setGroupName(testGroup.getGroupName());
	    revokeRequest.setIpProtocol(TCP_PROTOCOL);
	    revokeRequest.setCidrIp(CIDR_IP_RANGE);
	    revokeRequest.setFromPort(FROM_PORT);
	    revokeRequest.setToPort(TO_PORT);
	    ec2.revokeSecurityGroupIngress(revokeRequest);
	
	    ResponseMetadata responseMetadata = ec2.getCachedResponseMetadata(revokeRequest);
		Assert.assertThat(responseMetadata.getRequestId(), Matchers.not
				(Matchers.isEmptyOrNullString()));
	
	    assertFalse(doesIpPermissionExist(testGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
	}
	
	/**
	 * Tests that we can authorize an IP permission for the VPC security group we
	 * previously created using its group id
	 */
	@Test
	public void testAuthorizeIPSecurityGroupIngressVPC() {
	    assertFalse(doesIpPermissionExist(vpcGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
	
	    AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
	    request.setGroupName(vpcGroup.getGroupName());
	    request.withIpPermissions(new IpPermission()
	        .withIpProtocol(TCP_PROTOCOL)
	        .withIpRanges(CIDR_IP_RANGE)
	        .withFromPort(FROM_PORT)
	        .withToPort(TO_PORT));
	    try {
	    	ec2.authorizeSecurityGroupIngress(request);
	    	fail("Expected exception: group ID required");
	    } catch (Exception expected) {	    	
	    }
	    
	    request.withGroupName(null).withGroupId(vpcGroup.getGroupId());
    	ec2.authorizeSecurityGroupIngress(request);	    
	
	    assertTrue(doesIpPermissionExist(vpcGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
	    
	    // Now revoke the permission
	    RevokeSecurityGroupIngressRequest revoke = new RevokeSecurityGroupIngressRequest();
	    revoke.setGroupName(vpcGroup.getGroupName());
	    revoke.withIpPermissions(new IpPermission()
	        .withIpProtocol(TCP_PROTOCOL)
	        .withIpRanges(CIDR_IP_RANGE)
	        .withFromPort(FROM_PORT)
	        .withToPort(TO_PORT));
	    try {
	    	ec2.revokeSecurityGroupIngress(revoke);
	    	fail("Expected exception: group ID required");
	    } catch (Exception expected) {	    	
	    }
	    
	    revoke.withGroupName(null).withGroupId(vpcGroup.getGroupId());
    	ec2.revokeSecurityGroupIngress(revoke);
    	
	    assertFalse(doesIpPermissionExist(vpcGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));    	
	}

	/**
	 * Tests that we can authorize a user group permission for the security
	 * group we previously created, using the new IP permission query parameters.
	 */
	@Test
	public void testAuthorizeUserGroupSecurityGroupIngress() {
	    sourceGroup = createGroup(createUniqueGroupName(), GROUP_DESCRIPTION);
	    assertFalse(doesUserGroupPermissionExist(testGroup.getGroupId(), sourceGroup.getGroupId(), sourceGroup.getOwnerId()));
	
	    AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
	    request.setGroupName(testGroup.getGroupName());
	    request.withIpPermissions(new IpPermission()
	        .withIpProtocol(TCP_PROTOCOL)
	        .withFromPort(FROM_PORT)
	        .withToPort(TO_PORT)
	        .withUserIdGroupPairs(new UserIdGroupPair()
	            .withGroupName(sourceGroup.getGroupName())
	            .withUserId(sourceGroup.getOwnerId())));
	    ec2.authorizeSecurityGroupIngress(request);

	    // Revoke our permission to clean up
	    assertTrue(doesUserGroupPermissionExist(testGroup.getGroupId(), sourceGroup.getGroupId(), sourceGroup.getOwnerId()));
	
	    RevokeSecurityGroupIngressRequest revokeRequest = new RevokeSecurityGroupIngressRequest();
	    revokeRequest.setGroupName(testGroup.getGroupName());
	    revokeRequest.withIpPermissions(new IpPermission()
	        .withUserIdGroupPairs(new UserIdGroupPair()
	            .withGroupName(sourceGroup.getGroupName())
	            .withUserId(sourceGroup.getOwnerId()))
	        .withIpProtocol(TCP_PROTOCOL)
	        .withFromPort(FROM_PORT)
	        .withToPort(TO_PORT));
	    ec2.revokeSecurityGroupIngress(revokeRequest);
	
	    assertFalse(doesUserGroupPermissionExist(testGroup.getGroupId(), sourceGroup.getGroupId(), sourceGroup.getOwnerId()));
	}

	/**
	 * Tests that we can authorize a user group permission for the security
	 * group we previously created, using the legacy, deprecated query parameters.
	 */
	@Test
	public void testLegacyAuthorizeUserGroupSecurityGroupIngress() {
	    sourceGroup = createGroup(createUniqueGroupName(), GROUP_DESCRIPTION);
	    assertFalse(doesUserGroupPermissionExist(testGroup.getGroupId(), sourceGroup.getGroupId(), sourceGroup.getOwnerId()));
	
	    AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
	    request.setGroupName(testGroup.getGroupName());
	    request.setSourceSecurityGroupName(sourceGroup.getGroupName());
	    request.setSourceSecurityGroupOwnerId(sourceGroup.getOwnerId());
	
	    ec2.authorizeSecurityGroupIngress(request);
	
	    assertTrue(doesUserGroupPermissionExist(testGroup.getGroupId(), sourceGroup.getGroupId(), sourceGroup.getOwnerId()));
	}
	
	/**
	 * Tests authorizing and revoking vpc security group egress
	 */
	@Test
	public void testAuthorizeSecurityGroupEgress() {
	    assertFalse(doesIpEgressPermissionExist(vpcGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
		
		AuthorizeSecurityGroupEgressRequest request = new AuthorizeSecurityGroupEgressRequest();
		request.setGroupId(vpcGroup.getGroupId());
		request.withIpPermissions(new IpPermission()
	        .withIpProtocol(TCP_PROTOCOL)
	        .withIpRanges(CIDR_IP_RANGE)
	        .withFromPort(FROM_PORT)
	        .withToPort(TO_PORT));
    	ec2.authorizeSecurityGroupEgress(request);	    
	
	    assertTrue(doesIpEgressPermissionExist(vpcGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));
	    
	    // Now revoke the permission
	    RevokeSecurityGroupEgressRequest revoke = new RevokeSecurityGroupEgressRequest();
	    revoke.setGroupId(vpcGroup.getGroupId());
	    revoke.withIpPermissions(new IpPermission()
	        .withIpProtocol(TCP_PROTOCOL)
	        .withIpRanges(CIDR_IP_RANGE)
	        .withFromPort(FROM_PORT)
	        .withToPort(TO_PORT));
    	ec2.revokeSecurityGroupEgress(revoke);
    	
	    assertFalse(doesIpEgressPermissionExist(vpcGroup.getGroupId(), TCP_PROTOCOL, CIDR_IP_RANGE, FROM_PORT, TO_PORT));    	
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
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        try {
            DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request.withGroupNames(groupName));
            return result.getSecurityGroups().size() > 0;
        } catch (AmazonServiceException ase) {
            if (ase.getErrorCode().equals("InvalidGroup.NotFound")) return false;
            throw ase;
        }
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
            map.put(group.getGroupName(), group);
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
        if (group.getVpcId() != null && "tcp".equals(protocol))
        	acceptableProtocols.add("6");
        
        for (IpPermission permission : group.getIpPermissions()) {
            if (permission.getFromPort().equals(fromPort)
                && permission.getToPort().equals(toPort)
                && acceptableProtocols.contains(permission.getIpProtocol())) {

                for (String range : permission.getIpRanges()) {
                    if (range.equals(cidrIp)) return true;
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
        if (group.getVpcId() != null && "tcp".equals(protocol))
        	acceptableProtocols.add("6");
        
        for (IpPermission permission : group.getIpPermissionsEgress()) {
            if (permission.getFromPort() != null && permission.getFromPort().equals(fromPort)
                && permission.getToPort() != null && permission.getToPort().equals(toPort)
                && acceptableProtocols.contains(permission.getIpProtocol())) {

                for (String range : permission.getIpRanges()) {
                    if (range.equals(cidrIpRange)) return true;
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
        try {Thread.sleep(1000);} catch (Exception e) {}
        SecurityGroup group = getSecurityGroup(groupId);
        for (IpPermission permission : group.getIpPermissions()) {
            for (UserIdGroupPair pair : permission.getUserIdGroupPairs()) {
                if (pair.getGroupId().equals(sourceGroupId)
                    && pair.getUserId().equals(sourceGroupOwnerId)) {
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
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request.withGroupIds(id));
        ResponseMetadata responseMetadata = ec2.getCachedResponseMetadata(request);
		Assert.assertThat(responseMetadata.getRequestId(), Matchers.not
				(Matchers.isEmptyOrNullString()));

        List<SecurityGroup> groups = result.getSecurityGroups();
        assertEquals(1, groups.size());

        return groups.get(0);
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
        CreateSecurityGroupRequest createGroupRequest = new CreateSecurityGroupRequest();
        createGroupRequest.setGroupName(name);
        createGroupRequest.setDescription(description);
        ec2.createSecurityGroup(createGroupRequest);
        ResponseMetadata responseMetadata = ec2.getCachedResponseMetadata(createGroupRequest);
        assertStringNotEmpty(responseMetadata.getRequestId());

        DescribeSecurityGroupsRequest describeGroupRequest = new DescribeSecurityGroupsRequest();
        describeGroupRequest.withGroupNames(name);
        return ec2.describeSecurityGroups(describeGroupRequest).getSecurityGroups().get(0);
    }

	/**
     * Creates a unique security group name, using a timestamp suffix.
     *
     * @return A unique security group name.
     */
    private static String createUniqueGroupName() {
        return GROUP_NAME_PREFIX + "-" + System.currentTimeMillis();
    }

}
