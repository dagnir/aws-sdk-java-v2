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

/**
 * Annotation to mark a property as using a custom auto-generator.
 *
 * <p>May be annotated on a user-defined annotation to pass additional
 * properties to the {@link DynamoDbAutoGenerator}.</p>
 *
 * <pre class="brush: java">
 * &#064;DynamoDBHashKey
 * &#064;CustomGeneratedKey(prefix=&quot;test-&quot;) //&lt;- user-defined annotation
 * public String getKey()
 * </pre>
 *
 * <p>Where,</p>
 * <pre class="brush: java">
 * &#064;DynamoDBAutoGenerated(generator=CustomGeneratedKey.Generator.class)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Target({ElementType.METHOD})
 * public &#064;interface CustomGeneratedKey {
 *     String prefix() default &quot;&quot;;
 *
 *     public static class Generator implements DynamoDBAutoGenerator&lt;String&gt; {
 *         private final String prefix;
 *         public Generator(final Class&lt;String&gt; targetType, final CustomGeneratedKey annotation) {
 *             this.prefix = annotation.prefix();
 *         }
 *         public Generator() { //&lt;- required if annotating directly
 *             this.prefix = "";
 *         }
 *         &#064;Override
 *         public DynamoDBAutoGenerateStrategy getGenerateStrategy() {
 *             return DynamoDBAutoGenerateStrategy.CREATE;
 *         }
 *         &#064;Override
 *         public final String generate(final String currentValue) {
 *             return prefix + UUID.randomUUID.toString();
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>Alternately, the property/field may be annotated directly (which requires
 * the generator to provide a default constructor),</p>
 * <pre class="brush: java">
 * &#064;DynamoDBAutoGenerated(generator=CustomGeneratedKey.Generator.class)
 * public String getKey()
 * </pre>
 *
 * <p>May be used as a meta-annotation.</p>
 */
@DynamoDb
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DynamoDbAutoGenerated {

    /**
     * The auto-generator class for this property.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends DynamoDbAutoGenerator> generator();

}
