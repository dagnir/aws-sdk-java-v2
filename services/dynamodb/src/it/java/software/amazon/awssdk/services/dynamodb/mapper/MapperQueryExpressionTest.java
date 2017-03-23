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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DefaultDynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBIndexHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBIndexRangeKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBRangeKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResult;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDBClientWaiters;
import software.amazon.awssdk.util.ImmutableMapParameter;


/**
 * Unit test for the private method DynamoDBMapper#createQueryRequestFromExpression
 */
public class MapperQueryExpressionTest {

    private static final String TABLE_NAME = "table_name";
    private static final Condition RANGE_KEY_CONDITION = new Condition()
            .withAttributeValueList(new AttributeValue("some value"))
            .withComparisonOperator(ComparisonOperator.EQ);

    private static CaptureDynamoDB capture;
    private static DynamoDBMapper mapper;

    @BeforeClass
    public static void setUp() throws SecurityException, NoSuchMethodException {
        capture = new CaptureDynamoDB(Collections.<Map<String, AttributeValue>>emptyList());
        mapper = new DynamoDBMapper(capture);
    }

    private static <T> QueryRequest testCreateQueryRequestFromExpression(
            Class<T> clazz, DynamoDBQueryExpression<T> queryExpression) {
        return testCreateQueryRequestFromExpression(clazz, queryExpression, null);
    }

    private static <T> QueryRequest testCreateQueryRequestFromExpression(
            Class<T> clazz, DynamoDBQueryExpression<T> queryExpression,
            String expectedErrorMessage) {
        try {
            mapper.queryPage(clazz, queryExpression, DynamoDBMapperConfig.DEFAULT);
            if (expectedErrorMessage != null) {
                fail("Exception containing messsage ("
                     + expectedErrorMessage + ") is expected.");
            }
            return capture.request;
        } catch (RuntimeException e) {
            if (expectedErrorMessage != null && e.getMessage() != null) {
                assertTrue("Exception message [" + e.getMessage() + "] does not contain " +
                           "the expected message [" + expectedErrorMessage + "].",
                           e.getMessage().contains(expectedErrorMessage));
            } else {
                e.printStackTrace();
                fail("Internal error when calling createQueryRequestFromExpressio method");
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * Tests different scenarios of hash-only query
     **/
    @Test
    public void testHashConditionOnly() {
        // Primary hash only
        QueryRequest queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", null, null)));
        assertTrue(queryRequest.getKeyConditions().size() == 1);
        assertEquals("primaryHashKey", queryRequest.getKeyConditions().keySet().iterator().next());
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("primaryHashKey"));
        assertNull(queryRequest.getIndexName());

        // Primary hash used for a GSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", null, null))
                        .withIndexName("GSI-primary-hash"));
        assertTrue(queryRequest.getKeyConditions().size() == 1);
        assertEquals("primaryHashKey", queryRequest.getKeyConditions().keySet().iterator().next());
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("primaryHashKey"));
        assertEquals("GSI-primary-hash", queryRequest.getIndexName());

        // Primary hash query takes higher priority then index hash query
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", "bar", null)));
        assertTrue(queryRequest.getKeyConditions().size() == 1);
        assertEquals("primaryHashKey", queryRequest.getKeyConditions().keySet().iterator().next());
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("primaryHashKey"));
        assertNull(queryRequest.getIndexName());

