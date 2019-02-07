/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTION_ID_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Closes the channel if the execution future has been cancelled.
 */
@SdkInternalApi
public class FutureCancelHandler extends SimpleChannelInboundHandler {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object o) throws Exception {
        ReferenceCountUtil.retain(o);
        ctx.fireChannelRead(o);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        if (cancelled(ctx, e)) {
            RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
            requestContext.handler().onError(e);
            ctx.fireExceptionCaught(new IOException("Request cancelled"));
            ctx.close();
            requestContext.channelPool().release(ctx.channel());
        } else {
            ctx.fireExceptionCaught(e);
        }
    }

    private boolean cancelled(ChannelHandlerContext ctx, Throwable t) {
        if (!(t instanceof FutureCancelledException)) {
            return false;
        }

        FutureCancelledException e = (FutureCancelledException) t;

        return e.getExecutionId() == ctx.channel().attr(EXECUTION_ID_KEY).get();
    }
}
