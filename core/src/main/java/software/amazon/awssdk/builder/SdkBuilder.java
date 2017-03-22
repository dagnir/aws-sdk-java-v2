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

package software.amazon.awssdk.builder;

import java.util.function.UnaryOperator;

/**
 * A mutable object that can be used to create an immutable object of type T.
 *
 * @param <T> the type that the builder will build
 * @param <B> the builder type (this)
 */
public interface SdkBuilder<T extends ToSdkBuilder<T, B>, B extends SdkBuilder<T, B>> {

    /**
     * An immutable object that is created from the
     * properties that have been set on the builder.
     *
     * @return an instance of T
     */
    T build();

    /**
     * A shallow copy of this object created by building an
     * immutable T and then transforming it back to a builder.
     *
     * @return a copy of this object
     */
    default B copy() {
        return build().toBuilder();
    }

    /**
     * A convenience operator that takes something that will
     * mutate the builder in some way and allows inclusion of it
     * in chaining operations. For example instead of:
     *
     * <pre><code>
     * Builder builder = ClassBeingBuilt.builder();
     * builder = Util.addSomeDetailToTheBuilder(builder);
     * ClassBeingBuilt clz = builder.build();
     * </code></pre>
     * <p/>
     * This can be done in a statement:
     *
     * <pre><code>
     * ClassBeingBuilt = ClassBeingBuilt.builder().apply(Util::addSomeDetailToTheBuilder).build();
     * </code></pre>
     *
     * @param function the function that mutates and then returns the builder
     * @return B the mutated builder instance
     */
    @SuppressWarnings("unchecked")
    default B apply(UnaryOperator<B> function) {
        return function.apply((B) this);
    }
}
