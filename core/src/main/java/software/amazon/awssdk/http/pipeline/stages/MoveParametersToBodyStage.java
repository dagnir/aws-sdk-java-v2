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

import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.http.HttpMethodName;
import software.amazon.awssdk.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.util.SdkHttpUtils;
import software.amazon.awssdk.utils.StringUtils;

@ReviewBeforeRelease("Might only need to do this for certain protocols - ie query?")
public final class MoveParametersToBodyStage implements RequestToRequestPipeline {
    @Override
    public Request<?> execute(Request<?> input, RequestExecutionContext context) throws Exception {
        if (shouldPutParamsInBody(input)) {
            return putParams(input);
        }
        return input;
    }

    private boolean shouldPutParamsInBody(Request<?> input) {
        return input.getHttpMethod() == HttpMethodName.POST &&
            input.getContent() == null &&
            input.getParameters() != null &&
            input.getParameters().size() > 0;
    }

    private Request<?> putParams(Request<?> input) {
        byte[] params = SdkHttpUtils.encodeParameters(input).getBytes(StandardCharsets.UTF_8);

        input.setParameters(Collections.emptyMap());
        input.setContent(new ByteArrayInputStream(params));
        input.addHeader("Content-Length", String.valueOf(params.length));
        input.addHeader("Content-Type",
                        "application/x-www-form-urlencoded; charset=" +
                        lowerCase(StandardCharsets.UTF_8.toString()));
        return input;
    }
}
