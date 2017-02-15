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

package software.amazon.awssdk.services.dynamodbv2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.dynamodbv2.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ListTablesResult;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class BaseResultIntegrationTest extends AWSIntegrationTestBase {

    private AmazonDynamoDB dynamoDB;

    @Before
    public void setup() {
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
                                              .withCredentials(new AWSStaticCredentialsProvider(getCredentials()))
                                              .withRegion(Regions.US_WEST_2)
                                              .build();
    }

    @Test
    public void responseMetadataInBaseResultIsSameAsMetadataCache() {
        ListTablesRequest request = new ListTablesRequest();
        ListTablesResult result = dynamoDB.listTables(request);
        assertNotNull(result.getSdkResponseMetadata());
        assertEquals(dynamoDB.getCachedResponseMetadata(request), result.getSdkResponseMetadata());
    }

    @Test
    public void httpMetadataInBaseResultIsValid() {
        ListTablesResult result = dynamoDB.listTables(new ListTablesRequest());
        assertEquals(200, result.getSdkHttpMetadata().getHttpStatusCode());
        assertThat(result.getSdkHttpMetadata().getHttpHeaders(), hasKey("x-amz-crc32"));
    }
}
