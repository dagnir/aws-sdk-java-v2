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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapperConfig.BatchLoadRetryStrategy;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDBMapper.BatchGetItemException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResult;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

public class BatchLoadRetryStrategyTest {

    private static final String TABLE_NAME = "tableName";
    private static final String TABLE_NAME2 = "tableName2";
    private static final String TABLE_NAME3 = "tableName3";
    private static final String HASH_ATTR = "hash";

    // private static BatchGetItemResult batchGetItemResult;
    private static List<Object> itemsToGet;

    static {

        itemsToGet = new ArrayList<Object>();
        itemsToGet.add(new Item3("Bruce Wayne"));
        itemsToGet.add(new Item2("Is"));
        itemsToGet.add(new Item("Batman"));
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();
    private DynamoDBClient ddbMock;
    private DynamoDBMapper mapper;
    private BatchGetItemRequest mockItemRequest;
    private BatchGetItemResult mockItemResult;

    @Before
    public void setup() {
        ddbMock = createMock(DynamoDBClient.class);
        mockItemRequest = createMock(BatchGetItemRequest.class);
        mockItemResult = createMock(BatchGetItemResult.class);
    }

    @Test
    public void testBatchReadCallFailure_NoRetry() {
        expect(ddbMock.batchGetItem((BatchGetItemRequest) anyObject()))
                .andReturn(buildDefaultGetItemResult().withUnprocessedKeys(buildUnprocessedKeysMap(1)))
                .times(1);
        DynamoDBMapperConfig config =
                getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.NoRetryBatchLoadRetryStrategy());
        mapper = new DynamoDBMapper(ddbMock, config);

        replay(ddbMock);
        thrown.expect(BatchGetItemException.class);
        mapper.batchLoad(itemsToGet);
        verify(ddbMock);
    }

    @Test
    public void testBatchReadCallFailure_Retry() {
        expect(ddbMock.batchGetItem((BatchGetItemRequest) anyObject()))
                .andReturn(buildDefaultGetItemResult().withUnprocessedKeys(buildUnprocessedKeysMap(1)))
                .times(4);
        mapper = new DynamoDBMapper(ddbMock, getConfigWithCustomBatchLoadRetryStrategy(new BatchLoadRetryStrategyWithNoDelay(3)));

        replay(ddbMock);
        thrown.expect(BatchGetItemException.class);
        mapper.batchLoad(itemsToGet);
        verify(ddbMock);
    }

    @Test
    public void testBatchReadCallSuccess_Retry() {
        expect(ddbMock.batchGetItem((BatchGetItemRequest) anyObject())).andReturn(
                buildDefaultGetItemResult().withUnprocessedKeys(new HashMap<>(1))).times(1);
        DynamoDBMapperConfig config =
                getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy());
        mapper = new DynamoDBMapper(ddbMock, config);

