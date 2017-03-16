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

package software.amazon.awssdk.services.dynamodb;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResult;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.TestTableForParallelScan;
import utils.test.util.DynamoDBTestBase;

/**
 * DynamoDB integration tests on the low-level parallel scan operation.
 */
@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = TestTableForParallelScan.class,
                                              creationPolicy = ResourceCreationPolicy.REUSE_EXISTING,
                                              retentionPolicy = ResourceRetentionPolicy.KEEP)
                    })
public class ParallelScanIntegrationTest extends DynamoDBTestBase {

    private static final String tableName = TestTableForParallelScan.TABLE_NAME;
    private static final String HASH_KEY_NAME = TestTableForParallelScan.HASH_KEY_NAME;
    private static final String ATTRIBUTE_FOO = "attribute_foo";
    private static final String ATTRIBUTE_BAR = "attribute_bar";
    private static final String ATTRIBUTE_RANDOM = "attribute_random";
    private static final int itemNumber = 200;

    /**
     * Creates a test table with a local secondary index
     */
    @BeforeClass
    public static void setUp() {
        DynamoDBTestBase.setUpTestBase();
    }

    private static void putTestData() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        Random random = new Random();
        for (int hashKeyValue = 0; hashKeyValue < itemNumber; hashKeyValue++) {
            item.put(HASH_KEY_NAME, new AttributeValue().withN(Integer.toString(hashKeyValue)));
            item.put(ATTRIBUTE_RANDOM, new AttributeValue().withN(Integer.toString(random.nextInt(itemNumber))));
            if (hashKeyValue < itemNumber / 2) {
                item.put(ATTRIBUTE_FOO, new AttributeValue().withN(Integer.toString(hashKeyValue)));
            } else {
                item.put(ATTRIBUTE_BAR, new AttributeValue().withN(Integer.toString(hashKeyValue)));
            }

            dynamo.putItem(new PutItemRequest(tableName, item));
            item.clear();
        }
    }

    /**
     * Tests making parallel scan on DynamoDB table.
     */
    @Test
    public void testParallelScan() {
        putTestData();

        /**
         * Only one segment.
         */
        ScanRequest scanRequest = new ScanRequest(tableName)
                .withScanFilter(Collections.singletonMap(
                        ATTRIBUTE_RANDOM,
                        new Condition()
                                .withAttributeValueList(
                                        new AttributeValue().withN("" + itemNumber / 2))
                                .withComparisonOperator(
                                        ComparisonOperator.LT.toString())))
                .withTotalSegments(1).withSegment(0);
        ScanResult scanResult = dynamo.scan(scanRequest);
        assertEquals((Object) itemNumber, (Object) scanResult.getScannedCount());
        int filteredItems = scanResult.getCount();

        /**
         * Multiple segments.
         */
        int totalSegments = 10;
        int filteredItemsInSegments = 0;
        for (int segment = 0; segment < totalSegments; segment++) {
            scanRequest = new ScanRequest(tableName)
                    .withScanFilter(
                            Collections.singletonMap(
                                    ATTRIBUTE_RANDOM,
                                    new Condition().withAttributeValueList(
                                            new AttributeValue().withN(""
                                                                       + itemNumber / 2))
                                                   .withComparisonOperator(
                                                           ComparisonOperator.LT
                                                                   .toString())))
                    .withTotalSegments(totalSegments).withSegment(segment);
            scanResult = dynamo.scan(scanRequest);
            filteredItemsInSegments += scanResult.getCount();
        }
        assertEquals(filteredItems, filteredItemsInSegments);
    }
}
