/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.async;

import software.amazon.awssdk.http.SdkHttpRequest;

public final class AsyncExecuteRequest {
    private final SdkHttpRequest request;
    private final SdkHttpContentPublisher requestContentPublisher;
    private final SdkAsyncHttpResponseHandler responseHandler;

    private AsyncExecuteRequest(BuilderImpl builder) {
        this.request = builder.request;
        this.requestContentPublisher = builder.requestContentPublisher;
        this.responseHandler = builder.responseHandler;
    }

    public SdkHttpRequest request() {
        return request;
    }

    public SdkHttpContentPublisher requestContentPublisher() {
        return requestContentPublisher;
    }

    public SdkAsyncHttpResponseHandler responseHandler() {
        return responseHandler;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
         Builder request(SdkHttpRequest request);

         Builder requestContentPublisher(SdkHttpContentPublisher requestContentPublisher);

         Builder responseHandler(SdkAsyncHttpResponseHandler responseHandler);

         AsyncExecuteRequest build();
    }

    private static class BuilderImpl implements Builder {
        private SdkHttpRequest request;
        private SdkHttpContentPublisher requestContentPublisher;
        private SdkAsyncHttpResponseHandler responseHandler;

        @Override
        public Builder request(SdkHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public Builder requestContentPublisher(SdkHttpContentPublisher requestContentPublisher) {
            this.requestContentPublisher = requestContentPublisher;
            return this;
        }

        @Override
        public Builder responseHandler(SdkAsyncHttpResponseHandler responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }

        @Override
        public AsyncExecuteRequest build() {
            return new AsyncExecuteRequest(this);
        }
    }
}
