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
        col.setLastLowLevelResult(new QueryOutcome(new QueryResult()));
        assertTrue(0 == col.getTotalCount());
        assertTrue(0 == col.getTotalScannedCount());
        assertNull(col.getTotalConsumedCapacity());
    }

    @Test
    public void setLastLowLevelResult() {
        QueryCollection col = new QueryCollection(null, new QuerySpec());
        QueryResult result = new QueryResult()
                .withCount(rand.nextInt())
                .withScannedCount(rand.nextInt());

        Map<String, Capacity> gsi = new HashMap<String, Capacity>();
        gsi.put("gsi1", new Capacity().withCapacityUnits(rand.nextDouble()));
        gsi.put("gsi2", new Capacity().withCapacityUnits(rand.nextDouble()));

        Map<String, Capacity> lsi = new HashMap<String, Capacity>();
        lsi.put("lsi1", new Capacity().withCapacityUnits(rand.nextDouble()));
        lsi.put("lsi2", new Capacity().withCapacityUnits(rand.nextDouble()));

        ConsumedCapacity consumedCapacity = new ConsumedCapacity()
                .withCapacityUnits(rand.nextDouble())
                .withTable(new Capacity().withCapacityUnits(rand.nextDouble()))
                .withTableName("tableName")
                .withGlobalSecondaryIndexes(gsi)
                .withLocalSecondaryIndexes(lsi);
        // Once
        result.setConsumedCapacity(consumedCapacity);
        col.setLastLowLevelResult(new QueryOutcome(result));

        assertTrue(result.getCount() == col.getTotalCount());
        assertTrue(result.getScannedCount() == col.getTotalScannedCount());

        ConsumedCapacity total = col.getTotalConsumedCapacity();
        assertNotSame(total, consumedCapacity);
        assertEquals(total, consumedCapacity);

        assertNotSame(gsi, total.getGlobalSecondaryIndexes());
        assertNotSame(lsi, total.getLocalSecondaryIndexes());

        // Twice
        col.setLastLowLevelResult(new QueryOutcome(result));

        assertTrue(result.getCount() * 2 == col.getTotalCount());
        assertTrue(result.getScannedCount() * 2 == col.getTotalScannedCount());

        total = col.getTotalConsumedCapacity();
        assertTrue(total.getCapacityUnits() == 2 * consumedCapacity.getCapacityUnits());

        Map<String, Capacity> gsiTotal = total.getGlobalSecondaryIndexes();
        Map<String, Capacity> lsiTotal = total.getLocalSecondaryIndexes();
        assertTrue(2 == gsiTotal.size());
        assertTrue(2 == lsiTotal.size());

        assertTrue(gsi.get("gsi1").getCapacityUnits() * 2 == gsiTotal.get("gsi1").getCapacityUnits());
        assertTrue(gsi.get("gsi2").getCapacityUnits() * 2 == gsiTotal.get("gsi2").getCapacityUnits());

        assertTrue(lsi.get("lsi1").getCapacityUnits() * 2 == lsiTotal.get("lsi1").getCapacityUnits());
        assertTrue(lsi.get("lsi2").getCapacityUnits() * 2 == lsiTotal.get("lsi2").getCapacityUnits());

        // A different one
        QueryResult result3 = new QueryResult()
                .withCount(rand.nextInt())
                .withScannedCount(rand.nextInt());

        Map<String, Capacity> gsi3 = new HashMap<String, Capacity>();
        gsi3.put("gsi3", new Capacity().withCapacityUnits(rand.nextDouble()));

        Map<String, Capacity> lsi3 = new HashMap<String, Capacity>();
        lsi3.put("lsi3", new Capacity().withCapacityUnits(rand.nextDouble()));

        ConsumedCapacity consumedCapacity3 = new ConsumedCapacity()
                .withCapacityUnits(rand.nextDouble())
                .withTable(new Capacity().withCapacityUnits(rand.nextDouble()))
                .withTableName("tableName")
                .withGlobalSecondaryIndexes(gsi3)
                .withLocalSecondaryIndexes(lsi3);
        result3.setConsumedCapacity(consumedCapacity3);
        col.setLastLowLevelResult(new QueryOutcome(result3));

        assertTrue(result.getCount() * 2 + result3.getCount() == col.getTotalCount());
        assertTrue(result.getScannedCount() * 2 + result3.getScannedCount() == col.getTotalScannedCount());

        total = col.getTotalConsumedCapacity();
        assertTrue(total.getCapacityUnits() ==
                   2 * consumedCapacity.getCapacityUnits()
                   + consumedCapacity3.getCapacityUnits());

        gsiTotal = total.getGlobalSecondaryIndexes();
        lsiTotal = total.getLocalSecondaryIndexes();
        assertTrue(3 == gsiTotal.size());
        assertTrue(3 == lsiTotal.size());

        assertTrue(gsi.get("gsi1").getCapacityUnits() * 2 == gsiTotal.get("gsi1").getCapacityUnits());
        assertTrue(gsi.get("gsi2").getCapacityUnits() * 2 == gsiTotal.get("gsi2").getCapacityUnits());
        assertTrue(gsi3.get("gsi3").getCapacityUnits() == gsiTotal.get("gsi3").getCapacityUnits());

        assertTrue(lsi.get("lsi1").getCapacityUnits() * 2 == lsiTotal.get("lsi1").getCapacityUnits());
        assertTrue(lsi.get("lsi2").getCapacityUnits() * 2 == lsiTotal.get("lsi2").getCapacityUnits());
        assertTrue(lsi3.get("lsi3").getCapacityUnits() == lsiTotal.get("lsi3").getCapacityUnits());

        // An empty one
        QueryResult result4 = new QueryResult();
        ConsumedCapacity consumedCapacity4 = new ConsumedCapacity();
        result4.setConsumedCapacity(consumedCapacity4);
        col.setLastLowLevelResult(new QueryOutcome(result4));

        // all assertions are expected to be the same as the last set of assertions
        assertTrue(result.getCount() * 2 + result3.getCount() == col.getTotalCount());
        assertTrue(result.getScannedCount() * 2 + result3.getScannedCount() == col.getTotalScannedCount());

        total = col.getTotalConsumedCapacity();
        assertTrue(total.getCapacityUnits() ==
                   2 * consumedCapacity.getCapacityUnits()
                   + consumedCapacity3.getCapacityUnits());

        gsiTotal = total.getGlobalSecondaryIndexes();
        lsiTotal = total.getLocalSecondaryIndexes();
        assertTrue(3 == gsiTotal.size());
        assertTrue(3 == lsiTotal.size());

        assertTrue(gsi.get("gsi1").getCapacityUnits() * 2 == gsiTotal.get("gsi1").getCapacityUnits());
        assertTrue(gsi.get("gsi2").getCapacityUnits() * 2 == gsiTotal.get("gsi2").getCapacityUnits());
        assertTrue(gsi3.get("gsi3").getCapacityUnits() == gsiTotal.get("gsi3").getCapacityUnits());

        assertTrue(lsi.get("lsi1").getCapacityUnits() * 2 == lsiTotal.get("lsi1").getCapacityUnits());
        assertTrue(lsi.get("lsi2").getCapacityUnits() * 2 == lsiTotal.get("lsi2").getCapacityUnits());
        assertTrue(lsi3.get("lsi3").getCapacityUnits() == lsiTotal.get("lsi3").getCapacityUnits());
    }
}
