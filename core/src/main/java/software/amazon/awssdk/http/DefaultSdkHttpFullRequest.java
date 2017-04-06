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

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Internal implementation of {@link SdkHttpFullRequest}. Provided to HTTP implement to execute a request.
 */
@SdkInternalApi
public class DefaultSdkHttpFullRequest implements SdkHttpFullRequest {

    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final String resourcePath;
    private final Map<String, List<String>> queryParameters;
    private final URI endpoint;
    private final SdkHttpMethod httpMethod;
    private final InputStream content;

    private DefaultSdkHttpFullRequest(Builder builder) {
        headers.putAll(builder.headers);
        this.resourcePath = builder.resourcePath;
        this.queryParameters = builder.queryParameters;
        this.endpoint = builder.endpoint;
        this.httpMethod = builder.httpMethod;
        this.content = builder.content;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Collection<String> getValuesForHeader(String header) {
        Collection<String> values = headers.get(header);
        return values != null ? values : Collections.emptyList();
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return queryParameters;
    }

    @Override
    public URI getEndpoint() {
        return endpoint;
    }

    @Override
    public SdkHttpMethod getHttpMethod() {
        return httpMethod;
    }

    @Override
    public InputStream getContent() {
        return content;
    }

    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpFullRequest}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullRequest}.
     */
    public static final class Builder {

        private Map<String, List<String>> headers = new HashMap<>();
        private String resourcePath;
        private Map<String, List<String>> queryParameters = new HashMap<>();
        private URI endpoint;
        private SdkHttpMethod httpMethod;
        private InputStream content;

        private Builder() {
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder resourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        public Builder queryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.putAll(queryParameters);
            return this;
        }

        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder httpMethod(SdkHttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder content(InputStream content) {
            this.content = content;
            return this;
        }

        /**
         * @return An immutable {@link DefaultSdkHttpFullRequest} object.
         */
        public SdkHttpFullRequest build() {
            return new DefaultSdkHttpFullRequest(this);
        }
    }

}
