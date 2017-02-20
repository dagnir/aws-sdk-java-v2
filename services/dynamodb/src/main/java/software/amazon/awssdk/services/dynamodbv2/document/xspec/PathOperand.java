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
import software.amazon.awssdk.annotation.Immutable;

/**
 * A path operand used in building DynamooDB expressions such as update
 * expressions and condition (aka filter) expressions. In general, a path
 * operand refers to an attribute of some <a href=
 * "http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_AttributeValue.html"
 * >specific type</a> in DynamoDB.
 * <p>
 * Use {@link ExpressionSpecBuilder#s(String)},
 * {@link ExpressionSpecBuilder#n(String)}, etc. to instantiate path operands to
 * refer to attributes of specific data types. You can also use
 * {@link ExpressionSpecBuilder#attribute(String)} to instantiate a path operand
 * with an unspecified data type.
 *
 * @see ExpressionSpecBuilder
 */
@Beta
@Immutable
public class PathOperand extends Operand {
    private final Path path;
    private final String pathString;    // This is a performance optimization; not strictly necessary.

    /**
     * Constructs a path operand for building expressions.
     *
     * @param path
     *            a document path in which nested elements are assumed to be
     *            delimited by either "." or array indexing such as "[1]".
     */
    PathOperand(String path) {
        this.path = new Path(path);
        this.pathString = path;
    }

    @Override
    String asSubstituted(SubstitutionContext context) {
        return path.asSubstituted(context);
    }

    /**
     * Returns a <a href=
     * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference.Functions"
     * >function condition</a> (that evaluates to true if the attribute referred
     * to by this path operand exists) for building condition expression.
     */
    public final FunctionCondition exists() {
        return ExpressionSpecBuilder.attributeExists(this);
    }

    /**
     * Returns a <a href=
     * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference.Functions"
     * >function condition</a> (that evaluates to true if the attribute referred
     * to by this path operand does not exist) for building condition
     * expression.
     */
    public final FunctionCondition notExists() {
        return ExpressionSpecBuilder.attributeNotExists(this);
    }

    /**
     * Returns a <code>RemoveAction</code> for removing the attribute referred
     * to by this path operand from an item; used for building update
     * expression.
     */
    public final RemoveAction remove() {
        return new RemoveAction(this);
    }

    /**
     * Returns the path of this path operand as a string.
     */
    final String getPath() {
        return pathString;
    }

    @Override
    public final String toString() {
        return pathString;
    }

    @Override
    public final int hashCode() {
        return pathString.hashCode();
    }

    /**
     * Returns true if the given object is a path operand with the same path as
     * that of the current path operand; false otherwise.
     */
    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PathOperand)) {
            return false;
        }
        PathOperand that = (PathOperand) o;
        return this.getPath().equals(that.getPath());
    }
}
