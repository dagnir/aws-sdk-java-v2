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

import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.pipeline.RequestPipeline;

/**
 * Invoke the {@link RequestHandler2#beforeUnmarshalling(Request, HttpResponse)} callback to allow for pre-processing on the
 * {@link HttpResponse} before it is handed off to the unmarshaller.
 */
public class BeforeUnmarshallingCallbackStage implements RequestPipeline<HttpResponse, HttpResponse> {

    @Override
    public HttpResponse execute(HttpResponse httpResponse, RequestExecutionContext context) throws Exception {
        // TODO we should consider invoking beforeUnmarshalling regardless of success or error.
        if (!httpResponse.isSuccessful()) {
            return httpResponse;
        }
        HttpResponse toReturn = httpResponse;
        for (RequestHandler2 requestHandler : context.requestHandler2s()) {
            toReturn = requestHandler.beforeUnmarshalling(context.request(), toReturn);
        }
        return toReturn;
    }
}
