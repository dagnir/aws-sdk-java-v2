package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.logging.LogLevel;
import java.util.function.Supplier;
import software.amazon.awssdk.utils.Logger;

public class DongieFrameLogger extends Http2FrameLogger {
    private static final Logger LOG = Logger.loggerFor(DongieFrameLogger.class);


    private final Channel connection;
    private Supplier<Http2Connection> h2ConnectionSupplier;

    public DongieFrameLogger(Channel connection, LogLevel level) {
        super(level);
        this.connection = connection;
    }

    public void logData(Direction direction, ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
                        boolean endStream) {
        super.logData(direction, ctx, streamId, data, padding, endStream);

        if (getStream(streamId) == null) {
            logUnknownFrame(streamId, "DATA");
        }
    }

    public void logHeaders(Direction direction, ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                           int padding, boolean endStream) {
        super.logHeaders(direction, ctx, streamId, headers, padding, endStream);

        if (getStream(streamId) == null) {
            logUnknownFrame(streamId, "HEADERS");
        }
    }

    public void logHeaders(Direction direction, ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                           int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) {
        super.logHeaders(direction, ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);

        if (getStream(streamId) == null) {
            logUnknownFrame(streamId, "HEADERS");
        }
    }

    public void h2ConnectionSupplier(Supplier<Http2Connection> h2ConnectionSupplier) {
        this.h2ConnectionSupplier = h2ConnectionSupplier;
    }

    private Http2Stream getStream(int streamId) {
        return h2ConnectionSupplier.get().stream(streamId);
    }

    private void logUnknownFrame(int streamId, String type) {
        LOG.warn(() -> String.format("Received unknown %s frame on stream %d on connection %s", type, streamId, connection));
    }

}
