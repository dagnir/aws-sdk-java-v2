/*
 * Copyright 2015-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
 package software.amazon.awssdk.services.dynamodbv2.document.xspec;

import software.amazon.awssdk.annotation.Beta;

/**
 * An explicitly parenthesized condition, ie '(' condition ')', used in building
 * condition expressions.
 * <p>
 * This object is as immutable (or unmodifiable) as the underlying condition.
 */
@Beta
public final class ParenthesizedCondition extends Condition {
    private final Condition condition;

    /**
     * Returns a parenthesized condition for the given condition if the given
     * condition is not already a parenthesized condition; or the original
     * condition otherwise.
     */
    public static ParenthesizedCondition getInstance(Condition condition) {
        return condition instanceof ParenthesizedCondition ? (ParenthesizedCondition) condition
                : new ParenthesizedCondition(condition);
    }

    private ParenthesizedCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    String asSubstituted(SubstitutionContext context) {
        return "(" + condition.asSubstituted(context) + ")";
    }

    @Override
    boolean atomic() {
        return true;
    }

    @Override
    int precedence() {
        return Precedence.Parentheses.value();
    }
}
