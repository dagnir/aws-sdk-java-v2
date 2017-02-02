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
import software.amazon.awssdk.services.elasticache.model.CacheClusterNotFoundException;
import software.amazon.awssdk.services.elasticache.model.CacheEngineVersion;
import software.amazon.awssdk.services.elasticache.model.CacheNode;
import software.amazon.awssdk.services.elasticache.model.CreateCacheClusterRequest;
import software.amazon.awssdk.services.elasticache.model.CreateReplicationGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteCacheClusterRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteReplicationGroupRequest;
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
			elasticache.deleteCacheCluster(new DeleteCacheClusterRequest(memcachedCacheClusterId));
		} catch (Exception e) {}
		try {
			elasticache.deleteCacheCluster(new DeleteCacheClusterRequest(redisCacheClusterId));
		} catch (Exception e) {}
	}

	/** Tests the cache cluster operations on different cache engines. **/
	@Test
	public void testCacheClusterOperations() throws Exception {
		testCacheClusterOperations(MEMCACHED_ENGINE, memcachedCacheClusterId);
		testCacheClusterOperations(REDIS_ENGINE, redisCacheClusterId);
	}
	
	/** Tests the operations on replication group. **/
//	@Test
	public void testReplicationGroupOperations() {
		// Create the primary cache cluster
		List<String> cacheSecurityGroupNames = new ArrayList<String>();
		CacheCluster createCacheCluster = elasticache.createCacheCluster(new CreateCacheClusterRequest(replicationGroupPrimaryCacheClusterId, 1, CACHE_NODE_TYPE, REDIS_ENGINE, cacheSecurityGroupNames));
		assertValidCacheCluster(createCacheCluster, REDIS_ENGINE, replicationGroupPrimaryCacheClusterId);
		waitForClusterToTransitionToState("available", replicationGroupPrimaryCacheClusterId);
		
		// Create the replication group
		elasticache.createReplicationGroup(new CreateReplicationGroupRequest()
				.withPrimaryClusterId(replicationGroupPrimaryCacheClusterId)
				.withReplicationGroupDescription(replicationGroupDescription)
				.withReplicationGroupId(replicationGroupId));
		
		// Wait till the replication group is available, and check the group parameters
		ReplicationGroup createdReplicationGroup = waitForReplicationGroupToTransitionToState("available", replicationGroupId);
		assertEquals(replicationGroupId, createdReplicationGroup.getReplicationGroupId());
		assertEquals(replicationGroupDescription, createdReplicationGroup.getDescription());
		assertNotNull(createdReplicationGroup.getMemberClusters());
		assertEquals(1, createdReplicationGroup.getMemberClusters().size());
		assertEquals(replicationGroupPrimaryCacheClusterId, createdReplicationGroup.getMemberClusters().get(0));
		
		// Delete the replication group (all the included clusters would also be deleted)
		elasticache.deleteReplicationGroup(new DeleteReplicationGroupRequest().withReplicationGroupId(replicationGroupId));
		waitForReplicationGroupToBeDeleted(replicationGroupId);
		
		// Checks that the primary cache cluster has been deleted
		try {
			List<CacheCluster> deletedCacheClusters = elasticache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId(replicationGroupPrimaryCacheClusterId)).getCacheClusters();
			fail(replicationGroupPrimaryCacheClusterId + " should have alredy been deleted.");
		} catch (CacheClusterNotFoundException e) {
			assertTrue(e.getMessage().contains(replicationGroupPrimaryCacheClusterId));
		}
	}
	
	private void testCacheClusterOperations(String engine, String cacheClusterId) throws Exception {
		// Create Cache Cluster
		List<String> cacheSecurityGroupNames = new ArrayList<String>();
		CacheCluster createCacheCluster = elasticache.createCacheCluster(new CreateCacheClusterRequest(cacheClusterId, 1, CACHE_NODE_TYPE, engine, cacheSecurityGroupNames));
		assertValidCacheCluster(createCacheCluster, engine, cacheClusterId);
		
		//Wait for our cluster to start
		waitForClusterToTransitionToState("available", cacheClusterId);

		// Describe Engine Versions
		DescribeCacheEngineVersionsResult result = elasticache.describeCacheEngineVersions();
		assertTrue(result.getCacheEngineVersions().size() > 0);
		for (CacheEngineVersion version : result.getCacheEngineVersions()) {
			assertNotNull(version.getCacheEngineDescription());
			assertNotNull(version.getEngineVersion());
			assertNotNull(version.getEngine());
			assertNotNull(version.getCacheParameterGroupFamily());
		}

		result = elasticache.describeCacheEngineVersions(new DescribeCacheEngineVersionsRequest().withEngine(engine));
		assertTrue(result.getCacheEngineVersions().size() > 0);
		for (CacheEngineVersion version : result.getCacheEngineVersions()) {
			assertNotNull(version.getCacheEngineDescription());
			assertNotNull(version.getEngineVersion());
			assertNotNull(version.getEngine());
			assertNotNull(version.getCacheParameterGroupFamily());
		}


		// Describe Events
		DescribeEventsResult describeEvents = elasticache.describeEvents(new DescribeEventsRequest().withSourceIdentifier(cacheClusterId).withSourceType(SourceType.CacheCluster.toString()));
		assertTrue(describeEvents.getEvents().size() > 0);
		Event event = describeEvents.getEvents().get(0);
		assertNotNull(event.getDate());
		assertNotEmpty(event.getMessage());
		assertNotEmpty(event.getSourceIdentifier());
		assertNotEmpty(event.getSourceType());


		// Describe Cache Clusters
		DescribeCacheClustersResult describeCacheClusters = elasticache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId(cacheClusterId));
		assertEquals(1, describeCacheClusters.getCacheClusters().size());
		assertValidCacheCluster(describeCacheClusters.getCacheClusters().get(0), engine, cacheClusterId);


		// Modify Cache Cluster
		CacheCluster modifyCacheCluster = elasticache.modifyCacheCluster(new ModifyCacheClusterRequest(cacheClusterId).withApplyImmediately(true).withAutoMinorVersionUpgrade(true));
		assertValidCacheCluster(modifyCacheCluster, engine, cacheClusterId);

		// Commented out reboot operation, since reboot shortly after creating the cluster would result in 400 error - "Cluster cannot currently reboot due to an in-progress management operation.".
