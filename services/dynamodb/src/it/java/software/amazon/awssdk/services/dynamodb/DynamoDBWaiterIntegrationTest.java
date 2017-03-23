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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.test.AwsIntegrationTestBase;
import software.amazon.awssdk.waiters.WaiterHandler;
import software.amazon.awssdk.waiters.WaiterParameters;

public class DynamoDBWaiterIntegrationTest extends AwsIntegrationTestBase {

    private String tableName;
    private DynamoDBClient client;

    @Before
    public void setup() {
        tableName = getClass().getSimpleName() + "-" + System.currentTimeMillis();
        client = DynamoDBClient
                .builder()
                .withCredentials(new AwsStaticCredentialsProvider(getCredentials()))
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
            DynamoDBClient client, String tableName) throws Exception {
        client.deleteTable(new DeleteTableRequest(tableName));
        client.waiters()
              .tableNotExists()
              .run(new WaiterParameters().withRequest(new DescribeTableRequest(tableName)));
        try {
            client.describeTable(new DescribeTableRequest(tableName));
            fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException re) {
            // Ignored or expected.
        }
    }

    @Test
    public void tableExistsWaiterSync_ReturnsTrue_WhenTableActive() throws Exception {
        client.waiters()
              .tableExists()
              .run(new WaiterParameters<DescribeTableRequest>().withRequest(
                      new DescribeTableRequest(tableName)));
        Assert.assertEquals("Table status is not ACTIVE", "ACTIVE",
                            client.describeTable(new DescribeTableRequest(tableName)).getTable().getTableStatus());
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
                            client.describeTable(new DescribeTableRequest(tableName)).getTable().getTableStatus());
        deleteTableWaiterSync_ThrowsResourceNotFoundException_WhenDeleted(client, tableName);
    }

}

