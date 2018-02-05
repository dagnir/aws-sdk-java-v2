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

import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.nio.netty.h2.SdkHttp2FrameVisitor;

/**
 * Keys for attributes attached via {@link io.netty.channel.Channel#attr(AttributeKey)}.
 */
public class ChannelAttributeKeys {

    public static final AttributeKey<Boolean> FIRST_BYTE_RECEIVED = AttributeKey.newInstance("firstByteReceived");

    public static final AttributeKey<Long> REQUEST_START = AttributeKey.newInstance("requestStart");

    public static final AttributeKey<Long> REQUEST_FINISH = AttributeKey.newInstance("requestFinish");

    /**
     * Attribute key for {@link RequestContext}.
     */
    // TODO public
    public static final AttributeKey<RequestContext> REQUEST_CONTEXT_KEY = AttributeKey.newInstance("requestContext");

    public static final AttributeKey<Subscriber<? super ByteBuffer>> SUBSCRIBER_KEY = AttributeKey.newInstance("subscriber");

    // TODO public
    public static final AttributeKey<Boolean> RESPONSE_COMPLETE_KEY = AttributeKey.newInstance("responseComplete");

    /**
     * Future that when a protocol (http/1.1 or h2) has been selected.
     */
    public static final AttributeKey<CompletableFuture<String>> PROTOCOL_FUTURE = AttributeKey.newInstance("protocolFuture");

    public static final AttributeKey<AtomicInteger> AVAILABLE_STREAMS = AttributeKey.newInstance("availableStreams");

    public static final AttributeKey<SdkHttp2FrameVisitor> FRAME_VISITOR = AttributeKey.newInstance("frameVisitor");

    private ChannelAttributeKeys() {
    }
}