//		// Reboot Cache Cluster. 
//		CacheCluster rebootCacheCluster = elasticache.rebootCacheCluster(new RebootCacheClusterRequest(cacheClusterId, getAllCacheNodeIds(cacheClusterId)));
//		assertValidCacheCluster(rebootCacheCluster, engine, cacheClusterId);


		// Wait for our cluster to finish rebooting
		waitForClusterToTransitionToState("available", cacheClusterId);


		// Delete Cache Cluster
		CacheCluster deleteCacheCluster = elasticache.deleteCacheCluster(new DeleteCacheClusterRequest(cacheClusterId));
		assertValidCacheCluster(deleteCacheCluster, engine, cacheClusterId);
	}
	
	private List<String> getAllCacheNodeIds(String cacheClusterId) {
		DescribeCacheClustersResult describeCacheClusters = elasticache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId(cacheClusterId).withShowCacheNodeInfo(true));
		if (describeCacheClusters.getCacheClusters().size() != 1) fail("Couldn't find expected Cache Cluster");

		CacheCluster cacheCluster = describeCacheClusters.getCacheClusters().get(0);
		List<String> cacheNodeIds = new ArrayList<String>();
		for (CacheNode node : cacheCluster.getCacheNodes()) {
			cacheNodeIds.add(node.getCacheNodeId());
		}
		return cacheNodeIds;
	}

	private void waitForClusterToTransitionToState(String state, String cacheClusterId) {
		long startTime = System.currentTimeMillis();
		long endTime   = startTime + (1000 * 60 * 30);

		while (startTime < endTime) {
			List<CacheCluster> cacheClusters = elasticache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId(cacheClusterId)).getCacheClusters();
			if (cacheClusters.size() != 1) fail("Can't find expected cache cluster");
			CacheCluster cacheCluster = cacheClusters.get(0);
			 System.out.println(cacheCluster.getCacheClusterStatus());
			if (cacheCluster.getCacheClusterStatus().equalsIgnoreCase(state)) return;

			try {Thread.sleep(1000 * 10);} catch (Exception e) {}
		}

		fail("Cache cluster never transitioned to state '" + state + "'");
	}
	
	private ReplicationGroup waitForReplicationGroupToTransitionToState(String state, String replicationGroupId) {
		long startTime = System.currentTimeMillis();
		long endTime   = startTime + (1000 * 60 * 30);

		while (startTime < endTime) {
			List<ReplicationGroup> groups = elasticache.describeReplicationGroups(new DescribeReplicationGroupsRequest().withReplicationGroupId(replicationGroupId)).getReplicationGroups();
			if (groups.size() != 1) fail("Can't find expected replication group");
			ReplicationGroup replicationGroup = groups.get(0);
			System.out.println(replicationGroup.getStatus());
			if (replicationGroup.getStatus().equalsIgnoreCase(state)) {
				return replicationGroup;
			}

			try {Thread.sleep(1000 * 10);} catch (Exception e) {}
		}

		fail("Replication group never transitioned to state '" + state + "'");
		return null;
	}
	
	private void waitForReplicationGroupToBeDeleted(String replicationGroupId) {
		long startTime = System.currentTimeMillis();
		long endTime   = startTime + (1000 * 60 * 30);

		while (startTime < endTime) {
			try {
				List<ReplicationGroup> groups = elasticache.describeReplicationGroups(new DescribeReplicationGroupsRequest().withReplicationGroupId(replicationGroupId)).getReplicationGroups();
				System.out.println(groups.get(0).getStatus());
			} catch (ReplicationGroupNotFoundException e) {
				return;
			}

			try {Thread.sleep(1000 * 10);} catch (Exception e) {}
		}

		fail("Replication group is never deleted.");
	}

	private void assertValidCacheCluster(CacheCluster cacheCluster, String expectedEngine, String expectedCacheClusterId) {
		if (cacheCluster.getCacheClusterStatus().equalsIgnoreCase("creating") == false) {
			assertNotNull(cacheCluster.getCacheClusterCreateTime());
		}

		assertEquals(expectedCacheClusterId, cacheCluster.getCacheClusterId());
		assertNotEmpty(cacheCluster.getCacheClusterStatus());
		assertEquals(CACHE_NODE_TYPE, cacheCluster.getCacheNodeType());
		assertEquals(expectedEngine, cacheCluster.getEngine());
		assertNotEmpty(cacheCluster.getEngineVersion());
		assertEquals(1, cacheCluster.getNumCacheNodes().intValue());
	}
}