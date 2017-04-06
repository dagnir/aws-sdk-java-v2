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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.http.SdkHttpMethod;

public final class NettyUtils {

    private NettyUtils() {
    }

    public static HttpMethod toNettyHttpMethod(SdkHttpMethod method) {
        switch (method) {
            case GET:
                return HttpMethod.GET;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case DELETE:
                return HttpMethod.DELETE;
            case PATCH:
                return HttpMethod.PATCH;
            case HEAD:
                return HttpMethod.HEAD;
            case OPTIONS:
                return HttpMethod.OPTIONS;
            default:
                throw new IllegalArgumentException("Unknown http method: " + method);
        }
    }

    public static Map<String, List<String>> fromNettyHeaders(HttpHeaders headers) {
        return headers.entries()
                      .stream()
                      .collect(groupingBy(Map.Entry::getKey,
                                          mapping(Map.Entry::getValue,
                                                  Collectors.toList())));
    }
}
