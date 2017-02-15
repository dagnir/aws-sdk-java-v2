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
import software.amazon.awssdk.annotation.Immutable;

/**
 * Represents a <a href=
 * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Modifying.html"
 * >REMOVE</a> action in the REMOVE section of an update expression.
 * <p>
 * A REMOVE action is used to remove one or more attributes from an item.
 */
@Beta
@Immutable
public final class RemoveAction extends UpdateAction {
    RemoveAction(PathOperand attr) {
        super("REMOVE", attr, null);
    }
}
