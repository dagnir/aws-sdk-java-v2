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

import java.util.List;
import software.amazon.awssdk.annotation.Beta;

/**
 * Represents a <a href=
 * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference"
 * >IN</a> condition in building condition expression.
 * <p>
 * Underlying grammar:
 *
 * <pre>
 *    operand IN ( operand (',' operand (, ...) ))
 * </pre>
 * <p>
 * This object is as immutable (or unmodifiable) as the underlying set of
 * operands.
 */
@Beta
public final class InCondition extends Condition {
    private final PathOperand attribute;
    private final List<? extends Operand> operands;

    /**
     * @param operands assumed to be allocated on the stack so it will remain
     *     externally unmodifiable
     */
    InCondition(PathOperand attribute, List<? extends Operand> operands) {
        this.attribute = attribute;
        this.operands = operands;
    }

    @Override
    String asSubstituted(SubstitutionContext context) {
        StringBuilder sb = new StringBuilder(attribute.asSubstituted(context))
                .append(" IN (");
        boolean first = true;
        for (Operand operand : operands) {
            if (first) {
                sb.append(operand.asSubstituted(context));
                first = false;
            } else {
                sb.append(", ").append(operand.asSubstituted(context));
            }
        }
        return sb.append(")").toString();
    }

    @Override
    boolean atomic() {
        return true;
    }

    @Override
    int precedence() {
        return Precedence.IN.value();
    }
}
