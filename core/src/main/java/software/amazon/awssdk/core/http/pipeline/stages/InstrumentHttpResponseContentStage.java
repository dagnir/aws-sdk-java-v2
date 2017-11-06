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

package software.amazon.awssdk.core.http.pipeline.stages;

import static software.amazon.awssdk.core.event.SdkProgressPublisher.publishResponseContentLength;

import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.event.ProgressInputStream;
import software.amazon.awssdk.core.event.ProgressListener;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Pair;

/**
 * Instrument the response content so that it reports events to the {@link ProgressListener}.
 */
public class InstrumentHttpResponseContentStage
    implements RequestPipeline<Pair<SdkHttpFullRequest, HttpResponse>, Pair<SdkHttpFullRequest, HttpResponse>> {

    private static final Logger log = LoggerFactory.getLogger(InstrumentHttpResponseContentStage.class);

    @Override
    public Pair<SdkHttpFullRequest, HttpResponse> execute(Pair<SdkHttpFullRequest, HttpResponse> input,
                                                          RequestExecutionContext context) throws Exception {
        Optional<ProgressListener> listener = context.requestConfig().progressListener();
        HttpResponse httpResponse = input.right();
        InputStream is = input.right().getContent();

        if (is != null) {
            if (listener.isPresent()) {
                is = ProgressInputStream.inputStreamForResponse(is, listener.get());
            }
            httpResponse.setContent(is);
        }

        listener.ifPresent(l -> {
            try {
                Optional.ofNullable(httpResponse.getHeaders().get("Content-Length"))
                        .map(Long::parseLong)
                        .ifPresent(len -> publishResponseContentLength(l, len));
            } catch (NumberFormatException e) {
                log.warn("Cannot parse the Content-Length header of the response.");
            }
        });
        return input;
    }
}
