/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.MAX_CONCURRENT_STREAMS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.promiseNotifyingBiConsumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.http.nio.netty.internal.utils.BetterFixedChannelPool;

/**
 * Channel pool that establishes an initial connection to determine protocol. Delegates
 * to appropriate channel pool implementation depending on the protocol. This assumes that
 * all connections will be negotiated with the same protocol.
 */
public class HttpOrHttp2ChannelPool implements ChannelPool {

    private final ChannelPool simpleChannelPool;
    private final int maxConcurrency;
    private final EventLoop eventLoop;
    private final NettyConfiguration configuration;

    private Promise<ChannelPool> protocolImplPromise;
    private ChannelPool protocolImpl;

    public HttpOrHttp2ChannelPool(Bootstrap bootstrap,
                                  ChannelPoolHandler handler,
                                  int maxConcurrency,
                                  NettyConfiguration configuration) {
        this.simpleChannelPool = new SimpleChannelPool(bootstrap, handler);
        this.maxConcurrency = maxConcurrency;
        this.eventLoop = bootstrap.config().group().next();
        this.configuration = configuration;
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(new DefaultPromise<>(eventLoop));
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        doInEventLoop(eventLoop, () -> acquire0(promise), promise);
        return promise;
    }

    private void acquire0(Promise<Channel> promise) {
        if (protocolImpl != null) {
            protocolImpl.acquire(promise);
            return;
        }
        if (protocolImplPromise == null) {
            initializeProtocol();
        }
        protocolImplPromise.addListener((GenericFutureListener<Future<ChannelPool>>) future -> {
            if (future.isSuccess()) {
                future.getNow().acquire(promise);
            } else {
                // Couldn't negotiate protocol, fail this acquire.
                promise.setFailure(future.cause());
            }
        });
    }

    /**
     * Establishes a single connection to initialize the protocol and choose the appropriate {@link ChannelPool} implementation
     * for {@link #protocolImpl}.
     */
    private void initializeProtocol() {
        protocolImplPromise = new DefaultPromise<>(eventLoop);
        simpleChannelPool.acquire()
                         .addListener((GenericFutureListener<Future<Channel>>) future -> {
                             if (future.isSuccess()) {
                                 Channel newChannel = future.getNow();
                                 newChannel.attr(PROTOCOL_FUTURE).get()
                                           .whenComplete(promiseNotifyingBiConsumer(s -> configureProtocol(newChannel, s),
                                                                                    protocolImplPromise));
                             } else {
                                 protocolImplPromise.setFailure(future.cause());
                                 // Next acquire will attempt to renegotiate protocol
                                 protocolImplPromise = null;
                             }
                         });
    }

    private ChannelPool configureProtocol(Channel newChannel, Protocol protocol) {
        if (Protocol.HTTP1_1 == protocol) {
            // For HTTP/1.1 we use a traditional channel pool without multiplexing
            protocolImpl = BetterFixedChannelPool.builder()
                                                 .channelPool(simpleChannelPool)
                                                 .executor(eventLoop)
                                                 .acquireTimeoutAction(BetterFixedChannelPool.AcquireTimeoutAction.FAIL)
                                                 .acquireTimeoutMillis(configuration.connectionAcquireTimeoutMillis())
                                                 .maxConnections(maxConcurrency)
                                                 .maxPendingAcquires(configuration.maxPendingConnectionAcquires())
                                                 .build();
        } else {
            ChannelPool h2Pool = new Http2MultiplexedChannelPool(
                simpleChannelPool, eventLoop, newChannel.attr(MAX_CONCURRENT_STREAMS).get());
            protocolImpl = BetterFixedChannelPool.builder()
                                                 .channelPool(h2Pool)
                                                 .executor(eventLoop)
                                                 .acquireTimeoutAction(BetterFixedChannelPool.AcquireTimeoutAction.FAIL)
                                                 .acquireTimeoutMillis(configuration.connectionAcquireTimeoutMillis())
                                                 .maxConnections(maxConcurrency)
                                                 .maxPendingAcquires(configuration.maxPendingConnectionAcquires())
                                                 .build();
        }
        // Give the channel back so it can be acquired again by protocolImpl
        simpleChannelPool.release(newChannel);
        return protocolImpl;
    }

    @Override
    public Future<Void> release(Channel channel) {
        return release(channel, eventLoop.newPromise());
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        doInEventLoop(eventLoop,
            () -> release0(channel, promise),
                      promise);
        return promise;
    }

    private void release0(Channel channel, Promise<Void> promise) {
        if (protocolImpl == null) {
            // If protocolImpl is null that means the first connection failed to establish. Release it back to the
            // underlying connection pool.
            simpleChannelPool.release(channel, promise);
        } else {
            protocolImpl.release(channel, promise);
        }
    }

    @Override
    public void close() {
        doInEventLoop(eventLoop, protocolImpl::close);
    }

}
