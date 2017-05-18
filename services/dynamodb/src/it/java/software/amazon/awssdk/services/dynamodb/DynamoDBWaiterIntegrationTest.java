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
import software.amazon.awssdk.auth.StaticCredentialsProvider;
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
                .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                .region(Regions.US_WEST_2.getName())
                .build();
        client.createTable(CreateTableRequest.builder_().tableName(tableName)
                                                   .keySchema(KeySchemaElement.builder_().keyType(KeyType.HASH)
                                                                                        .attributeName("hashKey").build_())
                                                   .attributeDefinitions(AttributeDefinition.builder_()
                                                                                     .attributeType(
                                                                                             ScalarAttributeType.S)
                                                                                     .attributeName("hashKey").build_())
                                                   .provisionedThroughput(ProvisionedThroughput.builder_().readCapacityUnits(5L).writeCapacityUnits(5L).build_()).build_());
    }


    public void deleteTableWaiterSync_ThrowsResourceNotFoundException_WhenDeleted(
            DynamoDBClient client, String tableName) throws Exception {
        client.deleteTable(DeleteTableRequest.builder_().tableName(tableName).build_());
        client.waiters()
              .tableNotExists()
              .run(new WaiterParameters<DescribeTableRequest>().withRequest(DescribeTableRequest.builder_().tableName(tableName).build_()));
        try {
            client.describeTable(DescribeTableRequest.builder_().tableName(tableName).build_());
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
                      DescribeTableRequest.builder_().tableName(tableName).build_()));
        Assert.assertEquals("Table status is not ACTIVE", "ACTIVE",
                            client.describeTable(DescribeTableRequest.builder_().tableName(tableName).build_()).table().tableStatus());
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
                                              .withRequest(DescribeTableRequest.builder_().tableName(tableName).build_()),
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
                            client.describeTable(DescribeTableRequest.builder_().tableName(tableName).build_()).table().tableStatus());
        deleteTableWaiterSync_ThrowsResourceNotFoundException_WhenDeleted(client, tableName);
    }

}

