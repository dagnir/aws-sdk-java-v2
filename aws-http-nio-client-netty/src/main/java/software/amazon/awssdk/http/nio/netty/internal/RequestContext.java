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

import io.netty.channel.ChannelId;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.HttpRequest;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

public final class RequestContext {
    private final ChannelPool channelPool;
    private final SdkHttpRequestProvider requestProvider;
    private final HttpRequest nettyRequest;
    private final SdkHttpResponseHandler handler;

    public RequestContext(ChannelPool channelPool,
                          SdkHttpRequestProvider requestProvider,
                          HttpRequest nettyRequest,
                          SdkHttpResponseHandler handler) {
        this.channelPool = channelPool;
        this.requestProvider = requestProvider;
        this.nettyRequest = nettyRequest;
        this.handler = handler;
    }

    public SdkHttpResponseHandler handler() {
        return handler;
    }

    ChannelPool channelPool() {
        return channelPool;
    }

    SdkHttpRequestProvider sdkRequestProvider() {
        return requestProvider;
    }

    HttpRequest nettyRequest() {
        return nettyRequest;
    }
    
    @FunctionalInterface
    public interface RequestContextProvider<T> {
        RequestContext get(T key);
    }

    @FunctionalInterface
    public interface RequestContextSaver<T> {
        void save(T key, RequestContext requestContext);

        void delete(ChannelId channelId);
    }
}
