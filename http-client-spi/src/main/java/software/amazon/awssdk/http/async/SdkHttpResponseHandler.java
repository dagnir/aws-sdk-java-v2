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

package software.amazon.awssdk.http.async;

import java.nio.ByteBuffer;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Responsible for handling asynchronous http responses
 */
public interface SdkHttpResponseHandler {

    /**
     * Called when the initial response with headers is received.
     *
     * @param response the {@link SdkHttpResponse}
     */
    void headersReceived(SdkHttpResponse response);

    /**
     * Called when a chunk of data is received.
     *
     * @param part the data
     */
    void bodyPartReceived(ByteBuffer part);

    /**
     * Called when an exception occurs during the request/response.
     *
     * @param throwable the exception that occurred.
     */
    void exceptionOccurred(Throwable throwable);

    /**
     * Called when all parts of the response have been received.
     */
    void complete();
}
