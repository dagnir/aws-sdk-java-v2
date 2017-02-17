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

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.elasticache.model.CacheNodeTypeSpecificParameter;
import software.amazon.awssdk.services.elasticache.model.CacheParameterGroup;
import software.amazon.awssdk.services.elasticache.model.CreateCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParameterGroupsRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParameterGroupsResult;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParametersRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParametersResult;
import software.amazon.awssdk.services.elasticache.model.DescribeEngineDefaultParametersRequest;
import software.amazon.awssdk.services.elasticache.model.EngineDefaults;
import software.amazon.awssdk.services.elasticache.model.ModifyCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.ModifyCacheParameterGroupResult;
import software.amazon.awssdk.services.elasticache.model.Parameter;
import software.amazon.awssdk.services.elasticache.model.ParameterNameValue;
import software.amazon.awssdk.services.elasticache.model.ResetCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.ResetCacheParameterGroupResult;

public class ParameterGroupsIntegrationTest extends ElastiCacheIntegrationTestBase {

    private static final String DESCRIPTION = "Java SDK integ test param group";
    private static final String CACHE_PARAMETER_GROUP_FAMILY = "memcached1.4";

    private String cacheParameterGroupName;

    /** Releases all resources created by tests. */
    @After
    public void tearDown() throws Exception {
        if (cacheParameterGroupName != null) {
            try {
                elasticache.deleteCacheParameterGroup(new DeleteCacheParameterGroupRequest(cacheParameterGroupName));
            } catch (Exception e) {
                // Ignored or expected.
            }
        }
    }

    /** Tests that we can call the parameter group operations in the ElastiCache API. */
    @Test
    public void testParameterGroupOperations() throws Exception {

        // Describe Engine Default Parameters
        EngineDefaults engineDefaults = elasticache.describeEngineDefaultParameters(new DescribeEngineDefaultParametersRequest(CACHE_PARAMETER_GROUP_FAMILY));
        assertTrue(engineDefaults.getCacheNodeTypeSpecificParameters().size() > 0);
        CacheNodeTypeSpecificParameter cacheNodeParameter = engineDefaults.getCacheNodeTypeSpecificParameters().get(0);
        assertNotEmpty(cacheNodeParameter.getParameterName());
        assertTrue(cacheNodeParameter.getCacheNodeTypeSpecificValues().size() > 0);
        assertEquals(CACHE_PARAMETER_GROUP_FAMILY, engineDefaults.getCacheParameterGroupFamily());
        assertTrue(engineDefaults.getParameters().size() > 0);
        Parameter parameter = engineDefaults.getParameters().get(0);
        assertNotEmpty(parameter.getParameterName());
        assertNotEmpty(parameter.getParameterValue());


        // Create Cache Parameter Group
        cacheParameterGroupName = "java-sdk-integ-test-" + System.currentTimeMillis();
        CacheParameterGroup cacheParameterGroup = elasticache.createCacheParameterGroup(new CreateCacheParameterGroupRequest(cacheParameterGroupName, CACHE_PARAMETER_GROUP_FAMILY, DESCRIPTION));
        assertEquals(CACHE_PARAMETER_GROUP_FAMILY, cacheParameterGroup.getCacheParameterGroupFamily());
        assertEquals(cacheParameterGroupName, cacheParameterGroup.getCacheParameterGroupName());
        assertEquals(DESCRIPTION, cacheParameterGroup.getDescription());


        // Describe Cache Parameters
        DescribeCacheParametersResult describeCacheParameters = elasticache.describeCacheParameters(new DescribeCacheParametersRequest(cacheParameterGroupName));
        assertTrue(describeCacheParameters.getCacheNodeTypeSpecificParameters().size() > 0);
        cacheNodeParameter = describeCacheParameters.getCacheNodeTypeSpecificParameters().get(0);
        assertNotEmpty(cacheNodeParameter.getParameterName());
        assertTrue(cacheNodeParameter.getCacheNodeTypeSpecificValues().size() > 0);
        assertTrue(describeCacheParameters.getParameters().size() > 0);
        parameter = describeCacheParameters.getParameters().get(0);
        assertNotEmpty(parameter.getParameterName());
        assertNotEmpty(parameter.getParameterValue());


        // Modify Cache Parameter Group
        List<ParameterNameValue> paramsToModify = new ArrayList<ParameterNameValue>();
        paramsToModify.add(new ParameterNameValue("max_item_size", "100000"));
        ModifyCacheParameterGroupResult modifyCacheParameterGroup = elasticache.modifyCacheParameterGroup(new ModifyCacheParameterGroupRequest(cacheParameterGroupName, paramsToModify));
        assertEquals(cacheParameterGroupName, modifyCacheParameterGroup.getCacheParameterGroupName());


        // Reset Cache Parameter Group
        List<ParameterNameValue> paramsToReset = new ArrayList<ParameterNameValue>();
        paramsToReset.add(new ParameterNameValue().withParameterName("binding_protocol"));
        ResetCacheParameterGroupResult resetCacheParameterGroup = elasticache.resetCacheParameterGroup(new ResetCacheParameterGroupRequest(cacheParameterGroupName, paramsToReset));
        assertEquals(cacheParameterGroupName, resetCacheParameterGroup.getCacheParameterGroupName());


        // Describe Cache Parameter Groups
        DescribeCacheParameterGroupsResult describeCacheParameterGroups = elasticache.describeCacheParameterGroups(new DescribeCacheParameterGroupsRequest(cacheParameterGroupName));
        assertEquals(1, describeCacheParameterGroups.getCacheParameterGroups().size());
        CacheParameterGroup parameterGroup = describeCacheParameterGroups.getCacheParameterGroups().get(0);
        assertEquals(CACHE_PARAMETER_GROUP_FAMILY, parameterGroup.getCacheParameterGroupFamily());
        assertEquals(cacheParameterGroupName, parameterGroup.getCacheParameterGroupName());
        assertEquals(DESCRIPTION, parameterGroup.getDescription());


        // Delete Cache Parameter Group
        elasticache.deleteCacheParameterGroup(new DeleteCacheParameterGroupRequest(cacheParameterGroupName));
    }

}