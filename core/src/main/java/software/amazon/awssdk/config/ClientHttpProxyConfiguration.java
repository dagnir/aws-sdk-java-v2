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

import static software.amazon.awssdk.utils.StringUtils.isEmpty;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.utils.Validate;

/**
 * Configuration that defines how to communicate via an HTTP proxy.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
public final class ClientHttpProxyConfiguration {
    private final URI endpoint;
    private final String username;
    private final String password;
    private final String ntlmDomain;
    private final String ntlmWorkstation;
    private final Set<String> nonProxyHosts;
    private final Boolean preemptiveBasicAuthenticationEnabled;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientHttpProxyConfiguration(DefaultClientProxyConfigurationBuilder builder) {
        this.endpoint = builder.endpoint;
        this.username = builder.username;
        this.password = builder.password;
        this.ntlmDomain = builder.ntlmDomain;
        this.ntlmWorkstation = builder.ntlmWorkstation;
        this.nonProxyHosts = Collections.unmodifiableSet(new HashSet<>(builder.nonProxyHosts));
        this.preemptiveBasicAuthenticationEnabled = builder.preemptiveBasicAuthenticationEnabled;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientHttpProxyConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientProxyConfigurationBuilder();
    }

    /**
     * The endpoint of the proxy server that the SDK should connect through.
     *
     * @see Builder#endpoint(URI)
     */
    public Optional<URI> endpoint() {
        return Optional.ofNullable(endpoint);
    }

    /**
     * The username to use when connecting through a proxy.
     *
     * @see Builder#password(String)
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * The password to use when connecting through a proxy.
     *
     * @see Builder#password(String)
     */
    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    /**
     * For NTLM proxies: The Windows domain name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmDomain(String)
     */
    public Optional<String> ntlmDomain() {
        return Optional.ofNullable(ntlmDomain);
    }

    /**
     * For NTLM proxies: The Windows workstation name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmWorkstation(String)
     */
    public Optional<String> ntlmWorkstation() {
        return Optional.ofNullable(ntlmWorkstation);
    }

    /**
     * The hosts that the client is allowed to access without going through the proxy.
     *
     * @see Builder#nonProxyHosts(Set)
     */
    public Set<String> nonProxyHosts() {
        return nonProxyHosts;
    }

    /**
     * Whether to attempt to authenticate preemptively against the proxy server using basic authentication.
     *
     * @see Builder#preemptiveBasicAuthenticationEnabled(Boolean)
     */
    public Optional<Boolean> preemptiveBasicAuthenticationEnabled() {
        return Optional.ofNullable(preemptiveBasicAuthenticationEnabled);
    }

    /**
     * A builder for {@link ClientHttpProxyConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    interface Builder {

        /**
         * @see ClientHttpProxyConfiguration#endpoint().
         */
        Optional<URI> endpoint();

        /**
         * Configure the endpoint of the proxy server that the SDK should connect through. Currently, the endpoint is limited to
         * a host and port. Any other URI components will result in an exception being raised.
         *
         * @see ClientHttpProxyConfiguration#endpoint()
         */
        @ReviewBeforeRelease("Currently we only use host and port.")
        Builder endpoint(URI endpoint);

        /**
         * @see ClientHttpProxyConfiguration#username().
         */
        Optional<String> username();

        /**
         * Configure the username to use when connecting through a proxy.
         *
         * @see ClientHttpProxyConfiguration#username()
         */
        Builder username(String username);

        /**
         * @see ClientHttpProxyConfiguration#password().
         */
        Optional<String> password();

        /**
         * Configure the password to use when connecting through a proxy.
         *
         * @see ClientHttpProxyConfiguration#password()
         */
        Builder password(String password);

        /**
         * @see ClientHttpProxyConfiguration#ntlmDomain().
         */
        Optional<String> ntlmDomain();

        /**
         * For NTLM proxies: Configure the Windows domain name to use when authenticating with the proxy.
         *
         * @see ClientHttpProxyConfiguration#ntlmDomain()
         */
        Builder ntlmDomain(String proxyDomain);

        /**
         * @see ClientHttpProxyConfiguration#ntlmWorkstation().
         */
        Optional<String> ntlmWorkstation();

        /**
         * For NTLM proxies: Configure the Windows workstation name to use when authenticating with the proxy.
         *
         * @see ClientHttpProxyConfiguration#ntlmWorkstation()
         */
        Builder ntlmWorkstation(String proxyWorkstation);

        /**
         * @see ClientHttpProxyConfiguration#nonProxyHosts().
         */
        Set<String> nonProxyHosts();

        /**
         * Configure the hosts that the client is allowed to access without going through the proxy.
         *
         * @see ClientHttpProxyConfiguration#nonProxyHosts()
         */
        Builder nonProxyHosts(Set<String> nonProxyHosts);

        /**
         * Add a host that the client is allowed to access without going through the proxy.
         *
         * @see ClientHttpProxyConfiguration#nonProxyHosts()
         */
        Builder addNonProxyHost(String nonProxyHost);

        /**
         * @see ClientHttpProxyConfiguration#preemptiveBasicAuthenticationEnabled().
         */
        Optional<Boolean> preemptiveBasicAuthenticationEnabled();

        /**
         * Configure whether to attempt to authenticate pre-emptively against the proxy server using basic authentication.
         *
         * @see ClientHttpProxyConfiguration#preemptiveBasicAuthenticationEnabled()
         */
        Builder preemptiveBasicAuthenticationEnabled(Boolean preemptiveBasicAuthenticationEnabled);

        /**
         * Build a {@link ClientHttpProxyConfiguration} from the values currently configured in this builder.
         */
        ClientHttpProxyConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientProxyConfigurationBuilder implements Builder {
        private URI endpoint;
        private String username;
        private String password;
        private String ntlmDomain;
        private String ntlmWorkstation;
        private Set<String> nonProxyHosts = new HashSet<>();
        private Boolean preemptiveBasicAuthenticationEnabled;

        @Override
        public Optional<URI> endpoint() {
            return Optional.ofNullable(endpoint);
        }

        @Override
        public Builder endpoint(URI endpoint) {
            if (endpoint != null) {
                Validate.isTrue(isEmpty(endpoint.getUserInfo()), "Proxy endpoint user info is not supported.");
                Validate.isTrue(isEmpty(endpoint.getPath()), "Proxy endpoint path is not supported.");
                Validate.isTrue(isEmpty(endpoint.getQuery()), "Proxy endpoint query is not supported.");
                Validate.isTrue(isEmpty(endpoint.getFragment()), "Proxy endpoint fragment is not supported.");
            }

            this.endpoint = endpoint;
            return this;
        }

        public URI getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(URI endpoint) {
            endpoint(endpoint);
        }

        @Override
        public Optional<String> username() {
            return Optional.ofNullable(username);
        }

        @Override
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            username(username);
        }

        @Override
        public Optional<String> password() {
            return Optional.ofNullable(password);
        }

        @Override
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            password(password);
        }

        @Override
        public Optional<String> ntlmDomain() {
            return Optional.ofNullable(ntlmDomain);
        }

        @Override
        public Builder ntlmDomain(String proxyDomain) {
            this.ntlmDomain = proxyDomain;
            return this;
        }

        public String getNtlmDomain() {
            return ntlmDomain;
        }

        public void setNtlmDomain(String ntlmDomain) {
            ntlmDomain(ntlmDomain);
        }

        @Override
        public Optional<String> ntlmWorkstation() {
            return Optional.ofNullable(ntlmWorkstation);
        }

        @Override
        public Builder ntlmWorkstation(String proxyWorkstation) {
            this.ntlmWorkstation = proxyWorkstation;
            return this;
        }

        public String getNtlmWorkstation() {
            return ntlmWorkstation;
        }

        public void setNtlmWorkstation(String ntlmWorkstation) {
            ntlmWorkstation(ntlmWorkstation);
        }

        @Override
        public Set<String> nonProxyHosts() {
            return Collections.unmodifiableSet(nonProxyHosts);
        }

        @Override
        public Builder nonProxyHosts(Set<String> nonProxyHosts) {
            this.nonProxyHosts = new HashSet<>(nonProxyHosts);
            return this;
        }

        @Override
        public Builder addNonProxyHost(String nonProxyHost) {
            this.nonProxyHosts.add(nonProxyHost);
            return this;
        }

        public Set<String> getNonProxyHosts() {
            return nonProxyHosts();
        }

        public void setNonProxyHosts(Set<String> nonProxyHosts) {
            nonProxyHosts(nonProxyHosts);
        }

        @Override
        public Optional<Boolean> preemptiveBasicAuthenticationEnabled() {
            return Optional.ofNullable(preemptiveBasicAuthenticationEnabled);
        }

        @Override
        public Builder preemptiveBasicAuthenticationEnabled(Boolean preemptiveBasicAuthenticationEnabled) {
            this.preemptiveBasicAuthenticationEnabled = preemptiveBasicAuthenticationEnabled;
            return this;
        }

        public Boolean getPreemptiveBasicAuthenticationEnabled() {
            return preemptiveBasicAuthenticationEnabled;
        }

        public void setPreemptiveBasicAuthenticationEnabled(Boolean preemptiveBasicAuthenticationEnabled) {
            preemptiveBasicAuthenticationEnabled(preemptiveBasicAuthenticationEnabled);
        }

        @Override
        public ClientHttpProxyConfiguration build() {
            return new ClientHttpProxyConfiguration(this);
        }
    }
}
