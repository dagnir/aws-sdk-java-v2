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

import software.amazon.awssdk.annotation.Immutable;

/**
 * <a href=
 * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference"
 * >Precedence</a> of various Conditions.
 */
@Immutable
enum Precedence {
    Comparator(700),    // = <> < <= > >=
    IN(600),
    BETWEEN(500),
    Function(400),  // attribute_exists attribute_not_exists begins_with contains
    Parentheses(300),
    NOT(200),
    AND(100),
    OR(0),;
    /**
     * The higher this value, the higher the precedence.
     */
    private final int value;

    private Precedence(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }
}
