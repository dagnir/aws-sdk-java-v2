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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;
import software.amazon.awssdk.services.elasticache.model.CacheEngineVersion;
import software.amazon.awssdk.services.elasticache.model.CacheNode;
import software.amazon.awssdk.services.elasticache.model.CreateCacheClusterRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteCacheClusterRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersResult;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheEngineVersionsRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheEngineVersionsResult;
import software.amazon.awssdk.services.elasticache.model.DescribeEventsRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeEventsResult;
import software.amazon.awssdk.services.elasticache.model.DescribeReplicationGroupsRequest;
import software.amazon.awssdk.services.elasticache.model.Event;
import software.amazon.awssdk.services.elasticache.model.ModifyCacheClusterRequest;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroup;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroupNotFoundException;
import software.amazon.awssdk.services.elasticache.model.SourceType;

/**
 * Integration tests for the Amazon ElastiCache operations that require a Cache Cluster.
 */
public class CacheClusterIntegrationTest extends ElastiCacheIntegrationTestBase {

    private static final String CACHE_NODE_TYPE = "cache.m1.large";

    private String timestamp = "" + System.currentTimeMillis();

    private String memcachedCacheClusterId = "java-m-" + timestamp;
    private String redisCacheClusterId = "java-r-" + timestamp;

    private String replicationGroupId = "java-g-" + timestamp;
    private String replicationGroupDescription = "Replication group test.";
    private String replicationGroupPrimaryCacheClusterId = "java-p-" + timestamp;


    /** Releases all resources created by tests. */
    @After
    public void tearDown() throws Exception {
        try {
            elasticache.deleteCacheCluster(DeleteCacheClusterRequest.builder().cacheClusterId(memcachedCacheClusterId).build());
        } catch (Exception e) {
            // Ignore.
        }
        try {
            elasticache.deleteCacheCluster(DeleteCacheClusterRequest.builder().cacheClusterId(redisCacheClusterId).build());
        } catch (Exception e) {
            // Ignore.
        }
    }

    /** Tests the cache cluster operations on different cache engines. **/
    @Test
    public void testCacheClusterOperations() throws Exception {
        testCacheClusterOperations(MEMCACHED_ENGINE, memcachedCacheClusterId);
        testCacheClusterOperations(REDIS_ENGINE, redisCacheClusterId);
    }

    private void testCacheClusterOperations(String engine, String cacheClusterId) throws Exception {
        // Create Cache Cluster
        List<String> cacheSecurityGroupNames = new ArrayList<String>();
        CreateCacheClusterRequest request =
                CreateCacheClusterRequest.builder().cacheClusterId(cacheClusterId).numCacheNodes(1).cacheNodeType(CACHE_NODE_TYPE)
                                         .engine(engine).cacheSecurityGroupNames(cacheSecurityGroupNames).build();
        CacheCluster createCacheCluster = elasticache.createCacheCluster(request);
        assertValidCacheCluster(createCacheCluster, engine, cacheClusterId);

        //Wait for our cluster to start
        waitForClusterToTransitionToState("available", cacheClusterId);

        // Describe Engine Versions
        DescribeCacheEngineVersionsResult result =
                elasticache.describeCacheEngineVersions(DescribeCacheEngineVersionsRequest.builder().build());
        assertTrue(result.cacheEngineVersions().size() > 0);
        for (CacheEngineVersion version : result.cacheEngineVersions()) {
            assertNotNull(version.cacheEngineDescription());
            assertNotNull(version.engineVersion());
            assertNotNull(version.engine());
            assertNotNull(version.cacheParameterGroupFamily());
        }

        result = elasticache.describeCacheEngineVersions(DescribeCacheEngineVersionsRequest.builder().engine(engine).build());
        assertTrue(result.cacheEngineVersions().size() > 0);
        for (CacheEngineVersion version : result.cacheEngineVersions()) {
            assertNotNull(version.cacheEngineDescription());
            assertNotNull(version.engineVersion());
            assertNotNull(version.engine());
            assertNotNull(version.cacheParameterGroupFamily());
        }


        // Describe Events
        DescribeEventsResult describeEvents = elasticache.describeEvents(
                DescribeEventsRequest.builder().sourceIdentifier(cacheClusterId)
                                     .sourceType(SourceType.CacheCluster.toString()).build());
        assertTrue(describeEvents.events().size() > 0);
        Event event = describeEvents.events().get(0);
        assertNotNull(event.date());
        assertNotEmpty(event.message());
        assertNotEmpty(event.sourceIdentifier());
        assertNotEmpty(event.sourceType());


        // Describe Cache Clusters
        DescribeCacheClustersResult describeCacheClusters = elasticache.describeCacheClusters(
                DescribeCacheClustersRequest.builder().cacheClusterId(cacheClusterId).build());
        assertEquals(1, describeCacheClusters.cacheClusters().size());
        assertValidCacheCluster(describeCacheClusters.cacheClusters().get(0), engine, cacheClusterId);


        // Modify Cache Cluster
        CacheCluster modifyCacheCluster = elasticache.modifyCacheCluster(
                ModifyCacheClusterRequest.builder().cacheClusterId(cacheClusterId).applyImmediately(true)
                                         .autoMinorVersionUpgrade(true).build());
        assertValidCacheCluster(modifyCacheCluster, engine, cacheClusterId);


        // Wait for our cluster to finish rebooting
        waitForClusterToTransitionToState("available", cacheClusterId);


        // Delete Cache Cluster
        CacheCluster deleteCacheCluster =
                elasticache.deleteCacheCluster(DeleteCacheClusterRequest.builder().cacheClusterId(cacheClusterId).build());
        assertValidCacheCluster(deleteCacheCluster, engine, cacheClusterId);
    }

