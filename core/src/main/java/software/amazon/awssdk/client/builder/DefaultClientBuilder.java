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

package software.amazon.awssdk.client.builder;

import static software.amazon.awssdk.utils.Validate.notNull;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.config.ClientListenerConfiguration;
import software.amazon.awssdk.config.ClientMarshallerConfiguration;
import software.amazon.awssdk.config.ClientMetricsConfiguration;
import software.amazon.awssdk.config.ClientRetryConfiguration;
import software.amazon.awssdk.config.ClientSecurityConfiguration;
import software.amazon.awssdk.config.ClientTimeoutConfiguration;
import software.amazon.awssdk.config.ImmutableAsyncClientConfiguration;
import software.amazon.awssdk.config.ImmutableSyncClientConfiguration;
import software.amazon.awssdk.config.MutableClientConfiguration;
import software.amazon.awssdk.config.defaults.ClientConfigurationDefaults;
import software.amazon.awssdk.config.defaults.GlobalClientConfigurationDefaults;
import software.amazon.awssdk.handlers.HandlerChainFactory;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpConfigurationOptions;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.loader.DefaultSdkHttpClientFactory;
import software.amazon.awssdk.regions.AwsRegionProvider;
import software.amazon.awssdk.regions.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.util.EndpointUtils;
import software.amazon.awssdk.utils.OptionalUtils;

/**
 * An SDK-internal implementation of the methods in {@link ClientBuilder}, {@link AsyncClientBuilder} and
 * {@link SyncClientBuilder}. This implements all methods required by those interfaces, allowing service-specific builders to
 * just
 * implement the configuration they wish to add.
 *
 * <p>By implementing both the sync and async interface's methods, service-specific builders can share code between their sync
 * and
 * async variants without needing one to extend the other. Note: This only defines the methods in the sync and async builder
 * interfaces. It does not implement the interfaces themselves. This is because the sync and async client builder interfaces both
 * require a type-constrained parameter for use in fluent chaining, and a generic type parameter conflict is introduced into the
 * class hierarchy by this interface extending the builder interfaces themselves.</p>
 *
 * <p>Like all {@link ClientBuilder}s, this class is not thread safe.</p>
 *
 * @param <B> The type of builder, for chaining.
 * @param <C> The type of client generated by this builder.
 */
