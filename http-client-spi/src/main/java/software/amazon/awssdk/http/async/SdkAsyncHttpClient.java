/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.async;

import software.amazon.awssdk.http.ConfigurationProvider;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.builder.SdkBuilder;

import java.util.concurrent.CompletableFuture;

public interface SdkAsyncHttpClient extends SdkAutoCloseable, ConfigurationProvider {
    CompletableFuture<Void> execute(AsyncExecuteRequest request);

    @FunctionalInterface
    interface Builder<T extends SdkAsyncHttpClient.Builder<T>> extends SdkBuilder<T, SdkAsyncHttpClient> {
        /**
         * Create a {@link SdkAsyncHttpClient} without defaults applied. This is useful for reusing an HTTP client across multiple
         * services.
         */
        default SdkAsyncHttpClient build() {
            return buildWithDefaults(AttributeMap.empty());
        }

        /**
         * Create an {@link SdkAsyncHttpClient} with service specific defaults applied. Applying service defaults is optional
         * and some options may not be supported by a particular implementation.
         *
         * @param serviceDefaults Service specific defaults. Keys will be one of the constants defined in
         *                        {@link SdkHttpConfigurationOption}.
         * @return Created client
         */
        SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults);
    }
}
