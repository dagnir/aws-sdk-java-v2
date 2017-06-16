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

package software.amazon.awssdk.services.s3.handlers;

import java.util.Map;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class PutObjectRequestHandler extends RequestHandler2 {

    private static final String USER_HEADER_PREFIX = "x-amz-meta-";

    @Override
    public void beforeRequest(Request<?> request) {

        if (request.getOriginalRequest() instanceof PutObjectRequest) {
            PutObjectRequest putObjectRequest = (PutObjectRequest) request.getOriginalRequest();
            putObjectRequest.metadata().entrySet().forEach(e -> {
                if (e.getKey().startsWith(USER_HEADER_PREFIX)) {
                    request.addHeader(e.getKey(), e.getValue());
                } else {
                    request.addHeader(USER_HEADER_PREFIX + e.getKey(), e.getValue());
                }
            });
        }
    }

    private void addHeader(Map.Entry<String, String> entry, Request request) {
        if (entry.getKey().startsWith(USER_HEADER_PREFIX)) {
            request.addHeader(entry.getKey(), entry.getValue());
        } else {
            request.addHeader(USER_HEADER_PREFIX + entry.getKey(), entry.getValue());
        }
    }
}
