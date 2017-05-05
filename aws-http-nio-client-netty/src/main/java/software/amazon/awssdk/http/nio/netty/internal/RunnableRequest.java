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

import static software.amazon.awssdk.http.nio.netty.internal.RequestContext.REQUEST_CONTEXT_KEY;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;
import java.net.URI;
import java.util.function.Supplier;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.utils.Logger;

public final class RunnableRequest implements AbortableRunnable {

    private static final Logger log = Logger.loggerFor(RunnableRequest.class);
    private final RequestContext context;
    private volatile Channel channel;

    public RunnableRequest(RequestContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        context.channelPool().acquire().addListener((Future<Channel> channelFuture) -> {
            if (channelFuture.isSuccess()) {
                channel = channelFuture.getNow();
                channel.attr(REQUEST_CONTEXT_KEY).set(context);
                makeRequest(context.nettyRequest());
            } else {
                handleFailure(() -> "Failed to create connection to " + endpoint(), channelFuture.cause());
            }
        });
    }

    @Override
    public void abort() {
        if (channel != null) {
            channel.disconnect().addListener(ignored -> context.channelPool().release(channel));
        }
    }

    private void makeRequest(HttpRequest request) {
        log.debug(() -> "Writing request: " + request);
        channel.write(request).addListener(wireCall -> {
            if (!wireCall.isSuccess()) {
                handleFailure(() -> "Failed to make request to " + endpoint(), wireCall.cause());
            }
        });
        context.sdkRequestProvider().readyForData(new SdkNettyRequestChannel(channel,
                                                                             context.sdkRequestProvider()::exceptionOccurred,
                                                                             this::abort));
    }

    private URI endpoint() {
        return context.sdkRequestProvider().request().getEndpoint();
    }

    private void handleFailure(Supplier<String> msg, Throwable cause) {
        log.error(msg, cause);
        context.sdkRequestProvider().exceptionOccurred(cause);
        if (channel != null) {
            context.channelPool().release(channel);
        }
    }
}
