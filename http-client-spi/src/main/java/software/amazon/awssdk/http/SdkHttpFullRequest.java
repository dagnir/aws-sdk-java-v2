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

package software.amazon.awssdk.http;

import java.io.InputStream;

/**
 * Represents a request being sent to an Amazon Web Service, including the
 * parameters being sent as part of the request, the endpoint to which the
 * request should be sent, etc.
 *
 * <p>This class should not be implemented outside of the SDK.</p>
 */
public interface SdkHttpFullRequest extends SdkHttpRequest {

    /**
     * Returns the optional stream containing the payload data to include for
     * this request.
     * <br/>
     * Not all requests will contain payload data.
     *
     * @return The optional stream containing the payload data to include for this request or null if there is no payload.
     */
    InputStream getContent();

}
