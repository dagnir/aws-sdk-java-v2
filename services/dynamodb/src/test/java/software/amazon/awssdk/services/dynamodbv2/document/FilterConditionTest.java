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

package software.amazon.awssdk.services.dynamodbv2.document;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodbv2.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodbv2.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodbv2.model.Condition;

/**
 * Covers ScanFilter, which shares the same underlying implementation as QueryFilter.
 */
public class FilterConditionTest {

    private static Entry<String, Condition> toAttributeCondition(ScanFilter ScanFilter) {
        Map<String, Condition> map = InternalUtils
                .toAttributeConditionMap(Arrays.asList(ScanFilter));
        Assert.assertEquals(1, map.size());

        Iterator<Entry<String, Condition>> iter = map.entrySet().iterator();
        return iter.next();
    }

    @Test
    public void testScanFilter_EQ() {
        ScanFilter ScanFilter = new ScanFilter("foo").eq("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.EQ.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());

        ScanFilter = new ScanFilter("foo").eq(null);
        ddbScanFilter = toAttributeCondition(ScanFilter);
        ddbScanFilter_attrName = ddbScanFilter.getKey();
        ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.EQ.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals(true, ddbScanFilter_value.getAttributeValueList().get(0).getNULL());
    }

    @Test
    public void testScanFilter_NE() {
        ScanFilter ScanFilter = new ScanFilter("foo").ne("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NE.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_EXISTS() {
        ScanFilter ScanFilter = new ScanFilter("foo").exists();
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_NULL.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(null, ddbScanFilter_value.getAttributeValueList());
    }

    @Test
    public void testScanFilter_NOTEXISTS() {
        ScanFilter ScanFilter = new ScanFilter("foo").notExist();
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NULL.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(null, ddbScanFilter_value.getAttributeValueList());
    }

    @Test
    public void testScanFilter_CONTAINS() {
        ScanFilter ScanFilter = new ScanFilter("foo").contains("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.CONTAINS.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_NOTCONTAINS() {
        ScanFilter ScanFilter = new ScanFilter("foo").notContains("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_CONTAINS.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_BEGINSWITH() {
        ScanFilter ScanFilter = new ScanFilter("foo").beginsWith("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.BEGINS_WITH.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_IN() {
        // Single value
        ScanFilter ScanFilter = new ScanFilter("foo").in("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.IN.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());

        // Multi-value
        ScanFilter = new ScanFilter("foo").in("bar", "charlie", null);
        ddbScanFilter = toAttributeCondition(ScanFilter);
        ddbScanFilter_attrName = ddbScanFilter.getKey();
        ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(3, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals("charlie", ddbScanFilter_value.getAttributeValueList().get(1).getS());
        Assert.assertEquals(true, ddbScanFilter_value.getAttributeValueList().get(2).getNULL());
        Assert.assertEquals(ComparisonOperator.IN.toString(), ddbScanFilter_value.getComparisonOperator());

        // Null values
        try {
            ScanFilter = new ScanFilter("foo").in((Object[]) null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }

        // Empty values
        try {
            ScanFilter = new ScanFilter("foo").in();
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testScanFilter_BETWEEN() {
        ScanFilter ScanFilter = new ScanFilter("foo").between(0, 100);
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(2, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("0", ddbScanFilter_value.getAttributeValueList().get(0).getN());
        Assert.assertEquals("100", ddbScanFilter_value.getAttributeValueList().get(1).getN());
        Assert.assertEquals(ComparisonOperator.BETWEEN.toString(), ddbScanFilter_value.getComparisonOperator());
    }

    @Test
    public void testScanFilter_GE() {
        ScanFilter ScanFilter = new ScanFilter("foo").ge("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.GE.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_GT() {
        ScanFilter ScanFilter = new ScanFilter("foo").gt("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.GT.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_LE() {
        ScanFilter ScanFilter = new ScanFilter("foo").le("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.LE.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_LT() {
        ScanFilter ScanFilter = new ScanFilter("foo").lt("bar");
        Entry<String, Condition> ddbScanFilter = toAttributeCondition(ScanFilter);
        String ddbScanFilter_attrName = ddbScanFilter.getKey();
        Condition ddbScanFilter_value = ddbScanFilter.getValue();

        Assert.assertEquals("foo", ddbScanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.LT.toString(), ddbScanFilter_value.getComparisonOperator());
        Assert.assertEquals(1, ddbScanFilter_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbScanFilter_value.getAttributeValueList().get(0).getS());
    }

    @Test
    public void testScanFilter_EmptyAttributeName() {
        try {
            new ScanFilter(null);
            Assert.fail();
        } catch (IllegalArgumentException ScanFilter) {
        }

        try {
            new ScanFilter("");
            Assert.fail();
        } catch (IllegalArgumentException ScanFilter) {
        }
    }

    @Test
    public void testScanFilter_DuplicateAttribute() {
        Table fakeTable = new Table(new AmazonDynamoDBClient(), "fake-table");
        try {
            fakeTable.scan(
                    new ScanFilter("foo").eq("bar"),
                    new ScanFilter("foo").eq("charlie"));
            Assert.fail();
        } catch (IllegalArgumentException ScanFilter) {
        }
    }
}
