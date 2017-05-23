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

package software.amazon.awssdk.config.defaults;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.ClientMarshallerConfiguration;
import software.amazon.awssdk.config.ClientMetricsConfiguration;
import software.amazon.awssdk.config.ClientRetryConfiguration;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.util.VersionInfoUtils;

/**
 * A decorator for {@link ClientConfiguration} that adds global default values. This is the lowest-priority configuration
 * decorator that attempts to fill in any required values that higher-priority configurations (eg. service-specific
 * configurations
 * or customer-provided configurations) haven't already overridden.
 */
@SdkInternalApi
public final class GlobalClientConfigurationDefaults extends ClientConfigurationDefaults {

    @Override
    protected void applyMarshallerDefaults(ClientMarshallerConfiguration.Builder builder) {
        Supplier<Boolean> defaultValue = () -> false;
        builder.gzipEnabled(builder.gzipEnabled().orElseGet(defaultValue));
    }

    @Override
    protected void applyMetricsDefaults(ClientMetricsConfiguration.Builder builder) {
        builder.userAgentPrefix(builder.userAgentPrefix().orElseGet(VersionInfoUtils::getUserAgent));
        Supplier<String> defaultValue1 = () -> "";
        builder.userAgentSuffix(builder.userAgentSuffix().orElseGet(defaultValue1));
        Supplier<RequestMetricCollector> defaultValue = () -> RequestMetricCollector.NONE;
        builder.requestMetricCollector(builder.requestMetricCollector().orElseGet(defaultValue));
    }

    @Override
    protected void applyRetryDefaults(ClientRetryConfiguration.Builder builder) {
        Supplier<RetryPolicy> defaultValue = () -> PredefinedRetryPolicies.DEFAULT;
        builder.retryPolicy(builder.retryPolicy().orElseGet(defaultValue));
    }

    @Override
    protected ExecutorService getAsyncExecutorDefault(Integer maxConnections) {
        return maxConnections == null ? null : Executors.newFixedThreadPool(maxConnections);
    }
}
