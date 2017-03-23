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

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;

/**
 * An implementation of {@link ClientConfiguration} that is guaranteed to be immutable and thread-safe.
 */
@SdkInternalApi
public abstract class ImmutableClientConfiguration implements ClientConfiguration {
    private final ClientHttpConfiguration httpConfiguration;
    private final ClientHttpProxyConfiguration httpProxyConfiguration;
    private final ClientTcpConfiguration tcpConfiguration;
    private final ClientIpConfiguration ipConfiguration;
    private final ClientTimeoutConfiguration timeoutConfiguration;
    private final ClientMarshallerConfiguration marshallerConfiguration;
    private final ClientMetricsConfiguration metricsConfiguration;
    private final ClientSecurityConfiguration securityConfiguration;
    private final ClientRetryConfiguration retryConfiguration;
    private final ClientListenerConfiguration listenerConfiguration;
    private final AwsCredentialsProvider credentialsProvider;
    private final URI endpoint;
    private final LegacyClientConfiguration legacyConfiguration;

    /**
     * Copy the provided client configuration into an immutable version.
     */
    public ImmutableClientConfiguration(ClientConfiguration configuration) {
        this.httpConfiguration = configuration.httpConfiguration();
        this.httpProxyConfiguration = configuration.httpProxyConfiguration();
        this.tcpConfiguration = configuration.tcpConfiguration();
        this.ipConfiguration = configuration.ipConfiguration();
        this.timeoutConfiguration = configuration.timeoutConfiguration();
        this.marshallerConfiguration = configuration.marshallerConfiguration();
        this.metricsConfiguration = configuration.metricsConfiguration();
        this.securityConfiguration = configuration.securityConfiguration();
        this.retryConfiguration = configuration.retryConfiguration();
        this.listenerConfiguration = configuration.listenerConfiguration();
        this.credentialsProvider = require(configuration.credentialsProvider());
        this.endpoint = require(configuration.endpoint());
        this.legacyConfiguration = initializeLegacyConfiguration();
    }

    @Override
    public ClientHttpConfiguration httpConfiguration() {
        return httpConfiguration;
    }

    @Override
    public ClientHttpProxyConfiguration httpProxyConfiguration() {
        return httpProxyConfiguration;
    }

    @Override
    public ClientTcpConfiguration tcpConfiguration() {
        return tcpConfiguration;
    }

    @Override
    public ClientIpConfiguration ipConfiguration() {
        return ipConfiguration;
    }

    @Override
    public ClientTimeoutConfiguration timeoutConfiguration() {
        return timeoutConfiguration;
    }

    @Override
    public ClientMarshallerConfiguration marshallerConfiguration() {
        return marshallerConfiguration;
    }

    @Override
    public ClientMetricsConfiguration metricsConfiguration() {
        return metricsConfiguration;
    }

    @Override
    public ClientSecurityConfiguration securityConfiguration() {
        return securityConfiguration;
    }

    @Override
    public ClientRetryConfiguration retryConfiguration() {
        return retryConfiguration;
    }

    @Override
    public ClientListenerConfiguration listenerConfiguration() {
        return listenerConfiguration;
    }

    @Override
    public Optional<AwsCredentialsProvider> credentialsProvider() {
        return Optional.ofNullable(credentialsProvider);
    }

    @Override
    public Optional<URI> endpoint() {
        return Optional.ofNullable(endpoint);
    }

    /**
     * Convert this client configuration into a legacy-style configuration object.
     */
    @Deprecated
    @ReviewBeforeRelease("This should be removed once we remove our reliance on the legacy client configuration object.")
    public LegacyClientConfiguration asLegacyConfiguration() {
        return this.legacyConfiguration;
    }

    private LegacyClientConfiguration initializeLegacyConfiguration() {
        LegacyClientConfiguration configuration = new LegacyClientConfiguration();

        copyHttpConfiguration(configuration, httpConfiguration());
        copyHttpProxyConfiguration(configuration, httpProxyConfiguration());
        copyTcpConfiguration(configuration, tcpConfiguration());
        copyIpConfiguration(configuration, ipConfiguration());
        copyTimeoutConfiguration(configuration, timeoutConfiguration());
        copyMarshallerConfiguration(configuration, marshallerConfiguration());
        copyMetricsConfiguration(configuration, metricsConfiguration());
        copySecurityConfiguration(configuration, securityConfiguration());
        copyRetryConfiguration(configuration, retryConfiguration());

        configuration.setProtocol(require(endpoint().map(URI::getScheme).flatMap(this::schemeToProtocol)));

        return configuration;
    }

    private void copyHttpConfiguration(LegacyClientConfiguration configuration, ClientHttpConfiguration httpConfiguration) {
        configuration.setUseExpectContinue(require(httpConfiguration.expectContinueEnabled()));

        httpConfiguration.additionalHeaders().forEach((header, values) -> {
            if (values.size() > 1) {
                throw new IllegalArgumentException("Multiple values under the same header are not supported at this time.");
            }
            values.forEach(value -> configuration.addHeader(header, value));
        });
    }

