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
import software.amazon.awssdk.retry.RetryPolicy;

/**
 * Configure the automatic retry-on-failure behavior of the SDK.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
@ReviewBeforeRelease("Should we just rely on a retry policy encapsulating all of its required configuration?")
public final class ClientRetryConfiguration {
    private final Integer maxRetries;
    private final RetryPolicy retryPolicy;
    private final Boolean retryThrottlingEnabled;
    private final Integer maxRetriesBeforeThrottling;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientRetryConfiguration(DefaultClientRetryConfigurationBuilder builder) {
        this.maxRetries = builder.maxRetries;
        this.retryPolicy = builder.retryPolicy;
        this.retryThrottlingEnabled = builder.retryThrottlingEnabled;
        this.maxRetriesBeforeThrottling = builder.maxRetriesBeforeThrottling;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientRetryConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientRetryConfigurationBuilder();
    }

    /**
     * The maximum number of times that the SDK may retry a request when a retriable error is encountered.
     *
     * @see Builder#maxRetries(Integer)
     */
    public Optional<Integer> maxRetries() {
        return Optional.ofNullable(maxRetries);
    }

    /**
     * Whether retry throttling should be enabled.
     *
     * @see Builder#retryThrottlingEnabled(Boolean)
     */
    public Optional<Boolean> retryThrottlingEnabled() {
        return Optional.ofNullable(retryThrottlingEnabled);
    }

    /**
     * The number of retries encountered before retry throttling begins to be used.
     *
     * @see Builder#maxRetriesBeforeThrottling(Integer)
     */
    public Optional<Integer> maxRetriesBeforeThrottling() {
        return Optional.ofNullable(maxRetriesBeforeThrottling);
    }

    /**
     * The retry policy that should be used when handling failure cases.
     *
     * @see Builder#retryPolicy(RetryPolicy)
     */
    public Optional<RetryPolicy> retryPolicy() {
        return Optional.ofNullable(retryPolicy);
    }

    /**
     * A builder for {@link ClientRetryConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    interface Builder {
        /**
         * @see ClientRetryConfiguration#maxRetries().
         */
        Optional<Integer> maxRetries();

        /**
         * Configure the maximum number of times that the SDK may retry a request when a retriable error is encountered.
         *
         * @see ClientRetryConfiguration#maxRetries()
         */
        Builder maxRetries(Integer maxErrorRetries);

        /**
         * @see ClientRetryConfiguration#retryThrottlingEnabled().
         */
        Optional<Boolean> retryThrottlingEnabled();

        /**
         * Configure whether retry throttling should be enabled.
         *
         * @see ClientRetryConfiguration#retryThrottlingEnabled()
         */
        Builder retryThrottlingEnabled(Boolean enableRetryThrottling);

        /**
         * @see ClientRetryConfiguration#maxRetriesBeforeThrottling().
         */
        Optional<Integer> maxRetriesBeforeThrottling();

        /**
         * Configure the number of retries encountered before retry throttling begins to be used.
         *
         * @see ClientRetryConfiguration#maxRetriesBeforeThrottling()
         */
        Builder maxRetriesBeforeThrottling(Integer maxRetriesBeforeThrottling);

        /**
         * @see ClientRetryConfiguration#retryPolicy().
         */
        Optional<RetryPolicy> retryPolicy();

        /**
         * Configure the retry policy that should be used when handling failure cases.
         *
         * @see ClientRetryConfiguration#retryPolicy()
         */
        Builder retryPolicy(RetryPolicy retryPolicy);

        /**
         * Build a {@link ClientRetryConfiguration} from the values currently configured in this builder.
         */
        ClientRetryConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientRetryConfigurationBuilder implements Builder {
        private Integer maxRetries;
        private RetryPolicy retryPolicy;
        private Boolean retryThrottlingEnabled;
        private Integer maxRetriesBeforeThrottling;

        @Override
        public Optional<Integer> maxRetries() {
            return Optional.ofNullable(maxRetries);
        }

        @Override
        public Builder maxRetries(Integer maxErrorRetries) {
            this.maxRetries = maxErrorRetries;
            return this;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            maxRetries(maxRetries);
        }

        @Override
        public Optional<RetryPolicy> retryPolicy() {
            return Optional.ofNullable(retryPolicy);
        }

        @Override
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public RetryPolicy getRetryPolicy() {
            return retryPolicy;
        }

        public void setRetryPolicy(RetryPolicy retryPolicy) {
            retryPolicy(retryPolicy);
        }

        @Override
        public Optional<Boolean> retryThrottlingEnabled() {
            return Optional.ofNullable(retryThrottlingEnabled);
        }

        @Override
        public Builder retryThrottlingEnabled(Boolean enableRetryThrottling) {
            this.retryThrottlingEnabled = enableRetryThrottling;
            return this;
        }

        public Boolean getRetryThrottlingEnabled() {
            return retryThrottlingEnabled;
        }

        public void setRetryThrottlingEnabled(Boolean retryThrottlingEnabled) {
            retryThrottlingEnabled(retryThrottlingEnabled);
        }

        @Override
        public Optional<Integer> maxRetriesBeforeThrottling() {
            return Optional.ofNullable(maxRetriesBeforeThrottling);
        }

        @Override
        public Builder maxRetriesBeforeThrottling(Integer maxRetriesBeforeThrottling) {
            this.maxRetriesBeforeThrottling = maxRetriesBeforeThrottling;
            return this;
        }

        public Integer getMaxRetriesBeforeThrottling() {
            return maxRetriesBeforeThrottling;
        }

        public void setMaxRetriesBeforeThrottling(Integer maxRetriesBeforeThrottling) {
            maxRetriesBeforeThrottling(maxRetriesBeforeThrottling);
        }

        @Override
        public ClientRetryConfiguration build() {
            return new ClientRetryConfiguration(this);
        }
    }
}
