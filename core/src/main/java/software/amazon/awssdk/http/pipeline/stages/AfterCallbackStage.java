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

import static software.amazon.awssdk.event.SDKProgressPublisher.publishProgress;

import java.util.List;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.pipeline.RequestToResponsePipeline;

/**
 * Calls the {@link RequestHandler2#afterResponse(Request, Response)} or {@link RequestHandler2#afterError(Request, Response,
 * Exception)} callbacks, depending on whether the request succeeded or failed.
 */
public class AfterCallbackStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private final RequestPipeline<Request<?>, Response<OutputT>> wrapped;

    public AfterCallbackStage(RequestPipeline<Request<?>, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(Request<?> request, RequestExecutionContext context) throws Exception {
        Response<OutputT> response = null;
        try {
            response = wrapped.execute(request, context);
            afterResponse(context.requestHandler2s(), request, response);
            return response;
        } catch (Exception e) {
            publishProgress(context.requestConfig().getProgressListener(), ProgressEventType.CLIENT_REQUEST_FAILED_EVENT);
            afterError(context.requestHandler2s(), request, response, e);
            throw e;
        }
    }

    private <T> void afterResponse(List<RequestHandler2> requestHandler2s,
                                   Request<?> request,
                                   Response<T> response) throws InterruptedException {
        for (RequestHandler2 handler2 : requestHandler2s) {
            handler2.afterResponse(request, response);
            AmazonHttpClient.checkInterrupted(response);
        }
    }

    private void afterError(List<RequestHandler2> requestHandler2s,
                            Request<?> request,
                            Response<?> response,
                            Exception e) throws InterruptedException {
        for (RequestHandler2 handler2 : requestHandler2s) {
            handler2.afterError(request, response, e);
            AmazonHttpClient.checkInterrupted(response);
        }
    }
}
