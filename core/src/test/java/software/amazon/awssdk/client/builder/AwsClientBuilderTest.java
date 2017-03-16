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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonWebServiceClient;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.LegacyClientConfigurationFactory;
import software.amazon.awssdk.PredefinedLegacyClientConfigurations;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.auth.DefaultAwsCredentialsProviderChain;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.client.builder.AwsClientBuilder.EndpointConfiguration;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.regions.AwsRegionProvider;
import software.amazon.awssdk.regions.Regions;
import utils.builder.StaticExecutorFactory;

public class AwsClientBuilderTest {

    // Note that the tests rely on the socket timeout being set to some arbitrary unique value
    private static final LegacyClientConfiguration DEFAULT_CLIENT_CONFIG = new LegacyClientConfiguration()
            .withSocketTimeout(9001);

    /**
     * The sync client is tested less thoroughly then the async client primarily because the async
     * client exercises most of the same code paths so a bug introduced in the sync client builder
     * should be exposed via tests written against the async builder. This test is mainly for
     * additional coverage of the sync builder in case there is a regression specific to sync
     * builders.
     */
    @Test
    public void syncClientBuilder() {
        final List<RequestHandler2> requestHandlers = createRequestHandlerList(
                new ConcreteRequestHandler(), new ConcreteRequestHandler());
        final AwsCredentialsProvider credentials = mock(AwsCredentialsProvider.class);
        final RequestMetricCollector metrics = mock(RequestMetricCollector.class);

        //@formatter:off
        AmazonConcreteClient client = new ConcreteSyncBuilder()
                .withRegion(Regions.EU_CENTRAL_1)
                .withClientConfiguration(new LegacyClientConfiguration().withSocketTimeout(1234))
                .withCredentials(credentials)
                .withMetricsCollector(metrics)
                .withRequestHandlers(requestHandlers.toArray(new RequestHandler2[requestHandlers.size()]))
                .build();
        //@formatter:on

        assertEquals(URI.create("https://mockprefix.eu-central-1.amazonaws.com"),
                     client.getEndpoint());
        assertEquals(1234, client.getSyncParams().getClientConfiguration().getSocketTimeout());
        assertEquals(requestHandlers, client.getSyncParams().getRequestHandlers());
        assertEquals(credentials, client.getSyncParams().getCredentialsProvider());
        assertEquals(metrics, client.getSyncParams().getRequestMetricCollector());
    }

    @Test
    public void credentialsNotExplicitlySet_UsesDefaultCredentialChain() throws Exception {
        AwsAsyncClientParams params = builderWithRegion().build().getAsyncParams();
        assertThat(params.getCredentialsProvider(),
                   instanceOf(DefaultAwsCredentialsProviderChain.class));
    }

    @Test
    public void credentialsExplicitlySet_UsesExplicitCredentials() throws Exception {
        AwsCredentialsProvider provider = new AwsStaticCredentialsProvider(
                new BasicAwsCredentials("akid", "skid"));
        AwsAsyncClientParams params = builderWithRegion().withCredentials(provider).build()
                                                         .getAsyncParams();
        assertEquals(provider, params.getCredentialsProvider());
    }

    @Test
    public void metricCollectorNotExplicitlySet_UsesNullMetricsCollector() throws Exception {
        assertNull(builderWithRegion().build().getAsyncParams().getRequestMetricCollector());
    }

    @Test
    public void metricsCollectorExplicitlySet_UsesExplicitMetricsCollector() throws Exception {
        RequestMetricCollector metricCollector = RequestMetricCollector.NONE;
        AwsAsyncClientParams params = builderWithRegion().withMetricsCollector(metricCollector)
                                                         .build().getAsyncParams();
        assertEquals(metricCollector, params.getRequestMetricCollector());
    }

    @Test
    public void clientConfigurationNotExplicitlySet_UsesServiceDefaultClientConfiguration() {
        AwsAsyncClientParams params = builderWithRegion().build().getAsyncParams();
        LegacyClientConfiguration actualConfig = params.getClientConfiguration();
        assertEquals(DEFAULT_CLIENT_CONFIG.getSocketTimeout(), actualConfig.getSocketTimeout());
    }

    @Test
    public void clientConfigurationExplicitlySet_UsesExplicitConfiguration() {
        LegacyClientConfiguration config = new LegacyClientConfiguration().withSocketTimeout(1000);
        AwsAsyncClientParams params = builderWithRegion().withClientConfiguration(config).build()
                                                         .getAsyncParams();
        assertEquals(config.getSocketTimeout(), params.getClientConfiguration().getSocketTimeout());
    }

