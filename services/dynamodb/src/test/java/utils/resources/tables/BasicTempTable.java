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

package utils.resources.tables;

import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDB;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import utils.test.resources.DynamoDBTableResource;
import utils.test.util.DynamoDBTestBase;

public class BasicTempTable extends DynamoDBTableResource {

    public static final String TEMP_TABLE_NAME = "java-sdk-" + System.currentTimeMillis();
    public static final String HASH_KEY_NAME = "hash";
    public static final Long READ_CAPACITY = 10L;
    public static final Long WRITE_CAPACITY = 5L;
    public static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
            new ProvisionedThroughput().withReadCapacityUnits(READ_CAPACITY).withWriteCapacityUnits(WRITE_CAPACITY);

    @Override
    protected AmazonDynamoDB getClient() {
        return DynamoDBTestBase.getClient();
    }

    @Override
    protected CreateTableRequest getCreateTableRequest() {
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(TEMP_TABLE_NAME)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(HASH_KEY_NAME)
                                              .withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(
                                HASH_KEY_NAME).withAttributeType(
                                ScalarAttributeType.S));
        request.setProvisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT);
        return request;
    }

}
