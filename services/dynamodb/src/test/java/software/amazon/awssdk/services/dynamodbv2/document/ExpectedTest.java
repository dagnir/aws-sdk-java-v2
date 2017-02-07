/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
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
import software.amazon.awssdk.services.dynamodbv2.model.ExpectedAttributeValue;

public class ExpectedTest {

    @Test
    public void testExpected_EQ() {
        Expected expected = new Expected("foo").eq("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.EQ.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());

        expected = new Expected("foo").eq(null);
        ddbExpected = toExpectedAttributeValue(expected);
        ddbExpected_attrName = ddbExpected.getKey();
        ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.EQ.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals(true, ddbExpected_value.getAttributeValueList().get(0).getNULL());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_NE() {
        Expected expected = new Expected("foo").ne("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NE.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_EXISTS() {
        Expected expected = new Expected("foo").exists();
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_NULL.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.getAttributeValueList());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_NOTEXISTS() {
        Expected expected = new Expected("foo").notExist();
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NULL.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.getAttributeValueList());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_CONTAINS() {
        Expected expected = new Expected("foo").contains("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.CONTAINS.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_NOTCONTAINS() {
        Expected expected = new Expected("foo").notContains("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_CONTAINS.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_BEGINSWITH() {
        Expected expected = new Expected("foo").beginsWith("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.BEGINS_WITH.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_IN() {
        // Single value
        Expected expected = new Expected("foo").in("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.IN.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());

        // Multi-value
        expected = new Expected("foo").in("bar", "charlie", null);
        ddbExpected = toExpectedAttributeValue(expected);
        ddbExpected_attrName = ddbExpected.getKey();
        ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(3, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals("charlie", ddbExpected_value.getAttributeValueList().get(1).getS());
        Assert.assertEquals(true, ddbExpected_value.getAttributeValueList().get(2).getNULL());
        Assert.assertEquals(ComparisonOperator.IN.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());

        // Null values
        try {
            expected = new Expected("foo").in((Object[])null);
            Assert.fail();
        } catch (IllegalArgumentException e) {}

        // Empty values
        try {
            expected = new Expected("foo").in();
            Assert.fail();
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void testExpected_BETWEEN() {
        Expected expected = new Expected("foo").between(0, 100);
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(2, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("0", ddbExpected_value.getAttributeValueList().get(0).getN());
        Assert.assertEquals("100", ddbExpected_value.getAttributeValueList().get(1).getN());
        Assert.assertEquals(ComparisonOperator.BETWEEN.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_GE() {
        Expected expected = new Expected("foo").ge("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.GE.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_GT() {
        Expected expected = new Expected("foo").gt("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.GT.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_LE() {
        Expected expected = new Expected("foo").le("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.LE.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_LT() {
        Expected expected = new Expected("foo").lt("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.LT.toString(), ddbExpected_value.getComparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.getAttributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.getAttributeValueList().get(0).getS());
        Assert.assertEquals(null, ddbExpected_value.getValue());
        Assert.assertEquals(null, ddbExpected_value.getExists());
    }

    @Test
    public void testExpected_EmptyAttributeName() {
        try {
            new Expected(null);
            Assert.fail();
        } catch (IllegalArgumentException expected) {}

        try {
            new Expected("");
            Assert.fail();
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void testExpected_DuplicateAttribute() {
        Table fakeTable = new Table(new AmazonDynamoDBClient(), "fake-table");
        try {
            fakeTable.putItem(new Item(),
                    new Expected("foo").eq("bar"),
                    new Expected("foo").eq("charlie"));
            Assert.fail();
        } catch (IllegalArgumentException expected) {}
    }

    private static Entry<String, ExpectedAttributeValue> toExpectedAttributeValue(Expected expected) {
        Map<String, ExpectedAttributeValue> map = InternalUtils
                .toExpectedAttributeValueMap(Arrays.asList(expected));
        Assert.assertEquals(1, map.size());

        Iterator<Entry<String, ExpectedAttributeValue>> iter = map.entrySet().iterator();
        return iter.next();
    }
}
