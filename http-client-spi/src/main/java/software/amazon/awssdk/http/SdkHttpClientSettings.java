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

package software.amazon.awssdk.http;

import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * Settings to configure HTTP client implementation with.
 */
public interface SdkHttpClientSettings {

    boolean useBrowserCompatibleHostNameVerifier();

    boolean calculateCrc32FromCompressedData();

    int getMaxConnections();

    InetAddress getLocalAddress();

    String getProxyHost();

    int getProxyPort();

    String getProxyUsername();

    String getProxyPassword();

    String getNonProxyHosts();

    boolean useReaper();

    boolean useGzip();

    int getSocketTimeout();

    int[] getSocketBufferSize();

    boolean useTcpKeepAlive();

    SecureRandom getSecureRandom();

    int getConnectionTimeout();

    int getConnectionPoolRequestTimeout();

    long getConnectionPoolTtl();

    long getMaxIdleConnectionTime();

    String getProxyWorkstation();

    String getProxyDomain();

    boolean isPreemptiveBasicProxyAuth();

    boolean isUseExpectContinue();

}
