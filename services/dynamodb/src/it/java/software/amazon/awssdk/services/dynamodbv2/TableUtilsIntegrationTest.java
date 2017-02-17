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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableStatus;
import software.amazon.awssdk.services.dynamodbv2.util.TableUtils;
import software.amazon.awssdk.services.dynamodbv2.util.TableUtils.TableNeverTransitionedToStateException;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class TableUtilsIntegrationTest extends AwsIntegrationTestBase {

    private static final int CUSTOM_TIMEOUT = 5 * 1000;

    /**
     * Wait a generous amount of time after the custom timeout to account for
     * variance due to polling interval. This is only used in tests that use
     * {@link TableUtilsIntegrationTest#CUSTOM_TIMEOUT}
     */
    private static final int TEST_TIMEOUT = CUSTOM_TIMEOUT * 2;

    private static final int CUSTOM_POLLING_INTERVAL = 1 * 1000;
    private static final long READ_CAPACITY = 5L;
    private static final long WRITE_CAPACITY = 5L;
    private static final String HASH_KEY_NAME = "someHash";

    private static AmazonDynamoDBClient ddb;
    private String tableName;

    @BeforeClass
    public static void setupFixture() {
        ddb = new AmazonDynamoDBClient(getCredentials());
    }

    private CreateTableRequest createTableRequest() {
        return new CreateTableRequest().withTableName(tableName).withKeySchema(
                new KeySchemaElement().withKeyType(KeyType.HASH).withAttributeName(HASH_KEY_NAME))
                                       .withAttributeDefinitions(new AttributeDefinition().withAttributeName(HASH_KEY_NAME)
                                                                                          .withAttributeType(ScalarAttributeType.S))
                                       .withProvisionedThroughput(
                                               new ProvisionedThroughput().withReadCapacityUnits(READ_CAPACITY)
                                                                          .withWriteCapacityUnits(WRITE_CAPACITY));
    }

    private DeleteTableRequest deleteTableRequest() {
        return new DeleteTableRequest().withTableName(tableName);
    }

    private void createTable() {
        ddb.createTable(createTableRequest());
    }

    @Before
    public void setup() {
        tableName = "TableUtilsTest-" + System.currentTimeMillis();
    }

    @After
    public void tearDown() throws InterruptedException {
        if (getTableStatus() != null) {
            if (!getTableStatus().equals(TableStatus.DELETING)) {
                TableUtils.waitUntilActive(ddb, tableName);
                ddb.deleteTable(tableName);
            }
            waitUntilTableDeleted();
        }
    }

    /**
     * @return Table status or null if it doesn't exist.
     */
    private String getTableStatus() {
        try {
            return ddb.describeTable(tableName).getTable().getTableStatus();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    // TODO replace with waiters when available.
    private void waitUntilTableDeleted() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        // Wait up to five minutes for a table to be deleted.
        long endTime = startTime + 5 * 60 * 1000;
        while (System.currentTimeMillis() < endTime) {
            try {
                ddb.describeTable(tableName);
                Thread.sleep(1000);
            } catch (ResourceNotFoundException e) {
                return;
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitUntilActive_InvalidTimeout_ThrowsException() throws Exception {
        TableUtils.waitUntilActive(ddb, tableName, -1, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitUntilActive_InvalidInterval_ThrowsException() throws Exception {
        TableUtils.waitUntilActive(ddb, tableName, 10, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitUntilActive_IntervalGreaterThanTimeout_ThrowsException() throws Exception {
        TableUtils.waitUntilActive(ddb, tableName, 10, 100);
    }

    @Test
    public void waitUntilActive_MethodBlocksUntilTableIsActive() throws Exception {
        createTable();
        TableUtils.waitUntilActive(ddb, tableName);
        assertEquals(TableStatus.ACTIVE.toString(),
                     ddb.describeTable(tableName).getTable().getTableStatus());
    }

    @Test(expected = TableNeverTransitionedToStateException.class, timeout = TEST_TIMEOUT)
    public void waitUntilActive_TableNeverTransitionsToActive_ThrowsException() throws Exception {
        createTable();
        // We wait long enough for DescribeTable to return something but not
        // long enough for the table to transition to active
        TableUtils.waitUntilActive(ddb, tableName, 1 * 1000, 500);
    }

    @Test(expected = TableNeverTransitionedToStateException.class, timeout = TEST_TIMEOUT)
    public void waitUntilActive_NoSuchTable_BlocksUntilTimeoutThenThrowsException() throws
                                                                                    InterruptedException {
        TableUtils.waitUntilActive(ddb, tableName, CUSTOM_TIMEOUT, CUSTOM_POLLING_INTERVAL);
    }

    @Test
    public void waitUntilExists_MethodBlocksUntilTableExists() throws InterruptedException {
        createTable();
        TableUtils.waitUntilExists(ddb, tableName);
        assertNotNull(ddb.describeTable(tableName));
    }

    @Test(expected = AmazonClientException.class, timeout = TEST_TIMEOUT)
    public void waitUntilExists_NoSuchTable_BlocksUntilTimeoutThenThrowsException() throws
                                                                                    InterruptedException {
        TableUtils.waitUntilExists(ddb, tableName, CUSTOM_TIMEOUT, CUSTOM_POLLING_INTERVAL);
    }

    @Test
    public void testCreateTableIfNotExists() throws InterruptedException {
        assertTrue(TableUtils.createTableIfNotExists(ddb, createTableRequest()));
        TableUtils.waitUntilExists(ddb, tableName);
        assertFalse(TableUtils.createTableIfNotExists(ddb, createTableRequest()));
    }

    @Test
    public void testDeleteTableIfExists() throws InterruptedException {
        assertFalse(TableUtils.deleteTableIfExists(ddb, deleteTableRequest()));
        createTable();
        TableUtils.waitUntilActive(ddb, tableName);
        assertTrue(TableUtils.deleteTableIfExists(ddb, deleteTableRequest()));
        waitUntilTableDeleted();
    }

}
