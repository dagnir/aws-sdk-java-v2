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

package software.amazon.awssdk.http.nio.netty;

import static io.netty.handler.ssl.SslContext.defaultClientProvider;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import software.amazon.awssdk.http.SdkHttpClientSettings;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.ChannelPipelineInitializer;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.RequestContextPool;
import software.amazon.awssdk.http.nio.netty.internal.ResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.RunnableRequest;


public final class NettyNioAsyncHttpClient implements SdkAsyncHttpClient {

    private final EventLoopGroup group = new NioEventLoopGroup();
    private final RequestAdapter requestAdapter = new RequestAdapter();
    private final RequestContextPool requestContexts = new RequestContextPool();
    private final ChannelPoolMap<URI, ChannelPool> pools;

    public NettyNioAsyncHttpClient(SdkHttpClientSettings settings) {
        this.pools = createChannelPoolMap(settings);
    }

    private AbstractChannelPoolMap<URI, ChannelPool> createChannelPoolMap(SdkHttpClientSettings settings) {
        return new AbstractChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                final Bootstrap bootstrap =
                    new Bootstrap().group(group).channel(NioSocketChannel.class).remoteAddress(addressFor(key));
                ChannelHandler handler = new ResponseHandler(requestContexts);
                SslContext sslContext = sslContext(key.getScheme(), settings.trustAllCertificates());
                return new FixedChannelPool(bootstrap,
                                            new ChannelPipelineInitializer(sslContext, requestContexts, handler),
                                            settings.getMaxConnections());
            }
        };
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequestProvider requestProvider,
                                            SdkHttpResponseHandler handler) {
        final SdkHttpRequest sdkRequest = requestProvider.request();
        final RequestContext context = new RequestContext(pools.get(sdkRequest.getEndpoint()),
                                                          requestProvider,
                                                          requestAdapter.adapt(sdkRequest),
                                                          handler);
        return new RunnableRequest(context, requestContexts);
    }

    @Override
    public void close() throws Exception {
        group.shutdownGracefully().await();
    }

    private static InetSocketAddress addressFor(URI uri) {
        int port = uri.getPort() != -1 ? uri.getPort() : uri.getScheme().equalsIgnoreCase("https") ? 443 : 80;
        return new InetSocketAddress(uri.getHost(), port);
    }

    private static SslContext sslContext(String scheme, boolean trustAllCertificates) {
        if (scheme.equalsIgnoreCase("https")) {
            SslContextBuilder builder = SslContextBuilder.forClient().sslProvider(defaultClientProvider());
            if (trustAllCertificates) {
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }
            return invokeSafely(builder::build);
        }
        return null;
    }
}
