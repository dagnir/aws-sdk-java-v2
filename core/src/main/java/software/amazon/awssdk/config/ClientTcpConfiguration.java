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

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;

/**
 * Configures the TCP connection behavior of the AWS SDK client.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
@ReviewBeforeRelease("Configuration descriptions here are relatively short because it is expected that this will be "
                     + "heavily refactored for the pluggable HTTP layer. If that ends up not happening, these descriptions "
                     + "should be enhanced.")
public final class ClientTcpConfiguration {
    private final Boolean tcpKeepaliveEnabled;
    private final Integer maxConnections;
    private final Duration connectionTimeToLive;
    private final Duration connectionMaxIdleTime;
    private final Duration connectionValidationFrequency;
    private final Integer socketReceiveBufferSizeHint;
    private final Integer socketSendBufferSizeHint;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientTcpConfiguration(DefaultClientTcpConfigurationBuilder builder) {
        this.tcpKeepaliveEnabled = builder.tcpKeepaliveEnabled;
        this.maxConnections = builder.maxConnections;
        this.connectionTimeToLive = builder.connectionTimeToLive;
        this.connectionMaxIdleTime = builder.connectionMaxIdleTime;
        this.connectionValidationFrequency = builder.connectionValidationFrequency;
        this.socketReceiveBufferSizeHint = builder.socketReceiveBufferSizeHint;
        this.socketSendBufferSizeHint = builder.socketSendBufferSizeHint;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientTcpConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientTcpConfigurationBuilder();
    }

    /**
     * Whether TCP keepalive should be enabled on the connections to AWS.
     *
     * @see Builder#tcpKeepaliveEnabled(Boolean)
     */
    public Optional<Boolean> tcpKeepaliveEnabled() {
        return Optional.ofNullable(tcpKeepaliveEnabled);
    }

    /**
     * The maximum number of TCP connections to be created to AWS.
     *
     * @see Builder#maxConnections(Integer)
     */
    public Optional<Integer> maxConnections() {
        return Optional.ofNullable(maxConnections);
    }

    /**
     * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
     *
     * @see Builder#connectionTimeToLive(Duration)
     */
    public Optional<Duration> connectionTimeToLive() {
        return Optional.ofNullable(connectionTimeToLive);
    }

    /**
     * The maximum amount of time that a connection should be allowed to remain open while idle.
     *
     * @see Builder#connectionMaxIdleTime(Duration)
     */
    public Optional<Duration> connectionMaxIdleTime() {
        return Optional.ofNullable(connectionMaxIdleTime);
    }

    /**
     * The amount of time that a connection can be idle in the connection pool before it must be validated to ensure it's still
     * open.
     *
     * @see Builder#connectionValidationFrequency(Duration)
     */
    public Optional<Duration> connectionValidationFrequency() {
        return Optional.ofNullable(connectionValidationFrequency);
    }

    /**
     * A size hint (in bytes) for the low level TCP receive buffers.
     *
     * @see Builder#socketReceiveBufferSizeHint(Integer)
     */
    public Optional<Integer> socketReceiveBufferSizeHint() {
        return Optional.ofNullable(socketReceiveBufferSizeHint);
    }

    /**
     * A size hint (in bytes) for the low level TCP send buffers.
     *
     * @see Builder#socketSendBufferSizeHint(Integer)
     */
    public Optional<Integer> socketSendBufferSizeHint() {
        return Optional.ofNullable(socketSendBufferSizeHint);
    }

    /**
     * A builder for {@link ClientTcpConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    interface Builder {
        /**
         * @see ClientTcpConfiguration#tcpKeepaliveEnabled().
         */
        Optional<Boolean> tcpKeepaliveEnabled();

        /**
         * Configure whether TCP keepalive should be enabled on the connections to AWS.
         *
         * @see ClientTcpConfiguration#tcpKeepaliveEnabled()
         */
        Builder tcpKeepaliveEnabled(Boolean tcpKeepaliveEnabled);

        /**
         * @see ClientTcpConfiguration#maxConnections().
         */
        Optional<Integer> maxConnections();

        /**
         * Configure the maximum number of TCP connections to be created to AWS.
         *
         * @see ClientTcpConfiguration#maxConnections()
         */
        Builder maxConnections(Integer maxConnections);

        /**
         * @see ClientTcpConfiguration#connectionTimeToLive().
         */
        Optional<Duration> connectionTimeToLive();

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
         *
         * @see ClientTcpConfiguration#connectionTimeToLive()
         */
        Builder connectionTimeToLive(Duration connectionTimeToLive);

        /**
         * @see ClientTcpConfiguration#connectionMaxIdleTime().
         */
        Optional<Duration> connectionMaxIdleTime();

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
         *
         * @see ClientTcpConfiguration#connectionMaxIdleTime()
         */
        Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);

        /**
         * @see ClientTcpConfiguration#connectionValidationFrequency().
         */
        Optional<Duration> connectionValidationFrequency();

        /**
         * Configure the amount of time that a connection can be idle in the connection pool before it must be validated to ensure
         * it's still open.
         *
         * @see ClientTcpConfiguration#connectionValidationFrequency()
         */
        Builder connectionValidationFrequency(Duration connectionValidationFrequency);


        /**
         * @see ClientTcpConfiguration#socketReceiveBufferSizeHint().
         */
        Optional<Integer> socketReceiveBufferSizeHint();

        /**
         * Configure a size hint (in bytes) for the low level TCP receive buffers.
         *
         * @see ClientTcpConfiguration#socketReceiveBufferSizeHint()
         */
        Builder socketReceiveBufferSizeHint(Integer socketReceiveBufferSizeHint);

        /**
         * @see ClientTcpConfiguration#socketSendBufferSizeHint().
         */
        Optional<Integer> socketSendBufferSizeHint();

        /**
         * Configure a size hint (in bytes) for the low level TCP send buffers.
         *
         * @see ClientTcpConfiguration#socketSendBufferSizeHint()
         */
        Builder socketSendBufferSizeHint(Integer socketSendBufferSizeHint);

        /**
         * Build a {@link ClientTcpConfiguration} from the values currently configured in this builder.
         */
        ClientTcpConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientTcpConfigurationBuilder implements Builder {
        private Boolean tcpKeepaliveEnabled;
        private Integer maxConnections;
        private Duration connectionTimeToLive;
        private Duration connectionMaxIdleTime;
        private Duration connectionValidationFrequency;
        private Integer socketReceiveBufferSizeHint;
        private Integer socketSendBufferSizeHint;

        @Override
        public Optional<Boolean> tcpKeepaliveEnabled() {
            return Optional.ofNullable(tcpKeepaliveEnabled);
        }

        @Override
        public Builder tcpKeepaliveEnabled(Boolean tcpKeepaliveEnabled) {
            this.tcpKeepaliveEnabled = tcpKeepaliveEnabled;
            return this;
        }

        public Boolean getTcpKeepaliveEnabled() {
            return tcpKeepaliveEnabled;
        }

        public void setTcpKeepaliveEnabled(Boolean tcpKeepaliveEnabled) {
            tcpKeepaliveEnabled(tcpKeepaliveEnabled);
        }

        @Override
        public Optional<Integer> maxConnections() {
            return Optional.ofNullable(maxConnections);
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Integer getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(Integer maxConnections) {
            maxConnections(maxConnections);
        }

        @Override
        public Optional<Duration> connectionTimeToLive() {
            return Optional.ofNullable(connectionTimeToLive);
        }

        @Override
        public Builder connectionTimeToLive(Duration connectionTimeToLive) {
            this.connectionTimeToLive = connectionTimeToLive;
            return this;
        }

        public Duration getConnectionTimeToLive() {
            return connectionTimeToLive;
        }

        public void setConnectionTimeToLive(Duration connectionTimeToLive) {
            connectionTimeToLive(connectionTimeToLive);
        }

        @Override
        public Optional<Duration> connectionMaxIdleTime() {
            return Optional.ofNullable(connectionMaxIdleTime);
        }

        @Override
        public Builder connectionMaxIdleTime(Duration connectionMaxIdleTime) {
            this.connectionMaxIdleTime = connectionMaxIdleTime;
            return this;
        }

        public Duration getConnectionMaxIdleTime() {
            return connectionMaxIdleTime;
        }

        public void setConnectionMaxIdleTime(Duration connectionMaxIdleTime) {
            connectionMaxIdleTime(connectionMaxIdleTime);
        }

        @Override
        public Optional<Duration> connectionValidationFrequency() {
            return Optional.ofNullable(connectionValidationFrequency);
        }

        @Override
        public Builder connectionValidationFrequency(Duration connectionValidationFrequency) {
            this.connectionValidationFrequency = connectionValidationFrequency;
            return this;
        }

        public Duration getConnectionValidationFrequency() {
            return connectionValidationFrequency;
        }

        public void setConnectionValidationFrequency(Duration connectionValidationFrequency) {
            connectionValidationFrequency(connectionValidationFrequency);
        }

        @Override
        public Optional<Integer> socketReceiveBufferSizeHint() {
            return Optional.ofNullable(socketReceiveBufferSizeHint);
        }

        @Override
        public Builder socketReceiveBufferSizeHint(Integer socketReceiveBufferSizeHint) {
            this.socketReceiveBufferSizeHint = socketReceiveBufferSizeHint;
            return this;
        }

        public Integer getSocketReceiveBufferSizeHint() {
            return socketReceiveBufferSizeHint;
        }

        public void setSocketReceiveBufferSizeHint(Integer socketReceiveBufferSizeHint) {
            socketReceiveBufferSizeHint(socketReceiveBufferSizeHint);
        }

        @Override
        public Optional<Integer> socketSendBufferSizeHint() {
            return Optional.ofNullable(socketSendBufferSizeHint);
        }

        @Override
        public Builder socketSendBufferSizeHint(Integer socketSendBufferSizeHint) {
            this.socketSendBufferSizeHint = socketSendBufferSizeHint;
            return this;
        }

        public Integer getSocketSendBufferSizeHint() {
            return socketSendBufferSizeHint;
        }

        public void setSocketSendBufferSizeHint(Integer socketSendBufferSizeHint) {
            socketSendBufferSizeHint(socketSendBufferSizeHint);
        }

        @Override
        public ClientTcpConfiguration build() {
            return new ClientTcpConfiguration(this);
        }
    }
}
