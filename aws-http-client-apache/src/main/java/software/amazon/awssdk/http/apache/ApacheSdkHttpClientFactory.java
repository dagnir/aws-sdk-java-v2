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

package software.amazon.awssdk.http.apache;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpConfigurationOptions;
import software.amazon.awssdk.http.apache.internal.Defaults;

/**
 * Factory for creating an instance of {@link SdkHttpClient}. The factory can be configured through the builder {@link
 * #builder()}, once built it  can create a {@link SdkHttpClient} via {@link #createHttpClient()} or can be passed to the SDK
 * client builders directly to have the SDK create and manage the HTTP client. See documentation on the service's respective
 * client builder for more information on configuring the HTTP layer.
 *
 * <pre class="brush: java">
 * SdkHttpClient httpClient = ApacheSdkHttpClientFactory.builder()
 * .socketTimeout(Duration.ofSeconds(10))
 * .build()
 * .createHttpClient();
 * </pre>
 */
public final class ApacheSdkHttpClientFactory implements SdkHttpClientFactory {

    @ReviewBeforeRelease("Confirm defaults")
    private static final SdkHttpConfigurationOptions GLOBAL_DEFAULTS = SdkHttpConfigurationOptions
            .createEmpty()
            .option(SdkHttpConfigurationOption.SOCKET_TIMEOUT, Defaults.SOCKET_TIMEOUT)
            .option(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Defaults.CONNECTION_TIMEOUT)
            .option(SdkHttpConfigurationOption.MAX_CONNECTIONS, Defaults.MAX_CONNECTIONS);

    private final SdkHttpConfigurationOptions standardOptions;
    private final ProxyConfiguration proxyConfiguration;
    private final Optional<InetAddress> localAddress;
    private final Optional<Boolean> expectContinueEnabled;
    private final Optional<Duration> connectionPoolTtl;
    private final Optional<Duration> maxIdleConnectionTimeout;

    private ApacheSdkHttpClientFactory(DefaultBuilder builder) {
        this.standardOptions = builder.standardOptions.copy();
        this.proxyConfiguration = builder.proxyConfiguration();
        this.localAddress = builder.localAddress();
        this.expectContinueEnabled = builder.expectContinueEnabled();
        this.connectionPoolTtl = builder.connectionPoolTtl();
        this.maxIdleConnectionTimeout = builder.maxIdleConnectionTimeout();
    }

    public ProxyConfiguration proxyConfiguration() {
        return proxyConfiguration;
    }

    public Optional<InetAddress> localAddress() {
        return localAddress;
    }

    public Optional<Boolean> expectContinueEnabled() {
        return expectContinueEnabled;
    }

    public Optional<Duration> connectionPoolTtl() {
        return connectionPoolTtl;
    }

    public Optional<Duration> maxIdleConnectionTime() {
        return maxIdleConnectionTimeout;
    }

    public SdkHttpClient createHttpClient() {
        return createHttpClientWithDefaults(SdkHttpConfigurationOptions.createEmpty());
    }

    @Override
    public SdkHttpClient createHttpClientWithDefaults(SdkHttpConfigurationOptions serviceDefaults) {
        SdkHttpConfigurationOptions resolvedOptions = standardOptions.merge(serviceDefaults).merge(GLOBAL_DEFAULTS);
        return new ApacheHttpClientFactory().create(this, resolvedOptions, createRequestConfig(resolvedOptions));
    }

    private ApacheHttpRequestConfig createRequestConfig(SdkHttpConfigurationOptions resolvedOptions) {
        return ApacheHttpRequestConfig.builder()
                                      .socketTimeout(resolvedOptions.option(SdkHttpConfigurationOption.SOCKET_TIMEOUT))
                                      .connectionTimeout(resolvedOptions.option(SdkHttpConfigurationOption.CONNECTION_TIMEOUT))
                                      .proxyConfiguration(proxyConfiguration)
                                      .localAddress(localAddress.orElse(null))
                                      .expectContinueEnabled(expectContinueEnabled.orElse(Defaults.EXPECT_CONTINUE_ENABLED))
                                      .build();
    }

    /**
     * @return Builder instance to construct a {@link ApacheSdkHttpClientFactory}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Builder for {@link ApacheSdkHttpClientFactory}.
     */
    public interface Builder {
        Builder socketTimeout(Duration socketTimeout);

        Optional<Duration> socketTimeout();

        Builder connectionTimeout(Duration connectionTimeout);

        Optional<Duration> connectionTimeout();

        Builder maxConnections(Integer maxConnections);

        Optional<Integer> maxConnections();

        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        ProxyConfiguration proxyConfiguration();

