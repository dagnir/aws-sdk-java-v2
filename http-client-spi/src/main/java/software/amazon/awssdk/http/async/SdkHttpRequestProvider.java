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
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;

/**
 * A provider of HTTP request data, allows streaming operations for asynchronous HTTP clients.
 */
public interface SdkHttpRequestProvider {

    /**
     * The request to execute, including headers, parameters and the URI
     *
     * @return the {@link SdkHttpRequest} to execute
     */
    SdkHttpRequest request();

    /**
     * Any additional dependencies (e.g. metrics object) that go along with this request
     *
     * @return the {@link SdkRequestContext} instance for this request
     */
    SdkRequestContext context();

    /**
     * Called when the HTTP client has made the initial request and the
     * socket is open and ready to receive data.
     *
     * Expected behaviour is implementor calls {@link SdkRequestChannel#write(ByteBuffer)} with
     * data until all data is written and then calls {@link SdkRequestChannel#complete()} to signify
     * all data has been written.
     *
     * @param channel an instance of {@link SdkRequestChannel} that is ready to receive data
     */
    void readyForData(SdkRequestChannel channel);

    /**
     * Called when there is an exception establishing the socket, sending the initial
     * request.
     *
     * @param exception the exception that occurred
     */
    void exceptionOccurred(Throwable exception);
}
