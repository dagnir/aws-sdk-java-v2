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

package software.amazon.awssdk;

import software.amazon.awssdk.annotation.SdkProtectedApi;

/**
 * Factory producing predefined {@link LegacyClientConfiguration} instances.
 * Individual service clients may extend this factory to override
 * these with service specific configuration values.
 */
@SdkProtectedApi
public class LegacyClientConfigurationFactory {

    /**
     * Builds a {@link LegacyClientConfiguration} instance with the default configuration
     * for the current client.  If the {@link SDKGlobalConfiguration#ENABLE_IN_REGION_OPTIMIZED_MODE}
     * system property has been set, in-region optimized configuration will be used.
     *
     * @return constructed {@link LegacyClientConfiguration} instance
     */
    public final LegacyClientConfiguration getConfig() {
        return SDKGlobalConfiguration.isInRegionOptimizedModeEnabled()
               ? getInRegionOptimizedConfig() : getDefaultConfig();
    }

    /**
     * Builds a {@link LegacyClientConfiguration} instance with default configuration
     * values suitable for most use cases.
     *
     * @return constructed {@link LegacyClientConfiguration} with standard configuration.
     */
    protected LegacyClientConfiguration getDefaultConfig() {
        return new LegacyClientConfiguration();
    }

    /**
     * Builds a {@link LegacyClientConfiguration} instance with configuration values
     * tailored towards clients operating in the same AWS region as the service
     * endpoint they call.  Timeouts in in-region optimized configurations are
     * generally set much lower than the client standard configuration.
     *
     * @return constructed {@link LegacyClientConfiguration} with in-region optimized configuration
     */
    protected LegacyClientConfiguration getInRegionOptimizedConfig() {
        return new LegacyClientConfiguration().withConnectionTimeout(1000);
    }

}
