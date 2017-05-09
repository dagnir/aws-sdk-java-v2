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
import static org.junit.Assert.assertNotNull;

import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBAttribute;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBRangeKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import utils.test.util.DynamoDBIntegrationTestBase;

public class MapperSaveConfigTestBase extends DynamoDBIntegrationTestBase {

    protected static final DynamoDBMapperConfig defaultConfig = new DynamoDBMapperConfig(
            SaveBehavior.UPDATE);
    protected static final DynamoDBMapperConfig updateSkipNullConfig = new DynamoDBMapperConfig(
            SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES);
    protected static final DynamoDBMapperConfig appendSetConfig = new DynamoDBMapperConfig(
            SaveBehavior.APPEND_SET);
    protected static final DynamoDBMapperConfig clobberConfig = new DynamoDBMapperConfig(
            SaveBehavior.CLOBBER);
    protected static final String tableName = "aws-java-sdk-dynamodb-mapper-save-config-test";
    protected static final String hashKeyName = "hashKey";
    protected static final String rangeKeyName = "rangeKey";
    protected static final String nonKeyAttributeName = "nonKeyAttribute";
    protected static final String stringSetAttributeName = "stringSetAttribute";
    /**
     * Read capacity for the test table being created in Amazon DynamoDB.
     */
    protected static final Long READ_CAPACITY = 10L;
    /**
     * Write capacity for the test table being created in Amazon DynamoDB.
     */
    protected static final Long WRITE_CAPACITY = 5L;
    /**
     * Provisioned Throughput for the test table created in Amazon DynamoDB
     */
    protected static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT = ProvisionedThroughput.builder_()
            .readCapacityUnits(READ_CAPACITY).writeCapacityUnits(
                    WRITE_CAPACITY).build_();
    protected static DynamoDBMapper dynamoMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        dynamo = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        dynamoMapper = new DynamoDBMapper(dynamo);

        createTestTable(DEFAULT_PROVISIONED_THROUGHPUT);
        TableUtils.waitUntilActive(dynamo, tableName);
    }

    @AfterClass
    public static void tearDown() {
        dynamo.deleteTable(DeleteTableRequest.builder_().tableName(tableName).build_());
    }

    /**
     * Helper method to create a table in Amazon DynamoDB
     */
    protected static void createTestTable(
            ProvisionedThroughput provisionedThroughput) {
        CreateTableRequest createTableRequest = CreateTableRequest.builder_()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder_().attributeName(
                                hashKeyName).keyType(
                                KeyType.HASH).build_())
                .keySchema(
                        KeySchemaElement.builder_().attributeName(
                                rangeKeyName).keyType(
                                KeyType.RANGE).build_())
                .attributeDefinitions(
                        AttributeDefinition.builder_().attributeName(
                                hashKeyName).attributeType(
                                ScalarAttributeType.S).build_())
                .attributeDefinitions(
                        AttributeDefinition.builder_().attributeName(
                                rangeKeyName).attributeType(
                                ScalarAttributeType.N).build_())
                .provisionedThroughput(provisionedThroughput)
                .build_();

        TableDescription createdTableDescription = dynamo.createTable(
                createTableRequest).tableDescription();
        System.out.println("Created Table: " + createdTableDescription);
        assertEquals(tableName, createdTableDescription.tableName());
        assertNotNull(createdTableDescription.tableStatus());
        assertEquals(hashKeyName, createdTableDescription
                .keySchema().get(0).attributeName());
        assertEquals(KeyType.HASH.toString(), createdTableDescription
                .keySchema().get(0).keyType());
        assertEquals(rangeKeyName, createdTableDescription
                .keySchema().get(1).attributeName());
        assertEquals(KeyType.RANGE.toString(), createdTableDescription
                .keySchema().get(1).keyType());
    }

    @DynamoDBTable(tableName = tableName)
    public static class TestItem {

        private String hashKey;
        private Long rangeKey;
        private String nonKeyAttribute;
        private Set<String> stringSetAttribute;

        @DynamoDBHashKey(attributeName = hashKeyName)
        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        @DynamoDBRangeKey(attributeName = rangeKeyName)
        public Long getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(Long rangeKey) {
            this.rangeKey = rangeKey;
        }

        @DynamoDBAttribute(attributeName = nonKeyAttributeName)
        public String nonKeyAttribute() {
            return nonKeyAttribute;
        }

        public void setNonKeyAttribute(String nonKeyAttribute) {
            this.nonKeyAttribute = nonKeyAttribute;
        }

        @DynamoDBAttribute(attributeName = stringSetAttributeName)
        public Set<String> stringSetAttribute() {
            return stringSetAttribute;
        }

        public void setStringSetAttribute(Set<String> stringSetAttribute) {
            this.stringSetAttribute = stringSetAttribute;
        }

    }

    @DynamoDBTable(tableName = tableName)
    public static class TestAppendToScalarItem {

        private String hashKey;
        private Long rangeKey;
        private Set<String> fakeStringSetAttribute;

        @DynamoDBHashKey(attributeName = hashKeyName)
        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        @DynamoDBRangeKey(attributeName = rangeKeyName)
        public Long getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(Long rangeKey) {
            this.rangeKey = rangeKey;
        }

        @DynamoDBAttribute(attributeName = nonKeyAttributeName)
        public Set<String> getFakeStringSetAttribute() {
            return fakeStringSetAttribute;
        }

        public void setFakeStringSetAttribute(Set<String> stringSetAttribute) {
            this.fakeStringSetAttribute = stringSetAttribute;
        }
    }
}
