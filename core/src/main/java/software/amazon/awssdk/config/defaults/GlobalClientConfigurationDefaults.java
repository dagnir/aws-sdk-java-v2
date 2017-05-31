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

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.ClientHttpConfiguration;
import software.amazon.awssdk.config.ClientMarshallerConfiguration;
import software.amazon.awssdk.config.ClientMetricsConfiguration;
import software.amazon.awssdk.config.ClientRetryConfiguration;
import software.amazon.awssdk.config.ClientSecurityConfiguration;
import software.amazon.awssdk.config.ClientTcpConfiguration;
import software.amazon.awssdk.config.ClientTimeoutConfiguration;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.util.VersionInfoUtils;

/**
 * A decorator for {@link ClientConfiguration} that adds global default values. This is the lowest-priority configuration
 * decorator that attempts to fill in any required values that higher-priority configurations (eg. service-specific configurations
 * or customer-provided configurations) haven't already overridden.
 */
@SdkInternalApi
public final class GlobalClientConfigurationDefaults extends ClientConfigurationDefaults {
    @Override
    protected void applyHttpDefaults(ClientHttpConfiguration.Builder builder) {
        builder.expectContinueEnabled(builder.expectContinueEnabled().orElse(true));
    }

    @Override
    protected void applyTcpDefaults(ClientTcpConfiguration.Builder builder) {
        builder.maxConnections(builder.maxConnections().orElse(50));

        builder.connectionMaxIdleTime(builder.connectionMaxIdleTime().orElseGet(() -> Duration.ofSeconds(60)));
        builder.connectionValidationFrequency(builder.connectionValidationFrequency().orElseGet(() -> Duration.ofSeconds(5)));

        builder.tcpKeepaliveEnabled(builder.tcpKeepaliveEnabled().orElse(false));
    }

    @Override
    protected void applyTimeoutDefaults(ClientTimeoutConfiguration.Builder builder) {
        builder.connectionTimeout(builder.connectionTimeout().orElseGet(() -> Duration.ofSeconds(10)));
        builder.socketTimeout(builder.socketTimeout().orElseGet(() -> Duration.ofSeconds(50)));
    }

    @Override
    protected void applyMarshallerDefaults(ClientMarshallerConfiguration.Builder builder) {
        builder.gzipEnabled(builder.gzipEnabled().orElse(false));
    }

    @Override
    protected void applyMetricsDefaults(ClientMetricsConfiguration.Builder builder) {
        builder.userAgentPrefix(builder.userAgentPrefix().orElseGet(VersionInfoUtils::getUserAgent));
        builder.userAgentSuffix(builder.userAgentSuffix().orElse(""));
        builder.requestMetricCollector(builder.requestMetricCollector().orElse(RequestMetricCollector.NONE));
    }

    @Override
    protected void applySecurityDefaults(ClientSecurityConfiguration.Builder builder) {
        builder.secureRandom(builder.secureRandom().orElseGet(SecureRandom::new));
    }

    @Override
    protected void applyRetryDefaults(ClientRetryConfiguration.Builder builder) {
        builder.retryPolicy(builder.retryPolicy().orElse(PredefinedRetryPolicies.DEFAULT));
    }

    @Override
    protected ExecutorService getAsyncExecutorDefault(Integer maxConnections) {
        return maxConnections == null ? null : Executors.newFixedThreadPool(maxConnections);
    }
}
