/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.opensdk;

import java.util.Optional;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpMetadata;

/**
 * Metadata from the HTTP response. Primarily used for detailed logging or debugging.
 */
@Immutable
public class SdkResponseMetadata {

    public static final String HEADER_REQUEST_ID = "x-amzn-RequestId";

    private final SdkHttpMetadata httpMetadata;

    @SdkProtectedApi
    public SdkResponseMetadata(SdkHttpMetadata httpMetadata) {
        this.httpMetadata = httpMetadata;
    }

    /**
     * @return x-amzn-RequestId generated by the API Gateway frontend. Uniquely identifies a request
     * and can be used for troubleshooting server side issues.
     */
    public String requestId() {
        return httpMetadata.getHttpHeaders().get(HEADER_REQUEST_ID);
    }

    /**
     * Get a specific header from the HTTP response.
     *
     * @param headerName Header to retrieve.
     * @return Optional of header value.
     */
    public Optional<String> header(String headerName) {
        return Optional.ofNullable(httpMetadata.getHttpHeaders().get(headerName));
    }

    /**
     * @return HTTP status code of response.
     */
    public int httpStatusCode() {
        return httpMetadata.getHttpStatusCode();
    }
}
