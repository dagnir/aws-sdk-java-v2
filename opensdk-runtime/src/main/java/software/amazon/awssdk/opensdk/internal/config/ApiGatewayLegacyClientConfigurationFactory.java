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

package software.amazon.awssdk.opensdk.internal.config;

import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.LegacyClientConfigurationFactory;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;

/**
 * Factory producing predefined {@link LegacyClientConfiguration} instances.
 */
@SdkInternalApi
public class ApiGatewayLegacyClientConfigurationFactory extends LegacyClientConfigurationFactory {

    public static final int DEFAULT_SOCKET_TIMEOUT = 35 * 1000;
    public static final boolean DEFAULT_CACHE_RESPONSE_METADATA = false;

    @Override
    protected LegacyClientConfiguration getDefaultConfig() {
        return super.getDefaultConfig().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)
                    .withCacheResponseMetadata(DEFAULT_CACHE_RESPONSE_METADATA)
                    .withSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
    }

    @Override
    protected LegacyClientConfiguration getInRegionOptimizedConfig() {
        return getDefaultConfig();
    }

}