@SdkProtectedApi
public abstract class DefaultClientBuilder<B extends ClientBuilder<B, C>, C>
        implements ClientBuilder<B, C> {
    private static final String DEFAULT_ENDPOINT_PROTOCOL = "https";
    private static final AwsRegionProvider DEFAULT_REGION_PROVIDER = new DefaultAwsRegionProviderChain();
    private static final SdkHttpClientFactory DEFAULT_HTTP_CLIENT_FACTORY = new DefaultSdkHttpClientFactory();

    private final SdkHttpClientFactory defaultHttpClientFactory;

    private MutableClientConfiguration mutableClientConfiguration = new MutableClientConfiguration();

    private String region;
    private Boolean defaultRegionDetectionEnabled;
    private ExecutorProvider asyncExecutorProvider;
    private ClientHttpConfiguration httpConfiguration = ClientHttpConfiguration.builder().build();

    protected DefaultClientBuilder() {
        this(DEFAULT_HTTP_CLIENT_FACTORY);
    }

    @SdkTestInternalApi
    protected DefaultClientBuilder(SdkHttpClientFactory defaultHttpClientFactory) {
        this.defaultHttpClientFactory = defaultHttpClientFactory;
    }

    /**
     * Build a client using the current state of this builder. This is marked final in order to allow this class to add standard
     * "build" logic between all service clients. Service clients are expected to implement the {@link #buildClient} method, that
     * accepts the immutable client configuration generated by this build method.
     */
    public final C build() {
        return buildClient();
    }

    /**
     * Implemented by child classes to create a client using the provided immutable configuration objects. The async and sync
     * configurations are not yet immutable. Child classes will need to make them immutable in order to validate them and pass
     * them to the client's constructor.
     *
     * @return A client based on the provided configuration.
     */
    protected abstract C buildClient();

    /**
     * Implemented by child classes to define the endpoint prefix used when communicating with AWS. This constitutes the first
     * part of the URL in the DNS name for the service. Eg. in the endpoint "dynamodb.amazonaws.com", this is the "dynamodb".
     *
     * <p>For standard services, this should match the "endpointPrefix" field in the AWS model.</p>
     */
    protected abstract String serviceEndpointPrefix();

    /**
     * An optional hook that can be overridden by service client builders to set service-specific defaults.
     *
     * @return The service defaults that should be applied.
     */
    protected ClientConfigurationDefaults serviceDefaults() {
        return new ClientConfigurationDefaults() {
        };
    }

    /**
     * An optional hook that can be overridden by service client builders to supply service-specific defaults for HTTP related
     * configuraton.
     *
     * @return The service defaults that should be applied.
     */
    protected SdkHttpConfigurationOptions serviceSpecificHttpConfig() {
        return SdkHttpConfigurationOptions.empty();
    }

    /**
     * Used by child classes to get the signing region configured on this builder. This is usually used when generating the child
     * class's signer. This will never return null.
     */
    @ReviewBeforeRelease("Signing region is not always endpoint region. When dust settles with region refactor revisit this")
    protected final String signingRegion() {
        return resolveRegion().orElseThrow(() -> new IllegalStateException("The signing region could not be determined."));
    }

    /**
     * Return a sync client configuration object, populated with the following chain of priorities.
     * <ol>
     * <li>Customer Configuration</li>
     * <li>Builder-Specific Default Configuration</li>
     * <li>Service-Specific Default Configuration</li>
     * <li>Global Default Configuration</li>
     * </ol>
     */
    protected final ImmutableSyncClientConfiguration syncClientConfiguration() {
        MutableClientConfiguration configuration = mutableClientConfiguration.clone();
        builderDefaults().applySyncDefaults(configuration);
        serviceDefaults().applySyncDefaults(configuration);
        new GlobalClientConfigurationDefaults().applySyncDefaults(configuration);
        applySdkHttpClient(configuration);
        return new ImmutableSyncClientConfiguration(configuration);
    }

    private void applySdkHttpClient(MutableClientConfiguration config) {
        config.httpClient(resolveSdkHttpClient());
    }

    private SdkHttpClient resolveSdkHttpClient() {
        return httpConfiguration
                .toEither()
                .map(e -> e.map(NonManagedSdkHttpClient::new,
                    factory -> factory.createHttpClientWithDefaults(serviceSpecificHttpConfig())))
                .orElseGet(() -> defaultHttpClientFactory.createHttpClientWithDefaults(serviceSpecificHttpConfig()));
    }

    /**
     * Return an async client configuration object, populated with the following chain of priorities.
     * <ol>
     * <li>Customer Configuration</li>
     * <li>Builder-Specific Default Configuration</li>
     * <li>Service-Specific Default Configuration</li>
     * <li>Global Default Configuration</li>
     * </ol>
     */
    protected final ImmutableAsyncClientConfiguration asyncClientConfiguration() {
        MutableClientConfiguration configuration = mutableClientConfiguration.clone();
        builderDefaults().applyAsyncDefaults(configuration);
        serviceDefaults().applyAsyncDefaults(configuration);
        new GlobalClientConfigurationDefaults().applyAsyncDefaults(configuration);
        return new ImmutableAsyncClientConfiguration(configuration);
    }

    /**
     * Add builder-specific configuration on top of the customer-defined configuration, if needed. Specifically, if the customer
     * has specified a region in place of an endpoint, this will determine the endpoint to be used for AWS communication.
     */
    private ClientConfigurationDefaults builderDefaults() {
        return new ClientConfigurationDefaults() {
            /**
             * If the customer did not specify an endpoint themselves, attempt to generate one automatically.
             */
            @Override
            protected URI getEndpointDefault() {
                return resolveEndpoint().orElse(null);
            }

            /**
             * If the customer did not specify a region provider themselves, use the default chain.
             */
            @Override
            protected AwsCredentialsProvider getCredentialsDefault() {
                return new DefaultCredentialsProvider();
            }

            /**
             * Create the async executor service that should be used for async client executions.
             */
            @Override
            protected ExecutorService getAsyncExecutorDefault(Integer maxConnections) {
                return Optional.ofNullable(asyncExecutorProvider).map(ExecutorProvider::get).orElse(null);
            }

            /**
             * Add the global request handlers.
             */
            @Override
            protected void applyListenerDefaults(ClientListenerConfiguration.Builder builder) {
                new HandlerChainFactory().getGlobalHandlers().forEach(builder::addRequestListener);
            }
        };
    }

    /**
     * Resolve the region that should be used based on the customer's configuration.
     */
    private Optional<String> resolveRegion() {
        return OptionalUtils.firstPresent(region(), this::regionFromDefaultProvider);
    }

    /**
     * Resolve the service endpoint that should be used based on the customer's configuration.
     */
    private Optional<URI> resolveEndpoint() {
        return OptionalUtils.firstPresent(endpointOverride(), this::endpointFromRegion);
    }

    /**
     * Load the region from the default region provider if enabled.
     */
    private Optional<String> regionFromDefaultProvider() {
        return useRegionProviderChain() ? Optional.ofNullable(DEFAULT_REGION_PROVIDER.getRegion()) : Optional.empty();
    }

    /**
     * @return True if loading from region provider chain is allowed per options. False otherwise False otherwise.
     */
    private boolean useRegionProviderChain() {
        return defaultRegionDetectionEnabled == null || defaultRegionDetectionEnabled;
    }

    /**
     * Load the endpoint from the resolved region.
     */
    private Optional<URI> endpointFromRegion() {
        return resolveRegion().map(r -> EndpointUtils.buildEndpoint(DEFAULT_ENDPOINT_PROTOCOL, serviceEndpointPrefix(),
                                                                    RegionUtils.getRegion(r)));
    }

    // Getters and Setters

    @Override
    public final Optional<String> region() {
        return Optional.ofNullable(region);
    }

    @Override
    public final B region(String region) {
        this.region = region;
        return thisBuilder();
    }

    public final String getRegion() {
        return region;
    }

    public final void setRegion(String region) {
        region(region);
    }

    @Override
    public Optional<URI> endpointOverride() {
        return Optional.ofNullable(mutableClientConfiguration.endpoint());
    }

    @Override
    public B endpointOverride(URI endpointOverride) {
        mutableClientConfiguration.endpoint(endpointOverride);
        return thisBuilder();
    }

    public URI getEndpointOverride() {
        return endpointOverride().orElse(null);
    }

    public void setEndpointOverride(URI endpointOverride) {
        endpointOverride(endpointOverride);
    }

    @Override
    public Optional<Boolean> defaultRegionDetectionEnabled() {
        return Optional.ofNullable(defaultRegionDetectionEnabled);
    }

    @Override
    public B defaultRegionDetectionEnabled(Boolean defaultRegionDetectionEnabled) {
        this.defaultRegionDetectionEnabled = defaultRegionDetectionEnabled;
        return thisBuilder();
    }

    public Boolean getDefaultRegionDetectionEnabled() {
        return defaultRegionDetectionEnabled().orElse(null);
    }

    public void setDefaultRegionDetectionEnabled(Boolean defaultRegionDetectionEnabled) {
        defaultRegionDetectionEnabled(defaultRegionDetectionEnabled);
    }

    public Optional<ExecutorProvider> asyncExecutorProvider() {
        return Optional.ofNullable(asyncExecutorProvider);
    }

    public B asyncExecutorProvider(ExecutorProvider asyncExecutorProvider) {
        this.asyncExecutorProvider = asyncExecutorProvider;
        return thisBuilder();
    }

    public ExecutorProvider getAsyncExecutorProvider() {
        return asyncExecutorProvider;
    }

    public void setAsyncExecutorProvider(ExecutorProvider asyncExecutorProvider) {
        asyncExecutorProvider(asyncExecutorProvider);
    }

    // Getters and setters that just delegate to the mutable client configuration

    @Override
    public final ClientTimeoutConfiguration timeoutConfiguration() {
        return mutableClientConfiguration.timeoutConfiguration();
    }

    @Override
    public final B timeoutConfiguration(ClientTimeoutConfiguration timeoutConfiguration) {
        mutableClientConfiguration.timeoutConfiguration(timeoutConfiguration);
        return thisBuilder();
    }

    public final ClientTimeoutConfiguration getTimeoutConfiguration() {
        return timeoutConfiguration();
    }

    public final void setTimeoutConfiguration(ClientTimeoutConfiguration timeoutConfiguration) {
        timeoutConfiguration(timeoutConfiguration);
    }

    @Override
    public final ClientMarshallerConfiguration marshallerConfiguration() {
        return mutableClientConfiguration.marshallerConfiguration();
    }

    @Override
    public final B marshallerConfiguration(ClientMarshallerConfiguration marshallerConfiguration) {
        mutableClientConfiguration.marshallerConfiguration(marshallerConfiguration);
        return thisBuilder();
    }

    public final ClientMarshallerConfiguration getMarshallerConfiguration() {
        return marshallerConfiguration();
    }

    public final void setMarshallerConfiguration(ClientMarshallerConfiguration marshallerConfiguration) {
        marshallerConfiguration(marshallerConfiguration);
    }

    @Override
    public final ClientMetricsConfiguration metricsConfiguration() {
        return mutableClientConfiguration.metricsConfiguration();
    }

    @Override
    public final B metricsConfiguration(ClientMetricsConfiguration metricsConfiguration) {
        mutableClientConfiguration.metricsConfiguration(metricsConfiguration);
        return thisBuilder();
    }

    public final ClientMetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration();
    }

    public final void setMetricsConfiguration(ClientMetricsConfiguration metricsConfiguration) {
        metricsConfiguration(metricsConfiguration);
    }

    @Override
    public final ClientSecurityConfiguration securityConfiguration() {
        return mutableClientConfiguration.securityConfiguration();
    }

    @Override
    public final B securityConfiguration(ClientSecurityConfiguration securityConfiguration) {
        mutableClientConfiguration.securityConfiguration(securityConfiguration);
        return thisBuilder();
    }

    public final ClientSecurityConfiguration getSecurityConfiguration() {
        return securityConfiguration();
    }

    public final void setSecurityConfiguration(ClientSecurityConfiguration securityConfiguration) {
        securityConfiguration(securityConfiguration);
    }

    @Override
    public final ClientRetryConfiguration retryConfiguration() {
        return mutableClientConfiguration.retryConfiguration();
    }

    @Override
    public final B retryConfiguration(ClientRetryConfiguration retryConfiguration) {
        mutableClientConfiguration.retryConfiguration(retryConfiguration);
        return thisBuilder();
    }

    public final ClientRetryConfiguration getRetryConfiguration() {
        return retryConfiguration();
    }

    public final void setRetryConfiguration(ClientRetryConfiguration retryConfiguration) {
        retryConfiguration(retryConfiguration);
    }

    @Override
    public final ClientListenerConfiguration listenerConfiguration() {
        return mutableClientConfiguration.listenerConfiguration();
    }

    @Override
    public final B listenerConfiguration(ClientListenerConfiguration listenerConfiguration) {
        mutableClientConfiguration.listenerConfiguration(listenerConfiguration);
        return thisBuilder();
    }

    public final ClientListenerConfiguration getListenerConfiguration() {
        return listenerConfiguration();
    }

    public final void setListenerConfiguration(ClientListenerConfiguration listenerConfiguration) {
        listenerConfiguration(listenerConfiguration);
    }

    @Override
    public final B httpConfiguration(ClientHttpConfiguration httpConfiguration) {
        this.httpConfiguration = httpConfiguration;
        return thisBuilder();
    }

    @Override
    public final ClientHttpConfiguration httpConfiguration() {
        return this.httpConfiguration;
    }

    public final ClientHttpConfiguration getHttpConfiguration() {
        return this.httpConfiguration;
    }

    public final void setHttpConfiguration(ClientHttpConfiguration httpConfiguration) {
        this.httpConfiguration = httpConfiguration;
    }

    @Override
    public final Optional<AwsCredentialsProvider> credentialsProvider() {
        return Optional.ofNullable(mutableClientConfiguration.credentialsProvider());
    }

    @Override
    public final B credentialsProvider(AwsCredentialsProvider credentialsProvider) {
        mutableClientConfiguration.credentialsProvider(credentialsProvider);
        return thisBuilder();
    }

    public final AwsCredentialsProvider getCredentialsProvider() {
        return credentialsProvider().orElse(null);
    }

    public final void setCredentialsProvider(AwsCredentialsProvider credentialsProvider) {
        credentialsProvider(credentialsProvider);
    }

    /**
     * Return "this" for method chaining.
     */
    @SuppressWarnings("unchecked")
    protected final B thisBuilder() {
        return (B) this;
    }

    /**
     * Wrapper around {@link SdkHttpClient} to prevent it from being closed. Used when the customer provides
     * an already built client in which case they are responsible for the lifecycle of it.
     */
    static class NonManagedSdkHttpClient implements SdkHttpClient {

        private final SdkHttpClient delegate;

        private NonManagedSdkHttpClient(SdkHttpClient delegate) {
            this.delegate = notNull(delegate, "SdkHttpClient must not be null");
        }

        @Override
        public AbortableCallable<SdkHttpFullResponse> prepareRequest(SdkHttpFullRequest request,
                                                                     SdkRequestContext requestContext) {
            return delegate.prepareRequest(request, requestContext);
        }

        @Override
        public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
            return delegate.getConfigurationValue(key);
        }

        @Override
        public void close() throws Exception {
            // Do nothing, this client is managed by the customer.
        }
    }
}
