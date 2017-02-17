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
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.bs;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ns;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.s;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.ss;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.attribute;

import java.util.Map;
import org.junit.Test;

// http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html
public class ScanExpressionTest {

    @Test
    public void test() {
        ScanExpressionSpec xspec = new ExpressionSpecBuilder()
                .addProjections("attr1", "attr2")
                .withCondition(attribute("status").exists().and(s("status").eq("inactive")))
                .buildForScan();
        String projectionExpr = xspec.getProjectionExpression();
        String filterExpr = xspec.getFilterExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(projectionExpr);
        System.out.println(filterExpr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("#1, #2", projectionExpr);
        assertEquals("attribute_exists(#0) AND #0 = :0", filterExpr);

        assertEquals("status", nm.get("#0"));
        assertEquals("attr1", nm.get("#1"));
        assertEquals("attr2", nm.get("#2"));

        assertEquals("inactive", vm.get(":0"));
    }

    @Test
    public void testContains() {
        ScanExpressionSpec xspec = new ExpressionSpecBuilder()
                .addProjections("attr1", "attr2")
                .withCondition(s("status").contains("active").or(ss("ss").contains("a")).or(ns("ns").contains(123).or(
                        bs("bs").contains(new byte[0]))))
                .buildForScan();
        String projectionExpr = xspec.getProjectionExpression();
        String filterExpr = xspec.getFilterExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(projectionExpr);
        System.out.println(filterExpr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("#4, #5", projectionExpr);
        assertEquals("contains(#0, :0) OR contains(#1, :1) OR (contains(#2, :2) OR contains(#3, :3))", filterExpr);

        assertEquals("status", nm.get("#0"));
        assertEquals("ss", nm.get("#1"));
        assertEquals("ns", nm.get("#2"));
        assertEquals("bs", nm.get("#3"));

        assertEquals("active", vm.get(":0"));
    }
}
