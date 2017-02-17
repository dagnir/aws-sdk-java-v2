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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.b;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.bool;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.bs;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.l;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.m;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.n;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ns;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.null0;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.s;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ss;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.TreeSet;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.utils.FluentHashSet;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;

public class PathOperandTest {

    private void p(Object o) {
        System.out.println(String.valueOf(o));
    }

    @Test
    public void B_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        b("binaryAttr").in(
                                "bb1".getBytes(),
                                "bb2".getBytes())
                                       .or(b("binaryAttr").in(
                                               ByteBuffer.wrap("bb3".getBytes()),
                                               ByteBuffer.wrap("bb4".getBytes())))
                                       .or(b("binaryAttr").inByteBufferList(Arrays.asList(
                                               ByteBuffer.wrap("bb5".getBytes()),
                                               ByteBuffer.wrap("bb6".getBytes()))))
                                       .or(b("binaryAttr").inBytesList(Arrays.asList(
                                               "bb7".getBytes(),
                                               "bb8".getBytes())))
                                       .or(b("binaryAttr").eq(ByteBuffer.wrap("bb9".getBytes())))
                                       .or(b("binaryAttr").eq(b("binaryAttr2")))
                                       .or(b("binaryAttr").ne(ByteBuffer.wrap("bb10".getBytes())))
                                       .or(b("binaryAttr").ne(b("binaryAttr3"))))
                .buildForQuery();
        assertEquals(
                "#0 IN (:0, :1) OR #0 IN (:2, :3) OR #0 IN (:4, :5) OR #0 IN (:6, :7) OR #0 = :8 OR #0 = #1 OR #0 <> :9 OR #0 <> #2",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=binaryAttr, #1=binaryAttr2, #2=binaryAttr3}",
                     xspec.getNameMap().toString());
        assertTrue(10 == xspec.getValueMap().size());
    }

    @Test
    public void B_ifNotExists() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(b("binaryAttr").set(
                        b("binaryAttr").ifNotExists("defaultValue".getBytes())))
                .addUpdate(b("binaryAttr2").set(
                        b("binaryAttr2").ifNotExists(
                                ByteBuffer.wrap("defaultValue".getBytes()))))
                .addUpdate(b("binaryAttr3").set(
                        b("binaryAttr3").ifNotExists(b("binaryAttr4"))))
                .buildForUpdate();
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "SET #0 = if_not_exists(#0,:0), #1 = if_not_exists(#1,:1), #2 = if_not_exists(#2,#3)",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=binaryAttr, #1=binaryAttr2, #2=binaryAttr3, #3=binaryAttr4}",
                xspec.getNameMap().toString());
        assertTrue(2 == xspec.getValueMap().size());
    }

    @Test
    public void BOOL_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        bool("boolAttr").in(
                                true,
                                false)
                                        .or(bool("boolAttr").in(Arrays.asList(true)))
                                        .or(bool("boolAttr").eq(true))
                                        .or(bool("boolAttr").ne(false))
                                        .or(bool("boolAttr").eq(bool("boolAttr2")))
                                        .or(bool("boolAttr").ne(bool("boolAttr3"))))
                .buildForQuery();
        assertEquals(
                "#0 IN (:0, :1) OR #0 IN (:0) OR #0 = :0 OR #0 <> :1 OR #0 = #1 OR #0 <> #2",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=boolAttr, #1=boolAttr2, #2=boolAttr3}",
                     xspec.getNameMap().toString());
        assertTrue(2 == xspec.getValueMap().size());
    }

    @Test
    public void BOOL_ifNotExists() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(bool("boolAttr").set(
                        bool("boolAttr").ifNotExists(false)))
                .addUpdate(bool("boolAttr3").set(
                        bool("binaryAttr3").ifNotExists(bool("boolAttr4"))))
                .buildForUpdate();
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "SET #0 = if_not_exists(#0,:0), #1 = if_not_exists(#2,#3)",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=boolAttr, #1=boolAttr3, #2=binaryAttr3, #3=boolAttr4}",
                xspec.getNameMap().toString());
        assertTrue(1 == xspec.getValueMap().size());
    }

    @Test
    public void N_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        n("nAttr").in(1, 2, 3)
                                  .or(n("nAttr").in(Arrays.asList(1, 2, 3)))
                                  .or(n("nAttr").ne(n("nAttr2")))
                                  .or(n("nAttr").le(-1))
                                  .or(n("nAttr").le(n("nAttr2")))
                                  .or(n("nAttr").lt(n("nAttr3")))
                                  .or(n("nAttr").gt(n("nAttr4")))
                                  .or(n("nAttr").ge(n("nAttr5")))
                                  .or(n("nAttr").ge(9)))
                .buildForQuery();
        //        p(xspec.getKeyConditionExpression());
        //        p(xspec.getNameMap());
        //        p(xspec.getValueMap());
        assertEquals(
                "#0 IN (:0, :1, :2) OR #0 IN (:0, :1, :2) OR #0 <> #1 OR #0 <= :3 OR #0 <= #1 OR #0 < #2 OR #0 > #3 OR #0 >= #4 OR #0 >= :4",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=nAttr, #1=nAttr2, #2=nAttr3, #3=nAttr4, #4=nAttr5}",
                     xspec.getNameMap().toString());
        assertTrue(5 == xspec.getValueMap().size());
    }

    @Test
    public void N_ifNotExists() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(n("nAttr").set(
                        n("nAttr2").ifNotExists(0)))
                .addUpdate(n("nAttr1").set(
                        n("nAttr1").ifNotExists(n("nAttr2"))))
                .addUpdate(n("nAttr3").set(
                        n("nAttr3").plus(n("nAttr4"))))
                .addUpdate(n("nAttr5").set(
                        n("nAttr5").minus(n("nAttr6"))))
                .buildForUpdate();
        assertNull(xspec.getConditionExpression());
        //        p(xspec.getUpdateExpression());
        //        p(xspec.getNameMap());
        //        p(xspec.getValueMap());
        assertEquals(
                "SET #0 = if_not_exists(#1,:0), #2 = if_not_exists(#2,#1), #3 = #3 + #4, #5 = #5 - #6",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=nAttr, #1=nAttr2, #2=nAttr1, #3=nAttr3, #4=nAttr4, #5=nAttr5, #6=nAttr6}",
                xspec.getNameMap().toString());
        assertTrue(1 == xspec.getValueMap().size());
    }

    @Test
    public void N_UpdateExpressions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(n("targetAttr").set(n("sourceAttr")))
                .buildForUpdate();
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "SET #0 = #1",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=targetAttr, #1=sourceAttr}",
                xspec.getNameMap().toString());
        assertNull(xspec.getValueMap());
    }

    @Test
    public void S_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        s("sAttr").in("s1", "s2")
                                  .or(s("sAttr").in(Arrays.asList("s7", "s8")))
                                  .or(s("sAttr").beginsWith("prefix"))
                                  .or(s("sAttr").eq("foo"))
                                  .or(s("sAttr").eq(s("sAttr2")))
                                  .or(s("sAttr").ne("bar"))
                                  .or(s("sAttr").ne(s("sAttr3"))))
                .buildForQuery();
        //        p(xspec.getKeyConditionExpression());
        //        p(xspec.getNameMap());
        //        p(xspec.getValueMap());
        assertEquals(
                "#0 IN (:0, :1) OR #0 IN (:2, :3) OR begins_with(#0, :4) OR #0 = :5 OR #0 = #1 OR #0 <> :6 OR #0 <> #2",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=sAttr, #1=sAttr2, #2=sAttr3}",
                     xspec.getNameMap().toString());
        assertTrue(7 == xspec.getValueMap().size());
    }

    @Test
    public void S_ifNotExists() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(s("sAttr").set(
                        s("sAttr").ifNotExists("defaultValue")))
                .addUpdate(s("sAttr1").set(
                        s("sAttr1").ifNotExists(s("sAttr2"))))

                .withCondition(s("sAttr3").notExists())
                .buildForUpdate();
        //        p(xspec.getConditionExpression());
        //        p(xspec.getUpdateExpression());
        //        p(xspec.getNameMap());
        //        p(xspec.getValueMap());

        assertEquals("attribute_not_exists(#3)",
                     xspec.getConditionExpression());
        assertEquals(
                "SET #0 = if_not_exists(#0,:0), #1 = if_not_exists(#1,#2)",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=sAttr, #1=sAttr1, #2=sAttr2, #3=sAttr3}",
                xspec.getNameMap().toString());
        assertTrue(1 == xspec.getValueMap().size());
    }

    @Test
    public void S_UpdateExpressions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(s("targetAttr").set(s("sourceAttr")))
                .buildForUpdate();
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "SET #0 = #1",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=targetAttr, #1=sourceAttr}",
                xspec.getNameMap().toString());
        assertNull(xspec.getValueMap());
    }

    @Test
    public void SS_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        ss("ssAttr").eq("foo", "bar")
                                    .or(ss("ssAttr").eq(
                                            new TreeSet<String>(Arrays.asList("foo1", "bar1"))))
                                    .or(ss("ssAttr").eq(ss("ssAttr2")))
                                    .or(ss("ssAttr").ne("bar", "baz"))
                                    .or(ss("ssAttr").ne(
                                            new TreeSet<String>(Arrays.asList("bar1", "baz1"))))
                                    .or(ss("ssAttr").ne(ss("ssAttr3")))
                                    .or(ss("ssAttr").contains("alpha")))
                .buildForQuery();
        assertEquals(
                "#0 = :0 OR #0 = :1 OR #0 = #1 OR #0 <> :2 OR #0 <> :3 OR #0 <> #2 OR contains(#0, :4)",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=ssAttr, #1=ssAttr2, #2=ssAttr3}",
                     xspec.getNameMap().toString());
        assertTrue(5 == xspec.getValueMap().size());
    }

    @Test
    public void SS_UpdateExpressions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(ss("ssTarget1").set(ss("ssSource1")))
                .addUpdate(ss("ssTarget2").set(new FluentHashSet<String>("foo", "bar")))
                .addUpdate(ss("ssAttr4").append("a", "b"))
                .addUpdate(ss("ssAttr5").append(
                        new TreeSet<String>(Arrays.asList("a", "b"))))
                .addUpdate(ss("ssAttr6").set("foo", "bar"))
                .addUpdate(ss("ssAttr7").delete("foo", "bar"))
                .addUpdate(ss("ssAttr8").delete(
                        new TreeSet<String>(Arrays.asList("foo", "bar"))))
                .addUpdate(ss("ssAttr").set(
                        ss("ssAttr").ifNotExists("foo", "bar")))
                .addUpdate(ss("ssAttr3").set(
                        ss("ssAttr3").ifNotExists(ss("ssAttr4"))))
                .addUpdate(ss("ssAttr9").set(
                        ss("ssAttr9").ifNotExists(new FluentHashSet<String>("foo", "bar"))))
                .buildForUpdate();
        //        p(xspec.getUpdateExpression());
        //        p(xspec.getNameMap());
        //        p(xspec.getValueMap());
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "SET #0 = #1, #2 = :0, #3 = :0, #4 = if_not_exists(#4,:0), #5 = if_not_exists(#5,#6), #7 = if_not_exists(#7,:0) ADD #6 :1, #8 :1 DELETE #9 :0, #10 :0",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=ssTarget1, #1=ssSource1, #2=ssTarget2, #3=ssAttr6, #4=ssAttr, #5=ssAttr3, #6=ssAttr4, #7=ssAttr9, #8=ssAttr5, #9=ssAttr7, #10=ssAttr8}",
                xspec.getNameMap().toString());
        assertTrue(2 == xspec.getValueMap().size());
    }

    @Test
    public void NS_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        ns("nsAttr").eq(1, 2)
                                    .or(ns("nsAttr").eq(
                                            new TreeSet<Number>(Arrays.asList(1, 2))))
                                    .or(ns("nsAttr").eq(ns("nsAttr2")))
                                    .or(ns("nsAttr").ne(3, 4))
                                    .or(ns("nsAttr").ne(
                                            new TreeSet<Number>(Arrays.asList(3, 4))))
                                    .or(ns("nsAttr").ne(ns("nsAttr3")))
                                    .or(ns("nsAttr").contains(100)))
                .buildForQuery();
        assertEquals(
                "#0 = :0 OR #0 = :0 OR #0 = #1 OR #0 <> :1 OR #0 <> :1 OR #0 <> #2 OR contains(#0, :2)",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=nsAttr, #1=nsAttr2, #2=nsAttr3}",
                     xspec.getNameMap().toString());
        assertTrue(3 == xspec.getValueMap().size());
    }

    @Test
    public void NS_UpdateExpressions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(ns("nsAttr4").append(1, 2))
                .addUpdate(ns("nsAttr5").append(
                        new TreeSet<Number>(Arrays.asList(1, 2))))
                .addUpdate(ns("nsAttr6").set(1, 2))
                .addUpdate(ns("nsAttr7").delete(1, 2))
                .addUpdate(ns("nsAttr8").delete(
                        new TreeSet<Number>(Arrays.asList(3, 4))))
                .addUpdate(ns("nsAttr").set(
                        ns("nsAttr").ifNotExists(1, 2)))
                .addUpdate(ns("nsAttr3").set(
                        ns("nsAttr3").ifNotExists(ns("nsAttr4"))))
                .buildForUpdate();
        assertNull(xspec.getConditionExpression());
        //        System.out.println(xspec.getUpdateExpression());
        assertEquals(
                "ADD #0 :0, #1 :0 SET #2 = :0, #3 = if_not_exists(#3,:0), #4 = if_not_exists(#4,#0) DELETE #5 :0, #6 :1",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=nsAttr4, #1=nsAttr5, #2=nsAttr6, #3=nsAttr, #4=nsAttr3, #5=nsAttr7, #6=nsAttr8}",
                xspec.getNameMap().toString());
        assertTrue(2 == xspec.getValueMap().size());
    }

    @Test
    public void BS_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        bs("bsAttr").eq("foo".getBytes(), "bar".getBytes())
                                    .or(bs("bsAttr").eq(
                                            ByteBuffer.wrap("foo1".getBytes()),
                                            ByteBuffer.wrap("bar1".getBytes())))
                                    .or(bs("bsAttr").eq(bs("bsAttr2")))
                                    .or(bs("bsAttr").ne("bar".getBytes(), "baz".getBytes()))
                                    .or(bs("bsAttr").ne(
                                            ByteBuffer.wrap("bar1".getBytes()),
                                            ByteBuffer.wrap("baz1".getBytes())))
                                    .or(bs("bsAttr").ne(bs("bsAttr3")))
                                    .or(bs("bsAttr").contains("alpha".getBytes()))
                                    .or(bs("bsAttr").contains(ByteBuffer.wrap("beta".getBytes()))))
                .buildForQuery();
        assertEquals(
                "#0 = :0 OR #0 = :1 OR #0 = #1 OR #0 <> :2 OR #0 <> :3 OR #0 <> #2 OR contains(#0, :4) OR contains(#0, :5)",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=bsAttr, #1=bsAttr2, #2=bsAttr3}",
                     xspec.getNameMap().toString());
        assertTrue(6 == xspec.getValueMap().size());
    }

    @Test
    public void BS_UpdateExpressions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(bs("bsAttr4").append("a".getBytes(), "b".getBytes()))
                .addUpdate(bs("bsAttr5").append(
                        ByteBuffer.wrap("a".getBytes()),
                        ByteBuffer.wrap("b".getBytes())))
                .addUpdate(bs("bsAttr").set(
                        bs("bsAttr").ifNotExists("foo".getBytes(), "bar".getBytes())))
                .addUpdate(bs("bsAttr2").set(
                        bs("bsAttr2").ifNotExists(
                                ByteBuffer.wrap("foo".getBytes()),
                                ByteBuffer.wrap("bar".getBytes()))))
                .addUpdate(bs("ssAttr3").set(
                        bs("bsAttr3").ifNotExists(bs("bsAttr4"))))
                .addUpdate(bs("ssAttr6").delete("a".getBytes(), "b".getBytes()))
                .addUpdate(bs("ssAttr7").delete(
                        ByteBuffer.wrap("a".getBytes()),
                        ByteBuffer.wrap("b".getBytes())))
                .addUpdate(bs("ssAttr8").set("a".getBytes(), "b".getBytes()))
                .addUpdate(bs("ssAttr9").set(
                        ByteBuffer.wrap("a".getBytes()),
                        ByteBuffer.wrap("b".getBytes())))
                .buildForUpdate();
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "ADD #0 :0, #1 :1 SET #2 = if_not_exists(#2,:2), #3 = if_not_exists(#3,:3), #4 = if_not_exists(#5,#0), #6 = :4, #7 = :5 DELETE #8 :6, #9 :1",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=bsAttr4, #1=bsAttr5, #2=bsAttr, #3=bsAttr2, #4=ssAttr3, #5=bsAttr3, #6=ssAttr8, #7=ssAttr9, #8=ssAttr6, #9=ssAttr7}",
                xspec.getNameMap().toString());
        assertTrue(7 == xspec.getValueMap().size());
    }

    @Test
    public void NULL_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(null0("attr1").exists())
                .buildForQuery();
        System.out.println(xspec.getKeyConditionExpression());
        assertEquals("attribute_exists(#0)", xspec.getKeyConditionExpression());
        assertTrue(1 == xspec.getNameMap().size());
        assertNull(xspec.getValueMap());

        UpdateItemExpressionSpec uspec = new ExpressionSpecBuilder()
                .withKeyCondition(null0("attr1").exists())
                .addUpdate(null0("attr2").set())
                .buildForUpdate();
        assertEquals("attribute_exists(#0)", xspec.getKeyConditionExpression());
        assertTrue(1 == uspec.getNameMap().size());
        assertTrue(1 == uspec.getValueMap().size());
    }

    @Test
    public void L_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        l("listAttr").contains("foo")
                                     .or(l("listAttr").eq(l("listAttr2")))
                                     .or(l("listAttr").eq(Arrays.asList("foo", "bar")))
                                     .or(l("listAttr").eq(l("listAttr2").listAppend("a", "b")))
                                     .or(l("listAttr").eq(l("listAttr3").ifNotExists("c", "d")))

                                     .or(l("listAttr").ne(l("listAttr2")))
                                     .or(l("listAttr").ne(Arrays.asList("foo", "bar")))
                                     .or(l("listAttr").ne(l("listAttr2").listAppend("a", "b")))
                                     .or(l("listAttr").ne(l("listAttr3").ifNotExists("c", "d")))
                                 ).buildForQuery();
        //          p(xspec.getKeyConditionExpression());
        //          p(xspec.getNameMap());
        //          p(xspec.getValueMap());
        assertEquals(
                "contains(#0, :0) OR #0 = #1 OR #0 = :1 OR #0 = list_append(#1, :2) OR #0 = if_not_exists(#2,:3) OR #0 <> #1 OR #0 <> :1 OR #0 <> list_append(#1, :2) OR #0 <> if_not_exists(#2,:3)",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=listAttr, #1=listAttr2, #2=listAttr3}",
                     xspec.getNameMap().toString());
        assertTrue(4 == xspec.getValueMap().size());
    }

    @Test
    public void L_UpdateExpressions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(l("listTarget1").set(l("listSource1")))
                .addUpdate(l("listTarget2").set(Arrays.asList("a", "b")))

                .addUpdate(l("listAttr4").set(
                        l("listAttr4").listAppend(Arrays.asList("a", "b"))))
                .addUpdate(l("listAttr5").set(
                        l("listAttr5").listAppend("a", "b")))
                .addUpdate(l("listAttr5").set(
                        l("listAttr6").listAppend(l("listAttr7"))))

                .addUpdate(ss("listAttr7").delete("foo", "bar"))
                .addUpdate(ss("listAttr8").delete(
                        new TreeSet<String>(Arrays.asList("foo", "bar"))))

                .addUpdate(l("listAttr9").set(
                        l("listAttr9").ifNotExists("foo", "bar")))
                .addUpdate(l("listAttr10").set(
                        l("listAttr10").ifNotExists(Arrays.asList("foo", "bar"))))
                .addUpdate(l("listAttr11").set(
                        l("listAttr11").ifNotExists(l("listAttr12"))))
                .buildForUpdate();
        p(xspec.getUpdateExpression());
        p(xspec.getNameMap());
        p(xspec.getValueMap());
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "SET #0 = #1, #2 = :0, #3 = list_append(#3, :0), #4 = list_append(#4, :0), #4 = list_append(#5, #6), #7 = if_not_exists(#7,:1), #8 = if_not_exists(#8,:1), #9 = if_not_exists(#9,#10) DELETE #6 :2, #11 :2",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=listTarget1, #1=listSource1, #2=listTarget2, #3=listAttr4, #4=listAttr5, #5=listAttr6, #6=listAttr7, #7=listAttr9, #8=listAttr10, #9=listAttr11, #10=listAttr12, #11=listAttr8}",
                xspec.getNameMap().toString());
        assertTrue(3 == xspec.getValueMap().size());
    }

    @Test
    public void M_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .withKeyCondition(
                        m("mapAttr").eq(m("mapAttr2"))
                                    .or(m("mapAttr").eq(m("mapAttr").ifNotExists(m("mapAttr1"))))
                                    .or(m("mapAttr2").eq(
                                            new ValueMap().with("foo", "bar")))
                                    .or(m("mapAttr3").eq(
                                            m("mapAttr4").ifNotExists(
                                                    new ValueMap().with("foo", "bar"))))

                                    .or(m("mapAttr5").ne(m("mapAttr").ifNotExists(m("mapAttr1"))))
                                    .or(m("mapAttr6").ne(
                                            new ValueMap().with("foo", "bar")))
                                    .or(m("mapAttr7").ne(
                                            m("mapAttr8").ifNotExists(
                                                    new ValueMap().with("foo", "bar"))))
                                    .or(m("mapAttr9").ne(m("mapAttr10")))
                                 ).buildForQuery();
        //          p(xspec.getKeyConditionExpression());
        //          p(xspec.getNameMap());
        //          p(xspec.getValueMap());
        assertEquals(
                "#0 = #1 OR #0 = if_not_exists(#0,#2) OR #1 = :0 OR #3 = if_not_exists(#4,:0) OR #5 <> if_not_exists(#0,#2) OR #6 <> :0 OR #7 <> if_not_exists(#8,:0) OR #9 <> #10",
                xspec.getKeyConditionExpression());
        assertNull(xspec.getFilterExpression());
        assertEquals("{#0=mapAttr, #1=mapAttr2, #2=mapAttr1, #3=mapAttr3, #4=mapAttr4, #5=mapAttr5, #6=mapAttr6, #7=mapAttr7, #8=mapAttr8, #9=mapAttr9, #10=mapAttr10}",
                     xspec.getNameMap().toString());
        assertTrue(1 == xspec.getValueMap().size());
    }

    @Test
    public void M_UpdateExpressions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addUpdate(m("mapTarget1").set(m("mapSource1")))
                .addUpdate(m("mapTarget2").set(new ValueMap().with("foo", "bar")))
                .addUpdate(m("mapTarget3").set(m("mapTarget3").ifNotExists(m("mapSource2"))))
                .buildForUpdate();
        //        p(xspec.getUpdateExpression());
        //        p(xspec.getNameMap());
        //        p(xspec.getValueMap());
        assertNull(xspec.getConditionExpression());
        assertEquals(
                "SET #0 = #1, #2 = :0, #3 = if_not_exists(#3,#4)",
                xspec.getUpdateExpression());
        assertEquals(
                "{#0=mapTarget1, #1=mapSource1, #2=mapTarget2, #3=mapTarget3, #4=mapSource2}",
                xspec.getNameMap().toString());
        assertTrue(1 == xspec.getValueMap().size());
    }
}
