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

package software.amazon.awssdk.services.rds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.rds.model.AuthorizeDBSecurityGroupIngressRequest;
import software.amazon.awssdk.services.rds.model.CreateDBSecurityGroupRequest;
import software.amazon.awssdk.services.rds.model.DBSecurityGroup;
import software.amazon.awssdk.services.rds.model.DeleteDBSecurityGroupRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBSecurityGroupsRequest;
import software.amazon.awssdk.services.rds.model.RevokeDBSecurityGroupIngressRequest;

/**
 * Integration tests for RDS security group operations.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class RdsSecurityGroupsIntegrationTest extends IntegrationTestBase {

    private static final String CIDR_IP_RANGE = "0.0.0.0/0";
    private static final String DESCRIPTION = "description";

    /**
     * Tests the RDS security group operations and verifies that we can call
     * them and correctly unmarshall the response.
     */
    @Test
    public void testSecurityGroupOperations() throws Exception {
        // Setting this field will cause the IntegrationTestBase class to automatically
        // clean up this security group after our test finishes running.
        securityGroupName = "java-integ-test-sec-group-" + new Date().getTime();

        // Create a DB security group
        DBSecurityGroup createdDBSecurityGroup = rds.createDBSecurityGroup(
                new CreateDBSecurityGroupRequest()
                        .withDBSecurityGroupName(securityGroupName)
                        .withDBSecurityGroupDescription(DESCRIPTION));
        assertEquals(DESCRIPTION, createdDBSecurityGroup.getDBSecurityGroupDescription());
        assertEquals(securityGroupName, createdDBSecurityGroup.getDBSecurityGroupName());
        assertNotEmpty(createdDBSecurityGroup.getOwnerId());
        assertTrue(createdDBSecurityGroup.getIPRanges().isEmpty());
        assertTrue(createdDBSecurityGroup.getEC2SecurityGroups().isEmpty());


        // Add some permissions
        DBSecurityGroup authorizedDBSecurityGroup = rds.authorizeDBSecurityGroupIngress(
                new AuthorizeDBSecurityGroupIngressRequest()
                        .withDBSecurityGroupName(securityGroupName)
                        .withCIDRIP(CIDR_IP_RANGE));
        assertEquals(DESCRIPTION, authorizedDBSecurityGroup.getDBSecurityGroupDescription());
        assertEquals(securityGroupName, authorizedDBSecurityGroup.getDBSecurityGroupName());
        assertNotEmpty(authorizedDBSecurityGroup.getOwnerId());
        assertEquals(1, authorizedDBSecurityGroup.getIPRanges().size());
        assertEquals(CIDR_IP_RANGE, authorizedDBSecurityGroup.getIPRanges().get(0).getCIDRIP());
        assertTrue(authorizedDBSecurityGroup.getEC2SecurityGroups().isEmpty());


        // Remove some permissions
        waitForSecurityGroupIPRangeToTransitionToState(securityGroupName, CIDR_IP_RANGE, "authorized");
        DBSecurityGroup revokedDBSecurityGroup = rds.revokeDBSecurityGroupIngress(
                new RevokeDBSecurityGroupIngressRequest()
                        .withDBSecurityGroupName(securityGroupName)
                        .withCIDRIP(CIDR_IP_RANGE));
        assertEquals(DESCRIPTION, revokedDBSecurityGroup.getDBSecurityGroupDescription());
        assertEquals(securityGroupName, revokedDBSecurityGroup.getDBSecurityGroupName());
        assertNotEmpty(revokedDBSecurityGroup.getOwnerId());
        assertTrue(revokedDBSecurityGroup.getEC2SecurityGroups().isEmpty());
        if (revokedDBSecurityGroup.getIPRanges().isEmpty() == false) {
            assertEquals(1, revokedDBSecurityGroup.getIPRanges().size());
            assertEquals(CIDR_IP_RANGE, revokedDBSecurityGroup.getIPRanges().get(0).getCIDRIP());
            assertNotEmpty(revokedDBSecurityGroup.getIPRanges().get(0).getStatus());
        }


        // Describe it
        List<DBSecurityGroup> dbSecurityGroups = rds.describeDBSecurityGroups(
                new DescribeDBSecurityGroupsRequest()
                        .withMaxRecords(10)
                        .withDBSecurityGroupName(securityGroupName)).getDBSecurityGroups();
        assertEquals(1, dbSecurityGroups.size());
        DBSecurityGroup securityGroup = dbSecurityGroups.get(0);
        assertEquals(DESCRIPTION, securityGroup.getDBSecurityGroupDescription());
        assertEquals(securityGroupName, securityGroup.getDBSecurityGroupName());
        assertNotEmpty(securityGroup.getOwnerId());
        assertTrue(securityGroup.getEC2SecurityGroups().isEmpty());


        // Delete it 
        rds.deleteDBSecurityGroup(new DeleteDBSecurityGroupRequest()
                                          .withDBSecurityGroupName(securityGroupName));
    }

}
