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

package software.amazon.awssdk.http;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkProtectedApi;

/**
 * Container for several {@link SdkHttpConfigurationOption}.
 */
@Immutable
@SdkProtectedApi
// TODO write tests if we stick with this approach
public final class SdkHttpConfigurationOptions {

    private static final SdkHttpConfigurationOptions EMPTY = builder().build();

    private final Map<SdkHttpConfigurationOption<?>, Object> options;

    private SdkHttpConfigurationOptions(Map<SdkHttpConfigurationOption<?>, Object> options) {
        this.options = new HashMap<>(options);
    }

    /**
     * Retrieves an option by key.
     *
     * @param optionKey Key to retrieve appropriate option.
     * @param <T>       Type of option bound to the key.
     * @return Option value if present.
     */
    @SuppressWarnings("unchecked")
    public <T> T option(SdkHttpConfigurationOption<T> optionKey) {
        return (T) options.get(optionKey);
    }

    /**
     * @return An immutable copy of this options object.
     */
    public SdkHttpConfigurationOptions copy() {
        return new SdkHttpConfigurationOptions(new HashMap<>(options));
    }

    /**
     * Merges two options into one. This object is given higher precedence then the options passed in as a parameter.
     *
     * @param lowerPrecedence Options to merge into 'this' options object. Any option already specified in 'this' object will be
     *                        left as is since it has higher precedence.
     * @return New options with values merged.
     */
    public SdkHttpConfigurationOptions merge(SdkHttpConfigurationOptions lowerPrecedence) {
        Map<SdkHttpConfigurationOption<?>, Object> currentOptions = new HashMap<>(options);
        lowerPrecedence.options.forEach(currentOptions::putIfAbsent);
        return new SdkHttpConfigurationOptions(currentOptions);
    }

    /**
     * @return An empty {@link SdkHttpConfigurationOptions} object.
     */
    public static SdkHttpConfigurationOptions empty() {
        return EMPTY;
    }

    /**
     * @return Builder instance to construct a {@link SdkHttpConfigurationOptions}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link SdkHttpConfigurationOptions}.
     */
    public static final class Builder {

        private final Map<SdkHttpConfigurationOption<?>, Object> options = new HashMap<>();

        private Builder() {
        }

        /**
         * Retrieves an option by key.
         *
         * @param optionKey Key to retrieve appropriate option.
         * @param <T>       Type of option bound to the key.
         * @return Option value if present.
         */
        @SuppressWarnings("unchecked")
        public <T> T option(SdkHttpConfigurationOption<T> optionKey) {
            return (T) options.get(optionKey);
        }

        /**
         * Adds a new option to the container.
         *
         * @param optionKey   Option key.
         * @param optionValue Option value.
         * @param <T>         Type of option.
         * @return This builder for method chaining.
         */
        public <T> Builder option(SdkHttpConfigurationOption<T> optionKey, T optionValue) {
            options.put(optionKey, optionValue);
            return this;
        }

        /**
         * @return An immutable {@link SdkHttpConfigurationOptions} object.
         */
        public SdkHttpConfigurationOptions build() {
            return new SdkHttpConfigurationOptions(options);
        }
    }
}
