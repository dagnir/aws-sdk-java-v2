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
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;

import org.junit.Test;

public class BetweenConditionTest {

    @Test
    public void test() {
        BetweenCondition cond = N("num").between(1, 100);
        SubstitutionContext context = new SubstitutionContext();
        String s = cond.asSubstituted(context);

        System.out.println(s);
        System.out.println(context);

        assertEquals("#0 BETWEEN :0 AND :1", s);
        assertEquals("num", context.getNameByToken(0));
        assertEquals(1, context.getValueByToken(0));
        assertEquals(100, context.getValueByToken(1));
    }
}
