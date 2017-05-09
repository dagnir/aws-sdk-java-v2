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

package software.amazon.awssdk.services.dynamodb.document.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.document.QueryOutcome;
import software.amazon.awssdk.services.dynamodb.document.spec.QuerySpec;
import software.amazon.awssdk.services.dynamodb.model.Capacity;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.QueryResult;

public class QueryCollectionTest {
    private static final Random rand = new Random();

    @Test
    public void testEmptyResult() {
        QueryCollection col = new QueryCollection(null, new QuerySpec());
        col.setLastLowLevelResult(new QueryOutcome(QueryResult.builder_().build_()));
        assertTrue(0 == col.getTotalCount());
        assertTrue(0 == col.getTotalScannedCount());
        assertNull(col.getTotalConsumedCapacity());
    }

    @Test
    public void setLastLowLevelResult() {
        QueryCollection col = new QueryCollection(null, new QuerySpec());
        QueryResult result = QueryResult.builder_()
                .count(rand.nextInt())
                .scannedCount(rand.nextInt()).build_();

        Map<String, Capacity> gsi = new HashMap<String, Capacity>();
        gsi.put("gsi1", Capacity.builder_().capacityUnits(rand.nextDouble()).build_());
        gsi.put("gsi2", Capacity.builder_().capacityUnits(rand.nextDouble()).build_());

        Map<String, Capacity> lsi = new HashMap<String, Capacity>();
        lsi.put("lsi1", Capacity.builder_().capacityUnits(rand.nextDouble()).build_());
        lsi.put("lsi2", Capacity.builder_().capacityUnits(rand.nextDouble()).build_());

        ConsumedCapacity consumedCapacity = ConsumedCapacity.builder_()
                .capacityUnits(rand.nextDouble())
                .table(Capacity.builder_().capacityUnits(rand.nextDouble()).build_())
                .tableName("tableName")
                .globalSecondaryIndexes(gsi)
                .localSecondaryIndexes(lsi)
                .build_();
        // Once
        result = result.toBuilder().consumedCapacity(consumedCapacity).build_();
        col.setLastLowLevelResult(new QueryOutcome(result));

        assertTrue(result.count() == col.getTotalCount());
        assertTrue(result.scannedCount() == col.getTotalScannedCount());

        ConsumedCapacity total = col.getTotalConsumedCapacity();
        assertNotSame(total, consumedCapacity);
        assertEquals(total, consumedCapacity);

        assertNotSame(gsi, total.globalSecondaryIndexes());
        assertNotSame(lsi, total.localSecondaryIndexes());

        // Twice
        col.setLastLowLevelResult(new QueryOutcome(result));

        assertTrue(result.count() * 2 == col.getTotalCount());
        assertTrue(result.scannedCount() * 2 == col.getTotalScannedCount());

        total = col.getTotalConsumedCapacity();
        assertTrue(total.capacityUnits() == 2 * consumedCapacity.capacityUnits());

        Map<String, Capacity> gsiTotal = total.globalSecondaryIndexes();
        Map<String, Capacity> lsiTotal = total.localSecondaryIndexes();
        assertTrue(2 == gsiTotal.size());
        assertTrue(2 == lsiTotal.size());

        assertTrue(gsi.get("gsi1").capacityUnits() * 2 == gsiTotal.get("gsi1").capacityUnits());
        assertTrue(gsi.get("gsi2").capacityUnits() * 2 == gsiTotal.get("gsi2").capacityUnits());

        assertTrue(lsi.get("lsi1").capacityUnits() * 2 == lsiTotal.get("lsi1").capacityUnits());
        assertTrue(lsi.get("lsi2").capacityUnits() * 2 == lsiTotal.get("lsi2").capacityUnits());

        // A different one
        QueryResult result3 = QueryResult.builder_()
                .count(rand.nextInt())
                .scannedCount(rand.nextInt())
                .build_();

        Map<String, Capacity> gsi3 = new HashMap<String, Capacity>();
        gsi3.put("gsi3", Capacity.builder_().capacityUnits(rand.nextDouble()).build_());

        Map<String, Capacity> lsi3 = new HashMap<String, Capacity>();
        lsi3.put("lsi3", Capacity.builder_().capacityUnits(rand.nextDouble()).build_());

        ConsumedCapacity consumedCapacity3 = ConsumedCapacity.builder_()
                .capacityUnits(rand.nextDouble())
                .table(Capacity.builder_().capacityUnits(rand.nextDouble()).build_())
                .tableName("tableName")
                .globalSecondaryIndexes(gsi3)
                .localSecondaryIndexes(lsi3)
                .build_();
        result3 = result3.toBuilder().consumedCapacity(consumedCapacity3).build_();
        col.setLastLowLevelResult(new QueryOutcome(result3));

        assertTrue(result.count() * 2 + result3.count() == col.getTotalCount());
        assertTrue(result.scannedCount() * 2 + result3.scannedCount() == col.getTotalScannedCount());

        total = col.getTotalConsumedCapacity();
        assertTrue(total.capacityUnits() ==
                   2 * consumedCapacity.capacityUnits()
                   + consumedCapacity3.capacityUnits());

        gsiTotal = total.globalSecondaryIndexes();
        lsiTotal = total.localSecondaryIndexes();
        assertTrue(3 == gsiTotal.size());
        assertTrue(3 == lsiTotal.size());

        assertTrue(gsi.get("gsi1").capacityUnits() * 2 == gsiTotal.get("gsi1").capacityUnits());
        assertTrue(gsi.get("gsi2").capacityUnits() * 2 == gsiTotal.get("gsi2").capacityUnits());
        assertTrue(gsi3.get("gsi3").capacityUnits() == gsiTotal.get("gsi3").capacityUnits());

        assertTrue(lsi.get("lsi1").capacityUnits() * 2 == lsiTotal.get("lsi1").capacityUnits());
        assertTrue(lsi.get("lsi2").capacityUnits() * 2 == lsiTotal.get("lsi2").capacityUnits());
        assertTrue(lsi3.get("lsi3").capacityUnits() == lsiTotal.get("lsi3").capacityUnits());

        // An empty one
        QueryResult result4 = QueryResult.builder_().build_();
        ConsumedCapacity consumedCapacity4 = ConsumedCapacity.builder_().build_();
        result4 = result4.toBuilder().consumedCapacity(consumedCapacity4).build_();
        col.setLastLowLevelResult(new QueryOutcome(result4));

        // all assertions are expected to be the same as the last set of assertions
        assertTrue(result.count() * 2 + result3.count() == col.getTotalCount());
        assertTrue(result.scannedCount() * 2 + result3.scannedCount() == col.getTotalScannedCount());

        total = col.getTotalConsumedCapacity();
        assertTrue(total.capacityUnits() ==
                   2 * consumedCapacity.capacityUnits()
                   + consumedCapacity3.capacityUnits());

        gsiTotal = total.globalSecondaryIndexes();
        lsiTotal = total.localSecondaryIndexes();
        assertTrue(3 == gsiTotal.size());
        assertTrue(3 == lsiTotal.size());

        assertTrue(gsi.get("gsi1").capacityUnits() * 2 == gsiTotal.get("gsi1").capacityUnits());
        assertTrue(gsi.get("gsi2").capacityUnits() * 2 == gsiTotal.get("gsi2").capacityUnits());
        assertTrue(gsi3.get("gsi3").capacityUnits() == gsiTotal.get("gsi3").capacityUnits());

        assertTrue(lsi.get("lsi1").capacityUnits() * 2 == lsiTotal.get("lsi1").capacityUnits());
        assertTrue(lsi.get("lsi2").capacityUnits() * 2 == lsiTotal.get("lsi2").capacityUnits());
        assertTrue(lsi3.get("lsi3").capacityUnits() == lsiTotal.get("lsi3").capacityUnits());
    }
}
