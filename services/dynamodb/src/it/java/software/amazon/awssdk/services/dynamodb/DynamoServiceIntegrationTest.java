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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResult;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResult;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResult;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResult;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResult;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResult;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResult;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResult;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.BasicTempTable;
import utils.resources.tables.TempTableWithBinaryKey;
import utils.test.util.DynamoDBTestBase;


@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = BasicTempTable.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS),
                            @RequiredResource(resource = TempTableWithBinaryKey.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS)
                    })
public class DynamoServiceIntegrationTest extends DynamoDBTestBase {

    private static final String HASH_KEY_NAME = BasicTempTable.HASH_KEY_NAME;
    private static final String tableName = BasicTempTable.TEMP_TABLE_NAME;
    private static final String binaryKeyTableName = TempTableWithBinaryKey.TEMP_BINARY_TABLE_NAME;
    private static final Long READ_CAPACITY = BasicTempTable.READ_CAPACITY;
    private static final Long WRITE_CAPACITY = BasicTempTable.WRITE_CAPACITY;

    /**
     * The only @BeforeClass method.
     */
    @BeforeClass
    public static void setUp() {
        DynamoDBTestBase.setUpTestBase();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNullQueryKeyErrorHandling() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        // Put a valid item first
        item.put(HASH_KEY_NAME, AttributeValue.builder_().s("bar").build_());
        item.put("age", AttributeValue.builder_().s("30").build_());
        PutItemRequest putItemRequest = PutItemRequest.builder_().tableName(tableName).item(item).returnValues(ReturnValue.ALL_OLD
                                                                                                     .toString()).build_();
        dynamo.putItem(putItemRequest);
        Map<String, KeysAndAttributes> items = new HashMap<String, KeysAndAttributes>();
        // Put a valid key and a null one
        items.put(tableName,
                  KeysAndAttributes.builder_().keys(mapKey(HASH_KEY_NAME, AttributeValue.builder_().s("bar").build_()), null).build_());

        BatchGetItemRequest request =BatchGetItemRequest.builder_()
                .requestItems(items)
                .build_();

        try {
            dynamo.batchGetItem(request);
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        Map<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
        writeAttributes.put(HASH_KEY_NAME, AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
        writeAttributes.put("bar", AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
        writeRequests.add(WriteRequest.builder_().putRequest(PutRequest.builder_().item(writeAttributes).build_()).build_());
        writeRequests.add(WriteRequest.builder_().putRequest(PutRequest.builder_().item(null).build_()).build_());
        requestItems.put(tableName, writeRequests);
        try {
            dynamo.batchWriteItem(BatchWriteItemRequest.builder_().requestItems(requestItems).build_());
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
        }

    }

    /**
     * Tests that we correctly parse JSON error responses into AmazonServiceExceptions.
     */
    @Test
    public void testErrorHandling() throws Exception {

        DeleteTableRequest request = DeleteTableRequest.builder_().tableName("non-existant-table").build_();
        try {
            dynamo.deleteTable(request);
            fail("Expected an exception to be thrown");
        } catch (AmazonServiceException ase) {
            assertNotEmpty(ase.getErrorCode());
            assertEquals(AmazonServiceException.ErrorType.Client, ase.getErrorType());
            assertNotEmpty(ase.getMessage());
            assertNotEmpty(ase.getRequestId());
            assertNotEmpty(ase.getServiceName());
            assertTrue(ase.getStatusCode() >= 400);
            assertTrue(ase.getStatusCode() < 600);
        }
    }

    /**
     * Tests that we properly handle error responses for request entities that
     * are too large.
     */
    // DISABLED because DynamoDB apparently upped their max request size; we
    // should be hitting this with a unit test that simulates an appropriate
    // AmazonServiceException.
    // @Test
    public void testRequestEntityTooLargeErrorHandling() throws Exception {

        Map<String, KeysAndAttributes> items = new HashMap<String, KeysAndAttributes>();
        for (int i = 0; i < 1024; i++) {
            KeysAndAttributes kaa = KeysAndAttributes.builder_().build_();
            StringBuilder bigString = new StringBuilder();
            for (int j = 0; j < 1024; j++) {
                bigString.append("a");
            }
            bigString.append(i);
            items.put(bigString.toString(), kaa);
        }
        BatchGetItemRequest request = BatchGetItemRequest.builder_().requestItems(items).build_();

        try {
            dynamo.batchGetItem(request);
        } catch (AmazonServiceException ase) {
            assertNotNull(ase.getMessage());
            assertEquals("Request entity too large", ase.getErrorCode());
            assertEquals(AmazonServiceException.ErrorType.Client, ase.getErrorType());
            assertEquals(413, ase.getStatusCode());
        }
    }

    @Test
    public void testBatchWriteTooManyItemsErrorHandling() throws Exception {
        int itemNumber = 26;
        HashMap<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        for (int i = 0; i < itemNumber; i++) {
            HashMap<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
            writeAttributes.put(HASH_KEY_NAME, AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
            writeAttributes.put("bar", AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
            writeRequests.add(WriteRequest.builder_().putRequest(PutRequest.builder_().item(writeAttributes).build_()).build_());
        }
        requestItems.put(tableName, writeRequests);
        try {
            dynamo.batchWriteItem(BatchWriteItemRequest.builder_().requestItems(requestItems).build_());
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
            assertEquals(AmazonServiceException.ErrorType.Client, ase.getErrorType());
            assertNotEmpty(ase.getMessage());
            assertNotEmpty(ase.getRequestId());
            assertNotEmpty(ase.getServiceName());
            assertEquals(400, ase.getStatusCode());
        }
    }

    /**
     * Tests that we can call each service operation to create and describe
     * tables, put, update and delete data, and query.
     */
    @Test
    public void testServiceOperations() throws Exception {
        // Describe all tables
        ListTablesResult describeTablesResult = dynamo.listTables(ListTablesRequest.builder_().build_());

        // Describe our new table
        DescribeTableRequest describeTablesRequest = DescribeTableRequest.builder_().tableName(tableName).build_();
        TableDescription tableDescription = dynamo.describeTable(describeTablesRequest).table();
        assertEquals(tableName, tableDescription.tableName());
        assertNotNull(tableDescription.tableStatus());
        assertEquals(HASH_KEY_NAME, tableDescription.keySchema().get(0).attributeName());
        assertEquals(KeyType.HASH.toString(), tableDescription.keySchema().get(0).keyType());
        assertNotNull(tableDescription.provisionedThroughput().numberOfDecreasesToday());
        assertEquals(READ_CAPACITY, tableDescription.provisionedThroughput().readCapacityUnits());
        assertEquals(WRITE_CAPACITY, tableDescription.provisionedThroughput().writeCapacityUnits());

        // Add some data
        int contentLength = 1 * 1024;
        Set<ByteBuffer> byteBufferSet = new HashSet<ByteBuffer>();
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength)));
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength + 1)));

        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(HASH_KEY_NAME, AttributeValue.builder_().s("bar").build_());
        item.put("age", AttributeValue.builder_().n("30").build_());
        item.put("bar", AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
        item.put("foos", AttributeValue.builder_().ss("bleh", "blah").build_());
        item.put("S", AttributeValue.builder_().ss("ONE", "TWO").build_());
        item.put("blob", AttributeValue.builder_().b(ByteBuffer.wrap(generateByteArray(contentLength))).build_());
        item.put("blobs", AttributeValue.builder_().bs(ByteBuffer.wrap(generateByteArray(contentLength)),
                                                      ByteBuffer.wrap(generateByteArray(contentLength + 1))).build_());
        item.put("BS", AttributeValue.builder_().bs(byteBufferSet).build_());

        PutItemRequest putItemRequest = PutItemRequest.builder_().tableName(tableName).item(item).returnValues(ReturnValue.ALL_OLD.toString()).build_();

        PutItemResult putItemResult = dynamo.putItem(putItemRequest);

        // Get our new item
        GetItemResult itemResult = dynamo.getItem(GetItemRequest.builder_().tableName(tableName).key(mapKey(HASH_KEY_NAME,
                                                                                             AttributeValue.builder_().s("bar").build_()))
                                                             .consistentRead(true).build_());
        assertNotNull(itemResult.item().get("S").ss());
        assertEquals(2, itemResult.item().get("S").ss().size());
        assertTrue(itemResult.item().get("S").ss().contains("ONE"));
        assertTrue(itemResult.item().get("S").ss().contains("TWO"));
        assertEquals("30", itemResult.item().get("age").n());
        assertNotNull(itemResult.item().get("bar").s());
        assertNotNull(itemResult.item().get("blob").b());
        assertEquals(0, itemResult.item().get("blob").b().compareTo(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertNotNull(itemResult.item().get("blobs").bs());
        assertEquals(2, itemResult.item().get("blobs").bs().size());
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertNotNull(itemResult.item().get("BS").bs());
        assertEquals(2, itemResult.item().get("BS").bs().size());
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Pause to try and deal with ProvisionedThroughputExceededExceptions
        Thread.sleep(1000 * 20);

        // Add some data into the table with binary hash key
        ByteBuffer byteBuffer = ByteBuffer.allocate(contentLength * 2);
        byteBuffer.put(generateByteArray(contentLength));
        byteBuffer.flip();
        item = new HashMap<String, AttributeValue>();
        item.put(HASH_KEY_NAME, AttributeValue.builder_().b(byteBuffer).build_());
        // Reuse the byteBuffer
        item.put("blob", AttributeValue.builder_().b(byteBuffer).build_());
        item.put("blobs", AttributeValue.builder_().bs(ByteBuffer.wrap(generateByteArray(contentLength)),
                                                      ByteBuffer.wrap(generateByteArray(contentLength + 1))).build_());
        // Reuse the byteBufferSet
        item.put("BS", AttributeValue.builder_().bs(byteBufferSet).build_());

        putItemRequest = PutItemRequest.builder_().tableName(binaryKeyTableName).item(item).returnValues(ReturnValue.ALL_OLD.toString()).build_();
        dynamo.putItem(putItemRequest);

        // Get our new item
        itemResult = dynamo.getItem(GetItemRequest.builder_().tableName(binaryKeyTableName).key(mapKey(HASH_KEY_NAME,
                                                                                        AttributeValue.builder_().b(byteBuffer).build_()))
                                               .consistentRead(true).build_());
        assertNotNull(itemResult.item().get("blob").b());
        assertEquals(0, itemResult.item().get("blob").b().compareTo(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertNotNull(itemResult.item().get("blobs").bs());
        assertEquals(2, itemResult.item().get("blobs").bs().size());
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertNotNull(itemResult.item().get("BS").bs());
        assertEquals(2, itemResult.item().get("BS").bs().size());
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Pause to try and deal with ProvisionedThroughputExceededExceptions
        Thread.sleep(1000 * 20);

        // Load some random data
        System.out.println("Loading data...");
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            item = new HashMap<String, AttributeValue>();
            item.put(HASH_KEY_NAME, AttributeValue.builder_().s("bar-" + System.currentTimeMillis()).build_());
            item.put("age", AttributeValue.builder_().n(Integer.toString(random.nextInt(100) + 30)).build_());
            item.put("bar", AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
            item.put("foos", AttributeValue.builder_().ss("bleh", "blah").build_());
            dynamo.putItem(PutItemRequest.builder_().tableName(tableName).item(item).returnValues(ReturnValue.ALL_OLD.toString()).build_());
        }

        // Update an item
        Map<String, AttributeValueUpdate> itemUpdates = new HashMap<String, AttributeValueUpdate>();
        itemUpdates.put("1", AttributeValueUpdate.builder_().value(AttributeValue.builder_().s("¢").build_()).action(AttributeAction.PUT.toString()).build_());
        itemUpdates.put("foos", AttributeValueUpdate.builder_().value(AttributeValue.builder_().ss("foo").build_()).action(AttributeAction.PUT.toString()).build_());
        itemUpdates.put("S", AttributeValueUpdate.builder_().value(AttributeValue.builder_().ss("THREE").build_()).action(AttributeAction.ADD.toString()).build_());
        itemUpdates.put("age", AttributeValueUpdate.builder_().value(AttributeValue.builder_().n("10").build_()).action(AttributeAction.ADD.toString()).build_());
        itemUpdates.put("blob", AttributeValueUpdate.builder_().value(
                AttributeValue.builder_().b(ByteBuffer.wrap(generateByteArray(contentLength + 1))).build_()).action(
                AttributeAction.PUT.toString()).build_());
        itemUpdates.put("blobs",
                        AttributeValueUpdate.builder_().value(AttributeValue.builder_().bs(ByteBuffer.wrap(generateByteArray(contentLength))).build_()).action(
                                                 AttributeAction.PUT.toString()).build_());
        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder_().tableName(tableName).key(
                mapKey(HASH_KEY_NAME, AttributeValue.builder_().s("bar").build_())).attributeUpdates(
                itemUpdates).returnValues("ALL_NEW").build_();

        UpdateItemResult updateItemResult = dynamo.updateItem(updateItemRequest);

        assertEquals("¢", updateItemResult.attributes().get("1").s());
        assertEquals(1, updateItemResult.attributes().get("foos").ss().size());
        assertTrue(updateItemResult.attributes().get("foos").ss().contains("foo"));
        assertEquals(3, updateItemResult.attributes().get("S").ss().size());
        assertTrue(updateItemResult.attributes().get("S").ss().contains("ONE"));
        assertTrue(updateItemResult.attributes().get("S").ss().contains("TWO"));
        assertTrue(updateItemResult.attributes().get("S").ss().contains("THREE"));
        assertEquals(Integer.toString(30 + 10), updateItemResult.attributes().get("age").n());
        assertEquals(0, updateItemResult.attributes().get("blob").b()
                                        .compareTo(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertEquals(1, updateItemResult.attributes().get("blobs").bs().size());
        assertTrue(updateItemResult.attributes().get("blobs").bs()
                                   .contains(ByteBuffer.wrap(generateByteArray(contentLength))));

        itemUpdates.clear();
        itemUpdates.put("age", AttributeValueUpdate.builder_().value(AttributeValue.builder_().n("30").build_()).action(AttributeAction.PUT.toString()).build_());
        itemUpdates.put("blobs", AttributeValueUpdate.builder_()
                .value(AttributeValue.builder_().bs(ByteBuffer.wrap(generateByteArray(contentLength + 1))).build_())
                .action(AttributeAction.ADD.toString())
                .build_());
        updateItemRequest = UpdateItemRequest.builder_()
                .tableName(tableName)
                .key(mapKey(HASH_KEY_NAME, AttributeValue.builder_().s("bar").build_()))
                .attributeUpdates(itemUpdates)
                .returnValues("ALL_NEW")
                .build_();

        updateItemResult = dynamo.updateItem(updateItemRequest);

        assertEquals("30", updateItemResult.attributes().get("age").n());
        assertEquals(2, updateItemResult.attributes().get("blobs").bs().size());
        assertTrue(updateItemResult.attributes().get("blobs").bs()
                                   .contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(updateItemResult.attributes().get("blobs").bs()
                                   .contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Get an item that doesn't exist.
        GetItemRequest itemsRequest = GetItemRequest.builder_().tableName(tableName).key(mapKey(HASH_KEY_NAME, AttributeValue.builder_().s("3").build_()))
                .consistentRead(true).build_();
        GetItemResult itemsResult = dynamo.getItem(itemsRequest);
        assertNull(itemsResult.item());

        // Get an item that doesn't have any attributes,
        itemsRequest = GetItemRequest.builder_().tableName(tableName).key(mapKey(HASH_KEY_NAME, AttributeValue.builder_().s("bar").build_()))
                .consistentRead(true).attributesToGet("non-existent-attribute").build_();
        itemsResult = dynamo.getItem(itemsRequest);
        assertEquals(0, itemsResult.item().size());


        // Scan data
        ScanRequest scanRequest = ScanRequest.builder_().tableName(tableName).attributesToGet(HASH_KEY_NAME).build_();
        ScanResult scanResult = dynamo.scan(scanRequest);
        assertTrue(scanResult.count() > 0);
        assertTrue(scanResult.scannedCount() > 0);


        // Try a more advanced Scan query and run it a few times for performance metrics
        System.out.println("Testing Scan...");
        for (int i = 0; i < 10; i++) {
            HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
            scanFilter.put("age", Condition.builder_()
                    .attributeValueList(AttributeValue.builder_().n("40").build_())
                    .comparisonOperator(ComparisonOperator.GT.toString())
                    .build_());
            scanRequest = ScanRequest.builder_().tableName(tableName).scanFilter(scanFilter).build_();
            scanResult = dynamo.scan(scanRequest);
        }

        // Batch write
        HashMap<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        HashMap<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
        writeAttributes.put(HASH_KEY_NAME, AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
        writeAttributes.put("bar", AttributeValue.builder_().s("" + System.currentTimeMillis()).build_());
        writeRequests.add(WriteRequest.builder_().putRequest(PutRequest.builder_().item(writeAttributes).build_()).build_());
        writeRequests.add(WriteRequest.builder_()
                .deleteRequest(DeleteRequest.builder_()
                        .key(mapKey(HASH_KEY_NAME, AttributeValue.builder_().s("toDelete").build_()))
                        .build_())
                .build_());
        requestItems.put(tableName, writeRequests);
        BatchWriteItemResult batchWriteItem = dynamo.batchWriteItem(BatchWriteItemRequest.builder_().requestItems(requestItems).build_());
        //        assertNotNull(batchWriteItem.itemCollectionMetrics());
        //        assertEquals(1, batchWriteItem.itemCollectionMetrics().size());
        //        assertEquals(tableName, batchWriteItem.itemCollectionMetrics().entrySet().iterator().next().get);
        //        assertNotNull(tableName, batchWriteItem.getResponses().iterator().next().getCapacityUnits());
        assertNotNull(batchWriteItem.unprocessedItems());
        assertTrue(batchWriteItem.unprocessedItems().isEmpty());

        // Delete some data
        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder_()
                .tableName(tableName)
                .key(mapKey(HASH_KEY_NAME, AttributeValue.builder_().s("jeep").build_()))
                .returnValues(ReturnValue.ALL_OLD.toString())
                .build_();
        DeleteItemResult deleteItemResult = dynamo.deleteItem(deleteItemRequest);

        // Delete our table
        DeleteTableResult deleteTable = dynamo.deleteTable(DeleteTableRequest.builder_().tableName(tableName).build_());

    }

}

