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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResult;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.TempTableWithSecondaryIndexes;
import utils.test.util.DynamoDBTestBase;

/**
 * DynamoDB integration tests for LSI & GSI.
 */
@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = TempTableWithSecondaryIndexes.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS)
                    })
public class SecondaryIndexesIntegrationTest extends DynamoDBTestBase {

    private static final int MAX_RETRIES = 5;
    private static final int SLEEP_TIME = 20000;
    private static final String tableName = TempTableWithSecondaryIndexes.TEMP_TABLE_NAME;
    private static final String HASH_KEY_NAME = TempTableWithSecondaryIndexes.HASH_KEY_NAME;
    private static final String RANGE_KEY_NAME = TempTableWithSecondaryIndexes.RANGE_KEY_NAME;
    private static final String LSI_NAME = TempTableWithSecondaryIndexes.LSI_NAME;
    private static final String LSI_RANGE_KEY_NAME = TempTableWithSecondaryIndexes.LSI_RANGE_KEY_NAME;
    private static final String GSI_NAME = TempTableWithSecondaryIndexes.GSI_NAME;
    private static final String GSI_HASH_KEY_NAME = TempTableWithSecondaryIndexes.GSI_HASH_KEY_NAME;
    private static final String GSI_RANGE_KEY_NAME = TempTableWithSecondaryIndexes.GSI_RANGE_KEY_NAME;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBTestBase.setUpTestBase();
    }

    /**
     * Assert the tableDescription is as expected
     */
    @Test
    public void testDescribeTempTableWithIndexes() {
        TableDescription tableDescription = dynamo.describeTable(new DescribeTableRequest(tableName)).getTable();
        assertEquals(tableName, tableDescription.getTableName());
        assertNotNull(tableDescription.getTableStatus());
        assertEquals(2, tableDescription.getKeySchema().size());
        assertEquals(HASH_KEY_NAME,
                     tableDescription.getKeySchema().get(0)
                                     .getAttributeName());
        assertEquals(KeyType.HASH.toString(), tableDescription
                .getKeySchema().get(0).getKeyType());
        assertEquals(RANGE_KEY_NAME, tableDescription.getKeySchema()
                                                     .get(1).getAttributeName());
        assertEquals(KeyType.RANGE.toString(), tableDescription
                .getKeySchema().get(1).getKeyType());

        assertEquals(1, tableDescription.getLocalSecondaryIndexes().size());
        assertEquals(LSI_NAME, tableDescription
                .getLocalSecondaryIndexes().get(0).getIndexName());
        assertEquals(2, tableDescription
                .getLocalSecondaryIndexes().get(0).getKeySchema().size());
        assertEquals(HASH_KEY_NAME, tableDescription
                .getLocalSecondaryIndexes().get(0).getKeySchema().get(0).getAttributeName());
        assertEquals(KeyType.HASH.toString(), tableDescription
                .getLocalSecondaryIndexes().get(0).getKeySchema().get(0).getKeyType());
        assertEquals(LSI_RANGE_KEY_NAME, tableDescription
                .getLocalSecondaryIndexes().get(0).getKeySchema().get(1).getAttributeName());
        assertEquals(KeyType.RANGE.toString(), tableDescription
                .getLocalSecondaryIndexes().get(0).getKeySchema().get(1).getKeyType());
        assertEquals(ProjectionType.KEYS_ONLY.toString(),
                     tableDescription.getLocalSecondaryIndexes().get(0)
                                     .getProjection().getProjectionType());
        assertEquals(null, tableDescription.getLocalSecondaryIndexes().get(0)
                                           .getProjection().getNonKeyAttributes());

        assertEquals(1, tableDescription.getGlobalSecondaryIndexes().size());
        assertEquals(GSI_NAME, tableDescription
                .getGlobalSecondaryIndexes().get(0).getIndexName());
        assertEquals(2, tableDescription
                .getGlobalSecondaryIndexes().get(0).getKeySchema().size());
        assertEquals(GSI_HASH_KEY_NAME, tableDescription
                .getGlobalSecondaryIndexes().get(0).getKeySchema().get(0).getAttributeName());
        assertEquals(KeyType.HASH.toString(), tableDescription
                .getGlobalSecondaryIndexes().get(0).getKeySchema().get(0).getKeyType());
        assertEquals(GSI_RANGE_KEY_NAME, tableDescription
                .getGlobalSecondaryIndexes().get(0).getKeySchema().get(1).getAttributeName());
        assertEquals(KeyType.RANGE.toString(), tableDescription
                .getGlobalSecondaryIndexes().get(0).getKeySchema().get(1).getKeyType());
        assertEquals(ProjectionType.KEYS_ONLY.toString(),
                     tableDescription.getGlobalSecondaryIndexes().get(0)
                                     .getProjection().getProjectionType());
        assertEquals(null, tableDescription.getGlobalSecondaryIndexes().get(0)
                                           .getProjection().getNonKeyAttributes());

    }

    /**
     * Tests making queries with global secondary index.
     */
    @Test
    public void testQueryWithGlobalSecondaryIndex() throws InterruptedException {
        // GSI attributes don't have to be unique
        // so items with the same GSI keys but different primary keys
        // could co-exist in the table.
        int totalDuplicateGSIKeys = 10;
        Random random = new Random();
        String duplicateGSIHashValue = UUID.randomUUID().toString();
        int duplicateGSIRangeValue = random.nextInt();
        for (int i = 0; i < totalDuplicateGSIKeys; i++) {
            Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
            item.put(HASH_KEY_NAME, new AttributeValue().withS(UUID.randomUUID().toString()));
            item.put(RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(i)));
            item.put(GSI_HASH_KEY_NAME, new AttributeValue().withS(duplicateGSIHashValue));
            item.put(GSI_RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(duplicateGSIRangeValue)));
            dynamo.putItem(new PutItemRequest(tableName, item));
        }

        // Query the duplicate GSI key values should return all the items
        Map<String, Condition> keyConditions = new HashMap<String, Condition>();
        keyConditions.put(
                GSI_HASH_KEY_NAME,
                new Condition().withAttributeValueList(
                        new AttributeValue().withS((duplicateGSIHashValue)))
                               .withComparisonOperator(ComparisonOperator.EQ));
        keyConditions.put(
                GSI_RANGE_KEY_NAME,
                new Condition().withAttributeValueList(
                        new AttributeValue().withN(Integer
                                                           .toString(duplicateGSIRangeValue)))
                               .withComparisonOperator(ComparisonOperator.EQ));

        // All the items with the GSI keys should be returned
        assertQueryResultCount(totalDuplicateGSIKeys, new QueryRequest()
                .withTableName(tableName)
                .withIndexName(GSI_NAME)
                .withKeyConditions(keyConditions));

        // Other than this, the behavior of GSI query should be the similar
        // as LSI query. So following code is similar to that used for
        // LSI query test.

        String randomPrimaryHashKeyValue = UUID.randomUUID().toString();
        String randomGSIHashKeyValue = UUID.randomUUID().toString();
        int totalItemsPerHash = 10;
        int totalIndexedItemsPerHash = 5;
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();

        item.put(HASH_KEY_NAME, new AttributeValue().withS(randomPrimaryHashKeyValue));
        item.put(GSI_HASH_KEY_NAME, new AttributeValue().withS(randomGSIHashKeyValue));
        // Items with GSI keys
        for (int i = 0; i < totalIndexedItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(i)));
            item.put(GSI_RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(i)));
            item.put("attribute_" + i, new AttributeValue().withS(UUID.randomUUID().toString()));
            dynamo.putItem(new PutItemRequest(tableName, item));
            item.remove("attribute_" + i);
        }
        item.remove(GSI_RANGE_KEY_NAME);
        // Items with incomplete GSI keys (no GSI range key)
        for (int i = totalIndexedItemsPerHash; i < totalItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(i)));
            item.put("attribute_" + i, new AttributeValue().withS(UUID.randomUUID().toString()));
            dynamo.putItem(new PutItemRequest(tableName, item));
            item.remove("attribute_" + i);
        }

        /**
         *  1) Query-with-GSI (only by GSI hash key)
         */
        QueryResult result = dynamo.query(new QueryRequest()
                                                  .withTableName(tableName)
                                                  .withIndexName(GSI_NAME)
                                                  .withKeyConditions(
                                                          Collections.singletonMap(
                                                                  GSI_HASH_KEY_NAME,
                                                                  new Condition().withAttributeValueList(
                                                                          new AttributeValue()
                                                                                  .withS(randomGSIHashKeyValue))
                                                                                 .withComparisonOperator(
                                                                                         ComparisonOperator.EQ))));
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.getCount());
        // By default, the result includes all the key attributes (2 primary + 2 GSI).
        assertEquals(4, result.getItems().get(0).size());

        /**
         * 2) Query-with-GSI (by GSI hash + range)
         */
        int rangeKeyConditionRange = 2;
        keyConditions = new HashMap<String, Condition>();
        keyConditions.put(
                GSI_HASH_KEY_NAME,
                new Condition().withAttributeValueList(
                        new AttributeValue().withS(randomGSIHashKeyValue))
                               .withComparisonOperator(ComparisonOperator.EQ));
        keyConditions.put(
                GSI_RANGE_KEY_NAME,
                new Condition().withAttributeValueList(new AttributeValue()
                                                               .withN(Integer.toString(rangeKeyConditionRange)))
                               .withComparisonOperator(ComparisonOperator.LT));
        result = dynamo.query(new QueryRequest()
                                      .withTableName(tableName)
                                      .withIndexName(GSI_NAME)
                                      .withKeyConditions(keyConditions));
        assertEquals((Object) rangeKeyConditionRange, (Object) result.getCount());

        /**
         * 3) Query-with-GSI does not support Select.ALL_ATTRIBUTES if the index
         * was not created with this projection type.
         */
        try {
            result = dynamo.query(new QueryRequest()
                                          .withTableName(tableName)
                                          .withIndexName(GSI_NAME)
                                          .withKeyConditions(
                                                  Collections.singletonMap(
                                                          GSI_HASH_KEY_NAME,
                                                          new Condition().withAttributeValueList(
                                                                  new AttributeValue()
                                                                          .withS(randomGSIHashKeyValue))
                                                                         .withComparisonOperator(ComparisonOperator.EQ)))
                                          .withSelect(Select.ALL_ATTRIBUTES));
            fail("AmazonServiceException is expected");
        } catch (AmazonServiceException ase) {
            assertTrue(ase.getMessage().contains("Select type ALL_ATTRIBUTES is not supported for global secondary"));
        }

        /**
         * 4) Query-with-GSI on selected attributes (by AttributesToGet)
         */
        result = dynamo.query(new QueryRequest()
                                      .withTableName(tableName)
                                      .withIndexName(GSI_NAME)
                                      .withKeyConditions(
                                              Collections.singletonMap(
                                                      GSI_HASH_KEY_NAME,
                                                      new Condition().withAttributeValueList(
                                                              new AttributeValue()
                                                                      .withS(randomGSIHashKeyValue))
                                                                     .withComparisonOperator(ComparisonOperator.EQ)))
                                      .withAttributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME));
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.getCount());
        // Two attributes as specified in AttributesToGet
        assertEquals(2, result.getItems().get(0).size());

        /**
         * 5) Exception when using both Selection and AttributeToGet
         */
        try {
            result = dynamo.query(new QueryRequest()
                                          .withTableName(tableName)
                                          .withIndexName(GSI_NAME)
                                          .withKeyConditions(
                                                  Collections.singletonMap(
                                                          GSI_HASH_KEY_NAME,
                                                          new Condition().withAttributeValueList(
                                                                  new AttributeValue()
                                                                          .withS(randomGSIHashKeyValue))
                                                                         .withComparisonOperator(ComparisonOperator.EQ)))
                                          .withAttributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME, LSI_RANGE_KEY_NAME)
                                          .withSelect(Select.ALL_PROJECTED_ATTRIBUTES));
            fail("Should trigger exception when using both Select and AttributeToGet.");
        } catch (AmazonServiceException ase) {
            // Ignored or expected.
        }

        /**
         * 6) Query-with-GSI on selected attributes (by Select.SPECIFIC_ATTRIBUTES)
         */
        result = dynamo.query(new QueryRequest()
                                      .withTableName(tableName)
                                      .withIndexName(GSI_NAME)
                                      .withKeyConditions(
                                              Collections.singletonMap(
                                                      GSI_HASH_KEY_NAME,
                                                      new Condition().withAttributeValueList(
                                                              new AttributeValue()
                                                                      .withS(randomGSIHashKeyValue))
                                                                     .withComparisonOperator(
                                                                             ComparisonOperator.EQ)))
                                      .withAttributesToGet(HASH_KEY_NAME)
                                      .withSelect(Select.SPECIFIC_ATTRIBUTES));
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.getCount());
        // Only one attribute as specified in AttributesToGet
        assertEquals(1, result.getItems().get(0).size());
    }

    /**
     * Tests making queries with local secondary index.
     */
    @Test
    public void testQueryWithLocalSecondaryIndex() throws Exception {
        String randomHashKeyValue = UUID.randomUUID().toString();
        int totalItemsPerHash = 10;
        int totalIndexedItemsPerHash = 5;
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();

        item.put(HASH_KEY_NAME, new AttributeValue().withS(randomHashKeyValue));
        // Items with LSI range key
        for (int i = 0; i < totalIndexedItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(i)));
            item.put(LSI_RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(i)));
            item.put("attribute_" + i, new AttributeValue().withS(UUID.randomUUID().toString()));
            dynamo.putItem(new PutItemRequest(tableName, item));
            item.remove("attribute_" + i);
        }
        item.remove(LSI_RANGE_KEY_NAME);
        // Items without LSI range key
        for (int i = totalIndexedItemsPerHash; i < totalItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, new AttributeValue().withN(Integer.toString(i)));
            item.put("attribute_" + i, new AttributeValue().withS(UUID.randomUUID().toString()));
            dynamo.putItem(new PutItemRequest(tableName, item));
            item.remove("attribute_" + i);
        }

        /**
         *  1) Query-with-LSI (only by hash key)
         */
        QueryResult result = dynamo.query(new QueryRequest()
                                                  .withTableName(tableName)
                                                  .withIndexName(LSI_NAME)
                                                  .withKeyConditions(
                                                          Collections.singletonMap(
                                                                  HASH_KEY_NAME,
                                                                  new Condition().withAttributeValueList(
                                                                          new AttributeValue()
                                                                                  .withS(randomHashKeyValue))
                                                                                 .withComparisonOperator(
                                                                                         ComparisonOperator.EQ))));
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.getCount());
        // By default, the result includes all the projected attributes.
        assertEquals(3, result.getItems().get(0).size());

        /**
         * 2) Query-with-LSI (by hash + LSI range)
         */
        int rangeKeyConditionRange = 2;
        Map<String, Condition> keyConditions = new HashMap<String, Condition>();
        keyConditions.put(
                HASH_KEY_NAME,
                new Condition().withAttributeValueList(
                        new AttributeValue().withS(randomHashKeyValue))
                               .withComparisonOperator(ComparisonOperator.EQ));
        keyConditions.put(
                LSI_RANGE_KEY_NAME,
                new Condition().withAttributeValueList(new AttributeValue()
                                                               .withN(Integer.toString(rangeKeyConditionRange)))
                               .withComparisonOperator(ComparisonOperator.LT));
        result = dynamo.query(new QueryRequest()
                                      .withTableName(tableName)
                                      .withIndexName(LSI_NAME)
                                      .withKeyConditions(keyConditions));
        assertEquals((Object) rangeKeyConditionRange, (Object) result.getCount());

        /**
         * 3) Query-with-LSI on selected attributes (by Select)
         */
        result = dynamo.query(new QueryRequest()
                                      .withTableName(tableName)
                                      .withIndexName(LSI_NAME)
                                      .withKeyConditions(
                                              Collections.singletonMap(
                                                      HASH_KEY_NAME,
                                                      new Condition().withAttributeValueList(
                                                              new AttributeValue()
                                                                      .withS(randomHashKeyValue))
                                                                     .withComparisonOperator(ComparisonOperator.EQ)))
                                      .withSelect(Select.ALL_ATTRIBUTES));
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.getCount());
        // By setting Select.ALL_ATTRIBUTES, all attributes in the item will be returned
        assertEquals(4, result.getItems().get(0).size());

        /**
         * 4) Query-with-LSI on selected attributes (by AttributesToGet)
         */
        result = dynamo.query(new QueryRequest()
                                      .withTableName(tableName)
                                      .withIndexName(LSI_NAME)
                                      .withKeyConditions(
                                              Collections.singletonMap(
                                                      HASH_KEY_NAME,
                                                      new Condition().withAttributeValueList(
                                                              new AttributeValue()
                                                                      .withS(randomHashKeyValue))
                                                                     .withComparisonOperator(ComparisonOperator.EQ)))
                                      .withAttributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME));
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.getCount());
        // Two attributes as specified in AttributesToGet
        assertEquals(2, result.getItems().get(0).size());

        /**
         * 5) Exception when using both Selection and AttributeToGet
         */
        try {
            result = dynamo.query(new QueryRequest()
                                          .withTableName(tableName)
                                          .withIndexName(LSI_NAME)
                                          .withKeyConditions(
                                                  Collections.singletonMap(
                                                          HASH_KEY_NAME,
                                                          new Condition().withAttributeValueList(
                                                                  new AttributeValue()
                                                                          .withS(randomHashKeyValue))
                                                                         .withComparisonOperator(ComparisonOperator.EQ)))
                                          .withAttributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME, LSI_RANGE_KEY_NAME)
                                          .withSelect(Select.ALL_PROJECTED_ATTRIBUTES));
            fail("Should trigger exception when using both Select and AttributeToGet.");
        } catch (AmazonServiceException ase) {
            // Ignored or expected.
        }

        /**
         * 6) Query-with-LSI on selected attributes (by Select.SPECIFIC_ATTRIBUTES)
         */
        result = dynamo.query(new QueryRequest()
                                      .withTableName(tableName)
                                      .withIndexName(LSI_NAME)
                                      .withKeyConditions(
                                              Collections.singletonMap(
                                                      HASH_KEY_NAME,
                                                      new Condition().withAttributeValueList(
                                                              new AttributeValue()
                                                                      .withS(randomHashKeyValue))
                                                                     .withComparisonOperator(
                                                                             ComparisonOperator.EQ)))
                                      .withAttributesToGet(HASH_KEY_NAME)
                                      .withSelect(Select.SPECIFIC_ATTRIBUTES));
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.getCount());
        // Only one attribute as specified in AttributesToGet
        assertEquals(1, result.getItems().get(0).size());
    }

    private void assertQueryResultCount(Integer expected, QueryRequest request)
            throws InterruptedException {

        int retries = 0;
        QueryResult result = null;
        do {
            result = dynamo.query(request);

            if (expected == result.getCount()) {
                return;
            }
            // Handling eventual consistency.
            Thread.sleep(SLEEP_TIME);
            retries++;
        } while (retries <= MAX_RETRIES);

        Assert.fail("Failed to assert query count. Expected : " + expected
                    + " actual : " + result.getCount());
    }
}