        replay(ddbMock);
        mapper.batchLoad(itemsToGet);
        verify(ddbMock);
    }

    @Test
    public void testBatchReadCallFailure_Retry_RetryOnCompleteFailure() {
        expect(ddbMock.batchGetItem((BatchGetItemRequest) anyObject()))
                .andReturn(buildDefaultGetItemResult().withUnprocessedKeys(buildUnprocessedKeysMap(3)))
                .times(6);
        DynamoDBMapperConfig config =
                getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy());
        mapper = new DynamoDBMapper(ddbMock, config);

        replay(ddbMock);
        thrown.expect(BatchGetItemException.class);
        mapper.batchLoad(itemsToGet);
        verify(ddbMock);
    }

    @Test
    public void testBatchReadCallFailure_NoRetry_RetryOnCompleteFailure() {
        expect(ddbMock.batchGetItem((BatchGetItemRequest) anyObject()))
                .andReturn(buildDefaultGetItemResult().withUnprocessedKeys(buildUnprocessedKeysMap(3)))
                .times(1);
        DynamoDBMapperConfig config =
                getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.NoRetryBatchLoadRetryStrategy());
        mapper = new DynamoDBMapper(ddbMock, config);

        replay(ddbMock);
        thrown.expect(BatchGetItemException.class);
        mapper.batchLoad(itemsToGet);
        verify(ddbMock);
    }

    @Test
    public void testNoDelayOnPartialFailure_DefaultRetry() {
        BatchLoadRetryStrategy defaultRetryStrategy = new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy();
        expect(mockItemResult.getUnprocessedKeys()).andReturn(buildUnprocessedKeysMap(2));
        expect(mockItemRequest.getRequestItems()).andReturn(buildUnprocessedKeysMap(3));
        replay(mockItemRequest);
        replay(mockItemResult);
        BatchLoadContext context = new BatchLoadContext(mockItemRequest);
        context.setBatchGetItemResult(mockItemResult);
        context.setRetriesAttempted(2);
        assertEquals(0, defaultRetryStrategy.getDelayBeforeNextRetry(context));
    }

    @Test
    public void testDelayOnPartialFailure_DefaultRetry() {
        BatchLoadRetryStrategy defaultRetryStrategy = new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy();
        expect(mockItemResult.getUnprocessedKeys()).andReturn(buildUnprocessedKeysMap(3));
        expect(mockItemRequest.getRequestItems()).andReturn(buildUnprocessedKeysMap(3));
        replay(mockItemRequest);
        replay(mockItemResult);
        BatchLoadContext context = new BatchLoadContext(mockItemRequest);
        context.setBatchGetItemResult(mockItemResult);
        context.setRetriesAttempted(2);
        assertTrue(defaultRetryStrategy.getDelayBeforeNextRetry(context) > 0);
    }

    private DynamoDBMapperConfig getConfigWithCustomBatchLoadRetryStrategy(final BatchLoadRetryStrategy batchReadRetryStrategy) {
        return new DynamoDBMapperConfig.Builder().withBatchLoadRetryStrategy(batchReadRetryStrategy).build();
    }

    private Map<String, KeysAndAttributes> buildUnprocessedKeysMap(final int size) {
        final Map<String, KeysAndAttributes> unproccessedKeys = new HashMap<String, KeysAndAttributes>(size);
        for (int i = 0; i < size; i++) {
            unproccessedKeys.put("test" + i, new KeysAndAttributes());
        }

        return unproccessedKeys;
    }

    private BatchGetItemResult buildDefaultGetItemResult() {

        final Map<String, List<Map<String, AttributeValue>>> map = new HashMap<String, List<Map<String, AttributeValue>>>();
        return new BatchGetItemResult().withResponses(map);

    }

    static class BatchLoadRetryStrategyWithNoDelay implements BatchLoadRetryStrategy {

        private final int maxRetry;

        public BatchLoadRetryStrategyWithNoDelay(final int maxRetry) {
            this.maxRetry = maxRetry;
        }

        /**
         * @see BatchLoadRetryStrategy#getMaxRetryOnUnprocessedKeys(java.util.Map, java.util.Map)
         */
        @Override
        public boolean shouldRetry(final BatchLoadContext batchLoadContext) {
            return batchLoadContext.getRetriesAttempted() < maxRetry;
        }

        /**
         * @see BatchLoadRetryStrategy#getDelayBeforeNextRetry(java.util.Map, int)
         */
        @Override
        public long getDelayBeforeNextRetry(final BatchLoadContext batchLoadContext) {
            return 0;
        }


    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public static class Item {

        private String hash;

        public Item(final String hash) {
            this.hash = hash;
        }

        @DynamoDBAttribute(attributeName = HASH_ATTR)
        @DynamoDBHashKey
        public String getHash() {
            return hash;
        }

        public void setHash(final String hash) {
            this.hash = hash;
        }

        public WriteRequest toPutSaveRequest() {
            return new WriteRequest().withPutRequest(new PutRequest(Collections.singletonMap(HASH_ATTR,
                                                                                             new AttributeValue(hash))));
        }
    }

    @DynamoDBTable(tableName = TABLE_NAME2)
    public static class Item2 {

        private String hash;

        public Item2(final String hash) {
            this.hash = hash;
        }

        @DynamoDBAttribute(attributeName = HASH_ATTR)
        @DynamoDBHashKey
        public String getHash() {
            return hash;
        }

        public void setHash(final String hash) {
            this.hash = hash;
        }

        public WriteRequest toPutSaveRequest() {
            return new WriteRequest().withPutRequest(new PutRequest(Collections.singletonMap(HASH_ATTR,
                                                                                             new AttributeValue(hash))));
        }
    }

    @DynamoDBTable(tableName = TABLE_NAME3)
    public static class Item3 {

        private String hash;

        public Item3(final String hash) {
            this.hash = hash;
        }

        @DynamoDBAttribute(attributeName = HASH_ATTR)
        @DynamoDBHashKey
        public String getHash() {
            return hash;
        }

        public void setHash(final String hash) {
            this.hash = hash;
        }

        public WriteRequest toPutSaveRequest() {
            return new WriteRequest().withPutRequest(new PutRequest(Collections.singletonMap(HASH_ATTR,
                                                                                             new AttributeValue(hash))));
        }
    }
}
