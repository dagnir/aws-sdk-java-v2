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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import software.amazon.awssdk.AmazonWebServiceClient;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.LegacyClientConfigurationFactory;
import software.amazon.awssdk.PredefinedLegacyClientConfigurations;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultAwsCredentialsProviderChain;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.auth.SignerFactory;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.internal.auth.DefaultSignerProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.regions.AwsRegionProvider;
import software.amazon.awssdk.regions.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.runtime.endpoint.DefaultServiceEndpointBuilder;

/**
 * Base class for all service specific client builders.
 *
 * @param <SubclassT>    Concrete builder type, used for better fluent methods.
 * @param <TypeToBuildT> Type that this builder builds.
 */
@NotThreadSafe
@SdkProtectedApi
public abstract class AwsClientBuilder<SubclassT extends AwsClientBuilder, TypeToBuildT> {

    /**
     * Default Region Provider chain. Used only when the builder is not explicitly configured with a
     * region.
     */
    private static final AwsRegionProvider DEFAULT_REGION_PROVIDER = new DefaultAwsRegionProviderChain();

    /**
     * Different services may have custom client configuration factories to vend defaults tailored
     * for that service. If no explicit client configuration is provided to the builder the default
     * factory for the service is used.
     */
    private final LegacyClientConfigurationFactory clientConfigFactory;

    /**
     * {@link AwsRegionProvider} to use when no explicit region or endpointConfiguration is configured.
     * This is currently not exposed for customization by customers.
     */
    private final AwsRegionProvider regionProvider;

    private AwsCredentialsProvider credentials;
    private LegacyClientConfiguration clientConfig;
    private RequestMetricCollector metricsCollector;
    private Region region;
    private List<RequestHandler2> requestHandlers;
    private EndpointConfiguration endpointConfiguration;

    protected AwsClientBuilder(LegacyClientConfigurationFactory clientConfigFactory) {
        this(clientConfigFactory, DEFAULT_REGION_PROVIDER);
    }

    @SdkTestInternalApi
    protected AwsClientBuilder(LegacyClientConfigurationFactory clientConfigFactory,
                               AwsRegionProvider regionProvider) {
        this.clientConfigFactory = clientConfigFactory;
        this.regionProvider = regionProvider;
    }

    /**
     * Gets the AWSCredentialsProvider currently configured in the builder.
     */
    public final AwsCredentialsProvider getCredentials() {
        return this.credentials;
    }

    /**
     * Sets the AWSCredentialsProvider used by the client. If not specified the default is {@link
     * DefaultAwsCredentialsProviderChain}.
     *
     * @param credentialsProvider New AWSCredentialsProvider to use.
     */
    public final void setCredentials(AwsCredentialsProvider credentialsProvider) {
        this.credentials = credentialsProvider;
    }

    /**
     * Sets the AWSCredentialsProvider used by the client. If not specified the default is {@link
     * DefaultAwsCredentialsProviderChain}.
     *
     * @param credentialsProvider New AWSCredentialsProvider to use.
     * @return This object for method chaining.
     */
    public final SubclassT withCredentials(AwsCredentialsProvider credentialsProvider) {
        setCredentials(credentialsProvider);
        return getSubclass();
    }

    /**
     * If the builder isn't explicitly configured with credentials we use the {@link
     * DefaultAwsCredentialsProviderChain}.
     */
    private AwsCredentialsProvider resolveCredentials() {
        return (credentials == null) ? DefaultAwsCredentialsProviderChain.getInstance() : credentials;
    }

    /**
     * Gets the ClientConfiguration currently configured in the builder
     */
    public final LegacyClientConfiguration getClientConfiguration() {
        return this.clientConfig;
    }

    /**
     * Sets the ClientConfiguration to be used by the client. If not specified the default is
     * typically {@link PredefinedLegacyClientConfigurations#defaultConfig} but may differ per service.
     *
     * @param config Custom configuration to use
     */
    public final void setClientConfiguration(LegacyClientConfiguration config) {
        this.clientConfig = config;
    }

    /**
     * Sets the ClientConfiguration to be used by the client. If not specified the default is
     * typically {@link PredefinedLegacyClientConfigurations#defaultConfig} but may differ per service.
     *
     * @param config Custom configuration to use
     * @return This object for method chaining.
     */
    public final SubclassT withClientConfiguration(LegacyClientConfiguration config) {
        setClientConfiguration(config);
        return getSubclass();
    }

    /**
     * If not explicit client configuration is provided we consult the {@link
     * LegacyClientConfigurationFactory} of the service. If an explicit configuration is provided we use
     * ClientConfiguration's copy constructor to avoid mutation.
     */
    private LegacyClientConfiguration resolveClientConfiguration() {
        return (clientConfig == null) ? clientConfigFactory.getConfig() :
                new LegacyClientConfiguration(clientConfig);
    }

    /**
     * Gets the {@link RequestMetricCollector} in use by the builder.
     */
    public final RequestMetricCollector getMetricsCollector() {
        return this.metricsCollector;
    }

