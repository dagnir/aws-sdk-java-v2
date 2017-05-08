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

package software.amazon.awssdk.config;

import java.util.Optional;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;
import software.amazon.awssdk.metrics.RequestMetricCollector;

/**
 * Configuration related to metrics gathered by the SDK.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
@ReviewBeforeRelease("This will likely be updated when we change the way metrics work. If not, we should clean up the "
                     + "documentation.")
public final class ClientMetricsConfiguration
        implements ToCopyableBuilder<ClientMetricsConfiguration.Builder, ClientMetricsConfiguration> {
    private final String userAgentPrefix;
    private final String userAgentSuffix;
    private final RequestMetricCollector requestMetricCollector;

    private ClientMetricsConfiguration(DefaultClientMetricsConfigurationBuilder builder) {
        this.userAgentPrefix = builder.userAgentPrefix;
        this.userAgentSuffix = builder.userAgentSuffix;
        this.requestMetricCollector = builder.requestMetricCollector;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientMetricsConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientMetricsConfigurationBuilder();
    }

    @Override
    public ClientMetricsConfiguration.Builder toBuilder() {
        return builder().userAgentPrefix(userAgentPrefix)
                        .userAgentSuffix(userAgentSuffix)
                        .requestMetricCollector(requestMetricCollector);
    }

    /**
     * The prefix of the user agent that is sent with each request to AWS.
     *
     * @see Builder#userAgentPrefix(String)
     */
    public Optional<String> userAgentPrefix() {
        return Optional.ofNullable(userAgentPrefix);
    }

    /**
     * The suffix of the user agent that is sent with each request to AWS.
     *
     * @see Builder#userAgentSuffix(String)
     */
    public Optional<String> userAgentSuffix() {
        return Optional.ofNullable(userAgentSuffix);
    }

    /**
     * The metric collector that should be notified of each request event.
     *
     * @see Builder#requestMetricCollector(RequestMetricCollector)
     */
    public Optional<RequestMetricCollector> requestMetricCollector() {
        return Optional.ofNullable(requestMetricCollector);
    }

    /**
     * A builder for {@link ClientMetricsConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientMetricsConfiguration> {

        /**
         * @see ClientMetricsConfiguration#userAgentPrefix().
         */
        Optional<String> userAgentPrefix();

        /**
         * Set the prefix of the user agent that is sent with each request to AWS.
         *
         * @see ClientMetricsConfiguration#userAgentPrefix()
         */
        Builder userAgentPrefix(String userAgentPrefix);

        /**
         * @see ClientMetricsConfiguration#userAgentSuffix().
         */
        Optional<String> userAgentSuffix();

        /**
         * Set the suffix of the user agent that is sent with each request to AWS.
         *
         * @see ClientMetricsConfiguration#userAgentSuffix()
         */
        Builder userAgentSuffix(String userAgentSuffix);

        /**
         * @see ClientMetricsConfiguration#requestMetricCollector().
         */
        Optional<RequestMetricCollector> requestMetricCollector();

        /**
         * Set the metric collector that should be notified of each request event.
         *
         * @see ClientMetricsConfiguration#requestMetricCollector()
         */
        Builder requestMetricCollector(RequestMetricCollector metricCollector);
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientMetricsConfigurationBuilder implements Builder {
        private String userAgentPrefix;
        private String userAgentSuffix;
        private RequestMetricCollector requestMetricCollector;

        @Override
        public Optional<String> userAgentPrefix() {
            return Optional.ofNullable(userAgentPrefix);
        }

        @Override
        public Builder userAgentPrefix(String userAgentPrefix) {
            this.userAgentPrefix = userAgentPrefix;
            return this;
        }

        public String getUserAgentPrefix() {
            return userAgentPrefix;
        }

        public void setUserAgentPrefix(String userAgentPrefix) {
            userAgentPrefix(userAgentPrefix);
        }

        @Override
        public Optional<String> userAgentSuffix() {
            return Optional.ofNullable(userAgentSuffix);
        }

        @Override
        public Builder userAgentSuffix(String userAgentSuffix) {
            this.userAgentSuffix = userAgentSuffix;
            return this;
        }

        public String getUserAgentSuffix() {
            return userAgentSuffix;
        }

        public void setUserAgentSuffix(String userAgentSuffix) {
            userAgentSuffix(userAgentSuffix);
        }

        @Override
        public Optional<RequestMetricCollector> requestMetricCollector() {
            return Optional.ofNullable(requestMetricCollector);
        }

        @Override
        public Builder requestMetricCollector(RequestMetricCollector requestMetricCollector) {
            this.requestMetricCollector = requestMetricCollector;
            return this;
        }

        public RequestMetricCollector getRequestMetricCollector() {
            return requestMetricCollector;
        }

        public void setRequestMetricCollector(RequestMetricCollector requestMetricCollector) {
            requestMetricCollector(requestMetricCollector);
        }

        @Override
        public ClientMetricsConfiguration build() {
            return new ClientMetricsConfiguration(this);
        }
    }
}
