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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Internal implementation of {@link SdkHttpRequest}. Provided to HTTP implement to execute a request.
 */
@SdkInternalApi
public class DefaultAwsHttpRequest implements SdkHttpRequest {

    private final Map<String, List<String>> headers;
    private final String resourcePath;
    private final Map<String, List<String>> queryParameters;
    private final URI endpoint;
    private final SdkHttpMethod httpMethod;
    private final InputStream content;

    private DefaultAwsHttpRequest(Builder builder) {
        this.headers = builder.headers;
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
     * @return Builder instance to construct a {@link DefaultAwsHttpRequest}.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link DefaultAwsHttpRequest}.
     */
    public static final class Builder {

        Map<String, List<String>> headers = new HashMap<>();
        String resourcePath;
        Map<String, List<String>> queryParameters = new HashMap<>();
        URI endpoint;
        SdkHttpMethod httpMethod;
        InputStream content;

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
         * @return An immutable {@link DefaultAwsHttpRequest} object.
         */
        public SdkHttpRequest build() {
            return new DefaultAwsHttpRequest(this);
        }
    }

}
