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
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.PaginatedQueryList;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
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
        keySchema.add(KeySchemaElement.builder_().attributeName("id").keyType(KeyType.HASH).build_());

        CreateTableRequest req = CreateTableRequest.builder_()
                .tableName(HASH_KEY_ONLY_TABLE_NAME)
                .keySchema(keySchema)
                .provisionedThroughput(ProvisionedThroughput.builder_().readCapacityUnits(10L).writeCapacityUnits(10L).build_())
                .attributeDefinitions(
                        AttributeDefinition.builder_().attributeName("id").attributeType(ScalarAttributeType.S).build_(),
                        AttributeDefinition.builder_().attributeName("status").attributeType(ScalarAttributeType.S).build_(),
                        AttributeDefinition.builder_().attributeName("ts").attributeType(ScalarAttributeType.S).build_())
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder_()
                                .provisionedThroughput(ProvisionedThroughput.builder_().readCapacityUnits(10L).writeCapacityUnits(10L).build_())
                                .indexName("statusAndCreation")
                                .keySchema(
                                        KeySchemaElement.builder_().attributeName("status").keyType(KeyType.HASH).build_(),
                                        KeySchemaElement.builder_().attributeName("ts").keyType(KeyType.RANGE).build_())
                                .projection(
                                        Projection.builder_().projectionType(ProjectionType.ALL).build_()).build_()).build_();

        TableUtils.createTableIfNotExists(dynamo, req);
        TableUtils.waitUntilActive(dynamo, HASH_KEY_ONLY_TABLE_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        dynamo.deleteTable(DeleteTableRequest.builder_().tableName(HASH_KEY_ONLY_TABLE_NAME).build_());
    }

    /**
     * Tests that we can query using the hash/range GSI on our hash-key only table.
     */
    @Test
    public void testGSIQuery() throws Exception {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        String status = "foo-status";

        User user = new User();
        user.setId("123");
        user.setStatus(status);
        user.setTs("321");
        mapper.save(user);

        DynamoDBQueryExpression<User> expr = new DynamoDBQueryExpression<User>()
                .withIndexName("statusAndCreation")
                .withLimit(100)
                .withConsistentRead(false)
                .withHashKeyValues(user)
                .withRangeKeyCondition("ts",
                                       Condition.builder_()
                                               .comparisonOperator(ComparisonOperator.GT)
                                               .attributeValueList(AttributeValue.builder_().s("100").build_()).build_());

        PaginatedQueryList<User> query = mapper.query(User.class, expr);
        assertEquals(1, query.size());
        assertEquals(status, query.get(0).status());
    }

    @DynamoDBTable(tableName = HASH_KEY_ONLY_TABLE_NAME)
    public static class User {
        private String id;
        private String status;
        private String ts;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBIndexHashKey(globalSecondaryIndexName = "statusAndCreation")
        public String status() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @DynamoDBIndexRangeKey(globalSecondaryIndexName = "statusAndCreation")
        public String getTs() {
            return ts;
        }

        public void setTs(String ts) {
            this.ts = ts;
        }
    }

}
