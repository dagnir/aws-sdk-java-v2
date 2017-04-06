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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultAwsCredentialsProviderChain;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.runtime.auth.SignerProvider;

/**
 * Validate the functionality of {@link ImmutableClientConfiguration}.
 */
public class ImmutableClientConfigurationTest {
    private static final NoOpSignerProvider SIGNER_PROVIDER = new NoOpSignerProvider();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final RequestHandler2 REQUEST_HANDLER = new RequestHandler2() {};
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER = new DefaultAwsCredentialsProviderChain();
    private static final URI ENDPOINT = URI.create("https://www.example.com");
    private static final RetryPolicy RETRY_POLICY = new RetryPolicy(null, null, 10, true);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private static final LegacyClientConfiguration EXPECTED_LEGACY_CONFIGURATION =
            new LegacyClientConfiguration().withHeader("header", "value")
                                           .withUseExpectContinue(true)
                                           .withProxyUsername("username")
                                           .withNonProxyHosts("nonProxyHost1|nonProxyHost2")
                                           .withProxyDomain("domain")
                                           .withProxyPassword("password")
                                           .withProxyWorkstation("workstation")
                                           .withPreemptiveBasicProxyAuth(true)
                                           .withProxyHost("host")
                                           .withProxyPort(123)
                                           .withConnectionMaxIdleMillis(10_000)
                                           .withConnectionTtl(11_000)
                                           .withValidateAfterInactivityMillis(12_000)
                                           .withReaper(true)
                                           .withTcpKeepAlive(true)
                                           .withMaxConnections(1)
                                           .withSocketBufferSizeHints(2, 3)
                                           .withLocalAddress(InetAddress.getLoopbackAddress())
                                           .withConnectionTimeout(1_000)
                                           .withSocketTimeout(3_000)
                                           .withClientExecutionTimeout(4_000)
                                           .withGzip(true)
                                           .withUserAgentPrefix("userAgentPrefix")
                                           .withUserAgentSuffix("userAgentSuffix")
                                           .withSecureRandom(SECURE_RANDOM)
                                           .withRetryPolicy(RETRY_POLICY)
                                           .withThrottledRetries(true)
                                           .withMaxErrorRetry(10)
                                           .withMaxConsecutiveRetriesBeforeThrottling(11)
                                           .withProtocol(Protocol.HTTPS);

    private static final AwsSyncClientParams EXPECT_SYNC_CLIENT_PARAMS = new AwsSyncClientParams() {
        @Override
        public AwsCredentialsProvider getCredentialsProvider() {
            return CREDENTIALS_PROVIDER;
        }

        @Override
        public LegacyClientConfiguration getClientConfiguration() {
            return EXPECTED_LEGACY_CONFIGURATION;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return RequestMetricCollector.NONE;
        }

        @Override
        public List<RequestHandler2> getRequestHandlers() {
            return Collections.singletonList(REQUEST_HANDLER);
        }

        @Override
        public SignerProvider getSignerProvider() {
            return SIGNER_PROVIDER;
        }

        @Override
        public URI getEndpoint() {
            return ENDPOINT;
        }
    };

    private static final AwsAsyncClientParams EXPECT_ASYNC_CLIENT_PARAMS = new AwsAsyncClientParams() {
        @Override
        public AwsCredentialsProvider getCredentialsProvider() {
            return CREDENTIALS_PROVIDER;
        }

        @Override
        public LegacyClientConfiguration getClientConfiguration() {
            return EXPECTED_LEGACY_CONFIGURATION;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return RequestMetricCollector.NONE;
        }

        @Override
        public List<RequestHandler2> getRequestHandlers() {
            return Collections.singletonList(REQUEST_HANDLER);
        }

        @Override
        public SignerProvider getSignerProvider() {
            return SIGNER_PROVIDER;
        }

        @Override
        public URI getEndpoint() {
            return ENDPOINT;
        }

        @Override
        public ExecutorService getExecutor() {
            return EXECUTOR_SERVICE;
        }
    };

    @Test
    public void legacyConfigurationTranslationShouldBeCorrect() {
        ImmutableClientConfiguration config = new ImmutableClientConfiguration(new InitializedConfiguration()) {};
        assertLegacyConfigurationMatches(EXPECTED_LEGACY_CONFIGURATION, config.asLegacyConfiguration());
    }

    @Test
    public void syncParamsTranslationShouldBeCorrect() {
        ImmutableSyncClientConfiguration config = new ImmutableSyncClientConfiguration(new InitializedSyncConfiguration());
        assertSyncParamsMatch(EXPECT_SYNC_CLIENT_PARAMS, config.asLegacySyncClientParams());
    }

    @Test
    public void asyncParamsTranslationShouldBeCorrect() {
        ImmutableAsyncClientConfiguration config = new ImmutableAsyncClientConfiguration(new InitializedAsyncConfiguration());
        assertAsyncParamsMatch(EXPECT_ASYNC_CLIENT_PARAMS, config.toLegacyAsyncClientParams());
    }

