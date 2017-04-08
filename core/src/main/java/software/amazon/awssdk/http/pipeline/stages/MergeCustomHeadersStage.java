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

import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.pipeline.RequestToRequestPipeline;

/**
 * Merge customer supplied headers into the marshalled request.
 */
public class MergeCustomHeadersStage implements RequestToRequestPipeline {

    private final LegacyClientConfiguration config;

    public MergeCustomHeadersStage(HttpClientDependencies dependencies) {
        this.config = dependencies.config();
    }

    @Override
    public Request<?> execute(Request<?> request, RequestExecutionContext context) throws Exception {
        request.getHeaders().putAll(config.getHeaders());
        request.getHeaders().putAll(context.requestConfig().getCustomRequestHeaders());
        return request;
    }
}
