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

import static java.util.stream.Collectors.joining;
import static software.amazon.awssdk.utils.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;

public final class HttpUtils {
    private HttpUtils() {
    }

    public static String transformParameters(Map<String, List<String>> parameters) {
        return parameters.entrySet()
                         .stream()
                         .flatMap(e -> e.getValue().stream().map(v -> e.getKey() + (isNotBlank(v) ? "=" + v : "")))
                         .collect(joining("&"));
    }
}
