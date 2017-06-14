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

package software.amazon.awssdk.services.s3.request;

import software.amazon.awssdk.handlers.HandlerContextKey;
import software.amazon.awssdk.services.s3.AwsS3V4Signer;

/**
 * Constants for {@link HandlerContextKey} pertaining to S3
 */
public class S3HandlerContextKeys {

    /**
     * Context provided to {@link AwsS3V4Signer} to determine whether chunked encoding should be used
     * or not.
     */
    public static final HandlerContextKey<Boolean> IS_CHUNKED_ENCODING_DISABLED = new HandlerContextKey<>(
            "IsChunkedEncodingDisabled");

    /**
     * Context provided to {@link AwsS3V4Signer} to determine whether payloads should be signed.  If enabled,
     * payload hash will be computed when constructing the request.  This does incur a performance penalty.
     */
    public static final HandlerContextKey<Boolean> IS_PAYLOAD_SIGNING_ENABLED = new HandlerContextKey<>(
            "IsPayloadSigningEnabled");
}
