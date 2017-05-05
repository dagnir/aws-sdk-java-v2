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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext.RequestContextSaver;
import software.amazon.awssdk.http.nio.netty.internal.utils.LoggingHandler;
import software.amazon.awssdk.utils.Logger;

public class ChannelPipelineInitializer implements ChannelPoolHandler {
    private static final Logger log = Logger.loggerFor(NettyNioAsyncHttpClient.class);

    private final SslContext sslContext;
    private final RequestContextSaver<ChannelId> requestContexts;
    private final ChannelHandler[] handlers;

    public ChannelPipelineInitializer(SslContext sslContext,
                                      RequestContextSaver<ChannelId> requestContexts,
                                      ChannelHandler...handlers) {
        this.sslContext = sslContext;
        this.requestContexts = requestContexts;
        this.handlers = handlers;
    }

    @Override
    public void channelReleased(Channel ch) throws Exception {
        requestContexts.delete(ch.id());
    }

    @Override
    public void channelAcquired(Channel ch) throws Exception {

    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        if (sslContext != null) {
            SslHandler handler = sslContext.newHandler(ch.alloc());
            p.addLast(handler);
            handler.handshakeFuture().addListener(future -> {
                if (!future.isSuccess()) {
                    log.error(() -> "SSL handshake failed.", future.cause());
                }
            });
        }

        p.addLast(new HttpClientCodec());
        if (log.underlyingLogger().isDebugEnabled()) {
            p.addLast(new LoggingHandler(log::debug));
        }
        p.addLast(handlers);
    }
}
