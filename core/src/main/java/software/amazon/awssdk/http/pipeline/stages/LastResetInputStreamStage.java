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

import java.io.InputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.pipeline.RequestToResponsePipeline;

/**
 * Attempts to reset the request input stream one last time before throwing back to the caller.
 */
// TODO this is dubious and should be reconsidered. There's no guarentee this reset actually happens so it's
// not really safe for the caller to reuse that input stream. We should probably just document that any input stream
// passed to the SDK may be in an unusable state
public class LastResetInputStreamStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private static final Log log = LogFactory.getLog(LastResetInputStreamStage.class);

    private final RequestPipeline<Request<?>, Response<OutputT>> wrapped;

    public LastResetInputStreamStage(RequestPipeline<Request<?>, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(Request<?> input, RequestExecutionContext context) throws Exception {
        try {
            return wrapped.execute(input, context);
        } catch (RuntimeException e) {
            throw lastReset(input, e);
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Exception e) {
            throw lastReset(input, e);
        }
    }

    /**
     * Used to perform a last reset on the content input stream (if mark-supported); this is so
     * that, for backward compatibility reason, any "blind" retry (ie without calling reset) by
     * user of this library with the same input stream (such as ByteArrayInputStream) could
     * still succeed.
     *
     * @param t the failure
     * @return the failure as given
     */
    private <T extends Throwable> T lastReset(Request<?> request, final T t) {
        try {
            InputStream content = request.getContent();
            if (content != null && content.markSupported()) {
                content.reset();
            }
        } catch (Exception ex) {
            log.debug("FYI: failed to reset content inputstream before throwing up", ex);
        }
        return t;
    }
}
