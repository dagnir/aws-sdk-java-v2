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
 * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Modifying.html"
 * >if_not_exists(path, operand)</a> function in building expressions. If the
 * item does not contain an attribute at the specified path, then if_not_exists
 * evaluates to operand; otherwise, it evaluates to path. You can use this
 * function to avoid overwriting an attribute already present in the item.
 * <p>
 * This object is as immutable (or unmodifiable) as the underlying operand given
 * during construction.
 */
@Beta
public final class IfNotExistsFunction<T> extends FunctionOperand {
    private final PathOperand attr;
    private final Operand operand;

    IfNotExistsFunction(PathOperand attr, Operand operand) {
        this.attr = attr;
        this.operand = operand;
    }

    @Override
    String asSubstituted(SubstitutionContext context) {
        return "if_not_exists(" + attr.asSubstituted(context) + ","
               + operand.asSubstituted(context) + ")";
    }
}
