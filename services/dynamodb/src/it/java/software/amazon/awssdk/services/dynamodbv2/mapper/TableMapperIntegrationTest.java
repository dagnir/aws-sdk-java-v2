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

package software.amazon.awssdk.services.dynamodbv2.mapper;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedTimestamp;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBTable;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBTableMapper;
import software.amazon.awssdk.services.dynamodbv2.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodbv2.pojos.AutoKeyAndVal;

/**
 * Tests updating component attribute fields correctly.
 */
public class TableMapperIntegrationTest extends AbstractKeyAndValIntegrationTestCase {

    /**
     * Test using {@code Date}.
     */
    @Test
    public void testSaveIfNotExists() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();

        mapper.saveIfNotExists(object);
    }

    /**
     * Test using {@code Date}.
     */
    @Test(expected = ConditionalCheckFailedException.class)
    public void testSaveIfNotExistsWhenExists() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();

        mapper.saveIfNotExists(object);
        mapper.saveIfNotExists(object);
    }

    /**
     * Test using {@code Date}.
     */
    @Test
    public void testSaveWhenExists() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();

        mapper.saveIfNotExists(object);
        mapper.save(object);
    }

    /**
     * Test using {@code Date}.
     */
    @Test(expected = ConditionalCheckFailedException.class)
    public void testSaveIfExistsWhenNotExists() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setKey(UUID.randomUUID().toString());

        mapper.saveIfExists(object);
    }

    /**
     * Test using {@code Date}.
     */
    @Test
    public void testDeleteIfExistsWhenExists() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();

        mapper.saveIfNotExists(object);
        mapper.deleteIfExists(object);
    }

    /**
     * Test using {@code Date}.
     */
    @Test(expected = ConditionalCheckFailedException.class)
    public void testDeleteIfExistsWhenNotExists() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setKey(UUID.randomUUID().toString());

        mapper.deleteIfExists(object);
    }

    /**
     * Test batch load with no results.
     */
    @Test
    public void testBatchLoadItemList() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object1 = new KeyAndDateValue();
        final KeyAndDateValue object2 = new KeyAndDateValue();

        assertEquals(0, mapper.batchSave(Arrays.asList(object1, object2)).size());
        assertEquals(2, mapper.batchLoad(Arrays.asList(object1, object2)).size());
        assertEquals(0, mapper.batchDelete(Arrays.asList(object1, object2)).size());
        assertEquals(0, mapper.batchLoad(Arrays.asList(object1, object2)).size());
    }

    /**
     * Test batch load with no results.
     */
    @Test
    public void testBatchLoadItemListOnNull() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        assertEquals(0, mapper.batchLoad((List<KeyAndDateValue>) null).size());
    }

    /**
     * Test batch load with no results.
     */
    @Test
    public void testBatchLoadItemListOnEmpty() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        assertEquals(0, mapper.batchLoad(Collections.<KeyAndDateValue>emptyList()).size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryCount() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.count(new DynamoDBQueryExpression<KeyAndDateValue>()
                                             .withHashKeyValues(object).withConsistentRead(true)));
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryBeginsWith() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate", mapper.field("queryDate")
                                                                                          .beginsWith(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryBetween() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate", mapper.field("queryDate")
                                                                                          .between(object.getQueryDate(), object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryGreaterThanOrEqualTo() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate",
                                                                       mapper.field("queryDate").ge(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryGreaterThan() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(0, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate",
                                                                       mapper.field("queryDate").gt(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryEqualTo() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate",
                                                                       mapper.field("queryDate").eq(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryIn() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate",
                                                                       mapper.field("queryDate").in(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryIsNull() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(0, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate", mapper.field("queryDate").isNull())
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryLessThanOrEqualTo() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate",
                                                                       mapper.field("queryDate").le(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryLessThan() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(0, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate",
                                                                       mapper.field("queryDate").lt(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryNotEqualTo() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(0, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate",
                                                                       mapper.field("queryDate").ne(object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryNotNull() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate", mapper.field("queryDate").notNull())
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryAnyBetween() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate", mapper.field("queryDate")
                                                                                          .betweenAny(object.getQueryDate(), object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryAnyBetweenLoNull() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate", mapper.field("queryDate")
                                                                                          .betweenAny(null, object.getQueryDate()))
                                        ).getResults().size());
    }

    /**
     * Test a query.
     */
    @Test
    public void testQueryAnyBetweenHiNull() {
        final DynamoDBTableMapper<KeyAndDateValue, String, ?> mapper = util.newTableMapper(KeyAndDateValue.class);

        final KeyAndDateValue object = new KeyAndDateValue();
        object.setQueryDate(new Date());

        mapper.saveIfNotExists(object);

        assertEquals(1, mapper.queryPage(new DynamoDBQueryExpression<KeyAndDateValue>()
                                                 .withHashKeyValues(object).withConsistentRead(true)
                                                 .withQueryFilterEntry("queryDate", mapper.field("queryDate")
                                                                                          .betweenAny(object.getQueryDate(), null))
                                        ).getResults().size());
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndDateValue extends AutoKeyAndVal<Date> {
        private Date queryDate;

        @DynamoDBAutoGeneratedTimestamp
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }

        @DynamoDBAttribute(attributeName = "queryDate")
        public Date getQueryDate() {
            return this.queryDate;
        }

        public void setQueryDate(final Date queryDate) {
            this.queryDate = queryDate;
        }
    }

}
