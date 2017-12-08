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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

/**
 * Annotation for auto-generating a {@link UUID}.
 *
 * <pre class="brush: java">
 * &#064;DynamoDBGeneratedUuid(DynamoDBAutoGenerateStrategy.CREATE)
 * public UUID getKey()
 * </pre>
 *
 * <p>When applied to a key field, only the strategy
 * {@link DynamoDbAutoGenerateStrategy#CREATE} is supported.</p>
 *
 * <p>The short-formed {@link DynamoDbAutoGeneratedKey} may also be used for
 * create only.</p>
 *
 * <p>May be used as a meta-annotation.</p>
 *
 * @see java.util.UUID
 */
@DynamoDb
@DynamoDbAutoGenerated(generator = DynamoDbGeneratedUuid.Generator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DynamoDbGeneratedUuid {

    /**
     * The auto-generation strategy.
     */
    DynamoDbAutoGenerateStrategy value();

    /**
     * Default generator.
     */
    final class Generator<T> extends DynamoDbAutoGenerator.AbstractGenerator<T> {
        private final DynamoDbTypeConverter<T, UUID> converter;

        Generator(Class<T> targetType, DynamoDbGeneratedUuid annotation) {
            super(annotation.value());
            this.converter = StandardTypeConverters.factory().getConverter(targetType, UUID.class);
        }

        @Override
        public T generate(final T currentValue) {
            return converter.convert(UUID.randomUUID());
        }
    }

}
