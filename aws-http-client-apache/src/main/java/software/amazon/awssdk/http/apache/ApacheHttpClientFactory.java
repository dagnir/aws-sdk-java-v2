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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.SdkHttpClientSettings;
import software.amazon.awssdk.http.apache.internal.SdkHttpRequestExecutor;
import software.amazon.awssdk.http.apache.internal.SdkProxyRoutePlanner;
import software.amazon.awssdk.http.apache.internal.conn.ClientConnectionManagerFactory;
import software.amazon.awssdk.http.apache.internal.conn.IdleConnectionReaper;
import software.amazon.awssdk.http.apache.internal.conn.SdkConnectionKeepAliveStrategy;
import software.amazon.awssdk.http.apache.internal.impl.ApacheConnectionManagerFactory;
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkClient;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;

public class ApacheHttpClientFactory implements SdkHttpClientFactory {

    private static final Log LOG = LogFactory.getLog(ApacheHttpClientFactory.class);
    private final ApacheConnectionManagerFactory cmFactory = new ApacheConnectionManagerFactory();

    @Override
    public SdkHttpClient create(SdkHttpClientSettings settings) {
        return new ApacheHttpClient(createClient(settings), settings);
    }

    private ConnectionManagerAwareHttpClient createClient(SdkHttpClientSettings settings) {
        final HttpClientBuilder builder = HttpClients.custom();
        // Note that it is important we register the original connection manager with the
        // IdleConnectionReaper as it's required for the successful deregistration of managers
        // from the reaper. See https://github.com/aws/aws-sdk-java/issues/722.
        final HttpClientConnectionManager cm = cmFactory.create(settings);

        builder.setRequestExecutor(new SdkHttpRequestExecutor())
                // SDK handles decompression
                .disableContentCompression()
                .setKeepAliveStrategy(buildKeepAliveStrategy(settings))
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .setConnectionManager(ClientConnectionManagerFactory.wrap(cm));

        addProxyConfig(builder, settings);

        final ConnectionManagerAwareHttpClient httpClient = new ApacheSdkClient(
                builder.build(), cm);

        if (settings.useReaper()) {
            IdleConnectionReaper.registerConnectionManager(cm, settings.getMaxIdleConnectionTime());
        }

        return httpClient;
    }

    private void addProxyConfig(HttpClientBuilder builder,
                                SdkHttpClientSettings settings) {
        if (isProxyEnabled(settings)) {

            LOG.info("Configuring Proxy. Proxy Host: " + settings.getProxyHost() + " " +
                     "Proxy Port: " + settings.getProxyPort());

            builder.setRoutePlanner(new SdkProxyRoutePlanner(
                    settings.getProxyHost(), settings.getProxyPort(), settings.getNonProxyHosts()));

            if (isAuthenticatedProxy(settings)) {
                builder.setDefaultCredentialsProvider(ApacheUtils
                                                              .newProxyCredentialsProvider(settings));
            }
        }
    }

    private ConnectionKeepAliveStrategy buildKeepAliveStrategy(SdkHttpClientSettings settings) {
        return settings.getMaxIdleConnectionTime() > 0
                ? new SdkConnectionKeepAliveStrategy(settings.getMaxIdleConnectionTime())
                : null;
    }

    private boolean isAuthenticatedProxy(SdkHttpClientSettings settings) {
        return settings.getProxyUsername() != null
               && settings.getProxyPassword() != null;
    }

    private boolean isProxyEnabled(SdkHttpClientSettings settings) {
        return settings.getProxyHost() != null && settings.getProxyPort() > 0;
    }
}
