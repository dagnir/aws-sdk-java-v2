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

package software.amazon.awssdk.http.nio.netty.h2;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.CUMULATED_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;

public class SdkHttp2FrameListener implements Http2FrameListener {

    private static final Logger log = LoggerFactory.getLogger(SdkHttp2FrameListener.class);

    private final ByteToMessageDecoder.Cumulator cumulator = ByteToMessageDecoder.MERGE_CUMULATOR;

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
        throws Http2Exception {
        ctx.channel().attr(CUMULATED_KEY).compareAndSet(null, Unpooled.EMPTY_BUFFER);

        try {
            RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
            SdkHttpResponseHandler<?> responseHandler = requestContext.handler();
            // Cumulator will release the reference when it's released
            data.retain();

            ByteBuf cumulated = cumulator.cumulate(ctx.alloc(), ctx.channel().attr(CUMULATED_KEY).get(), data);
            ctx.channel().attr(CUMULATED_KEY).set(cumulated);

            // TODO backpressure
            if (endOfStream) {
                responseHandler.onStream(new Publisher<ByteBuffer>() {
                    @Override
                    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                        subscriber.onSubscribe(new Subscription() {
                            @Override
                            public void request(long l) {
                                try {
                                    subscriber.onNext(copyToByteBuffer(cumulated));
                                    subscriber.onComplete();
                                    responseHandler.complete();
                                } finally {
                                    runAndLogError("Could not release channel",
                                                   () -> requestContext.channelPool().release(ctx.channel()));
                                    ctx.channel().attr(CUMULATED_KEY).set(null);
                                    cumulated.release();
                                }
                            }

                            @Override
                            public void cancel() {
                                // TODO handle
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            log.error("Unable to read data frame", e);
        }
        // TODO we should return the number of bytes immediately processed. Any async processing of bytes should be notified
        // by the flow controller
        return data.nioBuffer().remaining();
    }

    /**
     * Runs a given {@link UnsafeRunnable} and logs an error without throwing.
     *
     * @param errorMsg Message to log with exception thrown.
     * @param runnable Action to perform.
     */
    private static void runAndLogError(String errorMsg, UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    private static ByteBuffer copyToByteBuffer(ByteBuf byteBuf) {
        ByteBuffer bb = ByteBuffer.allocate(byteBuf.readableBytes());
        byteBuf.getBytes(byteBuf.readerIndex(), bb);
        bb.flip();
        return bb;
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
                              boolean endOfStream) throws Http2Exception {
        deliverHeaders(ctx, streamId, headers);
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight,
                              boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
        deliverHeaders(ctx, streamId, headers);
    }

    public void deliverHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers) throws Http2Exception {
        HttpResponse response = HttpConversionUtil.toHttpResponse(streamId, headers, true);
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        SdkHttpResponseHandler<?> responseHandler = requestContext.handler();
        // TODO duplication
        responseHandler.headersReceived(SdkHttpFullResponse.builder()
                                                           .headers(fromNettyHeaders(response.headers()))
                                                           .statusCode(response.status().code())
                                                           .statusText(response.status().reasonPhrase())
                                                           .build());
    }

    private static Map<String, List<String>> fromNettyHeaders(HttpHeaders headers) {
        return headers.entries().stream()
                      .collect(groupingBy(Map.Entry::getKey,
                                          mapping(Map.Entry::getValue, Collectors.toList())));
    }

    @Override
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight,
                               boolean exclusive) throws Http2Exception {

        // TODO do we care about priority?
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        requestContext.handler().exceptionOccurred(new Http2ResetException(errorCode));

        runAndLogError("Could not release channel",
                       () -> requestContext.channelPool().release(ctx.channel()));
        ByteBuf oldValue = ctx.channel().attr(CUMULATED_KEY).getAndSet(null);
        if (oldValue != null) {
            oldValue.release();
        }
    }

    @Override
    public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {

    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
        // Store max concurrent streams for multiplexing
    }

    @Override
    public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
        // Do we need to do anything with this? Send a ping ack or does Netty handle that?
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
    }

    @Override
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers,
                                  int padding) throws Http2Exception {
        // Push promise out of scope
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws
                                                                                                             Http2Exception {
        // TODO need to stop accepting new streams but allow current streams to complete. Connection should be closed
        // after all streams complete. Goaway will send number of highest stream eligible for processing so we
        // should kill any streams that happened to be created after that.
    }

    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {
        /**
         * TODO Do we need to handle this or will Netty return false for {@link Channel#isWritable()} and the reactive streams
         * handler can just stop pushing data?
         */
    }

    @Override
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload)
        throws Http2Exception {
    }

    public static class Http2ResetException extends IOException {

        public Http2ResetException(long errorCode) {
            super(String.format("Connection reset. Error - %s(%d)", Http2Error.valueOf(errorCode).name(), errorCode));
        }

    }
}
