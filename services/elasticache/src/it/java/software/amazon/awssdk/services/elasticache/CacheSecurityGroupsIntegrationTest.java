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

package software.amazon.awssdk.services.elasticache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.elasticache.model.CacheSecurityGroup;
import software.amazon.awssdk.services.elasticache.model.CreateCacheSecurityGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteCacheSecurityGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheSecurityGroupsRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheSecurityGroupsResult;

public class CacheSecurityGroupsIntegrationTest extends ElastiCacheIntegrationTestBase {

    private static final String DESCRIPTION = "cache security group description";

    private String cacheSecurityGroupName;

    /** Releases all resources created by tests. */
    @After
    public void tearDown() throws Exception {
        if (cacheSecurityGroupName != null) {
            try {
                elasticache.deleteCacheSecurityGroup(
                        DeleteCacheSecurityGroupRequest.builder().cacheSecurityGroupName(cacheSecurityGroupName).build());
            } catch (Exception e) {
                // Ignore.
            }
        }
    }

    /** Tests that we can successfully call the Cache Security Group operations. */
    @Test
    public void testCacheSecurityGroupOperations() throws Exception {
        // Create Cache Security Group
        cacheSecurityGroupName = "java-sdk-integ-group-" + System.currentTimeMillis();
        CacheSecurityGroup createdCacheSecurityGroup = elasticache.createCacheSecurityGroup(
                CreateCacheSecurityGroupRequest.builder().cacheSecurityGroupName(cacheSecurityGroupName).description(DESCRIPTION)
                                               .build());
        assertEquals(cacheSecurityGroupName, createdCacheSecurityGroup.cacheSecurityGroupName());
        assertEquals(DESCRIPTION, createdCacheSecurityGroup.description());
        assertTrue(createdCacheSecurityGroup.ec2SecurityGroups().isEmpty());
        assertNotEmpty(createdCacheSecurityGroup.ownerId());


        // Describe Cache Security Groups
        DescribeCacheSecurityGroupsResult describeCacheSecurityGroups = elasticache.describeCacheSecurityGroups(
                DescribeCacheSecurityGroupsRequest.builder().cacheSecurityGroupName(cacheSecurityGroupName).build());
        assertEquals(1, describeCacheSecurityGroups.cacheSecurityGroups().size());
        CacheSecurityGroup cacheSecurityGroup = describeCacheSecurityGroups.cacheSecurityGroups().get(0);
        assertEquals(cacheSecurityGroupName, cacheSecurityGroup.cacheSecurityGroupName());
        assertEquals(DESCRIPTION, cacheSecurityGroup.description());
        assertTrue(cacheSecurityGroup.ec2SecurityGroups().isEmpty());
        assertNotEmpty(cacheSecurityGroup.ownerId());

        // Delete Cache Security Group
        elasticache.deleteCacheSecurityGroup(
                DeleteCacheSecurityGroupRequest.builder().cacheSecurityGroupName(cacheSecurityGroupName).build());
    }
}
