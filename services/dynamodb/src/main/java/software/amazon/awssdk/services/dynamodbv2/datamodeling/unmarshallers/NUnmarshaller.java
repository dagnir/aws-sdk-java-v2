/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.lang.reflect.Method;

import software.amazon.awssdk.services.dynamodbv2.datamodeling.ArgumentUnmarshaller;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;

abstract class NUnmarshaller implements ArgumentUnmarshaller {

    @Override
    public void typeCheck(AttributeValue value, Method setter) {
        if ( value.getN() == null ) {
            throw new DynamoDBMappingException("Expected N in value " + value + " when invoking " + setter);
        }
    }

}