    public void assertAsyncParamsMatch(AwsAsyncClientParams expected, AwsAsyncClientParams given) {
        assertSyncParamsMatch(expected, given);
        assertThat(expected.getExecutor()).isEqualTo(given.getExecutor());
    }

    public void assertSyncParamsMatch(AwsSyncClientParams expected, AwsSyncClientParams given) {
        assertThat(expected.getCredentialsProvider()).isEqualTo(given.getCredentialsProvider());
        assertThat(expected.getEndpoint()).isEqualTo(given.getEndpoint());
        assertThat(expected.getRequestHandlers()).isEqualTo(given.getRequestHandlers());
        assertThat(expected.getRequestMetricCollector()).isEqualTo(given.getRequestMetricCollector());
        assertThat(expected.getSignerProvider()).isEqualTo(given.getSignerProvider());
        assertLegacyConfigurationMatches(EXPECTED_LEGACY_CONFIGURATION, given.getClientConfiguration());
    }

    private void assertLegacyConfigurationMatches(LegacyClientConfiguration expected,
                                                  LegacyClientConfiguration given) {
        assertThat(given).isEqualToIgnoringGivenFields(expected, "apacheHttpClientConfig");
    }

    private static class InitializedSyncConfiguration extends InitializedConfiguration implements SyncClientConfiguration {

    }

    private static class InitializedAsyncConfiguration extends InitializedConfiguration implements AsyncClientConfiguration {
        @Override
        public Optional<ExecutorService> asyncExecutorService() {
            return Optional.of(EXECUTOR_SERVICE);
        }
    }

    private static class InitializedConfiguration implements ClientConfiguration {
        @Override
        public ClientHttpConfiguration httpConfiguration() {
            return ClientHttpConfiguration.builder()
                                          .addAdditionalHeader("header", "value")
                                          .expectContinueEnabled(true)
                                          .build();
        }

        @Override
        public ClientHttpProxyConfiguration httpProxyConfiguration() {
            return ClientHttpProxyConfiguration.builder()
                                               .username("username")
                                               .addNonProxyHost("nonProxyHost1")
                                               .addNonProxyHost("nonProxyHost2")
                                               .ntlmDomain("domain")
                                               .password("password")
                                               .ntlmWorkstation("workstation")
                                               .preemptiveBasicAuthenticationEnabled(true)
                                               .endpoint(URI.create("http://host:123"))
                                               .build();
        }

        @Override
        public ClientTcpConfiguration tcpConfiguration() {
            return ClientTcpConfiguration.builder()
                                         .connectionMaxIdleTime(Duration.ofSeconds(10))
                                         .connectionTimeToLive(Duration.ofSeconds(11))
                                         .connectionValidationFrequency(Duration.ofSeconds(12))
                                         .tcpKeepaliveEnabled(true)
                                         .maxConnections(1)
                                         .socketSendBufferSizeHint(2)
                                         .socketReceiveBufferSizeHint(3)
                                         .build();
        }

        @Override
        public ClientIpConfiguration ipConfiguration() {
            return ClientIpConfiguration.builder()
                                        .localAddress(InetAddress.getLoopbackAddress())
                                        .build();
        }

        @Override
        public ClientTimeoutConfiguration timeoutConfiguration() {
            return ClientTimeoutConfiguration.builder()
                                             .connectionTimeout(Duration.ofSeconds(1))
                                             .httpRequestTimeout(Duration.ofSeconds(2))
                                             .socketTimeout(Duration.ofSeconds(3))
                                             .totalExecutionTimeout(Duration.ofSeconds(4))
                                             .build();
        }

        @Override
        public ClientMarshallerConfiguration marshallerConfiguration() {
            return ClientMarshallerConfiguration.builder()
                                                .gzipEnabled(true)
                                                .build();
        }

        @Override
        public ClientMetricsConfiguration metricsConfiguration() {
            return ClientMetricsConfiguration.builder()
                                             .requestMetricCollector(RequestMetricCollector.NONE)
                                             .userAgentPrefix("userAgentPrefix")
                                             .userAgentSuffix("userAgentSuffix")
                                             .build();
        }

        @Override
        public ClientSecurityConfiguration securityConfiguration() {
            return ClientSecurityConfiguration.builder()
                                              .signerProvider(SIGNER_PROVIDER)
                                              .secureRandom(SECURE_RANDOM)
                                              .build();
        }

        @Override
        public ClientRetryConfiguration retryConfiguration() {
            return ClientRetryConfiguration.builder()
                                           .retryPolicy(RETRY_POLICY)
                                           .retryThrottlingEnabled(true)
                                           .maxRetries(10)
                                           .maxRetriesBeforeThrottling(11)
                                           .build();
        }

        @Override
        public ClientListenerConfiguration listenerConfiguration() {
            return ClientListenerConfiguration.builder()
                                              .addRequestListener(REQUEST_HANDLER)
                                              .build();
        }

        @Override
        public Optional<AwsCredentialsProvider> credentialsProvider() {
            return Optional.of(CREDENTIALS_PROVIDER);
        }

        @Override
        public Optional<URI> endpoint() {
            return Optional.of(ENDPOINT);
        }
    }
}
