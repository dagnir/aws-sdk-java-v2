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

package software.amazon.awssdk.http.pipeline.stages;

import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.pipeline.MutableRequestToRequestPipeline;

/**
 * If there is an {@link software.amazon.awssdk.async.AsyncRequestProvider} (i.e. for a streaming input operation) then
 * query it for the content length (unless it's already set).
 */
public class SetContentLengthStage implements MutableRequestToRequestPipeline {

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        if (shouldSetContentLength(request, context)) {
            return request.header("Content-Length", String.valueOf(context.requestProvider().contentLength()));
        }
        return request;
    }

    private boolean shouldSetContentLength(SdkHttpFullRequest.Builder request, RequestExecutionContext context) {
        return context.requestProvider() != null
               && !request.getFirstHeaderValue("Content-Length").isPresent()
               // Can cause issues with signing if content length is present for these method
               && request.getHttpMethod() != SdkHttpMethod.GET
               && request.getHttpMethod() != SdkHttpMethod.HEAD;
    }
}
