package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;

public class ChaosHandler extends ChannelInboundHandlerAdapter {
    private static int c = 0;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ++c;
        if (c > 5) {
            c = 0;
            ctx.fireExceptionCaught(new IOException("something wrong"));
        } else {
            System.out.println("MSG");
            ctx.fireChannelRead(msg);
        }
    }
}
