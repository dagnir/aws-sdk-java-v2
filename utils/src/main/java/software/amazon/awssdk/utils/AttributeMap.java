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

package software.amazon.awssdk.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotation.SdkProtectedApi;

/**
 * A map from {@code AttributeMap.Key<T>} to {@code T} that ensures the values stored with a key matches the type associated with
 * the key. This does not implement {@link Map} because it has more strict typing requirements, but a {@link Map} can be converted
 * to an {code AttributeMap} via the type-unsafe {@link AttributeMap} method.
 *
 * This can be used for storing configuration values ({@code OptionKey.LOG_LEVEL} to {@code Boolean.TRUE}), attaching
 * arbitrary attributes to a request chain ({@code RequestAttribute.CONFIGURATION} to {@code ClientConfiguration}) or similar
 * use-cases.
 */
@SdkProtectedApi
public final class AttributeMap {
    private final Map<Key<?>, Object> configuration;

    /**
     * Create an empty set of attributes.
     */
    public AttributeMap() {
        this.configuration = new HashMap<>();
    }

    /**
     * Create a copy of the provided attribute map.
     */
    public AttributeMap(AttributeMap attributeMap) {
        this.configuration = new HashMap<>(attributeMap.configuration);
    }

    /**
     * Create an attribute map from the provided java map. This is not type safe, and will throw an exception during creation if
     * a value in the map is not of the correct type for its key.
     */
    public AttributeMap(Map<? extends Key<?>, ?> javaMap) {
        this.configuration = javaMap.entrySet().stream()
                                    .peek(e -> e.getKey().validateValue(e.getValue()))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Return true if the provided key is configured in this map. Useful for differentiating between whether the provided key was
     * not configured in the map or if it is configured, but its value is null.
     */
    public boolean containsKey(Key<?> typedKey) {
        return configuration.containsKey(typedKey);
    }

    /**
     * Add a mapping between the provided key and value.
     */
    public <T> AttributeMap put(Key<T> key, T value) {
        Validate.notNull(key, "Key to set must not be null.");
        configuration.put(key, value);
        return this;
    }

    /**
     * Get the value associated with the provided key from this map. This will return null if the value is not set or if the value
     * stored is null. These cases can be disambiguated using {@link #containsKey(Key)}.
     */
    public <T> T get(Key<T> key) {
        Validate.notNull(key, "Key to retrieve must not be null.");
        return key.convertValue(configuration.get(key));
    }

    /**
     * An abstract class extended by pseudo-enums defining the key for data that is stored in the {@link AttributeMap}. For
     * example, a {@code ClientOption<T>} may extend this to define options that can be stored in an {@link AttributeMap}.
     */
    public abstract static class Key<T> {
        private final Class<T> valueClass;

        /**
         * Configure the class of {@code T}.
         */
        protected Key(Class<T> valueClass) {
            this.valueClass = valueClass;
        }

        /**
         * Validate the provided value is of the correct type.
         */
        final void validateValue(Object value) {
            if (value != null) {
                Validate.isAssignableFrom(valueClass, value.getClass(),
                                          "Invalid option: %s. Required value of type %s, but was %s.",
                                          this, valueClass, value.getClass());
            }
        }

        /**
         * Validate the provided value is of the correct type and convert it to the proper type for this option.
         */
        final T convertValue(Object value) {
            validateValue(value);
            return valueClass.cast(value);
        }
    }

    @Override
    public String toString() {
        return configuration.toString();
    }

    @Override
    public int hashCode() {
        return configuration.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AttributeMap &&
               configuration.equals(((AttributeMap) obj).configuration);
    }
}