    /**
     * Sets a custom RequestMetricCollector to use for the client.
     *
     * @param metrics Custom RequestMetricCollector to use.
     */
    public final void setMetricsCollector(RequestMetricCollector metrics) {
        this.metricsCollector = metrics;
    }

    /**
     * Sets a custom RequestMetricCollector to use for the client.
     *
     * @param metrics Custom RequestMetricCollector to use.
     * @return This object for method chaining.
     */
    public final SubclassT withMetricsCollector(RequestMetricCollector metrics) {
        setMetricsCollector(metrics);
        return getSubclass();
    }

    /**
     * Gets the region in use by the builder.
     */
    public final String getRegion() {
        return region == null ? null : region.getName();
    }

    /**
     * Sets the region to be used by the client. This will be used to determine both the
     * service endpoint (eg: https://sns.us-west-1.amazonaws.com) and signing region (eg: us-west-1)
     * for requests. If neither region or endpoint configuration {@link #setEndpointConfiguration(EndpointConfiguration)}
     * are explicitly provided in the builder the {@link #DEFAULT_REGION_PROVIDER} is consulted.
     *
     * @param region Region to use
     */
    public final void setRegion(String region) {
        withRegion(region);
    }

    private void setRegion(AmazonWebServiceClient client) {
        if (region != null && endpointConfiguration != null) {
            throw new IllegalStateException("Only one of Region or EndpointConfiguration may be set.");
        }
        if (endpointConfiguration != null) {
            client.setEndpoint(endpointConfiguration.getServiceEndpoint());
            client.setSignerRegionOverride(endpointConfiguration.getSigningRegion());
        } else if (region != null) {
            client.setRegion(region);
        } else {
            final String region = determineRegionFromRegionProvider();
            if (region != null) {
                client.setRegion(RegionUtils.getRegion(region));
            } else {
                throw new SdkClientException(
                        "Unable to find a region via the region provider chain. " +
                                "Must provide an explicit region in the builder or setup environment to supply a region.");
            }
        }
    }

    /**
     * Sets the region to be used by the client. This will be used to determine both the
     * service endpoint (eg: https://sns.us-west-1.amazonaws.com) and signing region (eg: us-west-1)
     * for requests. If neither region or endpoint configuration {@link #setEndpointConfiguration(EndpointConfiguration)}
     * are explicitly provided in the builder the {@link #DEFAULT_REGION_PROVIDER} is consulted.
     * <p>
     * <p> For regions not explicitly in the {@link Regions} enum use the {@link
     * #withRegion(String)} overload.</p>
     *
     * @param region Region to use
     * @return This object for method chaining.
     */
    public final SubclassT withRegion(Regions region) {
        return withRegion(region.getName());
    }

    /**
     * Sets the region to be used by the client. This will be used to determine both the
     * service endpoint (eg: https://sns.us-west-1.amazonaws.com) and signing region (eg: us-west-1)
     * for requests. If neither region or endpoint configuration {@link #setEndpointConfiguration(EndpointConfiguration)}
     * are explicitly provided in the builder the {@link #DEFAULT_REGION_PROVIDER} is consulted.
     *
     * @param region Region to use
     * @return This object for method chaining.
     */
    public final SubclassT withRegion(String region) {
        return withRegion(RegionUtils.getRegion(region));
    }

    /**
     * Sets the region to be used by the client. This will be used to determine both the
     * service endpoint (eg: https://sns.us-west-1.amazonaws.com) and signing region (eg: us-west-1)
     * for requests. If neither region or endpoint configuration {@link #setEndpointConfiguration(EndpointConfiguration)}
     * are explicitly provided in the builder the {@link #DEFAULT_REGION_PROVIDER} is consulted.
     *
     * @param region Region to use, this will be used to determine both service endpoint
     *               and the signing region
     * @return This object for method chaining.
     */
    private SubclassT withRegion(Region region) {
        this.region = region;
        return getSubclass();
    }

    /**
     * Gets the service endpointConfiguration in use by the builder
     */
    public final EndpointConfiguration getEndpoint() {
        return endpointConfiguration;
    }

