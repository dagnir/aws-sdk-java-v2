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

package software.amazon.awssdk.services.dynamodbv2.document.xspec;

import software.amazon.awssdk.annotation.Beta;

/**
 * Represents an <a href=
 * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference"
 * >OR</a> condition in building condition expressions.
 * <p>
 * This object is as immutable (or unmodifiable) as the underlying conditions.
 */
@Beta
public final class OrCondition extends Condition {
    private final Condition lhs;
    private final Condition rhs;

    OrCondition(Condition lhs, Condition rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    String asSubstituted(SubstitutionContext context) {
        return lhs.asSubstituted(context) + " OR " + rhs.asSubstituted(context);
    }

    @Override
    public AndCondition and(Condition that) {
        return new AndCondition(ExpressionSpecBuilder.paren(this), that.atomic() ? that : ExpressionSpecBuilder.paren(that));
    }

    @Override
    boolean atomic() {
        return false;
    }

    @Override
    int precedence() {
        return Precedence.OR.value();
    }
}
