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

package software.amazon.awssdk.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.util.ValidationUtils;

/**
 * Composite {@link RequestHandler} to execute a chain of {@link RequestHandler} implementations
 * in stack order. That is if you have request handlers R1, R2, R3 the order of execution is as
 * follows
 *
 * <pre>
 *
 * {@code
 * R1.beforeMarshalling
 * R2.beforeMarshalling
 * R3.beforeMarshalling
 *
 * R1.beforeRequest
 * R2.beforeRequest
 * R3.beforeRequest
 *
 * R3.beforeUnmarshalling
 * R2.beforeUnmarshalling
 * R1.beforeUnmarshalling
 *
 * R3.after(Response|Error)
 * R2.after(Response|Error)
 * R1.after(Response|Error)
 * }
 * </pre>
 */
@ThreadSafe
public class StackedRequestHandler implements IRequestHandler2 {

    private final List<RequestHandler> inOrderRequestHandlers;
    private final List<RequestHandler> reverseOrderRequestHandlers;

    public StackedRequestHandler(RequestHandler... requestHandlers) {
        this(Arrays.asList(ValidationUtils.assertNotNull(requestHandlers, "requestHandlers")));
    }

    public StackedRequestHandler(List<RequestHandler> requestHandlers) {
        this.inOrderRequestHandlers = ValidationUtils.assertNotNull(requestHandlers, "requestHandlers");
        this.reverseOrderRequestHandlers = new ArrayList<>(requestHandlers);
        Collections.reverse(reverseOrderRequestHandlers);
    }

    @Override
    public AmazonWebServiceRequest beforeMarshalling(AmazonWebServiceRequest origRequest) {
        AmazonWebServiceRequest toReturn = origRequest;
        for (RequestHandler handler : inOrderRequestHandlers) {
            toReturn = handler.beforeMarshalling(toReturn);
        }
        return toReturn;
    }

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        SdkHttpFullRequest toReturn = request;
        for (RequestHandler handler : inOrderRequestHandlers) {
            toReturn = handler.beforeRequest(toReturn);
        }
        return toReturn;
    }

    @Override
    public HttpResponse beforeUnmarshalling(SdkHttpFullRequest request, HttpResponse origHttpResponse) {
        HttpResponse toReturn = origHttpResponse;
        for (RequestHandler handler : reverseOrderRequestHandlers) {
            toReturn = handler.beforeUnmarshalling(request, toReturn);
        }
        return toReturn;
    }

    @Override
    public void afterResponse(SdkHttpFullRequest request, Response<?> response) {
        for (RequestHandler handler : reverseOrderRequestHandlers) {
            handler.afterResponse(request, response);
        }
    }

    @Override
    public void afterError(SdkHttpFullRequest request, Response<?> response, Exception e) {
        for (RequestHandler handler : reverseOrderRequestHandlers) {
            handler.afterError(request, response, e);
        }
    }

}
