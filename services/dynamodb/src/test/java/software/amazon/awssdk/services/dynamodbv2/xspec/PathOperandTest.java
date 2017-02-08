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

package software.amazon.awssdk.services.dynamodbv2.xspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.B;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.BOOL;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.BS;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.L;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.M;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.NS;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.NULL;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.SS;

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
                B("binaryAttr").in(
                    "bb1".getBytes(), 
                    "bb2".getBytes())
                .or(B("binaryAttr").in(
                    ByteBuffer.wrap("bb3".getBytes()), 
                    ByteBuffer.wrap("bb4".getBytes())))
                .or(B("binaryAttr").inByteBufferList(Arrays.asList(
                    ByteBuffer.wrap("bb5".getBytes()),
                    ByteBuffer.wrap("bb6".getBytes()))))
                .or(B("binaryAttr").inBytesList(Arrays.asList(
                    "bb7".getBytes(),
                    "bb8".getBytes())))
                .or(B("binaryAttr").eq(ByteBuffer.wrap("bb9".getBytes())))
                .or(B("binaryAttr").eq(B("binaryAttr2")))
                .or(B("binaryAttr").ne(ByteBuffer.wrap("bb10".getBytes())))
                .or(B("binaryAttr").ne(B("binaryAttr3"))))
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
            .addUpdate(B("binaryAttr").set(
                B("binaryAttr").ifNotExists("defaultValue".getBytes())))
            .addUpdate(B("binaryAttr2").set(
                B("binaryAttr2").ifNotExists(
                    ByteBuffer.wrap("defaultValue".getBytes()))))
            .addUpdate(B("binaryAttr3").set(
                B("binaryAttr3").ifNotExists(B("binaryAttr4"))))
            .buildForUpdate()
            ;
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
                BOOL("boolAttr").in(
                    true, 
                    false)
                .or(BOOL("boolAttr").in(Arrays.asList(true)))
                .or(BOOL("boolAttr").eq(true))
                .or(BOOL("boolAttr").ne(false))
                .or(BOOL("boolAttr").eq(BOOL("boolAttr2")))
                .or(BOOL("boolAttr").ne(BOOL("boolAttr3"))))
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
            .addUpdate(BOOL("boolAttr").set(
                    BOOL("boolAttr").ifNotExists(false)))
            .addUpdate(BOOL("boolAttr3").set(
                    BOOL("binaryAttr3").ifNotExists(BOOL("boolAttr4"))))
            .buildForUpdate()
            ;
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
                N("nAttr").in(1, 2, 3)
                .or(N("nAttr").in(Arrays.asList(1, 2, 3)))
                .or(N("nAttr").ne(N("nAttr2")))
                .or(N("nAttr").le(-1))
                .or(N("nAttr").le(N("nAttr2")))
                .or(N("nAttr").lt(N("nAttr3")))
                .or(N("nAttr").gt(N("nAttr4")))
                .or(N("nAttr").ge(N("nAttr5")))
                .or(N("nAttr").ge(9)))
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
            .addUpdate(N("nAttr").set(
                N("nAttr2").ifNotExists(0)))
            .addUpdate(N("nAttr1").set(
                N("nAttr1").ifNotExists(N("nAttr2"))))
            .addUpdate(N("nAttr3").set(
                N("nAttr3").plus(N("nAttr4"))))
            .addUpdate(N("nAttr5").set(
                N("nAttr5").minus(N("nAttr6"))))
            .buildForUpdate()
            ;
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
            .addUpdate(N("targetAttr").set(N("sourceAttr")))
            .buildForUpdate()
            ;
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
                S("sAttr").in("s1",  "s2")
                .or(S("sAttr").in(Arrays.asList("s7", "s8")))
                .or(S("sAttr").beginsWith("prefix"))
                .or(S("sAttr").eq("foo"))
                .or(S("sAttr").eq(S("sAttr2")))
                .or(S("sAttr").ne("bar"))
                .or(S("sAttr").ne(S("sAttr3"))))
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
            .addUpdate(S("sAttr").set(
                S("sAttr").ifNotExists("defaultValue")))
            .addUpdate(S("sAttr1").set(
                S("sAttr1").ifNotExists(S("sAttr2"))))

            .withCondition(S("sAttr3").notExists())
            .buildForUpdate()
            ;
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
            .addUpdate(S("targetAttr").set(S("sourceAttr")))
            .buildForUpdate()
            ;
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
                SS("ssAttr").eq("foo", "bar")
                .or(SS("ssAttr").eq(
                    new TreeSet<String>(Arrays.asList("foo1", "bar1"))))
                .or(SS("ssAttr").eq(SS("ssAttr2")))
                .or(SS("ssAttr").ne("bar", "baz"))
                .or(SS("ssAttr").ne(
                    new TreeSet<String>(Arrays.asList("bar1", "baz1"))))
                .or(SS("ssAttr").ne(SS("ssAttr3")))
                .or(SS("ssAttr").contains("alpha")))
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
            .addUpdate(SS("ssTarget1").set(SS("ssSource1")))
            .addUpdate(SS("ssTarget2").set(new FluentHashSet<String>("foo", "bar")))
            .addUpdate(SS("ssAttr4").append("a", "b"))
            .addUpdate(SS("ssAttr5").append(
                new TreeSet<String>(Arrays.asList("a", "b"))))
            .addUpdate(SS("ssAttr6").set("foo", "bar"))
            .addUpdate(SS("ssAttr7").delete("foo", "bar"))
            .addUpdate(SS("ssAttr8").delete(
                new TreeSet<String>(Arrays.asList("foo", "bar"))))
            .addUpdate(SS("ssAttr").set(
                    SS("ssAttr").ifNotExists("foo", "bar")))
            .addUpdate(SS("ssAttr3").set(
                    SS("ssAttr3").ifNotExists(SS("ssAttr4"))))
            .addUpdate(SS("ssAttr9").set(
                    SS("ssAttr9").ifNotExists(new FluentHashSet<String>("foo", "bar"))))
            .buildForUpdate()
            ;
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
                NS("nsAttr").eq(1, 2)
                .or(NS("nsAttr").eq(
                    new TreeSet<Number>(Arrays.asList(1, 2))))
                .or(NS("nsAttr").eq(NS("nsAttr2")))
                .or(NS("nsAttr").ne(3, 4))
                .or(NS("nsAttr").ne(
                    new TreeSet<Number>(Arrays.asList(3, 4))))
                .or(NS("nsAttr").ne(NS("nsAttr3")))
                .or(NS("nsAttr").contains(100)))
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
            .addUpdate(NS("nsAttr4").append(1, 2))
            .addUpdate(NS("nsAttr5").append(
                new TreeSet<Number>(Arrays.asList(1, 2))))
            .addUpdate(NS("nsAttr6").set(1, 2))
            .addUpdate(NS("nsAttr7").delete(1, 2))
            .addUpdate(NS("nsAttr8").delete(
                new TreeSet<Number>(Arrays.asList(3, 4))))
            .addUpdate(NS("nsAttr").set(
                    NS("nsAttr").ifNotExists(1, 2)))
            .addUpdate(NS("nsAttr3").set(
                    NS("nsAttr3").ifNotExists(NS("nsAttr4"))))
            .buildForUpdate()
            ;
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
                BS("bsAttr").eq("foo".getBytes(), "bar".getBytes())
                .or(BS("bsAttr").eq(
                    ByteBuffer.wrap("foo1".getBytes()), 
                    ByteBuffer.wrap("bar1".getBytes())))
                .or(BS("bsAttr").eq(BS("bsAttr2")))
                .or(BS("bsAttr").ne("bar".getBytes(), "baz".getBytes()))
                .or(BS("bsAttr").ne(
                    ByteBuffer.wrap("bar1".getBytes()),
                    ByteBuffer.wrap("baz1".getBytes())))
                .or(BS("bsAttr").ne(BS("bsAttr3")))
                .or(BS("bsAttr").contains("alpha".getBytes()))
                .or(BS("bsAttr").contains(ByteBuffer.wrap("beta".getBytes()))))
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
            .addUpdate(BS("bsAttr4").append("a".getBytes(), "b".getBytes()))
            .addUpdate(BS("bsAttr5").append(
                ByteBuffer.wrap("a".getBytes()),
                ByteBuffer.wrap("b".getBytes())))
            .addUpdate(BS("bsAttr").set(
                BS("bsAttr").ifNotExists("foo".getBytes(), "bar".getBytes())))
            .addUpdate(BS("bsAttr2").set(
                BS("bsAttr2").ifNotExists(
                    ByteBuffer.wrap("foo".getBytes()),
                    ByteBuffer.wrap("bar".getBytes()))))
            .addUpdate(BS("ssAttr3").set(
                BS("bsAttr3").ifNotExists(BS("bsAttr4"))))
            .addUpdate(BS("ssAttr6").delete("a".getBytes(), "b".getBytes()))
            .addUpdate(BS("ssAttr7").delete(
                ByteBuffer.wrap("a".getBytes()),
                ByteBuffer.wrap("b".getBytes())))
                .addUpdate(BS("ssAttr8").set("a".getBytes(), "b".getBytes()))
            .addUpdate(BS("ssAttr9").set(
                ByteBuffer.wrap("a".getBytes()),
                ByteBuffer.wrap("b".getBytes())))
            .buildForUpdate()
            ;
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
            .withKeyCondition(NULL("attr1").exists())
            .buildForQuery();
        System.out.println(xspec.getKeyConditionExpression());
        assertEquals("attribute_exists(#0)", xspec.getKeyConditionExpression());
        assertTrue(1 == xspec.getNameMap().size());
        assertNull(xspec.getValueMap());

        UpdateItemExpressionSpec uspec = new ExpressionSpecBuilder()
            .withKeyCondition(NULL("attr1").exists())
            .addUpdate(NULL("attr2").set())
            .buildForUpdate();
        assertEquals("attribute_exists(#0)", xspec.getKeyConditionExpression());
        assertTrue(1 == uspec.getNameMap().size());
        assertTrue(1 == uspec.getValueMap().size());
    }

    @Test
    public void L_conditions() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
            .withKeyCondition(
                L("listAttr").contains("foo")
                .or(L("listAttr").eq(L("listAttr2")))
                .or(L("listAttr").eq(Arrays.asList("foo", "bar")))
                .or(L("listAttr").eq( L("listAttr2").listAppend("a", "b") ))
                .or(L("listAttr").eq( L("listAttr3").ifNotExists("c", "d") ))

                .or(L("listAttr").ne(L("listAttr2")))
                .or(L("listAttr").ne(Arrays.asList("foo", "bar")))
                .or(L("listAttr").ne( L("listAttr2").listAppend("a", "b") ))
                .or(L("listAttr").ne( L("listAttr3").ifNotExists("c", "d") ))
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
            .addUpdate(L("listTarget1").set(L("listSource1")))
            .addUpdate(L("listTarget2").set(Arrays.asList("a", "b")))
            
            .addUpdate(L("listAttr4").set(
                    L("listAttr4").listAppend(Arrays.asList("a", "b"))))
            .addUpdate(L("listAttr5").set(
                    L("listAttr5").listAppend("a", "b")))
            .addUpdate(L("listAttr5").set(
                    L("listAttr6").listAppend(L("listAttr7"))))
            
            .addUpdate(SS("listAttr7").delete("foo", "bar"))
            .addUpdate(SS("listAttr8").delete(
                new TreeSet<String>(Arrays.asList("foo", "bar"))))

            .addUpdate(L("listAttr9").set(
                L("listAttr9").ifNotExists("foo", "bar")))
            .addUpdate(L("listAttr10").set(
                L("listAttr10").ifNotExists(Arrays.asList("foo", "bar"))))
            .addUpdate(L("listAttr11").set(
                L("listAttr11").ifNotExists(L("listAttr12"))))
            .buildForUpdate()
            ;
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
                M("mapAttr").eq(M("mapAttr2"))
                .or(M("mapAttr").eq( M("mapAttr").ifNotExists(M("mapAttr1") )))
                .or(M("mapAttr2").eq( 
                        new ValueMap().with("foo", "bar") ))
                .or(M("mapAttr3").eq( 
                    M("mapAttr4").ifNotExists( 
                        new ValueMap().with("foo", "bar") )))

                .or(M("mapAttr5").ne( M("mapAttr").ifNotExists(M("mapAttr1") )))
                .or(M("mapAttr6").ne( 
                        new ValueMap().with("foo", "bar") ))
                .or(M("mapAttr7").ne( 
                    M("mapAttr8").ifNotExists( 
                        new ValueMap().with("foo", "bar") )))
                .or(M("mapAttr9").ne( M("mapAttr10") ))
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
            .addUpdate(M("mapTarget1").set(M("mapSource1")))
            .addUpdate(M("mapTarget2").set(new ValueMap().with("foo", "bar")))
            .addUpdate(M("mapTarget3").set(M("mapTarget3").ifNotExists(M("mapSource2"))))
            .buildForUpdate()
            ;
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
