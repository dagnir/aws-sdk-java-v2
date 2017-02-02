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

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.*;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

// http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html
public class DeleteExpressionTest {

    @Test
    public void test() {
        DeleteItemExpressionSpec xspec = new ExpressionSpecBuilder()
            .withCondition(S("status").eq("inactive"))
            .buildForDeleteItem();
        String expr = xspec.getConditionExpression();
        Map<String,String> nm = xspec.getNameMap();
        Map<String,Object> vm = xspec.getValueMap();
    
        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);
    
        assertEquals("#0 = :0", expr);
        assertEquals("status", nm.get("#0"));
        
        assertEquals("inactive", vm.get(":0"));
    }
}
