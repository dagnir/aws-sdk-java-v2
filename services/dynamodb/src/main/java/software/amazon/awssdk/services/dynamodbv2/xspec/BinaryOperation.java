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

/**
 * Represents a binary operation in building expressions.
 */
abstract class BinaryOperation extends UnitOfExpression {
    private final Operand leftOperand;
    private final String operator;
    private final Operand rightOperand;

    BinaryOperation(Operand lhs, String operator, Operand rhs) {
        this.leftOperand = lhs;
        this.operator = operator;
        this.rightOperand = rhs;
    }

    final String asSubstituted(SubstitutionContext context) {
        return leftOperand.asSubstituted(context) + " " + operator + " "
               + rightOperand.asSubstituted(context);
    }

    Operand getLhs() {
        return leftOperand;
    }

    String getOperator() {
        return operator;
    }

    Operand getRhs() {
        return rightOperand;
    }
}
