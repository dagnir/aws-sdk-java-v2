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
import software.amazon.awssdk.retry.RetryPolicy;

/**
 * Configure the automatic retry-on-failure behavior of the SDK.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
@ReviewBeforeRelease("Should we just rely on a retry policy encapsulating all of its required configuration?")
public final class ClientRetryConfiguration
        implements ToCopyableBuilder<ClientRetryConfiguration.Builder, ClientRetryConfiguration> {
    private final RetryPolicy retryPolicy;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientRetryConfiguration(DefaultClientRetryConfigurationBuilder builder) {
        this.retryPolicy = builder.retryPolicy;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientRetryConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientRetryConfigurationBuilder();
    }

    @Override
    public ClientRetryConfiguration.Builder toBuilder() {
        return builder().retryPolicy(retryPolicy);
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
    public interface Builder extends CopyableBuilder<Builder, ClientRetryConfiguration> {
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
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientRetryConfigurationBuilder implements Builder {
        private RetryPolicy retryPolicy;

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
        public ClientRetryConfiguration build() {
            return new ClientRetryConfiguration(this);
        }
    }
}
