package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;

import java.io.IOException;

public class IoeThrowingHandler extends SimpleChannelInboundHandler<HttpObject> {
    private final int throwAfter;
    private int seen = 0;

    public IoeThrowingHandler(int throwAfter) {
        this.throwAfter = throwAfter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject obj) throws Exception {
        if (seen++ >= throwAfter) {
            channelHandlerContext.fireExceptionCaught(new IOException("boom"));
        } else {
            if (obj instanceof HttpContent) {
                ((HttpContent) obj).retain();
            }
            channelHandlerContext.fireChannelRead(obj);
        }
    }
}
