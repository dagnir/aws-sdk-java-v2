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

import java.util.HashMap;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.ScanRequest;
import software.amazon.awssdk.services.dynamodbv2.util.TableUtils;
import software.amazon.awssdk.services.dynamodbv2.util.TableUtils.TableNeverTransitionedToStateException;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class GzipConfigurationIntegrationTest extends AwsIntegrationTestBase {

    private static final String TABLE_NAME = "test-gzip-" + System.currentTimeMillis();
    private static final String KEY_NAME = "key";
    private static final String VALUE_NAME = "value";
    private static final int ITEMS_COUNT = 500;
    private static AmazonDynamoDB dynamo;

    @BeforeClass
    public static void setup() throws TableNeverTransitionedToStateException,
                                      InterruptedException {
        dynamo = new AmazonDynamoDBClient(getCredentials(),
                                          new LegacyClientConfiguration().withGzip(true));
        createTable();
        // For this integration test, if the payload is not big enough, the service will not compress the data.
        putItems(ITEMS_COUNT);
    }

    @AfterClass
    public static void tearDown() {
        dynamo.deleteTable(new DeleteTableRequest().withTableName(TABLE_NAME));
    }

    private static void createTable() throws TableNeverTransitionedToStateException, InterruptedException {
        dynamo.createTable(new CreateTableRequest()
                                   .withTableName(TABLE_NAME)
                                   .withAttributeDefinitions(
                                           new AttributeDefinition().withAttributeName(KEY_NAME)
                                                                    .withAttributeType(ScalarAttributeType.S))
                                   .withKeySchema(
                                           new KeySchemaElement().withKeyType(KeyType.HASH)
                                                                 .withAttributeName(KEY_NAME))
                                   .withProvisionedThroughput(
                                           new ProvisionedThroughput(100L, 100L)));

        TableUtils.waitUntilActive(dynamo, TABLE_NAME);
    }

    @SuppressWarnings("serial")
    private static void putItems(int count) {
        for (int i = 0; i < count; ++i) {
            dynamo.putItem(new PutItemRequest().withTableName(TABLE_NAME)
                                               .withItem(new HashMap<String, AttributeValue>() {
                                                   {
                                                       put(KEY_NAME, new AttributeValue().withS(UUID
                                                                                                        .randomUUID().toString()));
                                                       put(VALUE_NAME, new AttributeValue().withS(UUID
                                                                                                          .randomUUID().toString()));
                                                   }
                                               }));
        }
    }

    @Test
    public void gzipConfigurationIntegrationTest() throws TableNeverTransitionedToStateException,
                                                          InterruptedException {
        int count = dynamo.scan(new ScanRequest().withTableName(TABLE_NAME))
                          .getCount();
        Assert.assertEquals(ITEMS_COUNT, count);
    }

}
