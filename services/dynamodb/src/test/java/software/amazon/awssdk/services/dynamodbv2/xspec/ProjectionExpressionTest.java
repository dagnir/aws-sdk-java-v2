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

import java.util.Map;
import org.junit.Test;

// http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html#Expressions.AccessingItemAttributes.ProjectionExpressions
public class ProjectionExpressionTest {

    @Test
    public void projectionExpression() {
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
                .addProjection("Price")
                .addProjection("Color")
                .addProjection("Pictures.FrontView")
                .addProjection("ProductReviews.FiveStar[0]")
                .buildForQuery();
        String expr = xspec.getProjectionExpression();
        Map<String, String> nm = xspec.getNameMap();
        Map<String, Object> vm = xspec.getValueMap();

        System.out.println(expr);
        System.out.println(nm);
        System.out.println(vm);

        assertEquals("#0, #1, #2.#3, #4.#5[0]", expr);
        assertEquals("Price", nm.get("#0"));
        assertEquals("Color", nm.get("#1"));
        assertEquals("Pictures", nm.get("#2"));
        assertEquals("FrontView", nm.get("#3"));
        assertEquals("ProductReviews", nm.get("#4"));
        assertEquals("FiveStar", nm.get("#5"));

        assertNull(vm);
    }
}
