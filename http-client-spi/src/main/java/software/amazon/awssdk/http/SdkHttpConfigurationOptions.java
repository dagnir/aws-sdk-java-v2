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

public final class SdkHttpConfigurationOptions {

    private final Map<SdkHttpConfigurationOption<?>, Object> options;

    private SdkHttpConfigurationOptions() {
        this(new HashMap<>());
    }

    private SdkHttpConfigurationOptions(Map<SdkHttpConfigurationOption<?>, Object> options) {
        this.options = options;
    }

    @SuppressWarnings("unchecked")
    public <T> T option(SdkHttpConfigurationOption<T> optionKey) {
        return (T) options.get(optionKey);
    }

    public <T> SdkHttpConfigurationOptions option(SdkHttpConfigurationOption<T> optionKey, T optionValue) {
        options.put(optionKey, optionValue);
        return this;
    }

    public SdkHttpConfigurationOptions copy() {
        return new SdkHttpConfigurationOptions(new HashMap<>(options));
    }

    public SdkHttpConfigurationOptions merge(SdkHttpConfigurationOptions lowerPrecedence) {
        Map<SdkHttpConfigurationOption<?>, Object> currentOptions = new HashMap<>(options);
        lowerPrecedence.options.forEach(currentOptions::putIfAbsent);
        return new SdkHttpConfigurationOptions(currentOptions);
    }

    public static SdkHttpConfigurationOptions createEmpty() {
        return new SdkHttpConfigurationOptions();
    }
}
