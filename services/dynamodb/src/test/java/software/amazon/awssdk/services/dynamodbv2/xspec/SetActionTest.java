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
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.n;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.s;

import org.junit.Test;

public class SetActionTest {

    // SET num1 = num1 + 20
    @Test
    public void number() {
        SetAction setSection = n("num1").set(n("num1").plus(20));
        SubstitutionContext context = new SubstitutionContext();
        String s = setSection.asSubstituted(context);

        System.out.println(s);
        System.out.println(context);

        assertEquals("#0 = #0 + :0", s);
        assertEquals("num1", context.getNameByToken(0));
        assertEquals(20, context.getValueByToken(0));
    }

    // SET string-attr = "value"
    @Test
    public void string() {
        SetAction setSection = s("string-attr").set("string-value");
        SubstitutionContext context = new SubstitutionContext();
        String s = setSection.asSubstituted(context);

        System.out.println(s);
        System.out.println(context);

        assertEquals("#0 = :0", s);
        assertEquals("string-attr", context.getNameByToken(0));
        assertEquals("string-value", context.getValueByToken(0));
    }
}
