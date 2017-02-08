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
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.L;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.SS;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.if_not_exists;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.list_append;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.remove;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;

//http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Modifying.html#Expressions.Modifying.UpdateExpressions.SET.Functions
public class UpdateExpressionTest {

    // SET Brand = :b, Price = :p
    @Test
    public void udpateAttributes() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(S("Brand").set("AWS"))
            .addUpdate(N("Price").set(100))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
    
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);
    
        assertEquals("SET #0 = :0, #1 = :1", expr);
        assertEquals("Brand", nm.get("#0"));
        assertEquals("Price", nm.get("#1"));
        
        assertTrue(vm.get(":0").equals("AWS"));
        assertTrue(vm.get(":1").equals(100));
    }
    
    // SET RelatedItems[0] = :ri
    @Test
    public void updateListAttribute() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(S("RelatedItems[0]").set("Amazon"))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
    
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);
    
        assertEquals("SET #0[0] = :0", expr);
        assertEquals("RelatedItems", nm.get("#0"));
        
        assertTrue(vm.get(":0").equals("Amazon"));
    }

    // SET #pr.FiveStar[0] = :r1, #pr.FiveStar[1] = :r2
    @Test
    public void nestedMapAttributes() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(S("ProductReviews[0]").set("Good"))
            .addUpdate(S("ProductReviews[1]").set("Bad"))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
    
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);
    
        assertEquals("SET #0[0] = :0, #0[1] = :1", expr);
        assertEquals("ProductReviews", nm.get("#0"));
        assertTrue(vm.get(":0").equals("Good"));
        assertTrue(vm.get(":1").equals("Bad"));
    }
    
    // SET Price = Price - :p
    @Test
    public void decreasthePrice() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(N("Price").set(N("Price").minus(10)))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
    
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);
    
        assertEquals("SET #0 = #0 - :0", expr);
        assertEquals("Price", nm.get("#0"));
        assertTrue(vm.get(":0").equals(10));
    }
    
    // SET Price = if_not_exists(Price, 100) 
    @Test
    public void ifNotExists() {
        UpdateItemExpressionSpec builder = new ExpressionSpecBuilder()
            .addUpdate(N("Price").set(if_not_exists("Price", 100)))
            .buildForUpdate();
        String expr = builder.getUpdateExpression();
        Map<String,String> nm = builder.getNameMap();
        Map<String,Object> vm = builder.getValueMap();
    
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);
    
        assertEquals("SET #0 = if_not_exists(#0,:0)", expr);
        assertEquals("Price", nm.get("#0"));
        assertEquals(100, vm.get(":0"));
    }

    // SET #pr.FiveStar = list_append(#pr.FiveStar, :r) 
    @Test
    public void listAppend() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(L("ProductReview.FiveStar").set(list_append("ProductReview.FiveStar", 100)))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
    
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);
    
        assertEquals("SET #0.#1 = list_append(#0.#1, :0)", expr);
        assertEquals("ProductReview", nm.get("#0"));
        assertEquals("FiveStar", nm.get("#1"));
        assertEquals(Arrays.asList(100), vm.get(":0"));
    }
    
    // SET #pr.FiveStar = list_append(:r, #pr.FiveStar)
    @Test
    public void listAppend2() {
        List<Number> list = Arrays.<Number>asList(100);
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
              .addUpdate(L("ProductReview.FiveStar").set(list_append(list, "ProductReview.FiveStar")))
              .buildForUpdate();
          String expr = xspec.getUpdateExpression();
          Map<String,String> nm = xspec.getNameMap();
          Map<String,Object> vm = xspec.getValueMap();
      
          System.out.println(expr);
          System.out.println(nm);
          System.out.println(vm);
      
          assertEquals("SET #0.#1 = list_append(:0, #0.#1)", expr);
          assertEquals("ProductReview", nm.get("#0"));
          assertEquals("FiveStar", nm.get("#1"));
          assertEquals(Arrays.asList(100), vm.get(":0"));
    }
    
    // REMOVE Title, RelatedItems[2], Pictures.RearView
    @Test
    public void removeExpr() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(remove("Title"))
            .addUpdate(remove("RelatedItems[2]"))
            .addUpdate(remove("Pictures.RearView"))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();

        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("REMOVE #0, #1[2], #2.#3", expr);
        assertEquals("Title", nm.get("#0"));
        assertEquals("RelatedItems", nm.get("#1"));
        assertEquals("Pictures", nm.get("#2"));
        assertEquals("RearView", nm.get("#3"));
        assertNull(vm);
    }

    // ADD Price :n
    @Test
    public void addNumeric() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(N("Price").add(100))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();

        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("ADD #0 :0", expr);
        assertEquals("Price", nm.get("#0"));
        assertEquals(100, vm.get(":0"));
    }

    // ADD Color :c 
    @Test
    public void addToStringSet() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(SS("Color").append("blue"))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();

        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("ADD #0 :0", expr);
        assertEquals("Color", nm.get("#0"));
        assertEquals(new HashSet<String>(Arrays.asList("blue")), vm.get(":0"));
    }

    // DELETE Color :c
    @Test
    public void deleteFromStringSet() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(SS("Color").delete("blue"))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();

        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("DELETE #0 :0", expr);
        assertEquals("Color", nm.get("#0"));
        assertEquals(new HashSet<String>(Arrays.asList("blue")), vm.get(":0"));
    }

    // http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Modifying.html
    // SET list[0] = :val1 REMOVE #m.nestedField1, #m.nestedField2 ADD aNumber :val2, anotherNumber :val3 DELETE aSet :val4
    @Test
    public void multiUpdates() {
        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .addUpdate(S("list[0]").set("someValue"))
            .addUpdate(remove("someMap.nestedField1"))
            .addUpdate(remove("someMap.nestedField2"))
            .addUpdate(N("aNumber").add(10))
            .addUpdate(N("anotherNumber").add(20))
            .addUpdate(SS("aSet").delete("elementToDelete"))
            .buildForUpdate();
        String expr = xspec.getUpdateExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();

        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("SET #0[0] = :0 REMOVE #1.#2, #1.#3 ADD #4 :1, #5 :2 DELETE #6 :3", expr);
        assertEquals("list", nm.get("#0"));
        assertEquals("someMap", nm.get("#1"));
        assertEquals("nestedField1", nm.get("#2"));
        assertEquals("nestedField2", nm.get("#3"));
        assertEquals("aNumber", nm.get("#4"));
        assertEquals("anotherNumber", nm.get("#5"));
        assertEquals("aSet", nm.get("#6"));

        assertEquals("someValue", vm.get(":0"));
        assertEquals(10, vm.get(":1"));
        assertEquals(20, vm.get(":2"));
        assertEquals(new HashSet<String>(Arrays.asList("elementToDelete")), vm.get(":3"));
    }
}
