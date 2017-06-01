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
@ReviewBeforeRelease("Review which options are required and which are optional.")
public final class ProxyConfiguration {

    private final String proxyHost;
    private final Integer proxyPort;
    private final URI endpoint;
    private final String username;
    private final String password;
    private final String ntlmDomain;
    private final String ntlmWorkstation;
    private final Set<String> nonProxyHosts;
    private final boolean preemptiveBasicAuthenticationEnabled;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ProxyConfiguration(DefaultClientProxyConfigurationBuilder builder) {
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
        this.endpoint = builder.endpoint;
        this.username = builder.username;
        this.password = builder.password;
        this.ntlmDomain = builder.ntlmDomain;
        this.ntlmWorkstation = builder.ntlmWorkstation;
        this.nonProxyHosts = Collections.unmodifiableSet(new HashSet<>(builder.nonProxyHosts));
        this.preemptiveBasicAuthenticationEnabled =
                builder.preemptiveBasicAuthenticationEnabled == null ? false : builder.preemptiveBasicAuthenticationEnabled;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ProxyConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientProxyConfigurationBuilder();
    }

    /**
     * Returns the optional proxy host the client will connect through.
     *
     * @see Builder#proxyHost(String)
     */
    public String proxyHost() {
        return proxyHost;
    }

    /**
     * Returns the optional proxy port the client will connect through.
     *
     * @see Builder#proxyPort(Integer)
     */
    public Integer proxyPort() {
        return proxyPort;
    }

    /**
     * The endpoint of the proxy server that the SDK should connect through.
     *
     * @see Builder#endpoint(URI)
     */
    public URI endpoint() {
        return endpoint;
    }

    /**
     * The username to use when connecting through a proxy.
     *
     * @see Builder#password(String)
     */
    public String username() {
        return username;
    }

    /**
     * The password to use when connecting through a proxy.
     *
     * @see Builder#password(String)
     */
    public String password() {
        return password;
    }

    /**
     * For NTLM proxies: The Windows domain name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmDomain(String)
     */
    public String ntlmDomain() {
        return ntlmDomain;
    }

    /**
     * For NTLM proxies: The Windows workstation name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmWorkstation(String)
     */
    public String ntlmWorkstation() {
        return ntlmWorkstation;
    }

    /**
     * The hosts that the client is allowed to access without going through the proxy.
     *
     * @see Builder#nonProxyHosts(Set)
     */
    @ReviewBeforeRelease("Revisit the presentation of this option and support http.nonProxyHosts property")
    public Set<String> nonProxyHosts() {
        return nonProxyHosts;
    }

    /**
     * Whether to attempt to authenticate preemptively against the proxy server using basic authentication.
     *
     * @see Builder#preemptiveBasicAuthenticationEnabled(Boolean)
     */
    public Boolean preemptiveBasicAuthenticationEnabled() {
        return preemptiveBasicAuthenticationEnabled;
    }

    /**
     * A builder for {@link ProxyConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    interface Builder {

        Optional<String> proxyHost();

        Builder proxyHost(String proxyHost);

        Optional<Integer> proxyPort();

        Builder proxyPort(Integer proxyPort);


        Optional<URI> endpoint();

        @ReviewBeforeRelease("Currently we only use host and port.")
        Builder endpoint(URI endpoint);


        Optional<String> username();


        Builder username(String username);

        Optional<String> password();

        /**
         * Configure the password to use when connecting through a proxy.
         *
         * @see ProxyConfiguration#password()
         */
        Builder password(String password);

        /**
         * @see ProxyConfiguration#ntlmDomain().
         */
        Optional<String> ntlmDomain();

        /**
         * For NTLM proxies: Configure the Windows domain name to use when authenticating with the proxy.
         *
         * @see ProxyConfiguration#ntlmDomain()
         */
        Builder ntlmDomain(String proxyDomain);

        /**
         * @see ProxyConfiguration#ntlmWorkstation().
         */
        Optional<String> ntlmWorkstation();

        /**
         * For NTLM proxies: Configure the Windows workstation name to use when authenticating with the proxy.
         *
         * @see ProxyConfiguration#ntlmWorkstation()
         */
        Builder ntlmWorkstation(String proxyWorkstation);

        /**
         * @see ProxyConfiguration#nonProxyHosts().
         */
        Set<String> nonProxyHosts();

        /**
         * Configure the hosts that the client is allowed to access without going through the proxy.
         *
         * @see ProxyConfiguration#nonProxyHosts()
         */
        Builder nonProxyHosts(Set<String> nonProxyHosts);

        /**
         * Add a host that the client is allowed to access without going through the proxy.
         *
         * @see ProxyConfiguration#nonProxyHosts()
         */
        Builder addNonProxyHost(String nonProxyHost);

        /**
         * @see ProxyConfiguration#preemptiveBasicAuthenticationEnabled().
         */
        Optional<Boolean> preemptiveBasicAuthenticationEnabled();


        Builder preemptiveBasicAuthenticationEnabled(Boolean preemptiveBasicAuthenticationEnabled);

        ProxyConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientProxyConfigurationBuilder implements Builder {

        private String proxyHost;
        private Integer proxyPort;
        private URI endpoint;
        private String username;
        private String password;
        private String ntlmDomain;
        private String ntlmWorkstation;
        private Set<String> nonProxyHosts = new HashSet<>();
        private Boolean preemptiveBasicAuthenticationEnabled;

        @Override
        public Optional<String> proxyHost() {
            return Optional.ofNullable(proxyHost);
        }

        @Override
        public Builder proxyHost(String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        @Override
        public Optional<Integer> proxyPort() {
            return Optional.ofNullable(proxyPort);
        }

        @Override
        public Builder proxyPort(Integer proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

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
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
