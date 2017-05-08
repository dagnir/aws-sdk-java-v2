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

package utils.test.util;

import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResult;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;

public class DynamoDBIntegrationTestBase extends DynamoDBTestBase {

    protected static final String KEY_NAME = "key";
    protected static final String TABLE_NAME = "aws-java-sdk-util";
    protected static final String TABLE_WITH_RANGE_ATTRIBUTE = "aws-java-sdk-range-test";
    protected static final String TABLE_WITH_INDEX_RANGE_ATTRIBUTE = "aws-java-sdk-index-range-test";
    protected static long startKey = System.currentTimeMillis();

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        dynamo = DynamoDBClient.builder().region(Regions.US_EAST_1.getName()).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        // Create a table
        String keyName = KEY_NAME;
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(TABLE_NAME)
                .withKeySchema(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(keyName).withAttributeType(
                                ScalarAttributeType.S));
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(10L)
                                                                               .withWriteCapacityUnits(5L));

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_NAME);
        }
    }

    /**
     * Quick utility method to delete all tables when we have too much capacity
     * reserved for the region.
     */
    public static void deleteAllTables() {
        ListTablesResult listTables = dynamo.listTables(new ListTablesRequest());
        for (String name : listTables.getTableNames()) {
            dynamo.deleteTable(new DeleteTableRequest().withTableName(name));
        }
    }

    protected static void setUpTableWithRangeAttribute() throws Exception {
        setUp();

        String keyName = DynamoDBIntegrationTestBase.KEY_NAME;
        String rangeKeyAttributeName = "rangeKey";
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(TABLE_WITH_RANGE_ATTRIBUTE)
                .withKeySchema(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH),
                               new KeySchemaElement().withAttributeName(rangeKeyAttributeName).withKeyType(KeyType.RANGE))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(keyName).withAttributeType(
                                ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(rangeKeyAttributeName).withAttributeType(
                                ScalarAttributeType.N));
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(10L)
                                                                               .withWriteCapacityUnits(5L));

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_WITH_RANGE_ATTRIBUTE);
        }
    }

    protected static void setUpTableWithIndexRangeAttribute(boolean recreateTable) throws Exception {
        setUp();
        if (recreateTable) {
            dynamo.deleteTable(new DeleteTableRequest().withTableName(TABLE_WITH_INDEX_RANGE_ATTRIBUTE));
            waitForTableToBecomeDeleted(TABLE_WITH_INDEX_RANGE_ATTRIBUTE);
        }

        String keyName = DynamoDBIntegrationTestBase.KEY_NAME;
        String rangeKeyAttributeName = "rangeKey";
        String indexFooRangeKeyAttributeName = "indexFooRangeKey";
        String indexBarRangeKeyAttributeName = "indexBarRangeKey";
        String multipleIndexRangeKeyAttributeName = "multipleIndexRangeKey";
        String fooAttributeName = "fooAttribute";
        String barAttributeName = "barAttribute";
        String indexFooName = "index_foo";
        String indexBarName = "index_bar";
        String indexFooCopyName = "index_foo_copy";
        String indexBarCopyName = "index_bar_copy";

        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(TABLE_WITH_INDEX_RANGE_ATTRIBUTE)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH),
                        new KeySchemaElement().withAttributeName(rangeKeyAttributeName).withKeyType(KeyType.RANGE))
                .withLocalSecondaryIndexes(
                        new LocalSecondaryIndex()
                                .withIndexName(indexFooName)
                                .withKeySchema(
                                        new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH),
                                        new KeySchemaElement().withAttributeName(indexFooRangeKeyAttributeName)
                                                              .withKeyType(KeyType.RANGE))
                                .withProjection(new Projection()
                                                        .withProjectionType(ProjectionType.INCLUDE)
                                                        .withNonKeyAttributes(fooAttributeName)),
                        new LocalSecondaryIndex()
                                .withIndexName(indexBarName)
                                .withKeySchema(
                                        new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH),
                                        new KeySchemaElement().withAttributeName(indexBarRangeKeyAttributeName)
                                                              .withKeyType(KeyType.RANGE))
                                .withProjection(new Projection()
                                                        .withProjectionType(ProjectionType.INCLUDE)
                                                        .withNonKeyAttributes(barAttributeName)),
                        new LocalSecondaryIndex()
                                .withIndexName(indexFooCopyName)
                                .withKeySchema(
                                        new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH),
                                        new KeySchemaElement().withAttributeName(multipleIndexRangeKeyAttributeName)
                                                              .withKeyType(KeyType.RANGE))
                                .withProjection(new Projection()
                                                        .withProjectionType(ProjectionType.INCLUDE)
                                                        .withNonKeyAttributes(fooAttributeName)),
                        new LocalSecondaryIndex()
                                .withIndexName(indexBarCopyName)
                                .withKeySchema(
                                        new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH),
                                        new KeySchemaElement().withAttributeName(multipleIndexRangeKeyAttributeName)
                                                              .withKeyType(KeyType.RANGE))
                                .withProjection(new Projection()
                                                        .withProjectionType(ProjectionType.INCLUDE)
                                                        .withNonKeyAttributes(barAttributeName)))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(keyName).withAttributeType(ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(rangeKeyAttributeName)
                                                 .withAttributeType(ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(indexFooRangeKeyAttributeName)
                                                 .withAttributeType(ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(indexBarRangeKeyAttributeName)
                                                 .withAttributeType(ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(multipleIndexRangeKeyAttributeName)
                                                 .withAttributeType(ScalarAttributeType.N));
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(10L)
                                                                               .withWriteCapacityUnits(5L));

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_WITH_INDEX_RANGE_ATTRIBUTE);
        }
    }
}
