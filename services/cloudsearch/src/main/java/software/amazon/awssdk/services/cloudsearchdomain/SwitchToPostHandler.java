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

package software.amazon.awssdk.services.cloudsearchdomain;

import java.io.ByteArrayInputStream;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.HttpMethodName;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchRequest;
import software.amazon.awssdk.util.SdkHttpUtils;

/**
 * Ensures that all SearchRequests use <code>POST</code> instead of <code>GET</code>.
 */
public class SwitchToPostHandler extends RequestHandler2 {
    @Override
    public void beforeRequest(Request<?> request) {
        if (request.getOriginalRequest() instanceof SearchRequest && request.getHttpMethod() == HttpMethodName.GET) {
            request.setHttpMethod(HttpMethodName.POST);
            final byte[] content = SdkHttpUtils.encodeParameters(request).getBytes();
            request.setContent(new ByteArrayInputStream(content));
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.addHeader("Content-Length", Integer.toString(content.length));
            request.getParameters().clear();
        }
    }
}
