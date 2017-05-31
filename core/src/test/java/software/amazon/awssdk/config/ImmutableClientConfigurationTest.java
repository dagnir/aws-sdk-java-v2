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
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.runtime.auth.SignerProvider;

/**
 * Validate the functionality of {@link ImmutableClientConfiguration}.
 */
@SuppressWarnings("deprecation") // Intentional use of deprecated class
public class ImmutableClientConfigurationTest {
    private static final NoOpSignerProvider SIGNER_PROVIDER = new NoOpSignerProvider();
    private static final RequestHandler2 REQUEST_HANDLER = new RequestHandler2() {
    };
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER = new DefaultCredentialsProvider();
    private static final URI ENDPOINT = URI.create("https://www.example.com");
    private static final RetryPolicy RETRY_POLICY = new RetryPolicy(null, null, 10, true);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static final SdkHttpClient SDK_HTTP_CLIENT = mock(SdkHttpClient.class);

    private static final LegacyClientConfiguration EXPECTED_LEGACY_CONFIGURATION =
            new LegacyClientConfiguration()
                    .withHeader("header", "value")
                    .withClientExecutionTimeout(4_000)
                    .withGzip(true)
                    .withUserAgentPrefix("userAgentPrefix")
                    .withUserAgentSuffix("userAgentSuffix")
                    .withRetryPolicy(RETRY_POLICY)
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

        @Override
        public SdkHttpClient sdkHttpClient() {
            return SDK_HTTP_CLIENT;
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

        @Override
        public SdkHttpClient sdkHttpClient() {
            return SDK_HTTP_CLIENT;
        }
    };

    @Test
    public void syncParamsTranslationShouldBeCorrect() {
        ImmutableSyncClientConfiguration config = new ImmutableSyncClientConfiguration(new InitializedSyncConfiguration());
        AwsSyncClientParams legacySyncParams = config.asLegacySyncClientParams();
        assertSyncParamsMatch(EXPECT_SYNC_CLIENT_PARAMS, legacySyncParams);
        assertLegacyConfigurationMatches(EXPECTED_LEGACY_CONFIGURATION, legacySyncParams.getClientConfiguration());
    }

    @Test
    public void asyncParamsTranslationShouldBeCorrect() {
        ImmutableAsyncClientConfiguration config = new ImmutableAsyncClientConfiguration(new InitializedAsyncConfiguration());
        AwsAsyncClientParams legacyAsyncParams = config.asLegacyAsyncClientParams();
        assertAsyncParamsMatch(EXPECT_ASYNC_CLIENT_PARAMS, legacyAsyncParams);
        assertLegacyConfigurationMatches(EXPECTED_LEGACY_CONFIGURATION, legacyAsyncParams.getClientConfiguration());
    }

    private void assertAsyncParamsMatch(AwsAsyncClientParams expected, AwsAsyncClientParams given) {
        assertSyncParamsMatch(expected, given);
        assertThat(expected.getExecutor()).isEqualTo(given.getExecutor());
    }

    private void assertSyncParamsMatch(AwsSyncClientParams expected, AwsSyncClientParams given) {
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
        public ExecutorService asyncExecutorService() {
            return EXECUTOR_SERVICE;
        }
    }

    private static class InitializedConfiguration implements ClientConfiguration {

        @Override
        public ClientTimeoutConfiguration timeoutConfiguration() {
            return ClientTimeoutConfiguration.builder()
                                             .httpRequestTimeout(Duration.ofSeconds(2))
                                             .totalExecutionTimeout(Duration.ofSeconds(4))
                                             .build();
        }

        @Override
        public ClientMarshallerConfiguration marshallerConfiguration() {
            return ClientMarshallerConfiguration.builder()
                                                .gzipEnabled(true)
                                                .addAdditionalHeader("header", "value")
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
                                              .build();
        }

        @Override
        public ClientRetryConfiguration retryConfiguration() {
            return ClientRetryConfiguration.builder()
                                           .retryPolicy(RETRY_POLICY)
                                           .build();
        }

        @Override
        public ClientListenerConfiguration listenerConfiguration() {
            return ClientListenerConfiguration.builder()
                                              .addRequestListener(REQUEST_HANDLER)
                                              .build();
        }

        @Override
        public AwsCredentialsProvider credentialsProvider() {
            return CREDENTIALS_PROVIDER;
        }

        @Override
        public URI endpoint() {
            return ENDPOINT;
        }

        @Override
        public SdkHttpClient httpClient() {
            return SDK_HTTP_CLIENT;
        }
    }
}
