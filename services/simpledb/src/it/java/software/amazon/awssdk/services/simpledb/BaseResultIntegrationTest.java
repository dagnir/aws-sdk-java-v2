/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.simpledb;

import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.simpledb.model.ListDomainsRequest;
import software.amazon.awssdk.services.simpledb.model.ListDomainsResult;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BaseResultIntegrationTest extends AWSIntegrationTestBase {

    private AmazonSimpleDBClient simpleDB;

    @Before
    public void setup() {
        simpleDB = new AmazonSimpleDBClient(getCredentials());
        simpleDB.configureRegion(Regions.US_WEST_2);
    }

    @Test
    public void responseMetadataInBaseResultIsSameAsCache() {
        ListDomainsRequest request = new ListDomainsRequest().withMaxNumberOfDomains(1);
        final ListDomainsResult result = simpleDB.listDomains(request);
        SimpleDBResponseMetadata cachedMetadata = simpleDB.getCachedResponseMetadata(request);
        SimpleDBResponseMetadata resultMetadata = result.getSdkResponseMetadata();
        assertNotNull(result.getSdkResponseMetadata());
        assertEquals(cachedMetadata.getRequestId(), resultMetadata.getRequestId());
        assertEquals(cachedMetadata.getBoxUsage(), resultMetadata.getBoxUsage(), 0.0001f);
    }

}
