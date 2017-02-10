/*
 * Copyright (c) 2017. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package utils.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.internal.http.apache.conn.SdkTLSSocketFactory;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;
import software.amazon.awssdk.internal.net.SdkSSLContext;

public class RecordingSocketFactory implements ConnectionSocketFactory {

    private final ConnectionSocketFactory delegate;

    private final List<ConnectSocketRequest> connectSocketRequests = new ArrayList<ConnectSocketRequest>();
    private final List<HttpContext> createSocketRequests = new ArrayList<HttpContext>();

    public RecordingSocketFactory() {
        HttpClientSettings settings = HttpClientSettings.adapt(new ClientConfiguration(), false);
        this.delegate = new SdkTLSSocketFactory(
                SdkSSLContext.getPreferredSSLContext(settings.getSecureRandom()),
                settings.useBrowserCompatibleHostNameVerifier()
                        ? SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER
                        : SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER);
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        createSocketRequests.add(context);
        return delegate.createSocket(context);
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
        connectSocketRequests.add(new ConnectSocketRequest(connectTimeout, sock, host, remoteAddress, localAddress, context));
        return delegate.connectSocket(connectTimeout, sock, host, remoteAddress, localAddress, context);
    }

    public void clear(){
        connectSocketRequests.clear();
        createSocketRequests.clear();
    }

    public List<ConnectSocketRequest> getConnectSocketRequests() {
        return connectSocketRequests;
    }

    public List<HttpContext> getCreateSocketRequests() {
        return createSocketRequests;
    }


    public class ConnectSocketRequest {
        public final int connectTimeout;
        public final Socket sock;
        public final HttpHost host;
        public final InetSocketAddress remoteAddress;
        public final InetSocketAddress localAddress;
        public final HttpContext context;

        ConnectSocketRequest(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) {
            this.connectTimeout = connectTimeout;
            this.sock = sock;
            this.host = host;
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
            this.context = context;
        }
    }
}
