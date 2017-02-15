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

import java.util.Map;
import org.junit.Test;

public class GetItemExpressionTest {

    @Test
    public void test() {
        GetItemExpressionSpec xspec = new ExpressionSpecBuilder()
                .addProjections("attr1", "attr2", "attr3")
                .buildForGetItem();
        String expr = xspec.getProjectionExpression();
        Map<String, String> nm = xspec.getNameMap();

        System.out.println(expr);
        System.out.println(nm);

        assertEquals("#0, #1, #2", expr);
        assertEquals("attr1", nm.get("#0"));
        assertEquals("attr2", nm.get("#1"));
        assertEquals("attr3", nm.get("#2"));
    }
}
