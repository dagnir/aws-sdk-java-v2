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

package software.amazon.awssdk.http;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.Request;

/**
 * Adapts a {@link Request} to the new {@link SdkHttpFullRequest} interface.
 *
 * TODO this should eventually be removed and SdkHttpFullRequest should completely replace Request
 */
public class SdkHttpFullRequestAdapter implements SdkHttpFullRequest {

    private final Request<?> request;
    private final Map<String, List<String>> headers;

    public SdkHttpFullRequestAdapter(Request<?> request) {
        this.request = request;
        this.headers = request.getHeaders().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> singletonList(e.getValue())));
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return this.headers;
    }

    @Override
    public Optional<String> getFirstHeaderValue(String header) {
        return Optional.ofNullable(headers.get(header))
                .filter(h -> !h.isEmpty())
                .map(h -> h.get(0));
    }

    @Override
    public String getResourcePath() {
        return request.getResourcePath();
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return request.getParameters();
    }

    @Override
    public URI getEndpoint() {
        return request.getEndpoint();
    }

    @Override
    public SdkHttpMethod getHttpMethod() {
        return SdkHttpMethod.fromValue(request.getHttpMethod().name());
    }

    @Override
    public InputStream getContent() {
        return request.getContent();
    }
}