    @Test
    public void explicitRegionIsSet_UsesRegionToConstructEndpoint() {
        URI actualUri = new ConcreteAsyncBuilder().withRegion(Regions.US_WEST_2).build()
                                                  .getEndpoint();
        assertEquals(URI.create("https://mockprefix.us-west-2.amazonaws.com"), actualUri);
    }

    /**
     * If no region is explicitly given and no region can be found from the {@link
     * AwsRegionProvider} implementation then the builder should fail to build clients. We mock the
     * provider to yield consistent results for the tests.
     */
    @Test(expected = AmazonClientException.class)
    public void noRegionProvidedExplicitlyOrImplicitly_ThrowsException() {
        AwsRegionProvider mockRegionProvider = mock(AwsRegionProvider.class);
        when(mockRegionProvider.getRegion()).thenReturn(null);
        new ConcreteAsyncBuilder(mockRegionProvider).build();
    }

    /**
     * Customers may not need to explicitly configure a builder with a region if one can be found
     * from the {@link AwsRegionProvider} implementation. We mock the provider to yield consistent
     * results for the tests.
     */
    @Test
    public void regionImplicitlyProvided_UsesRegionToConstructEndpoint() {
        AwsRegionProvider mockRegionProvider = mock(AwsRegionProvider.class);
        when(mockRegionProvider.getRegion()).thenReturn("ap-southeast-2");
        final URI actualUri = new ConcreteAsyncBuilder(mockRegionProvider).build().getEndpoint();
        assertEquals(URI.create("https://mockprefix.ap-southeast-2.amazonaws.com"), actualUri);
    }

    @Test
    public void endpointAndSigningRegionCanBeUsedInPlaceOfSetRegion() {
        AmazonConcreteClient client = new ConcreteSyncBuilder()
                .withEndpointConfiguration(new EndpointConfiguration(
                        "https://mockprefix.ap-southeast-2.amazonaws.com",
                        "us-east-1"))
                .build();
        assertEquals("us-east-1", client.getSignerRegionOverride());
        assertEquals(URI.create("https://mockprefix.ap-southeast-2.amazonaws.com"), client.getEndpoint());
    }

    @Test(expected = IllegalStateException.class)
    public void cannotSetBothEndpointConfigurationAndRegionOnBuilder() {
        new ConcreteSyncBuilder()
                .withEndpointConfiguration(new EndpointConfiguration(
                        "http://localhost:3030",
                        "us-west-2"))
                .withRegion("us-east-1")
                .build();
    }

    @Test
    public void defaultClientConfigAndNoExplicitExecutor_UsesDefaultExecutorBasedOnMaxConns() {
        ExecutorService executor = builderWithRegion().build().getAsyncParams().getExecutor();
        assertThat(executor, instanceOf(ThreadPoolExecutor.class));
        assertEquals(PredefinedLegacyClientConfigurations.defaultConfig().getMaxConnections(),
                     ((ThreadPoolExecutor) executor).getMaximumPoolSize());
    }

    @Test
    public void customMaxConnsAndNoExplicitExecutor_UsesDefaultExecutorBasedOnMaxConns() {
        final int maxConns = 10;
        ExecutorService executor = builderWithRegion()
                .withClientConfiguration(new LegacyClientConfiguration().withMaxConnections(maxConns))
                .build().getAsyncParams().getExecutor();
        assertThat(executor, instanceOf(ThreadPoolExecutor.class));
        assertEquals(maxConns, ((ThreadPoolExecutor) executor).getMaximumPoolSize());
    }

    /**
     * If a custom executor is set then the Max Connections in Client Configuration should be
     * ignored and the executor should be used as is.
     */
    @Test
    public void customMaxConnsAndExplicitExecutor_UsesExplicitExecutor() throws Exception {
        final int clientConfigMaxConns = 10;
        final int customExecutorThreadCount = 15;
        final ExecutorService customExecutor = Executors
                .newFixedThreadPool(customExecutorThreadCount);
        LegacyClientConfiguration config = new LegacyClientConfiguration().withMaxConnections(clientConfigMaxConns);
        ExecutorService actualExecutor = builderWithRegion().withClientConfiguration(config)
                                                            .withExecutorFactory(new StaticExecutorFactory(customExecutor))
                                                            .build().getAsyncParams().getExecutor();
        assertThat(actualExecutor, instanceOf(ThreadPoolExecutor.class));
        assertEquals(customExecutor, actualExecutor);
        assertEquals(customExecutorThreadCount,
                     ((ThreadPoolExecutor) actualExecutor).getMaximumPoolSize());

    }

    @Test
    public void noRequestHandlersExplicitlySet_UsesEmptyRequestHandlerList() throws Exception {
        List<RequestHandler2> requestHandlers = builderWithRegion().build().getAsyncParams()
                                                                   .getRequestHandlers();
        assertThat(requestHandlers, empty());
    }

