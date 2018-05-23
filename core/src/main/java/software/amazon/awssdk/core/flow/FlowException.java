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

package software.amazon.awssdk.core.flow;

import software.amazon.awssdk.core.exception.SdkException;

/**
 * Exception thrown during the processing of a flow stream.
 */
public final class FlowException extends SdkException {

    private final String errorCode;

    private FlowException(String errorMessage, String errorCode) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    /**
     * Creates a new {@link FlowException}.
     *
     * @param errorMessage Error message returned by the service.
     * @param errorCode Error code returned by the service.
     * @return New {@link FlowException}.
     */
    static FlowException create(String errorMessage, String errorCode) {
        return new FlowException(errorMessage, errorCode);
    }

    /**
     * Error code returned by the service.
     */
    public String errorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "; Error Code: " + errorCode;
    }
}
