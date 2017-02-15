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

package software.amazon.awssdk.services.s3.internal;

import software.amazon.awssdk.http.HttpResponse;

/**
 * Assistant response handler that can pull an HTTP header out of the response
 * and apply it to a response object.
 */
public interface HeaderHandler<T> {

    /**
     * Applies one or more headers to the response object given.
     *
     * @param result
     *            The response object to be returned to the client.
     * @param response
     *            The HTTP response from s3.
     */
    public void handle(T result, HttpResponse response);
}
