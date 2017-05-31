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
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;

/**
 * Configuration specifying request and response timeouts within the SDK.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
public final class ClientTimeoutConfiguration
        implements ToCopyableBuilder<ClientTimeoutConfiguration.Builder, ClientTimeoutConfiguration> {

    private final Duration httpRequestTimeout;
    private final Duration totalExecutionTimeout;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientTimeoutConfiguration(DefaultClientTimeoutConfigurationBuilder builder) {
        this.httpRequestTimeout = builder.httpRequestTimeout;
        this.totalExecutionTimeout = builder.totalExecutionTimeout;
    }

    @Override
    public ClientTimeoutConfiguration.Builder toBuilder() {
        return builder().httpRequestTimeout(httpRequestTimeout)
                .totalExecutionTimeout(totalExecutionTimeout);
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientTimeoutConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientTimeoutConfigurationBuilder();
    }

    /**
     * The amount of time to wait for the request to complete before giving up and timing out. An empty value disables this
     * feature.
     *
     * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout when
     * reading the response. For APIs that return large responses this could be expensive.</p>
     *
     * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
     * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that don't
     * get aborted until several seconds after the timer has been breached. Because of this, the request timeout feature should
     * not be used when absolute precision is needed.</p>
     *
     * @see Builder#httpRequestTimeout(Duration)
     */
    public Optional<Duration> httpRequestTimeout() {
        return Optional.ofNullable(httpRequestTimeout);
    }

    /**
     * The amount of time to allow the client to complete the execution of an API call. This timeout covers the entire client
     * execution except for marshalling. This includes request handler execution, all HTTP requests including retries,
     * unmarshalling, etc. An empty value disables this feature.
     *
     * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout when
     * reading the response. For APIs that return large responses this could be expensive.</p>
     *
     * <p>The client execution timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
     * timeout
     * is breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
     * don't get aborted until several seconds after the timer has been breached. Because of this, the client execution timeout
     * feature should not be used when absolute precision is needed.</p>
     *
     * <p>This may be used together with {@link #httpRequestTimeout()} to enforce both a timeout on each individual HTTP request
     * (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'client execution' time). A
     * non-positive value disables this feature.</p>
     *
     * @see Builder#totalExecutionTimeout(Duration)
     */
    public Optional<Duration> totalExecutionTimeout() {
        return Optional.ofNullable(totalExecutionTimeout);
    }

    /**
     * A builder for {@link ClientTimeoutConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientTimeoutConfiguration> {

        /**
         * @see ClientTimeoutConfiguration#httpRequestTimeout().
         */
        Optional<Duration> httpRequestTimeout();

        /**
         * Configure the amount of time to wait for the request to complete before giving up and timing out. A non-positive value
         * disables this feature.
         *
         * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout
         * when reading the response. For APIs that return large responses this could be expensive.</p>
         *
         * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
         * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
         * don't get aborted until several seconds after the timer has been breached. Because of this, the request timeout
         * feature
         * should not be used when absolute precision is needed.</p>
         *
         * @see ClientTimeoutConfiguration#httpRequestTimeout()
         */
        Builder httpRequestTimeout(Duration httpRequestTimeout);

        /**
         * @see ClientTimeoutConfiguration#totalExecutionTimeout().
         */
        Optional<Duration> totalExecutionTimeout();

        /**
         * Configure the amount of time to allow the client to complete the execution of an API call. This timeout covers the
         * entire client execution except for marshalling. This includes request handler execution, all HTTP request including
         * retries, unmarshalling, etc.
         *
         * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout
         * when reading the response. For APIs that return large responses this could be expensive.</p>
         *
         * <p>The client execution timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
         * timeout is breached. The typical case aborts the request within a few milliseconds but there may occasionally be
         * requests that don't get aborted until several seconds after the timer has been breached. Because of this, the client
         * execution timeout feature should not be used when absolute precision is needed.</p>
         *
         * <p>This may be used together with {@link #httpRequestTimeout()} to enforce both a timeout on each individual HTTP
         * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'client execution' time).
         * A non-positive value disables this feature.</p>
         *
         * @see ClientTimeoutConfiguration#totalExecutionTimeout()
         */
        Builder totalExecutionTimeout(Duration totalExecutionTimeout);
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientTimeoutConfigurationBuilder implements Builder {
        private Duration httpRequestTimeout;
        private Duration totalExecutionTimeout;

        @Override
        public Optional<Duration> httpRequestTimeout() {
            return Optional.ofNullable(httpRequestTimeout);
        }

        @Override
        public Builder httpRequestTimeout(Duration httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
            return this;
        }

        public Duration getHttpRequestTimeout() {
            return httpRequestTimeout;
        }

        public void setHttpRequestTimeout(Duration httpRequestTimeout) {
            httpRequestTimeout(httpRequestTimeout);
        }

        @Override
        public Optional<Duration> totalExecutionTimeout() {
            return Optional.ofNullable(totalExecutionTimeout);
        }

        @Override
        public Builder totalExecutionTimeout(Duration totalExecutionTimeout) {
            this.totalExecutionTimeout = totalExecutionTimeout;
            return this;
        }

        public Duration getTotalExecutionTimeout() {
            return totalExecutionTimeout;
        }

        public void setTotalExecutionTimeout(Duration totalExecutionTimeout) {
            totalExecutionTimeout(totalExecutionTimeout);
        }

        @Override
        public ClientTimeoutConfiguration build() {
            return new ClientTimeoutConfiguration(this);
        }
    }
}
