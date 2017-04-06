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

/**
 * Configuration that allows manipulating the way in which the SDK converts request objects to messages to be sent to AWS.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
public final class ClientMarshallerConfiguration {
    @ReviewBeforeRelease("Should this be included in the HTTP configuration object?")
    private final Boolean gzipEnabled;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientMarshallerConfiguration(DefaultClientMarshallerConfigurationBuilder builder) {
        this.gzipEnabled = builder.gzipEnabled;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientMarshallerConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientMarshallerConfigurationBuilder();
    }

    /**
     * Whether GZIP should be used when communication with AWS.
     *
     * @see Builder#gzipEnabled(Boolean)
     */
    public Optional<Boolean> gzipEnabled() {
        return Optional.ofNullable(gzipEnabled);
    }

    /**
     * A builder for {@link ClientMarshallerConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    interface Builder {
        /**
         * @see ClientMarshallerConfiguration#gzipEnabled().
         */
        Optional<Boolean> gzipEnabled();

        /**
         * Configure whether GZIP should be used when communicating with AWS. Enabling GZIP increases CPU utilization and memory
         * usage, while decreasing the amount of data sent over the network.
         *
         * @see ClientMarshallerConfiguration#gzipEnabled()
         */
        Builder gzipEnabled(Boolean gzipEnabled);

        /**
         * Build a {@link ClientMarshallerConfiguration} from the values currently configured in this builder.
         */
        ClientMarshallerConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientMarshallerConfigurationBuilder implements Builder {
        private Boolean gzipEnabled;

        @Override
        public Optional<Boolean> gzipEnabled() {
            return Optional.ofNullable(gzipEnabled);
        }

        @Override
        public Builder gzipEnabled(Boolean gzipEnabled) {
            this.gzipEnabled = gzipEnabled;
            return this;
        }

        public Boolean getGzipEnabled() {
            return gzipEnabled;
        }

        public void setGzipEnabled(Boolean gzipEnabled) {
            gzipEnabled(gzipEnabled);
        }

        @Override
        public ClientMarshallerConfiguration build() {
            return new ClientMarshallerConfiguration(this);
        }
    }
}