        // Ambiguous query on multiple index hash keys
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass(null, "bar", "charlie")),
                "Ambiguous query expression: More than one index hash key EQ conditions");

        // Ambiguous query when not specifying index name
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass(null, "bar", null)),
                "Ambiguous query expression: More than one GSIs");

        // Explicitly specify a GSI.
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", "bar", null))
                        .withIndexName("GSI-index-hash-1"));
        assertTrue(queryRequest.getKeyConditions().size() == 1);
        assertEquals("indexHashKey", queryRequest.getKeyConditions().keySet().iterator().next());
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("bar"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("indexHashKey"));
        assertEquals("GSI-index-hash-1", queryRequest.getIndexName());

        // Non-existent GSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", "bar", null))
                        .withIndexName("some fake gsi"),
                "No hash key condition is applicable to the specified index");

        // No hash key condition specified
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass(null, null, null)),
                "Illegal query expression: No hash key condition is found in the query");
    }

    /**
     * Tests hash + range query
     **/
    @Test
    public void testHashAndRangeCondition() {
        // Primary hash + primary range
        QueryRequest queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("primaryRangeKey", RANGE_KEY_CONDITION));
        assertTrue(queryRequest.getKeyConditions().size() == 2);
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryHashKey"));
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.getKeyConditions().get("primaryRangeKey"));
        assertNull(queryRequest.getIndexName());

        // Primary hash + primary range on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("primaryRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("LSI-primary-range"));
        assertTrue(queryRequest.getKeyConditions().size() == 2);
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryHashKey"));
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.getKeyConditions().get("primaryRangeKey"));
        assertEquals("LSI-primary-range", queryRequest.getIndexName());

        // Primary hash + index range used by multiple LSI. But also a GSI hash + range
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION));
        assertTrue(queryRequest.getKeyConditions().size() == 2);
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryHashKey"));
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.getKeyConditions().containsKey("indexRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.getKeyConditions().get("indexRangeKey"));
        assertEquals("GSI-primary-hash-index-range-1", queryRequest.getIndexName());


        // Primary hash + index range on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("LSI-index-range-1"));
        assertTrue(queryRequest.getKeyConditions().size() == 2);
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryHashKey"));
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.getKeyConditions().containsKey("indexRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.getKeyConditions().get("indexRangeKey"));
        assertEquals("LSI-index-range-1", queryRequest.getIndexName());

        // Non-existent LSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("some fake lsi"),
                "No range key condition is applicable to the specified index");

        // Illegal query: Primary hash + primary range on a GSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("GSI-index-hash-index-range-1"),
                "Illegal query expression: No hash key condition is applicable to the specified index");

        // GSI hash + GSI range
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass(null, "foo"))
                        .withRangeKeyCondition("primaryRangeKey", RANGE_KEY_CONDITION));
        assertTrue(queryRequest.getKeyConditions().size() == 2);
        assertTrue(queryRequest.getKeyConditions().containsKey("indexHashKey"));
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("indexHashKey"));
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.getKeyConditions().get("primaryRangeKey"));
        assertEquals("GSI-index-hash-primary-range", queryRequest.getIndexName());

        // Ambiguous query: GSI hash + index range used by multiple GSIs
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass(null, "foo"))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION),
                "Illegal query expression: Cannot infer the index name from the query expression.");

        // Explicitly specify the GSI name
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass(null, "foo"))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("GSI-index-hash-index-range-2"));
        assertTrue(queryRequest.getKeyConditions().size() == 2);
        assertTrue(queryRequest.getKeyConditions().containsKey("indexHashKey"));
        assertEquals(
                new Condition().withAttributeValueList(new AttributeValue("foo"))
                               .withComparisonOperator(ComparisonOperator.EQ),
                queryRequest.getKeyConditions().get("indexHashKey"));
        assertTrue(queryRequest.getKeyConditions().containsKey("indexRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.getKeyConditions().get("indexRangeKey"));
        assertEquals("GSI-index-hash-index-range-2", queryRequest.getIndexName());

        // Ambiguous query: (1) primary hash + LSI range OR (2) GSI hash + range
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("anotherIndexRangeKey", RANGE_KEY_CONDITION),
                "Ambiguous query expression: Found multiple valid queries:");

        // Multiple range key conditions specified
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyConditions(
                                ImmutableMapParameter.of(
                                        "primaryRangeKey", RANGE_KEY_CONDITION,
                                        "indexRangeKey", RANGE_KEY_CONDITION)),
                "Illegal query expression: Conditions on multiple range keys");

        // Using an un-annotated range key
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexHashKey", RANGE_KEY_CONDITION),
                "not annotated with either @DynamoDBRangeKey or @DynamoDBIndexRangeKey.");
    }

    @Test
    public void testHashOnlyQueryOnHashRangeTable() {
        // Primary hash only query on a Hash+Range table
        QueryRequest queryRequest = testCreateQueryRequestFromExpression(
                LSIRangeKeyClass.class,
                new DynamoDBQueryExpression<LSIRangeKeyClass>()
                        .withHashKeyValues(new LSIRangeKeyClass("foo", null)));
        assertTrue(queryRequest.getKeyConditions().size() == 1);
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryHashKey"));
        assertNull(queryRequest.getIndexName());

        // Hash+Range query on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                LSIRangeKeyClass.class,
                new DynamoDBQueryExpression<LSIRangeKeyClass>()
                        .withHashKeyValues(new LSIRangeKeyClass("foo", null))
                        .withRangeKeyCondition("lsiRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("LSI"));
        assertTrue(queryRequest.getKeyConditions().size() == 2);
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryHashKey"));
        assertTrue(queryRequest.getKeyConditions().containsKey("lsiRangeKey"));
        assertEquals("LSI", queryRequest.getIndexName());

        // Hash-only query on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                LSIRangeKeyClass.class,
                new DynamoDBQueryExpression<LSIRangeKeyClass>()
                        .withHashKeyValues(new LSIRangeKeyClass("foo", null))
                        .withIndexName("LSI"));
        assertTrue(queryRequest.getKeyConditions().size() == 1);
        assertTrue(queryRequest.getKeyConditions().containsKey("primaryHashKey"));
        assertEquals("LSI", queryRequest.getIndexName());
    }

    private static final class CaptureDynamoDB extends DefaultDynamoDBClient {
        private QueryRequest request;
        private QueryResult result;

        private CaptureDynamoDB(final List<Map<String, AttributeValue>> items) {
            super(null);
            this.result = new QueryResult();
            this.result.setItems(items);
        }

        @Override
        public QueryResult query(QueryRequest request) {
            this.request = request;
            return this.result;
        }

        @Override
        public DynamoDBClientWaiters waiters() {
            return null;
        }

        @Override
        public void close() throws Exception {

        }
    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public final class HashOnlyClass {

        @DynamoDBHashKey
        @DynamoDBIndexHashKey(
                globalSecondaryIndexNames = "GSI-primary-hash"
        )
        private String primaryHashKey;

        @DynamoDBIndexHashKey(
                globalSecondaryIndexNames = {"GSI-index-hash-1", "GSI-index-hash-2"}
        )
        private String indexHashKey;

        @DynamoDBIndexHashKey(
                globalSecondaryIndexNames = {"GSI-another-index-hash"}
        )
        private String anotherIndexHashKey;

        public HashOnlyClass(String primaryHashKey, String indexHashKey, String anotherIndexHashKey) {
            this.primaryHashKey = primaryHashKey;
            this.indexHashKey = indexHashKey;
            this.anotherIndexHashKey = anotherIndexHashKey;
        }

        public String getPrimaryHashKey() {
            return primaryHashKey;
        }

        public void setPrimaryHashKey(String primaryHashKey) {
            this.primaryHashKey = primaryHashKey;
        }

        public String getIndexHashKey() {
            return indexHashKey;
        }

        public void setIndexHashKey(String indexHashKey) {
            this.indexHashKey = indexHashKey;
        }

        public String getAnotherIndexHashKey() {
            return anotherIndexHashKey;
        }

        public void setAnotherIndexHashKey(String anotherIndexHashKey) {
            this.anotherIndexHashKey = anotherIndexHashKey;
        }
    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public final class HashRangeClass {
        private String primaryHashKey;
        private String indexHashKey;
        private String primaryRangeKey;
        private String indexRangeKey;
        private String anotherIndexRangeKey;

        public HashRangeClass(String primaryHashKey, String indexHashKey) {
            this.primaryHashKey = primaryHashKey;
            this.indexHashKey = indexHashKey;
        }

        @DynamoDBHashKey
        @DynamoDBIndexHashKey(
                globalSecondaryIndexNames = {
                        "GSI-primary-hash-index-range-1",
                        "GSI-primary-hash-index-range-2"}
        )
        public String getPrimaryHashKey() {
            return primaryHashKey;
        }

        public void setPrimaryHashKey(String primaryHashKey) {
            this.primaryHashKey = primaryHashKey;
        }

        @DynamoDBIndexHashKey(
                globalSecondaryIndexNames = {
                        "GSI-index-hash-primary-range",
                        "GSI-index-hash-index-range-1",
                        "GSI-index-hash-index-range-2"}
        )
        public String getIndexHashKey() {
            return indexHashKey;
        }

        public void setIndexHashKey(String indexHashKey) {
            this.indexHashKey = indexHashKey;
        }

        @DynamoDBRangeKey
        @DynamoDBIndexRangeKey(
                globalSecondaryIndexNames = {"GSI-index-hash-primary-range"},
                localSecondaryIndexName = "LSI-primary-range"
        )
        public String getPrimaryRangeKey() {
            return primaryRangeKey;
        }

        public void setPrimaryRangeKey(String primaryRangeKey) {
            this.primaryRangeKey = primaryRangeKey;
        }

        @DynamoDBIndexRangeKey(
                globalSecondaryIndexNames = {
                        "GSI-primary-hash-index-range-1",
                        "GSI-index-hash-index-range-1",
                        "GSI-index-hash-index-range-2"},
                localSecondaryIndexNames = {"LSI-index-range-1", "LSI-index-range-2"}
        )
        public String getIndexRangeKey() {
            return indexRangeKey;
        }

        public void setIndexRangeKey(String indexRangeKey) {
            this.indexRangeKey = indexRangeKey;
        }

        @DynamoDBIndexRangeKey(
                localSecondaryIndexName = "LSI-index-range-3",
                globalSecondaryIndexName = "GSI-primary-hash-index-range-2"
        )
        public String getAnotherIndexRangeKey() {
            return anotherIndexRangeKey;
        }

        public void setAnotherIndexRangeKey(String anotherIndexRangeKey) {
            this.anotherIndexRangeKey = anotherIndexRangeKey;
        }
    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public final class LSIRangeKeyClass {
        private String primaryHashKey;
        private String primaryRangeKey;
        private String lsiRangeKey;

        public LSIRangeKeyClass(String primaryHashKey, String primaryRangeKey) {
            this.primaryHashKey = primaryHashKey;
            this.primaryRangeKey = primaryRangeKey;
        }

        @DynamoDBHashKey
        public String getPrimaryHashKey() {
            return primaryHashKey;
        }

        public void setPrimaryHashKey(String primaryHashKey) {
            this.primaryHashKey = primaryHashKey;
        }

        @DynamoDBRangeKey
        public String getPrimaryRangeKey() {
            return primaryRangeKey;
        }

        public void setPrimaryRangeKey(String primaryRangeKey) {
            this.primaryRangeKey = primaryRangeKey;
        }

        @DynamoDBIndexRangeKey(localSecondaryIndexName = "LSI")
        public String getLsiRangeKey() {
            return lsiRangeKey;
        }

        public void setLsiRangeKey(String lsiRangeKey) {
            this.lsiRangeKey = lsiRangeKey;
        }
    }

}
