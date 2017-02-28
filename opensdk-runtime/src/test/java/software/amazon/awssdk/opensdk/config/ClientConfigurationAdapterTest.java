/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk.config;

import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.opensdk.internal.config.ApiGatewayClientConfiguration;
import software.amazon.awssdk.opensdk.internal.config.ApiGatewayClientConfigurationFactory;
import software.amazon.awssdk.opensdk.internal.config.ClientConfigurationAdapter;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

public class ClientConfigurationAdapterTest {

    private static final int DEFAULT_MAX_ERROR_RETRY = -1;
    private static final int DEFAULT_PROXY_PORT = -1;
    private static final int[] SOCKET_BUFFER_HINTS = {0,0};

    @BeforeClass
    public static void setup() {
        clearProxyProperties();
    }

    /**
     * When no custom configuration is provided,
     * the global ClientConfiguration defaults are honored.
     */
    @Test
    public void testDefaultApiGatewayClientConfiguration() {
        ClientConfiguration config = ClientConfigurationAdapter.adapt(new ApiGatewayClientConfiguration(), new ClientConfiguration());

        assertEquals(ClientConfiguration.DEFAULT_USER_AGENT, config.getUserAgentPrefix());
        assertEquals(Protocol.HTTPS, config.getProtocol());
        assertEquals(ClientConfiguration.DEFAULT_MAX_CONNECTIONS, config.getMaxConnections());
        assertEquals(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT, config.getSocketTimeout());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT, config.getConnectionTimeout());
        assertEquals(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT, config.getRequestTimeout());
        assertEquals(ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT, config.getClientExecutionTimeout());
        assertEquals(ClientConfiguration.DEFAULT_THROTTLE_RETRIES, config.useThrottledRetries());
        assertEquals(ClientConfiguration.DEFAULT_USE_REAPER, config.useReaper());
        assertEquals(ClientConfiguration.DEFAULT_USE_GZIP, config.useGzip());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_TTL, config.getConnectionTtl());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_MAX_IDLE_MILLIS, config.getConnectionMaxIdleMillis());
        assertEquals(ClientConfiguration.DEFAULT_TCP_KEEP_ALIVE, config.useTcpKeepAlive());
        assertEquals(ClientConfiguration.DEFAULT_CACHE_RESPONSE_METADATA, config.getCacheResponseMetadata());
        assertEquals(ClientConfiguration.DEFAULT_RESPONSE_METADATA_CACHE_SIZE, config.getResponseMetadataCacheSize());
        assertEquals(ClientConfiguration.DEFAULT_USE_EXPECT_CONTINUE, config.isUseExpectContinue());
        assertEquals(DEFAULT_MAX_ERROR_RETRY, config.getMaxErrorRetry());
        assertEquals(DEFAULT_PROXY_PORT, config.getProxyPort());
        assertArrayEquals(SOCKET_BUFFER_HINTS, config.getSocketBufferSizeHints());

        assertNull(config.getLocalAddress());
        assertNull(config.getProxyHost());
        assertNull(config.getProxyUsername());
        assertNull(config.getProxyPassword());
        assertNull(config.getProxyDomain());
        assertNull(config.getProxyWorkstation());
        assertNull(config.getNonProxyHosts());
        assertNull(config.getUserAgentSuffix());
        assertNull(config.getSignerOverride());

        assertNotNull(config.getRetryPolicy());
        assertNotNull(config.getDnsResolver());
        assertNotNull(config.getSecureRandom());
        assertNotNull(config.getApacheHttpClientConfig());
        assertFalse(config.isPreemptiveBasicProxyAuth());
    }

    /**
     * When custom ApiGatewayClientConfiguration is provided,
     * tests that the adapted configuration contains all values from
     * the custom ApiGatewayClientConfiguration.
     */
    @Test
    public void testCustomApiGatewayClientConfiguration() {
        Protocol protocol = Protocol.HTTP;
        String proxyHost = "host", proxyUsername = "username", proxyPassword = "password", proxyDomain = "domain",
                proxyWorkstation = "workstation", nonProxyHost = "optionalhosts";
        int proxyPort = 3456;
        boolean preemptiveBasicProxyAuth = true;

        int maxConnections = 100;
        long ttl = 3000, maxIdle = 10000;
        boolean useReaper = true;

        int socketTimeout = 1000, connectionTimeout = 2000, requestTimeout = 3000, clientExecutionTimeout = 4000;

        ApiGatewayClientConfiguration apigConfig = new ApiGatewayClientConfiguration()
                .proxyConfiguration(new ProxyConfiguration()
                        .protocol(protocol)
                        .proxyHost(proxyHost)
                        .proxyPort(proxyPort)
                        .proxyUsername(proxyUsername)
                        .proxyPassword(proxyPassword)
                        .proxyDomain(proxyDomain)
                        .proxyWorkstation(proxyWorkstation)
                        .nonProxyHosts(nonProxyHost)
                        .preemptiveBasicProxyAuth(preemptiveBasicProxyAuth)
                )
                .connectionConfiguration(new ConnectionConfiguration()
                        .maxConnections(maxConnections)
                        .connectionTtl(ttl)
                        .connectionMaxIdleMillis(maxIdle)
                        .useReaper(useReaper)
                )
                .timeoutConfiguration(new TimeoutConfiguration()
                        .socketTimeout(socketTimeout)
                        .connectionTimeout(connectionTimeout)
                        .httpRequestTimeout(requestTimeout)
                        .totalExecutionTimeout(clientExecutionTimeout)
                );


        ClientConfiguration config = ClientConfigurationAdapter.adapt(apigConfig, new ClientConfiguration());
        
        assertEquals(protocol, config.getProtocol());
        assertEquals(proxyHost, config.getProxyHost());
        assertEquals(proxyPort, config.getProxyPort());
        assertEquals(proxyUsername, config.getProxyUsername());
        assertEquals(proxyPassword, config.getProxyPassword());
        assertEquals(proxyDomain, config.getProxyDomain());
        assertEquals(proxyWorkstation, config.getProxyWorkstation());
        assertEquals(nonProxyHost, config.getNonProxyHosts());
        assertEquals(preemptiveBasicProxyAuth, config.isPreemptiveBasicProxyAuth());

        assertEquals(maxConnections, config.getMaxConnections());
        assertEquals(ttl, config.getConnectionTtl());
        assertEquals(maxIdle, config.getConnectionMaxIdleMillis());
        assertEquals(useReaper, config.useReaper());

        assertEquals(socketTimeout, config.getSocketTimeout());
        assertEquals(connectionTimeout, config.getConnectionTimeout());
        assertEquals(requestTimeout, config.getRequestTimeout());
        assertEquals(clientExecutionTimeout, config.getClientExecutionTimeout());

        assertEquals(ClientConfiguration.DEFAULT_USER_AGENT, config.getUserAgentPrefix());
        assertEquals(ClientConfiguration.DEFAULT_THROTTLE_RETRIES, config.useThrottledRetries());
        assertEquals(ClientConfiguration.DEFAULT_USE_GZIP, config.useGzip());
        assertEquals(ClientConfiguration.DEFAULT_TCP_KEEP_ALIVE, config.useTcpKeepAlive());
        assertEquals(ClientConfiguration.DEFAULT_CACHE_RESPONSE_METADATA, config.getCacheResponseMetadata());
        assertEquals(ClientConfiguration.DEFAULT_RESPONSE_METADATA_CACHE_SIZE, config.getResponseMetadataCacheSize());
        assertEquals(ClientConfiguration.DEFAULT_USE_EXPECT_CONTINUE, config.isUseExpectContinue());
        assertEquals(DEFAULT_MAX_ERROR_RETRY, config.getMaxErrorRetry());
        assertArrayEquals(SOCKET_BUFFER_HINTS, config.getSocketBufferSizeHints());

        assertNull(config.getLocalAddress());
        assertNull(config.getUserAgentSuffix());
        assertNull(config.getSignerOverride());

        assertNotNull(config.getRetryPolicy());
        assertNotNull(config.getDnsResolver());
        assertNotNull(config.getSecureRandom());
        assertNotNull(config.getApacheHttpClientConfig());
    }

    /**
     * When custom ApiGatewayClientConfiguration and service default configuration are provided,
     * the custom configuration takes precedence over service default config which
     * takes precedence over the global default ClientConfiguration.
     */
    @Test
    public void testCustomConfigHasPrecedenceOverServiceDefaultConfig() {

        int serviceDefaultMaxConnections = 500, requestTimeout = 50000, proxyPort = 1234;
        String proxyHost = "host";

        ApiGatewayClientConfiguration apigConfig = new ApiGatewayClientConfiguration()
                                                                           .proxyConfiguration(new ProxyConfiguration().proxyHost(proxyHost).proxyPort(proxyPort))
                                                                           .timeoutConfiguration(new TimeoutConfiguration().httpRequestTimeout(requestTimeout));

        ClientConfiguration serviceDefaultConfig = new ClientConfiguration()
                .withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)
                .withMaxConnections(serviceDefaultMaxConnections)
                .withRequestTimeout(3000)
                .withTcpKeepAlive(true);

        ClientConfiguration config = ClientConfigurationAdapter.adapt(apigConfig, serviceDefaultConfig);

        assertTrue(config.useTcpKeepAlive());
        assertEquals(proxyHost, config.getProxyHost());
        assertEquals(proxyPort, config.getProxyPort());
        assertEquals(serviceDefaultMaxConnections, config.getMaxConnections());
        assertEquals(requestTimeout, config.getRequestTimeout());

        assertEquals(PredefinedRetryPolicies.NO_RETRY_POLICY, config.getRetryPolicy());
        assertEquals(ClientConfiguration.DEFAULT_USER_AGENT, config.getUserAgentPrefix());
        assertEquals(Protocol.HTTPS, config.getProtocol());
        assertEquals(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT, config.getSocketTimeout());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT, config.getConnectionTimeout());
        assertEquals(ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT, config.getClientExecutionTimeout());
        assertEquals(ClientConfiguration.DEFAULT_THROTTLE_RETRIES, config.useThrottledRetries());
        assertEquals(ClientConfiguration.DEFAULT_USE_REAPER, config.useReaper());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_TTL, config.getConnectionTtl());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_MAX_IDLE_MILLIS, config.getConnectionMaxIdleMillis());
        assertEquals(ClientConfiguration.DEFAULT_CACHE_RESPONSE_METADATA, config.getCacheResponseMetadata());
        assertEquals(ClientConfiguration.DEFAULT_RESPONSE_METADATA_CACHE_SIZE, config.getResponseMetadataCacheSize());
        assertEquals(ClientConfiguration.DEFAULT_USE_EXPECT_CONTINUE, config.isUseExpectContinue());
        assertEquals(DEFAULT_MAX_ERROR_RETRY, config.getMaxErrorRetry());
        assertArrayEquals(SOCKET_BUFFER_HINTS, config.getSocketBufferSizeHints());

        assertNull(config.getLocalAddress());
        assertNull(config.getProxyUsername());
        assertNull(config.getProxyPassword());
        assertNull(config.getProxyDomain());
        assertNull(config.getProxyWorkstation());
        assertNull(config.getNonProxyHosts());
        assertNull(config.getUserAgentSuffix());
        assertNull(config.getSignerOverride());


        assertNotNull(config.getRetryPolicy());
        assertNotNull(config.getDnsResolver());
        assertNotNull(config.getSecureRandom());
        assertNotNull(config.getApacheHttpClientConfig());
        assertFalse(config.isPreemptiveBasicProxyAuth());
        assertFalse(config.useGzip());
    }

    /**
     * Tests that when no custom configuration is provided,
     * the defaults from ApiGatewayClientConfigurationFactory are honored.
     */
    @Test
    public void testConfigFromApiGatewayClientConfigurationFactory() {
        ClientConfiguration config = ClientConfigurationAdapter.adapt(new ApiGatewayClientConfiguration(), new ApiGatewayClientConfigurationFactory().getConfig());

        assertEquals(PredefinedRetryPolicies.NO_RETRY_POLICY, config.getRetryPolicy());
        assertEquals(ClientConfiguration.DEFAULT_USER_AGENT, config.getUserAgentPrefix());
        assertEquals(Protocol.HTTPS, config.getProtocol());
        assertEquals(ClientConfiguration.DEFAULT_MAX_CONNECTIONS, config.getMaxConnections());
        assertEquals(ApiGatewayClientConfigurationFactory.DEFAULT_SOCKET_TIMEOUT, config.getSocketTimeout());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT, config.getConnectionTimeout());
        assertEquals(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT, config.getRequestTimeout());
        assertEquals(ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT, config.getClientExecutionTimeout());
        assertEquals(ClientConfiguration.DEFAULT_THROTTLE_RETRIES, config.useThrottledRetries());
        assertEquals(ClientConfiguration.DEFAULT_USE_REAPER, config.useReaper());
        assertEquals(ClientConfiguration.DEFAULT_USE_GZIP, config.useGzip());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_TTL, config.getConnectionTtl());
        assertEquals(ClientConfiguration.DEFAULT_CONNECTION_MAX_IDLE_MILLIS, config.getConnectionMaxIdleMillis());
        assertEquals(ClientConfiguration.DEFAULT_TCP_KEEP_ALIVE, config.useTcpKeepAlive());
        assertEquals(ApiGatewayClientConfigurationFactory.DEFAULT_CACHE_RESPONSE_METADATA, config.getCacheResponseMetadata());
        assertEquals(ClientConfiguration.DEFAULT_RESPONSE_METADATA_CACHE_SIZE, config.getResponseMetadataCacheSize());
        assertEquals(ClientConfiguration.DEFAULT_USE_EXPECT_CONTINUE, config.isUseExpectContinue());
        assertEquals(DEFAULT_MAX_ERROR_RETRY, config.getMaxErrorRetry());
        assertEquals(DEFAULT_PROXY_PORT, config.getProxyPort());
        assertArrayEquals(SOCKET_BUFFER_HINTS, config.getSocketBufferSizeHints());

        assertNull(config.getLocalAddress());
        assertNull(config.getProxyHost());
        assertNull(config.getProxyUsername());
        assertNull(config.getProxyPassword());
        assertNull(config.getProxyDomain());
        assertNull(config.getProxyWorkstation());
        assertNull(config.getNonProxyHosts());
        assertNull(config.getUserAgentSuffix());
        assertNull(config.getSignerOverride());

        assertNotNull(config.getDnsResolver());
        assertNotNull(config.getSecureRandom());
        assertNotNull(config.getApacheHttpClientConfig());
        assertFalse(config.isPreemptiveBasicProxyAuth());
    }

    private static void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("http.nonProxyHosts");
    }

}