    private void copyHttpProxyConfiguration(LegacyClientConfiguration configuration,
                                            ClientHttpProxyConfiguration proxyConfiguration) {
        configuration.setNonProxyHosts(String.join("|", proxyConfiguration.nonProxyHosts()));
        configuration.setPreemptiveBasicProxyAuth(require(proxyConfiguration.preemptiveBasicAuthenticationEnabled()));
        configuration.setProxyDomain(require(proxyConfiguration.ntlmDomain()));
        configuration.setProxyHost(require(proxyConfiguration.endpoint().map(URI::getHost)));
        configuration.setProxyPort(require(proxyConfiguration.endpoint().map(URI::getPort)));
        configuration.setProxyPassword(require(proxyConfiguration.password()));
        configuration.setProxyUsername(require(proxyConfiguration.username()));
        configuration.setProxyWorkstation(require(proxyConfiguration.ntlmWorkstation()));
    }

    private void copyTcpConfiguration(LegacyClientConfiguration configuration, ClientTcpConfiguration tcpConfiguration) {
        configuration.setConnectionMaxIdleMillis(toRequiredMillis(tcpConfiguration.connectionMaxIdleTime()));
        configuration.setConnectionTtl(toRequiredMillis(tcpConfiguration.connectionTimeToLive()));
        configuration.setValidateAfterInactivityMillis(
                Math.toIntExact(toRequiredMillis(tcpConfiguration.connectionValidationFrequency())));

        configuration.setUseReaper(require(tcpConfiguration.connectionReaperEnabled()));
        configuration.setUseTcpKeepAlive(require(tcpConfiguration.tcpKeepaliveEnabled()));
        configuration.setMaxConnections(require(tcpConfiguration.maxConnections()));
        configuration.setSocketBufferSizeHints(require(tcpConfiguration.socketSendBufferSizeHint()),
                                               require(tcpConfiguration.socketReceiveBufferSizeHint()));
    }

    private void copyIpConfiguration(LegacyClientConfiguration configuration, ClientIpConfiguration ipConfiguration) {
        configuration.setDnsResolver(require(ipConfiguration.dnsResolver()));
        configuration.setLocalAddress(require(ipConfiguration.localAddress()));
    }

    private void copyTimeoutConfiguration(LegacyClientConfiguration configuration,
                                          ClientTimeoutConfiguration timeoutConfiguration) {
        configuration.setConnectionTimeout(Math.toIntExact(toRequiredMillis(timeoutConfiguration.connectionTimeout())));
        configuration.setRequestTimeout(Math.toIntExact(toRequiredMillis(timeoutConfiguration.httpRequestTimeout())));
        configuration.setSocketTimeout(Math.toIntExact(toRequiredMillis(timeoutConfiguration.socketTimeout())));
        configuration.setClientExecutionTimeout(Math.toIntExact(toRequiredMillis(timeoutConfiguration.totalExecutionTimeout())));
    }

    private void copyMarshallerConfiguration(LegacyClientConfiguration configuration,
                                             ClientMarshallerConfiguration compressionConfiguration) {
        configuration.setUseGzip(require(compressionConfiguration.gzipEnabled()));
    }

    private void copyMetricsConfiguration(LegacyClientConfiguration configuration,
                                          ClientMetricsConfiguration metricsConfiguration) {
        configuration.setUserAgentPrefix(require(metricsConfiguration.userAgentPrefix()));
        configuration.setUserAgentSuffix(require(metricsConfiguration.userAgentSuffix()));
    }

    private void copySecurityConfiguration(LegacyClientConfiguration configuration,
                                           ClientSecurityConfiguration signingConfiguration) {
        configuration.setSecureRandom(require(signingConfiguration.secureRandom()));
    }

    private void copyRetryConfiguration(LegacyClientConfiguration configuration,
                                        ClientRetryConfiguration retryConfiguration) {
        configuration.setUseThrottleRetries(require(retryConfiguration.retryThrottlingEnabled()));
        configuration.setMaxErrorRetry(require(retryConfiguration.maxRetries()));
        configuration.setMaxConsecutiveRetriesBeforeThrottling(require(retryConfiguration.maxRetriesBeforeThrottling()));
        configuration.setRetryPolicy(require(retryConfiguration.retryPolicy()));
    }

    private Optional<Protocol> schemeToProtocol(String s) {
        return Arrays.stream(Protocol.values()).filter(p -> s.equals(p.toString())).findFirst();
    }

    private Long toRequiredMillis(Optional<Duration> duration) {
        return require(duration.map(Duration::toMillis));
    }

    protected final <T> T require(Optional<T> requiredOptional) {
        return requiredOptional.orElseThrow(() -> new IllegalArgumentException("All configuration fields must be specified."));
    }
}
