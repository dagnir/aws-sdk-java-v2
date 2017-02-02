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

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.not;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.parenthesize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.UpdateItemExpressionSpec;

public class Condition2Test {

    // (a = 1) AND b = 2 AND c = 3
    @Test
    public void case1() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            parenthesize( N("a").eq(1) ).and( N("b").eq(2).and(N("c").eq(3)) )
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("(#0 = :0) AND #1 = :1 AND #2 = :2", c);
    }

    // a = 1 OR b = 2 AND c = 3
    @Test
    public void case2() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").eq(1).or( N("b").eq(2).and(N("c").eq(3)) )
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("#0 = :0 OR #1 = :1 AND #2 = :2", c);
    }

    // (a = 1 OR b = 2) AND c = 3
    @Test
    public void case2a() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").eq(1).or(N("b").eq(2)).and(N("c").eq(3))
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("(#0 = :0 OR #1 = :1) AND #2 = :2", c);
    }

    // (a = 1)
    @Test
    public void case3() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            parenthesize(N("a").eq(1)) 
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("(#0 = :0)", c);
    }

    // (a = 1)
    @Test
    public void case4() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            parenthesize(parenthesize( N("a").eq(1) ))
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("(#0 = :0)", c);
    }

    // NOT (a == 10 AND b < 20)
    @Test
    public void case5() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            not( N("a").eq(10).and( N("b").lt(20) ))
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("NOT (#0 = :0 AND #1 < :1)", c);
    }

    // NOT a == 10 AND b < 20
    @Test
    public void case6() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            not( N("a").eq(10) ).and( N("b").lt(20) )
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("NOT #0 = :0 AND #1 < :1", c);
    }

    // a == 10 AND b < 20 AND c > 30
    @Test
    public void case7() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").eq(10).and( N("b").lt(20).and(N("c").gt(30) ))
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("#0 = :0 AND #1 < :1 AND #2 > :2", c);
    }

    // a == 10 AND (b < 20 OR c > 30)
    @Test
    public void case8() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").eq(10).and( N("b").lt(20).or(N("c").gt(30) ))
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("#0 = :0 AND (#1 < :1 OR #2 > :2)", c);
    }

    // a == 10 AND b < 20 OR c > 30
    @Test
    public void case9() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").eq(10).and(N("b").lt(20)).or(N("c").gt(30))
        ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);
        assertEquals("#0 = :0 AND #1 < :1 OR #2 > :2", c);
    }

    // a == 10 AND d = 40 AND (b < 20 OR c > 30) 
    @Test
    public void case10() {
        // Nesting structure: a == 10 AND (d = 40 AND (b < 20 OR c > 30))
        UpdateItemExpressionSpec exprs1 = new ExpressionSpecBuilder().withCondition(
            N("a").eq(10)
                .and(N("d").eq(40)
                    .and( N("b").lt(20).or(N("c").gt(30)) ))
        ).buildForUpdate();
        String c1 = exprs1.getConditionExpression();
        System.out.println("c1: " + c1);
        // Flat structure: a == 10 AND d = 40 AND (b < 20 OR c > 30)
        UpdateItemExpressionSpec exprs2 = new ExpressionSpecBuilder().withCondition(
                N("a").eq(10)
                    .and(N("d").eq(40))
                    .and(N("b").lt(20).or(N("c").gt(30)))
            ).buildForUpdate();
        String c2 = exprs2.getConditionExpression();
        System.out.println("c2: " + c2);
        // Both yield the same expression in the absence of explicit parenthesis
        assertEquals(c1, c2);
        assertEquals("#0 = :0 AND #1 = :1 AND (#2 < :2 OR #3 > :3)", c1);

        // Explicit parenthesis: a == 10 AND (d = 40 AND (b < 20 OR c > 30))
        UpdateItemExpressionSpec exprs3 = new ExpressionSpecBuilder().withCondition(
                N("a").eq(10)
                    .and(parenthesize(N("d").eq(40)
                        .and( N("b").lt(20).or(N("c").gt(30)) )))
            ).buildForUpdate();
        String c3 = exprs3.getConditionExpression();
        System.out.println("c3: " + c3);
        assertFalse(c3.equals(c1));
        assertEquals("#0 = :0 AND (#1 = :1 AND (#2 < :2 OR #3 > :3))", c3);
     }
}
