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

package software.amazon.awssdk.http.apache.internal.impl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import software.amazon.awssdk.http.SdkHttpClientSettings;
import software.amazon.awssdk.http.apache.internal.conn.SdkTLSSocketFactory;

/**
 * Factory class to create connection manager used by the apache client.
 */
public class ApacheConnectionManagerFactory {

    public HttpClientConnectionManager create(final SdkHttpClientSettings settings) {
        ConnectionSocketFactory sslsf = getPreferredSocketFactory(settings);

        final PoolingHttpClientConnectionManager cm = new
                PoolingHttpClientConnectionManager(
                createSocketFactoryRegistry(sslsf),
                null,
                DefaultSchemePortResolver.INSTANCE,
                null,
                settings.getConnectionPoolTTL(),
                TimeUnit.MILLISECONDS);

        cm.setDefaultMaxPerRoute(settings.getMaxConnections());
        cm.setMaxTotal(settings.getMaxConnections());
        cm.setDefaultSocketConfig(buildSocketConfig(settings));
        cm.setDefaultConnectionConfig(buildConnectionConfig(settings));

        return cm;
    }

    private ConnectionSocketFactory getPreferredSocketFactory(SdkHttpClientSettings settings) {
        // TODO v2 custom socket factory
        return new SdkTLSSocketFactory(getPreferredSSLContext(settings.getSecureRandom()),
                                       getHostNameVerifier(settings));
    }

    private static SSLContext getPreferredSSLContext(final SecureRandom secureRandom) {
        try {
            final SSLContext sslcontext = SSLContext.getInstance("TLS");
            // http://download.java.net/jdk9/docs/technotes/guides/security/jsse/JSSERefGuide.html
            sslcontext.init(null, null, secureRandom);
            return sslcontext;
        } catch (final NoSuchAlgorithmException | KeyManagementException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        }
    }

    private SocketConfig buildSocketConfig(SdkHttpClientSettings settings) {
        return SocketConfig.custom()
                .setSoKeepAlive(settings.useTcpKeepAlive())
                .setSoTimeout(settings.getSocketTimeout())
                .setTcpNoDelay(true)
                .build();
    }

    private ConnectionConfig buildConnectionConfig(SdkHttpClientSettings settings) {

        int socketBufferSize = Math.max(settings.getSocketBufferSize()[0],
                                        settings.getSocketBufferSize()[1]);

        return socketBufferSize <= 0
                ? null
                : ConnectionConfig.custom()
                .setBufferSize(socketBufferSize)
                .build();
    }

    private HostnameVerifier getHostNameVerifier(SdkHttpClientSettings options) {
        // TODO Need to find a better way to handle these deprecations.
        return options.useBrowserCompatibleHostNameVerifier()
                ? SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER
                : SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER;
    }

    private Registry<ConnectionSocketFactory> createSocketFactoryRegistry(ConnectionSocketFactory sslSocketFactory) {
        // TODO v2 disable cert checking
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();
    }

}
