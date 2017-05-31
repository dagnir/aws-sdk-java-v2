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
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;
import software.amazon.awssdk.runtime.auth.SignerProvider;

/**
 * Configuration related to the security of the integration with AWS.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
public final class ClientSecurityConfiguration
        implements ToCopyableBuilder<ClientSecurityConfiguration.Builder, ClientSecurityConfiguration> {

    private final SignerProvider signerProvider;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientSecurityConfiguration(DefaultClientSecurityConfigurationBuilder builder) {
        this.signerProvider = builder.signerProvider;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientSecurityConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientSecurityConfigurationBuilder();
    }

    @Override
    public ClientSecurityConfiguration.Builder toBuilder() {
        return builder().signerProvider(signerProvider);
    }

    /**
     * The signer factory that should be used when generating signers in communication with AWS.
     *
     * @see Builder#signerProvider(SignerProvider)
     */
    public Optional<SignerProvider> signerProvider() {
        return Optional.ofNullable(signerProvider);
    }

    /**
     * A builder for {@link ClientSecurityConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientSecurityConfiguration> {
        /**
         * @see ClientSecurityConfiguration#signerProvider().
         */
        Optional<SignerProvider> signerProvider();

        /**
         * Configure the signer factory that should be used when generating signers in communication with AWS.
         *
         * @see ClientSecurityConfiguration#signerProvider()
         */
        Builder signerProvider(SignerProvider signerProvider);

    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientSecurityConfigurationBuilder implements Builder {
        private SignerProvider signerProvider;

        @Override
        public Optional<SignerProvider> signerProvider() {
            return Optional.ofNullable(signerProvider);
        }

        @Override
        public Builder signerProvider(SignerProvider signerProvider) {
            this.signerProvider = signerProvider;
            return this;
        }

        public SignerProvider getSignerProvider() {
            return signerProvider;
        }

        public void setSignerProvider(SignerProvider signerProvider) {
            signerProvider(signerProvider);
        }

        @Override
        public ClientSecurityConfiguration build() {
            return new ClientSecurityConfiguration(this);
        }
    }
}
