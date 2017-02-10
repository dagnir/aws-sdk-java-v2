/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License is
 * located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodbv2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.test.AWSIntegrationTestBase;
import software.amazon.awssdk.waiters.WaiterHandler;
import software.amazon.awssdk.waiters.WaiterParameters;

/**
 * Created by meghbyar on 6/15/16.
 */
public class DynamoDBWaiterIntegrationTest extends AWSIntegrationTestBase {

    private String tableName;
    private AmazonDynamoDB client;

    @Before
    public void setup() {
        tableName = getClass().getSimpleName() + "-" + System.currentTimeMillis();
        client = AmazonDynamoDBClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(getCredentials()))
                .withRegion(Regions.US_WEST_2)
                .build();
        client.createTable(new CreateTableRequest().withTableName(tableName)
                                   .withKeySchema(new KeySchemaElement().withKeyType(KeyType.HASH)
                                                          .withAttributeName("hashKey"))
                                   .withAttributeDefinitions(new AttributeDefinition()
                                                                     .withAttributeType(
                                                                             ScalarAttributeType.S)
                                                                     .withAttributeName("hashKey"))
                                   .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)));
    }


    public void deleteTableWaiterSync_ThrowsResourceNotFoundException_WhenDeleted(
            AmazonDynamoDB client, String tableName) throws Exception {
        client.deleteTable(tableName);
        client.waiters()
                .tableNotExists()
                .run(new WaiterParameters().withRequest(new DescribeTableRequest(tableName)));
        try {
            client.describeTable(tableName);
            fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException re) {
        }
    }

    @Test
    public void tableExistsWaiterSync_ReturnsTrue_WhenTableActive() throws Exception {
        client.waiters()
                .tableExists()
                .run(new WaiterParameters<DescribeTableRequest>().withRequest(
                        new DescribeTableRequest(tableName)));
        Assert.assertEquals("Table status is not ACTIVE", "ACTIVE",
                            client.describeTable(tableName).getTable().getTableStatus());
        deleteTableWaiterSync_ThrowsResourceNotFoundException_WhenDeleted(client, tableName);

    }

    @Test
    public void tableExistsWaiterAsync_ReturnsTrue_WhenTableActive() throws Exception {
        final AtomicBoolean onWaitSuccessCalled = new AtomicBoolean(false);
        final AtomicBoolean onWaitFailureCalled = new AtomicBoolean(false);
        Future future = client.waiters()
                .tableExists()
                .runAsync(
                        new WaiterParameters<DescribeTableRequest>()
                                .withRequest(new DescribeTableRequest(tableName)),
                        new WaiterHandler<DescribeTableRequest>() {
                            @Override
                            public void onWaitSuccess(DescribeTableRequest request) {
                                onWaitSuccessCalled.set(true);
                                System.out.println("Table creation success!!!!!");
                            }

                            @Override
                            public void onWaitFailure(Exception e) {
                                onWaitFailureCalled.set(true);
                            }
                        });
        future.get(5, TimeUnit.MINUTES);
        assertTrue(onWaitSuccessCalled.get());
        assertFalse(onWaitFailureCalled.get());
        Assert.assertEquals("Table status is not ACTIVE", "ACTIVE",
                            client.describeTable(tableName).getTable().getTableStatus());
        deleteTableWaiterSync_ThrowsResourceNotFoundException_WhenDeleted(client, tableName);
    }

}

