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

import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.util.AwsRequestMetrics;
import software.amazon.awssdk.util.TimingInfo;

/**
 * Internal class used to adapt a request handler that implements the
 * deprecated {@link RequestHandler} interface to the deprecating
 * {@link RequestHandler2} interface.
 */
final class RequestHandler2Adaptor extends RequestHandler2 {
    @SuppressWarnings("deprecation")
    private final RequestHandler old;

    RequestHandler2Adaptor(@SuppressWarnings("deprecation") RequestHandler old) {
        if (old == null) {
            throw new IllegalArgumentException();
        }
        this.old = old;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void beforeRequest(Request<?> request) {
        old.beforeRequest(request);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void afterResponse(Request<?> request, Response<?> response) {
        AwsRequestMetrics awsRequestMetrics = request == null ? null : request
                .getAwsRequestMetrics();
        Object awsResponse = response == null ? null : response
                .getAwsResponse();
        TimingInfo timingInfo = awsRequestMetrics == null ? null
                                                          : awsRequestMetrics.getTimingInfo();
        old.afterResponse(request, awsResponse, timingInfo);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void afterError(Request<?> request, Response<?> response,
                           Exception e) {
        old.afterError(request, e);
    }

    @Override
    public int hashCode() {
        return old.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RequestHandler2Adaptor)) {
            return false;
        }
        RequestHandler2Adaptor that = (RequestHandler2Adaptor) o;
        return this.old.equals(that.old);
    }
}
