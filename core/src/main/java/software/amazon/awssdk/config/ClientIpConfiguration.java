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

import java.net.InetAddress;
import java.util.Optional;
import software.amazon.awssdk.DnsResolver;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;

/**
 * Configures the low-level IP layer behavior of the AWS SDK client.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
@ReviewBeforeRelease("Configuration descriptions here are relatively short because it is expected that this will be "
                     + "heavily refactored for the pluggable HTTP layer. If that ends up not happening, these descriptions "
                     + "should be enhanced.")
public final class ClientIpConfiguration {
    private final InetAddress localAddress;
    private final DnsResolver dnsResolver;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientIpConfiguration(DefaultClientIpConfigurationBuilder builder) {
        this.localAddress = builder.localAddress;
        this.dnsResolver = builder.dnsResolver;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientIpConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientIpConfigurationBuilder();
    }

    /**
     * The local address that the AWS client should use for communication.
     *
     * @see Builder#localAddress(InetAddress)
     */
    public Optional<InetAddress> localAddress() {
        return Optional.ofNullable(localAddress);
    }

    /**
     * Special logic to be used when resolving DNS entries.
     *
     * @see Builder#dnsResolver(DnsResolver)
     */
    public Optional<DnsResolver> dnsResolver() {
        return Optional.ofNullable(dnsResolver);
    }

    /**
     * A builder for {@link ClientIpConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    interface Builder {
        /**
         * @see ClientIpConfiguration#localAddress().
         */
        Optional<InetAddress> localAddress();

        /**
         * Configure the local address that the AWS client should use for communication.
         *
         * @see ClientIpConfiguration#localAddress()
         */
        Builder localAddress(InetAddress localAddress);

        /**
         * @see ClientIpConfiguration#dnsResolver().
         */
        Optional<DnsResolver> dnsResolver();

        /**
         * Configure special logic to be used when resolving DNS entries.
         *
         * @see ClientIpConfiguration#dnsResolver()
         */
        Builder dnsResolver(DnsResolver dnsResolver);

        /**
         * Build a {@link ClientIpConfiguration} from the values currently configured in this builder.
         */
        ClientIpConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientIpConfigurationBuilder implements Builder {
        private InetAddress localAddress;
        private DnsResolver dnsResolver;

        @Override
        public Optional<InetAddress> localAddress() {
            return Optional.ofNullable(localAddress);
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
        public Optional<DnsResolver> dnsResolver() {
            return Optional.ofNullable(dnsResolver);
        }

        @Override
        public Builder dnsResolver(DnsResolver dnsResolver) {
            this.dnsResolver = dnsResolver;
            return this;
        }

        public DnsResolver getDnsResolver() {
            return dnsResolver;
        }

        public void setDnsResolver(DnsResolver dnsResolver) {
            dnsResolver(dnsResolver);
        }

        @Override
        public ClientIpConfiguration build() {
            return new ClientIpConfiguration(this);
        }
    }
}
