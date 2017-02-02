/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.http.timers.client;

import software.amazon.awssdk.Response;
import software.amazon.awssdk.Response;

public class SdkInterruptedException extends InterruptedException {

    private static final long serialVersionUID = 8194951388566545094L;

    private final transient Response<?> response;

    public SdkInterruptedException(Response<?> response) {
        this.response = response;
    }

    public Response<?> getResponse() {
        return response;
    }
}