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
 * Unit of expression. A unit of expression is a building block for composing an
 * expression, such as update expression, condition (aka filter) expression, and
 * projection Expression.
 */
abstract class UnitOfExpression {
    /**
     * Returns this unit of expression as a string substituted if necessary with
     * tokens using the given substitution context.
     *
     * @param context the substitution context which may get mutated as a side
     * effect upon completion of this method
     */
    abstract String asSubstituted(SubstitutionContext context);
}
