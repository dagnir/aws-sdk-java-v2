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
                elasticache.deleteCacheSecurityGroup(new DeleteCacheSecurityGroupRequest(cacheSecurityGroupName));
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
                new CreateCacheSecurityGroupRequest(cacheSecurityGroupName, DESCRIPTION));
        assertEquals(cacheSecurityGroupName, createdCacheSecurityGroup.getCacheSecurityGroupName());
        assertEquals(DESCRIPTION, createdCacheSecurityGroup.getDescription());
        assertTrue(createdCacheSecurityGroup.getEC2SecurityGroups().isEmpty());
        assertNotEmpty(createdCacheSecurityGroup.getOwnerId());


        // Describe Cache Security Groups
        DescribeCacheSecurityGroupsResult describeCacheSecurityGroups = elasticache.describeCacheSecurityGroups(
                new DescribeCacheSecurityGroupsRequest().withCacheSecurityGroupName(cacheSecurityGroupName));
        assertEquals(1, describeCacheSecurityGroups.getCacheSecurityGroups().size());
        CacheSecurityGroup cacheSecurityGroup = describeCacheSecurityGroups.getCacheSecurityGroups().get(0);
        assertEquals(cacheSecurityGroupName, cacheSecurityGroup.getCacheSecurityGroupName());
        assertEquals(DESCRIPTION, cacheSecurityGroup.getDescription());
        assertTrue(cacheSecurityGroup.getEC2SecurityGroups().isEmpty());
        assertNotEmpty(cacheSecurityGroup.getOwnerId());

        // Delete Cache Security Group
        elasticache.deleteCacheSecurityGroup(new DeleteCacheSecurityGroupRequest(cacheSecurityGroupName));
    }
}