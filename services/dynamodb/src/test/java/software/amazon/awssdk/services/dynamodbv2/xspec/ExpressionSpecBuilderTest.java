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

package software.amazon.awssdk.services.dynamodbv2.xspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.attributeExists;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.b;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.bool;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.bs;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ifNotExists;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.l;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.m;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.n;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ns;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.null0;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.s;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ss;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;

public class ExpressionSpecBuilderTest {

    private void p(Object o) {
        System.out.println(String.valueOf(o));
    }

    @Test
    public void test() {
        ExpressionSpecBuilder builder = new ExpressionSpecBuilder();
        UpdateItemExpressionSpec xspec = builder
                // SET num1 = num1 + 20
                .addUpdate(
                        n("num1").set(n("num1").plus(20)))
                // SET string-attr = "string-value"
                .addUpdate(
                        s("string-attr").set("string-value")
                          )
                // num BETWEEN 0 AND 100
                .withCondition(
                        n("num").between(0, 100)
                              ).buildForUpdate();
        String c = xspec.getConditionExpression();
        String u = xspec.getUpdateExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        p(c);
        p(u);
        p(nm);
        p(vm);

        ExpressionSpecBuilder builderClone = builder.clone();
        UpdateItemExpressionSpec xspec2 = builderClone.buildForUpdate();

        assertEquals(xspec2.getConditionExpression(), xspec.getConditionExpression());
        assertEquals(xspec2.getUpdateExpression(), xspec.getUpdateExpression());
        assertEquals(xspec2.getNameMap(), xspec.getNameMap());
        assertEquals(xspec2.getValueMap(), xspec.getValueMap());

        ExpressionSpecBuilder builderClone2 = builder.clone();
        builderClone2.addUpdate(null0("nullAttr").set());

        UpdateItemExpressionSpec xspec3 = builderClone2.buildForUpdate();
        assertFalse(xspec3.getUpdateExpression().equals(xspec.getUpdateExpression()));
        assertFalse(xspec3.getNameMap().equals(xspec.getNameMap()));
        assertFalse(xspec3.getValueMap().equals(xspec.getValueMap()));
    }

    @Test
    public void testIfNotExists() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(b("binaryAttr").set(
                        ifNotExists("binaryAttr", "testing".getBytes())))
                .addUpdate(bs("bsAttr").set(
                        ifNotExists("bsAttr", "a".getBytes(), "b".getBytes())))
                .addUpdate(bool("boolAttr").set(
                        ifNotExists("boolAttr", true)))
                .addUpdate(l("listAttr").set(
                        ifNotExists("listAttr", Arrays.asList("foo", "bar"))))
                .addUpdate(m("mapAttr").set(
                        ifNotExists("mapAttr", new ValueMap().with("foo", "far"))))
                .addUpdate(n("numericAttr").set(ifNotExists("numericAttr", 123)))
                .addUpdate(ns("nsAttr").set(ifNotExists("nsAttr", 123, 456)))
                .addUpdate(s("stringAttr").set(ifNotExists("stringAttr", "foo")))
                .addUpdate(ss("ssAttr").set(ifNotExists("ssAttr", "foo", "bar")))
                .addUpdate(b("binaryAttr2").set(ifNotExists("binaryAttr2",
                                                              ByteBuffer.wrap("bar".getBytes()))))
                .addUpdate(bs("bytebufferSetAttr").set(ifNotExists("bytebufferSetAttr",
                                                                     ByteBuffer.wrap("a".getBytes()),
                                                                     ByteBuffer.wrap("b".getBytes()))))
                .buildForUpdate();

        //          p(xspec.getUpdateExpression());
        //          p(xspec.getNameMap());
        //          p(xspec.getValueMap());

        assertEquals("SET #0 = if_not_exists(#0,:0), #1 = if_not_exists(#1,:1), #2 = if_not_exists(#2,:2), #3 = if_not_exists(#3,:3), #4 = if_not_exists(#4,:4), #5 = if_not_exists(#5,:5), #6 = if_not_exists(#6,:6), #7 = if_not_exists(#7,:7), #8 = if_not_exists(#8,:8), #9 = if_not_exists(#9,:9), #10 = if_not_exists(#10,:10)",
                     xspec.getUpdateExpression());
        assertEquals("{#0=binaryAttr, #1=bsAttr, #2=boolAttr, #3=listAttr, #4=mapAttr, #5=numericAttr, #6=nsAttr, #7=stringAttr, #8=ssAttr, #9=binaryAttr2, #10=bytebufferSetAttr}",
                     xspec.getNameMap().toString());
        assertTrue(11 == xspec.getValueMap().size());
    }

    @Test
    public void testMisc() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(null0("nullAttr").remove())
                .withCondition(attributeExists("someAttr"))
                .buildForUpdate();

        //          p(xspec.getConditionExpression());
        //          p(xspec.getUpdateExpression());
        //          p(xspec.getNameMap());
        //          p(xspec.getValueMap());

        assertEquals("attribute_exists(#1)", xspec.getConditionExpression());
        assertEquals("REMOVE #0", xspec.getUpdateExpression());
        assertEquals("{#0=nullAttr, #1=someAttr}",
                     xspec.getNameMap().toString());
        assertNull(xspec.getValueMap());
    }
}