    @Test
    public void requestHandlersExplicitlySet_UsesClonedListOfExplicitRequestHandlers() throws
                                                                                       Exception {
        List<RequestHandler2> expectedHandlers = createRequestHandlerList(
                new ConcreteRequestHandler(), new ConcreteRequestHandler());
        List<RequestHandler2> actualHandlers = builderWithRegion()
                .withRequestHandlers(expectedHandlers.toArray(new RequestHandler2[0])).build()
                .getAsyncParams().getRequestHandlers();
        assertEquals(expectedHandlers, actualHandlers);
        // List should be copied or cloned
        assertThat(actualHandlers, not(sameInstance(expectedHandlers)));
    }

    @Test
    public void requestHandlersExplicitlySetWithVarArgs_UsesExplicitRequestHandlers() throws
                                                                                      Exception {
        RequestHandler2 handlerOne = new ConcreteRequestHandler();
        RequestHandler2 handlerTwo = new ConcreteRequestHandler();
        RequestHandler2 handlerThree = new ConcreteRequestHandler();
        List<RequestHandler2> actualHandlers = builderWithRegion()
                .withRequestHandlers(handlerOne, handlerTwo, handlerThree).build().getAsyncParams()
                .getRequestHandlers();
        assertEquals(createRequestHandlerList(handlerOne, handlerTwo, handlerThree),
                     actualHandlers);
    }

    /**
     * @return A {@link ConcreteAsyncBuilder} instance with an explicitly configured region.
     */
    private ConcreteAsyncBuilder builderWithRegion() {
        return new ConcreteAsyncBuilder().withRegion(Regions.AP_NORTHEAST_1);
    }

    private List<RequestHandler2> createRequestHandlerList(RequestHandler2... handlers) {
        List<RequestHandler2> requestHandlers = new ArrayList<RequestHandler2>();
        Collections.addAll(requestHandlers, handlers);
        return requestHandlers;
    }

    private static class ConcreteRequestHandler extends RequestHandler2 {
    }

    private static class MockLegacyClientConfigurationFactory extends LegacyClientConfigurationFactory {
        @Override
        protected LegacyClientConfiguration getDefaultConfig() {
            return DEFAULT_CLIENT_CONFIG;
        }
    }

    private static class ConcreteAsyncBuilder extends
                                              AwsAsyncClientBuilder<ConcreteAsyncBuilder, AmazonConcreteClient> {
        private ConcreteAsyncBuilder() {
            super(new MockLegacyClientConfigurationFactory());
        }

        private ConcreteAsyncBuilder(AwsRegionProvider mockRegionProvider) {
            super(new MockLegacyClientConfigurationFactory(), mockRegionProvider);
        }

        @Override
        protected AmazonConcreteClient build(AwsAsyncClientParams asyncClientParams) {
            return new AmazonConcreteClient(asyncClientParams);
        }

        @Override
        public String getServiceName() {
            return "mockprefix";
        }
    }

    private static class ConcreteSyncBuilder extends
                                             AwsSyncClientBuilder<ConcreteSyncBuilder, AmazonConcreteClient> {
        private ConcreteSyncBuilder() {
            super(new MockLegacyClientConfigurationFactory());
        }

        @Override
        protected AmazonConcreteClient build(AwsSyncClientParams asyncClientParams) {
            return new AmazonConcreteClient(asyncClientParams);
        }

        @Override
        public String getServiceName() {
            return "mockprefix";
        }
    }

    /**
     * Dummy client used by both the {@link ConcreteSyncBuilder} and {@link ConcreteAsyncBuilder}.
     * Captures the param object the client was created for for verification in tests.
     */
    private static class AmazonConcreteClient extends AmazonWebServiceClient {

        private AwsAsyncClientParams asyncParams;
        private AwsSyncClientParams syncParams;

        private AmazonConcreteClient(AwsAsyncClientParams asyncParams) {
            super(new LegacyClientConfiguration());
            this.asyncParams = asyncParams;
        }

        private AmazonConcreteClient(AwsSyncClientParams syncParams) {
            super(new LegacyClientConfiguration());
            this.syncParams = syncParams;
        }

        @Override
        public String getServiceNameIntern() {
            return "mockservice";
        }

        @Override
        public String getEndpointPrefix() {
            return "mockprefix";
        }

        public URI getEndpoint() {
            return this.endpoint;
        }

        public AwsAsyncClientParams getAsyncParams() {
            return asyncParams;
        }

        public AwsSyncClientParams getSyncParams() {
            return syncParams;
        }
    }
}