    private List<String> getAllCacheNodeIds(String cacheClusterId) {
        DescribeCacheClustersRequest req =
                DescribeCacheClustersRequest.builder().cacheClusterId(cacheClusterId).showCacheNodeInfo(true).build();
        DescribeCacheClustersResult describeCacheClusters = elasticache.describeCacheClusters(req);
        if (describeCacheClusters.cacheClusters().size() != 1) {
            fail("Couldn't find expected Cache Cluster");
        }

        CacheCluster cacheCluster = describeCacheClusters.cacheClusters().get(0);
        List<String> cacheNodeIds = new ArrayList<String>();
        for (CacheNode node : cacheCluster.cacheNodes()) {
            cacheNodeIds.add(node.cacheNodeId());
        }
        return cacheNodeIds;
    }

    private void waitForClusterToTransitionToState(String state, String cacheClusterId) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (1000 * 60 * 30);

        while (startTime < endTime) {
            DescribeCacheClustersRequest request = DescribeCacheClustersRequest.builder().cacheClusterId(cacheClusterId).build();
            List<CacheCluster> cacheClusters = elasticache.describeCacheClusters(request).cacheClusters();
            if (cacheClusters.size() != 1) {
                fail("Can't find expected cache cluster");
            }
            CacheCluster cacheCluster = cacheClusters.get(0);
            System.out.println(cacheCluster.cacheClusterStatus());
            if (cacheCluster.cacheClusterStatus().equalsIgnoreCase(state)) {
                return;
            }

            try {
                Thread.sleep(1000 * 10);
            } catch (Exception e) {
                // Expected.
            }
        }

        fail("Cache cluster never transitioned to state '" + state + "'");
    }

    private ReplicationGroup waitForReplicationGroupToTransitionToState(String state, String replicationGroupId) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (1000 * 60 * 30);

        while (startTime < endTime) {
            DescribeReplicationGroupsRequest request =
                    DescribeReplicationGroupsRequest.builder().replicationGroupId(replicationGroupId).build();
            List<ReplicationGroup> groups = elasticache.describeReplicationGroups(request).replicationGroups();
            if (groups.size() != 1) {
                fail("Can't find expected replication group");
            }
            ReplicationGroup replicationGroup = groups.get(0);
            System.out.println(replicationGroup.status());
            if (replicationGroup.status().equalsIgnoreCase(state)) {
                return replicationGroup;
            }

            try {
                Thread.sleep(1000 * 10);
            } catch (Exception e) {
                // Ignored.
            }
        }

        fail("Replication group never transitioned to state '" + state + "'");
        return null;
    }

    private void waitForReplicationGroupToBeDeleted(String replicationGroupId) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (1000 * 60 * 30);

        while (startTime < endTime) {
            try {
                List<ReplicationGroup> groups = elasticache.describeReplicationGroups(
                        DescribeReplicationGroupsRequest.builder().replicationGroupId(replicationGroupId).build())
                                                           .replicationGroups();
                System.out.println(groups.get(0).status());
            } catch (ReplicationGroupNotFoundException e) {
                return;
            }

            try {
                Thread.sleep(1000 * 10);
            } catch (Exception e) {
                // Ignored.
            }
        }

        fail("Replication group is never deleted.");
    }

    private void assertValidCacheCluster(CacheCluster cacheCluster, String expectedEngine, String expectedCacheClusterId) {
        if (!cacheCluster.cacheClusterStatus().equalsIgnoreCase("creating")) {
            assertNotNull(cacheCluster.cacheClusterCreateTime());
        }

        assertEquals(expectedCacheClusterId, cacheCluster.cacheClusterId());
        assertNotEmpty(cacheCluster.cacheClusterStatus());
        assertEquals(CACHE_NODE_TYPE, cacheCluster.cacheNodeType());
        assertEquals(expectedEngine, cacheCluster.engine());
        assertNotEmpty(cacheCluster.engineVersion());
        assertEquals(1, cacheCluster.numCacheNodes().intValue());
    }
}
