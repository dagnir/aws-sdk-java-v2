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
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.attributeNotExists;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.n;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.not;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.paren;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.s;

import java.util.Map;
import org.junit.Test;

public class ConditionTest {
    /*
     * <pre>
        ((attribute_not_exists(item_version) AND attribute_not_exists(config_id) AND attribute_not_exists(config_version)) OR
         (item_version < :item_version) OR 
         (item_version = :item_version AND config_id < :config_id) OR
         (item_version = :item_version AND config_id = :config_id AND config_version < :config_version))     
     * </pre>
     */
    @Test
    public void explicitBracketingForJava_850() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .withCondition(
                        paren(paren(attributeNotExists("item_version")
                                    .and(attributeNotExists("config_id"))
                                    .and(attributeNotExists("config_version"))
                                   ).or(paren(n("item_version").lt(123)))
                            .or(paren(n("item_version").eq(123)
                                                       .and(n("config_id").lt(456))))
                            .or(paren(n("item_version").eq(123)
                                                       .and(n("config_id").eq(456))
                                                       .and(n("config_version").lt(999))))))
                .buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals(
                "((attribute_not_exists(#0) AND attribute_not_exists(#1) AND attribute_not_exists(#2)) OR " +
                "(#0 < :0) OR " +
                "(#0 = :0 AND #1 < :1) OR " +
                "(#0 = :0 AND #1 = :1 AND #2 < :2))",
                c);
        assertEquals("item_version", nm.get("#0"));
        assertEquals("config_id", nm.get("#1"));
        assertEquals("config_version", nm.get("#2"));

        assertTrue(vm.get(":0").equals(123));
        assertTrue(vm.get(":1").equals(456));
        assertTrue(vm.get(":2").equals(999));
    }

    /*
     * <pre>
        attribute_not_exists(item_version) AND attribute_not_exists(config_id) AND (attribute_not_exists(config_version) OR
        item_version < :item_version) OR
        item_version = :item_version AND config_id < :config_id OR
        item_version = :item_version AND config_id = :config_id AND config_version < :config_version     
     * </pre>
     */
    @Test
    public void minBracketingForJava_850() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .withCondition(
                        attributeNotExists("item_version")
                                .and(attributeNotExists("config_id"))
                                .and(attributeNotExists("config_version")
                                             .or(n("item_version").lt(123)))
                                .or(n("item_version").eq(123)
                                                     .and(n("config_id").lt(456)))
                                .or(n("item_version").eq(123)
                                                     .and(n("config_id").eq(456))
                                                     .and(n("config_version").lt(999))))
                .buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals(
                "attribute_not_exists(#0) AND attribute_not_exists(#1) AND (attribute_not_exists(#2) OR #0 < :0) OR " +
                "#0 = :0 AND #1 < :1 OR " +
                "#0 = :0 AND #1 = :1 AND #2 < :2",
                c);
        assertEquals("item_version", nm.get("#0"));
        assertEquals("config_id", nm.get("#1"));
        assertEquals("config_version", nm.get("#2"));

        assertTrue(vm.get(":0").equals(123));
        assertTrue(vm.get(":1").equals(456));
        assertTrue(vm.get(":2").equals(999));
    }

    // http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference.Functions
    // (#P between :lo and :hi) and (#PC in (:cat1, :cat2))
    @Test
    public void anotherExample() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .withCondition(
                        n("Price").between(100, 200)
                                  .and(s("ProductCategory").in("category1", "category2", "category3")))
                .buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("#0 BETWEEN :0 AND :1 AND #1 IN (:2, :3, :4)", c);
        assertEquals("Price", nm.get("#0"));
        assertEquals("ProductCategory", nm.get("#1"));

        assertTrue(vm.get(":0").equals(100));
        assertTrue(vm.get(":1").equals(200));
        assertTrue(vm.get(":2").equals("category1"));
        assertTrue(vm.get(":3").equals("category2"));
        assertTrue(vm.get(":4").equals("category3"));
    }

    // (a = 1 AND (b = 2 OR c = 3) OR d <> 4) AND 
    //   e = 5 AND (f = 6 OR (g = 7 OR h = i and j = 8))
    @Test
    public void someComplexConditions() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .withCondition(
                        paren(n("a").eq(1).and(n("b").eq(2).or(n("c").eq(3))).or(n("d").ne(4)))
                                .and(n("e").eq(5).and(n("f").eq(6).or(n("g").eq(7).or(n("h").eq(n("i")).and(n("j").eq(8))))))
                              ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("(#0 = :0 AND (#1 = :1 OR #2 = :2) OR #3 <> :3) AND "
                     + "#4 = :4 AND (#5 = :5 OR (#6 = :6 OR #7 = #8 AND #9 = :7))", c);
        assertEquals("a", nm.get("#0"));
        assertEquals("b", nm.get("#1"));
        assertEquals("c", nm.get("#2"));
        assertEquals("d", nm.get("#3"));
        assertEquals("e", nm.get("#4"));
        assertEquals("f", nm.get("#5"));
        assertEquals("g", nm.get("#6"));
        assertEquals("h", nm.get("#7"));
        assertEquals("i", nm.get("#8"));
        assertEquals("j", nm.get("#9"));

        assertTrue(vm.get(":0").equals(1));
        assertTrue(vm.get(":1").equals(2));
        assertTrue(vm.get(":2").equals(3));
        assertTrue(vm.get(":3").equals(4));
        assertTrue(vm.get(":4").equals(5));
        assertTrue(vm.get(":5").equals(6));
        assertTrue(vm.get(":6").equals(7));
        assertTrue(vm.get(":7").equals(8));
    }

    @Test
    public void negation() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .withCondition(
                        not(n("a").eq(1).and(n("b").eq(2).or(n("c").eq(3))).and(n("d").ne(4)))
                                .and(not(n("e").eq(5).and(n("f").eq(6).or(n("g").eq(7).or(n("h").eq(n("i")).and(n("j").eq(8)))))))
                              ).buildForUpdate();
        String c = xspec.getConditionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(c);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("NOT (#0 = :0 AND (#1 = :1 OR #2 = :2) AND #3 <> :3) AND "
                     + "NOT (#4 = :4 AND (#5 = :5 OR (#6 = :6 OR #7 = #8 AND #9 = :7)))", c);
        assertEquals("a", nm.get("#0"));
        assertEquals("b", nm.get("#1"));
        assertEquals("c", nm.get("#2"));
        assertEquals("d", nm.get("#3"));
        assertEquals("e", nm.get("#4"));
        assertEquals("f", nm.get("#5"));
        assertEquals("g", nm.get("#6"));
        assertEquals("h", nm.get("#7"));
        assertEquals("i", nm.get("#8"));
        assertEquals("j", nm.get("#9"));

        assertTrue(vm.get(":0").equals(1));
        assertTrue(vm.get(":1").equals(2));
        assertTrue(vm.get(":2").equals(3));
        assertTrue(vm.get(":3").equals(4));
        assertTrue(vm.get(":4").equals(5));
        assertTrue(vm.get(":5").equals(6));
        assertTrue(vm.get(":6").equals(7));
        assertTrue(vm.get(":7").equals(8));
    }
}
