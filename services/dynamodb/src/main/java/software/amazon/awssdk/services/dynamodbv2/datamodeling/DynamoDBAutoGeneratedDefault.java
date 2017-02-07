/*
 * Copyright 2016-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Annotation to assign a default value on creation if value is null.
 *
 * <pre class="brush: java">
 * &#064;DynamoDBAutoGeneratedDefault(&quot;OPEN&quot;)
 * public String getStatus()
 * </pre>
 *
 * <p>Only compatible with standard string types.</p>
 *
 */
@DynamoDB
@DynamoDBAutoGenerated(generator=DynamoDBAutoGeneratedDefault.Generator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDBAutoGeneratedDefault {

    /**
     * The default value.
     */
    String value();


    /**
     * Default generator.
     */
    static final class Generator<T> extends DynamoDBAutoGenerator.AbstractGenerator<T> {
        private final DynamoDBTypeConverter<T,String> converter;
        private final String defaultValue;

        public Generator(Class<T> targetType, DynamoDBAutoGeneratedDefault annotation) {
            super(DynamoDBAutoGenerateStrategy.CREATE);
            this.converter = StandardTypeConverters.factory().getConverter(targetType, String.class);
            this.defaultValue = annotation.value();
        }

        @Override
        public final T generate(T currentValue) {
            return converter.convert(defaultValue);
        }
    }


}
