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
package software.amazon.awssdk.services.dynamodbv2.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking a hash key or range key property in a class to
 * auto-generate this key. Only String typed keys can be auto generated, and are
 * given a random UUID. The annotation can be applied to either the getter
 * method or the class field for the auto-generated key property. If the
 * annotation is applied directly to the class field, the corresponding getter
 * and setter must be declared in the same class. This annotation can be applied
 * to both primary and index keys.
 *
 * @see software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBGeneratedUuid
 * @see java.util.UUID
 */
@DynamoDBGeneratedUuid(DynamoDBAutoGenerateStrategy.CREATE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDBAutoGeneratedKey {

}
