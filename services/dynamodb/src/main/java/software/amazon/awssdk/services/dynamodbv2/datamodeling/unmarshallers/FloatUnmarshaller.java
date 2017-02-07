/*
 * Copyright 2014-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.dynamodbv2.datamodeling.unmarshallers;

import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;

/**
 * An unmarshaller that unmarshals DynamoDB Numbers into Java
 * {@code Float}s.
 */
public class FloatUnmarshaller extends NUnmarshaller {

    private static final FloatUnmarshaller INSTANCE =
            new FloatUnmarshaller();

    public static FloatUnmarshaller instance() {
        return INSTANCE;
    }

    private FloatUnmarshaller() {
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        return Float.valueOf(value.getN());
    }
}
