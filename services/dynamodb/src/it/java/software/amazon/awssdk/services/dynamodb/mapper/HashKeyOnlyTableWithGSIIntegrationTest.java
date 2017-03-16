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

import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBIndexHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBIndexRangeKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.PaginatedQueryList;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import utils.test.util.DynamoDBTestBase;

/**
 * Integration test for GSI support with a table that has no primary range key (only a primary hash key).
 */
public class HashKeyOnlyTableWithGSIIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    public static final String HASH_KEY_ONLY_TABLE_NAME = "no-primary-range-key-gsi-test";


    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBTestBase.setUpTestBase();
        List<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(new KeySchemaElement("id", KeyType.HASH));

        CreateTableRequest req = new CreateTableRequest(HASH_KEY_ONLY_TABLE_NAME, keySchema)
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                .withAttributeDefinitions(
                        new AttributeDefinition("id", ScalarAttributeType.S),
                        new AttributeDefinition("status", ScalarAttributeType.S),
                        new AttributeDefinition("ts", ScalarAttributeType.S))
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                                .withIndexName("statusAndCreation")
                                .withKeySchema(
                                        new KeySchemaElement("status", KeyType.HASH),
                                        new KeySchemaElement("ts", KeyType.RANGE))
                                .withProjection(
                                        new Projection().withProjectionType(ProjectionType.ALL)));

        TableUtils.createTableIfNotExists(dynamo, req);
        TableUtils.waitUntilActive(dynamo, HASH_KEY_ONLY_TABLE_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        dynamo.deleteTable(HASH_KEY_ONLY_TABLE_NAME);
    }

    /**
     * Tests that we can query using the hash/range GSI on our hash-key only table.
     */
    @Test
    public void testGSIQuery() throws Exception {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        String status = "foo-status";

        User user = new User();
        user.setId("123");
        user.setStatus(status);
        user.setTs("321");
        mapper.save(user);

        DynamoDbQueryExpression<User> expr = new DynamoDbQueryExpression<User>()
                .withIndexName("statusAndCreation")
                .withLimit(100)
                .withConsistentRead(false)
                .withHashKeyValues(user)
                .withRangeKeyCondition("ts",
                                       new Condition()
                                               .withComparisonOperator(ComparisonOperator.GT)
                                               .withAttributeValueList(new AttributeValue("100")));

        PaginatedQueryList<User> query = mapper.query(User.class, expr);
        assertEquals(1, query.size());
        assertEquals(status, query.get(0).getStatus());
    }

    @DynamoDbTable(tableName = HASH_KEY_ONLY_TABLE_NAME)
    public static class User {
        private String id;
        private String status;
        private String ts;

        @DynamoDbHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbIndexHashKey(globalSecondaryIndexName = "statusAndCreation")
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @DynamoDbIndexRangeKey(globalSecondaryIndexName = "statusAndCreation")
        public String getTs() {
            return ts;
        }

        public void setTs(String ts) {
            this.ts = ts;
        }
    }

}
