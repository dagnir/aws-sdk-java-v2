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

import software.amazon.awssdk.annotation.Beta;

/**
 * Represents an update action for building update expression.
 * <p>
 * This object is as immutable (or unmodifiable) as the underlying value (of
 * type <code>UnitOfExpression</code>) given during construction.
 */
@Beta
public abstract class UpdateAction extends UnitOfExpression {
    private final String operator;

    private final PathOperand attribute;
    private final UnitOfExpression value;

    UpdateAction(String operator, PathOperand attribute, UnitOfExpression value) {
        this.operator = operator;
        this.attribute = attribute;
        this.value = value;
    }

    /**
     * Returns the attribute as a string, substituted if necessary with tokens
     * using the given substitution context.
     */
    @Override
    String asSubstituted(SubstitutionContext context) {
        return value == null
               ? attribute.asSubstituted(context)
               : attribute.asSubstituted(context) + " " + value.asSubstituted(context);
    }

    final String getOperator() {
        return operator;
    }

    final PathOperand getPathOperand() {
        return attribute;
    }

    final UnitOfExpression getValue() {
        return value;
    }
}
