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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic representation of an HTTP response.
 *
 * TODO should we allow HTTP impl authors to implement this or should they only use the builder?
 */
public interface SdkHttpResponse {

    /**
     * Returns the HTTP headers returned with this response.
     *
     * @return The set of HTTP headers returned with this HTTP response.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Looks up a header by name and returns its values. Does case insensitive comparison.
     *
     * @param headerName Name of header to get value for.
     * @return The header value of the given header. Null if header is not present.
     */
    List<String> getHeaderValues(String headerName);

    /**
     * Returns the input stream containing the response content.
     *
     * @return The input stream containing the response content.
     */
    InputStream getContent();

    /**
     * Returns the HTTP status text associated with this response.
     *
     * @return The HTTP status text associated with this response.
     */
    String getStatusText();

    /**
     * Returns the HTTP status code (ex: 200, 404, etc) associated with this
     * response.
     *
     * @return The HTTP status code associated with this response.
     */
    int getStatusCode();

    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpResponse}.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link DefaultSdkHttpResponse}.
     */
    final class Builder {

        String statusText;
        int statusCode;
        InputStream content;
        Map<String, List<String>> headers = new HashMap<>();

        private Builder() {
        }

        public Builder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder content(InputStream content) {
            this.content = content;
            return this;
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder header(String headerName, List<String> headerValues) {
            this.headers.put(headerName, headerValues);
            return this;
        }

        /**
         * @return An immutable {@link DefaultSdkHttpResponse} object.
         */
        public SdkHttpResponse build() {
            return new DefaultSdkHttpResponse(this);
        }
    }
}
