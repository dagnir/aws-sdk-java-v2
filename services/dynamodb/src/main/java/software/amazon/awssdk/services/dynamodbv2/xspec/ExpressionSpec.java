/*
 * Copyright 2015 Amazon Technologies, Inc.
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
 package software.amazon.awssdk.services.dynamodbv2.xspec;

import java.util.Map;

/**
 * Expression specification for making request to Amazon DynamoDB.
 */
abstract class ExpressionSpec {
    /**
     * Returns the name map which is unmodifiable; or null if there is none.
     */
    public abstract Map<String, String> getNameMap();
}