        Builder localAddress(InetAddress localAddress);

        Optional<InetAddress> localAddress();

        Builder expectContinueEnabled(Boolean expectContinueEnabled);

        Optional<Boolean> expectContinueEnabled();

        Builder connectionPoolTtl(Duration connectionPoolTtl);

        Optional<Duration> connectionPoolTtl();

        Builder maxIdleConnectionTimeout(Duration maxIdleConnectionTimeout);

        Optional<Duration> maxIdleConnectionTimeout();

        /**
         * @return An immutable {@link ApacheSdkHttpClientFactory} object.
         */
        ApacheSdkHttpClientFactory build();
    }

    /**
     * Builder for a {@link ApacheSdkHttpClientFactory}.
     */
    @ReviewBeforeRelease("Review the options we expose and revisit organization of options.")
    private static final class DefaultBuilder implements Builder {

        private final SdkHttpConfigurationOptions standardOptions = SdkHttpConfigurationOptions.createEmpty();
        private ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();
        private InetAddress localAddress;
        private Boolean expectContinueEnabled;
        private Duration connectionPoolTtl;
        private Duration maxIdleConnectionTimeout;

        private DefaultBuilder() {
        }

        @Override
        public Optional<Duration> socketTimeout() {
            return Optional.ofNullable(getSocketTimeout());
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.option(SdkHttpConfigurationOption.SOCKET_TIMEOUT, socketTimeout);
            return this;
        }

        public Duration getSocketTimeout() {
            return standardOptions.option(SdkHttpConfigurationOption.SOCKET_TIMEOUT);
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        @Override
        public Optional<Duration> connectionTimeout() {
            return Optional.ofNullable(getConnectionTimeout());
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.option(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        public Duration getConnectionTimeout() {
            return standardOptions.option(SdkHttpConfigurationOption.CONNECTION_TIMEOUT);
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        @Override
        public Optional<Integer> maxConnections() {
            return Optional.ofNullable(getMaxConnections());
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            standardOptions.option(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConnections);
            return this;
        }

        public Integer getMaxConnections() {
            return standardOptions.option(SdkHttpConfigurationOption.MAX_CONNECTIONS);
        }

        public void setMaxConnections(Integer maxConnections) {
            maxConnections(maxConnections);
        }

        @Override
        public ProxyConfiguration proxyConfiguration() {
            return proxyConfiguration;
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        public ProxyConfiguration getProxyConfiguration() {
            return proxyConfiguration;
        }

        public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
            proxyConfiguration(proxyConfiguration);
        }

        @Override
        public Optional<InetAddress> localAddress() {
            return Optional.ofNullable(getLocalAddress());
        }

        @Override
        public Builder localAddress(InetAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public InetAddress getLocalAddress() {
            return localAddress;
        }

        public void setLocalAddress(InetAddress localAddress) {
            localAddress(localAddress);
        }

        @Override
        public Optional<Boolean> expectContinueEnabled() {
            return Optional.ofNullable(getExpectContinueEnabled());
        }

        @Override
        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public Boolean getExpectContinueEnabled() {
            return expectContinueEnabled;
        }

        public void setExpectContinueEnabled(Boolean useExpectContinue) {
            this.expectContinueEnabled = useExpectContinue;
        }

        @Override
        public Optional<Duration> connectionPoolTtl() {
            return Optional.ofNullable(getConnectionPoolTtl());
        }

        @Override
        public Builder connectionPoolTtl(Duration connectionPoolTtl) {
            this.connectionPoolTtl = connectionPoolTtl;
            return this;
        }

        public Duration getConnectionPoolTtl() {
            return connectionPoolTtl;
        }

        public void setConnectionPoolTtl(Duration connectionPoolTtl) {
            connectionPoolTtl(connectionPoolTtl);
        }

        @Override
        public Optional<Duration> maxIdleConnectionTimeout() {
            return Optional.ofNullable(getMaxIdleConnectionTimeout());
        }

        @Override
        public Builder maxIdleConnectionTimeout(Duration maxIdleConnectionTimeout) {
            this.maxIdleConnectionTimeout = maxIdleConnectionTimeout;
            return this;
        }

        public Duration getMaxIdleConnectionTimeout() {
            return maxIdleConnectionTimeout;
        }

        public void setMaxIdleConnectionTimeout(Duration maxIdleConnectionTimeout) {
            maxIdleConnectionTimeout(maxIdleConnectionTimeout);
        }

        @Override
        public ApacheSdkHttpClientFactory build() {
            return new ApacheSdkHttpClientFactory(this);
        }
    }
}

