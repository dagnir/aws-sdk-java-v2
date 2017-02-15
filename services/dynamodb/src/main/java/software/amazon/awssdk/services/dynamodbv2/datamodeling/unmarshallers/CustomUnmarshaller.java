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

package software.amazon.awssdk.services.dynamodbv2.datamodeling.unmarshallers;

import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBMarshaller;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;

/**
 * An unmarshaller that delegates to an instance of a
 * {@code DynamoDBMarshaller}-derived custom marshaler.
 */
public class CustomUnmarshaller extends SUnmarshaller {

    private final Class<?> targetClass;
    private final Class<? extends DynamoDBMarshaller<?>> unmarshallerClass;

    public CustomUnmarshaller(
            Class<?> targetClass,
            Class<? extends DynamoDBMarshaller<?>> unmarshallerClass) {

        this.targetClass = targetClass;
        this.unmarshallerClass = unmarshallerClass;
    }

    @Override
    @SuppressWarnings( {"rawtypes", "unchecked"})
    public Object unmarshall(AttributeValue value) {

        // TODO: Would be nice to cache this object, but not sure if we can
        // do that now without a breaking change; user's unmarshallers
        // might not all be thread-safe.

        DynamoDBMarshaller unmarshaller =
                createUnmarshaller(unmarshallerClass);

        return unmarshaller.unmarshall(targetClass, value.getS());
    }

    @SuppressWarnings( {"rawtypes"})
    private static DynamoDBMarshaller createUnmarshaller(Class<?> clazz) {
        try {

            return (DynamoDBMarshaller) clazz.newInstance();

        } catch (InstantiationException e) {
            throw new DynamoDBMappingException(
                    "Failed to instantiate custom marshaler for class " + clazz,
                    e);

        } catch (IllegalAccessException e) {
            throw new DynamoDBMappingException(
                    "Failed to instantiate custom marshaler for class " + clazz,
                    e);
        }
    }
}
