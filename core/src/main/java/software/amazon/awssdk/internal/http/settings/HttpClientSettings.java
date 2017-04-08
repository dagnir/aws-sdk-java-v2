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

package software.amazon.awssdk.internal.http.settings;

import java.net.InetAddress;
import java.security.SecureRandom;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpClientSettings;
import software.amazon.awssdk.util.ValidationUtils;

/**
 * A convienient class that expose all settings in {@link LegacyClientConfiguration} and other internal settings to the
 * underlying http client.
 */
@SdkInternalApi
public class HttpClientSettings implements SdkHttpClientSettings {

    private final LegacyClientConfiguration config;

    private final boolean useBrowserCompatibleHostNameVerifier;

    private final boolean calculateCrc32FromCompressedData;

    HttpClientSettings(final LegacyClientConfiguration config,
                       final boolean useBrowserCompatibleHostNameVerifier,
                       final boolean calculateCrc32FromCompressedData) {
        this.config = ValidationUtils.assertNotNull(config, "client configuration");
        this.useBrowserCompatibleHostNameVerifier = useBrowserCompatibleHostNameVerifier;
        this.calculateCrc32FromCompressedData = calculateCrc32FromCompressedData;
    }

    public static HttpClientSettings adapt(final LegacyClientConfiguration config,
                                           final boolean useBrowserCompatibleHostNameVerifier,
                                           final boolean calculateCrc32FromCompressedData) {
        return new HttpClientSettings(config, useBrowserCompatibleHostNameVerifier, calculateCrc32FromCompressedData);
    }

    public static HttpClientSettings adapt(final LegacyClientConfiguration config,
                                           final boolean useBrowserCompatibleHostNameVerifier) {
        return adapt(config, useBrowserCompatibleHostNameVerifier, false);
    }

    public static HttpClientSettings adapt(final LegacyClientConfiguration config) {
        return adapt(config, false);
    }

    public boolean useBrowserCompatibleHostNameVerifier() {
        return useBrowserCompatibleHostNameVerifier;
    }

    public boolean calculateCrc32FromCompressedData() {
        return calculateCrc32FromCompressedData;
    }

    public int getMaxConnections() {
        return config.getMaxConnections();
    }

    public InetAddress getLocalAddress() {
        return config.getLocalAddress();
    }

    public String getProxyHost() {
        return config.getProxyHost();
    }

    public int getProxyPort() {
        return config.getProxyPort();
    }

    public String getProxyUsername() {
        return config.getProxyUsername();
    }

    public String getProxyPassword() {
        return config.getProxyPassword();
    }

    public String getNonProxyHosts() {
        return config.getNonProxyHosts();
    }

    public boolean useReaper() {
        return config.useReaper();
    }

    public boolean useGzip() {
        return config.useGzip();
    }

    public int getSocketTimeout() {
        return config.getSocketTimeout();
    }

    public int[] getSocketBufferSize() {
        return config.getSocketBufferSizeHints();
    }

    public boolean useTcpKeepAlive() {
        return config.useTcpKeepAlive();
    }

    public SecureRandom getSecureRandom() {
        return config.getSecureRandom();
    }

    public int getConnectionTimeout() {
        return config.getConnectionTimeout();
    }

    public int getConnectionPoolRequestTimeout() {
        return config.getConnectionTimeout();
    }

    public long getConnectionPoolTtl() {
        return config.getConnectionTtl();
    }

    public long getMaxIdleConnectionTime() {
        return config.getConnectionMaxIdleMillis();
    }

    public int getValidateAfterInactivityMillis() {
        return config.getValidateAfterInactivityMillis();
    }

    public String getProxyWorkstation() {
        return config.getProxyWorkstation();
    }

    public String getProxyDomain() {
        return config.getProxyDomain();
    }

    public boolean isPreemptiveBasicProxyAuth() {
        return config.isPreemptiveBasicProxyAuth();
    }

    public boolean isUseExpectContinue() {
        return config.isUseExpectContinue();
    }

}