    /**
     * Sets the endpoint configuration (service endpoint & signing region) to be used for requests. If neither region
     * {@link #setRegion(String)} or endpoint configuration are explicitly provided in the builder the
     * {@link #DEFAULT_REGION_PROVIDER} is consulted.
     * <p>
     * <p><b>Only use this if using a non-standard service endpoint - the recommended approach for configuring a client is to use
     * {@link #setRegion(String)}</b>
     *
     * @param endpointConfiguration The endpointConfiguration to use
     */
    public final void setEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
        withEndpointConfiguration(endpointConfiguration);
    }

    /**
     * Sets the endpoint configuration (service endpoint & signing region) to be used for requests. If neither region
     * {@link #withRegion(String)} or endpoint configuration are explicitly provided in the builder the
     * {@link #DEFAULT_REGION_PROVIDER} is consulted.
     * <p>
     * <p><b>Only use this if using a non-standard service endpoint - the recommended approach for configuring a client is to use
     * {@link #withRegion(String)}</b>
     *
     * @param endpointConfiguration The endpointConfiguration to use
     * @return This object for method chaining.
     */
    public final SubclassT withEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
        this.endpointConfiguration = endpointConfiguration;
        return getSubclass();
    }

    /**
     * Gets the list of request handlers in use by the builder.
     */
    public final List<RequestHandler2> getRequestHandlers() {
        return this.requestHandlers == null ? null :
                Collections.unmodifiableList(this.requestHandlers);
    }

    /**
     * Sets the request handlers to use in the client.
     *
     * @param handlers Request handlers to use for client.
     */
    public final void setRequestHandlers(RequestHandler2... handlers) {
        this.requestHandlers = Arrays.asList(handlers);
    }

    /**
     * Sets the request handlers to use in the client.
     *
     * @param handlers Request handlers to use for client.
     * @return This object for method chaining.
     */
    public final SubclassT withRequestHandlers(RequestHandler2... handlers) {
        setRequestHandlers(handlers);
        return getSubclass();
    }

    /**
     * Request handlers are copied to a new list to avoid mutation, if no request handlers are
     * provided to the builder we supply an empty list.
     */
    private List<RequestHandler2> resolveRequestHandlers() {
        return (requestHandlers == null) ? new ArrayList<RequestHandler2>() :
                new ArrayList<RequestHandler2>(requestHandlers);
    }

    /**
     * Builds a client with the configure properties.
     *
     * @return Client instance to make API calls with.
     */
    public abstract TypeToBuildT build();

    public abstract String getServiceName();

    /**
     * @return An instance of AwsSyncClientParams that has all params to be used in the sync client constructor.
     */
    protected final AwsSyncClientParams getSyncClientParams() {
        return new SyncBuilderParams();
    }

    /**
     * Attempt to determine the region from the configured region provider. This will return null in the event that the
     * region provider could not determine the region automatically.
     */
    private String determineRegionFromRegionProvider() {
        try {
            return regionProvider.getRegion();
        } catch (SdkClientException e) {
            // The AwsRegionProviderChain that is used by default throws an exception instead of returning null when
            // the region is not defined. For that reason, we have to support both throwing an exception and returning
            // null as the region not being defined.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected final SubclassT getSubclass() {
        return (SubclassT) this;
    }

    /**
     * A container for configuration required to submit requests to a service (service endpoint and signing region)
     */
    public static final class EndpointConfiguration {
        private final String serviceEndpoint;
        private final String signingRegion;

        /**
         * @param serviceEndpoint the service endpoint either with or without the protocol
         *                        (e.g. https://sns.us-west-1.amazonaws.com or sns.us-west-1.amazonaws.com)
         * @param signingRegion   the region to use for SigV4 signing of requests (e.g. us-west-1)
         */
        public EndpointConfiguration(String serviceEndpoint, String signingRegion) {
            this.serviceEndpoint = serviceEndpoint;
            this.signingRegion = signingRegion;
        }

        public String getServiceEndpoint() {
            return serviceEndpoint;
        }

        public String getSigningRegion() {
            return signingRegion;
        }
    }

    /**
     * Presents a view of the builder to be used in a client constructor.
     */
    protected class SyncBuilderParams extends AwsAsyncClientParams {


        private final LegacyClientConfiguration clientConfig;
        private final AwsCredentialsProvider credentials;
        private final RequestMetricCollector metricsCollector;
        private final List<RequestHandler2> requestHandlers;

        protected SyncBuilderParams() {
            this.clientConfig = resolveClientConfiguration();
            this.credentials = resolveCredentials();
            this.metricsCollector = AwsClientBuilder.this.metricsCollector;
            this.requestHandlers = resolveRequestHandlers();
        }

        @Override
        public AwsCredentialsProvider getCredentialsProvider() {
            return this.credentials;
        }

        @Override
        public LegacyClientConfiguration getClientConfiguration() {
            return this.clientConfig;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return this.metricsCollector;
        }

        @Override
        public List<RequestHandler2> getRequestHandlers() {
            return this.requestHandlers;
        }

        @Override
        public SignerProvider getSignerProvider() {
            Region clientRegion = region != null ? region : RegionUtils.getRegion(determineRegionFromRegionProvider());

            Signer signer = SignerFactory.getSigner(getServiceName(), clientRegion.getName());

            return new DefaultSignerProvider(signer);
        }

        @Override
        public URI getEndpoint() {
            if (endpointConfiguration != null) {
                return URI.create(endpointConfiguration.getServiceEndpoint());
            }

            Region clientRegion = region;

            if (clientRegion == null) {
                clientRegion = RegionUtils.getRegion(determineRegionFromRegionProvider());
            }

            return new DefaultServiceEndpointBuilder(getServiceName(), Protocol.HTTPS.toString())
                    .withRegion(clientRegion)
                    .getServiceEndpoint();
        }

        @Override
        public ExecutorService getExecutor() {
            throw new UnsupportedOperationException("ExecutorService is not used for sync client.");
        }

    }

}
