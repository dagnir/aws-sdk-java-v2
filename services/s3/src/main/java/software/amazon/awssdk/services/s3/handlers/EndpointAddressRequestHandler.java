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

package software.amazon.awssdk.services.s3.handlers;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;

import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.handlers.HandlerContextKey;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.services.s3.BucketUtils;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

public class EndpointAddressRequestHandler extends RequestHandler2 {

    private static List<Class> ACCELERATE_DISABLED_OPERATIONS = Arrays.asList(
        ListBucketsRequest.class, CreateBucketRequest.class, DeleteBucketRequest.class);

    @Override
    public void beforeRequest(Request<?> request) {

        S3AdvancedConfiguration advancedConfiguration =
                    (S3AdvancedConfiguration) request.getHandlerContext(HandlerContextKey.SERVICE_ADVANCED_CONFIG);


        if (advancedConfiguration == null || !advancedConfiguration.pathStyleAccessEnabled()) {
            try {
                String bucketName = (String) request
                    .getOriginalRequest()
                    .getClass()
                    .getMethod("bucket")
                    .invoke(request.getOriginalRequest());

                if (BucketUtils.isValidS3BucketName(bucketName, false)) {
                    changeToDnsEndpoint(request, bucketName);
                }
            } catch (Exception e) {
                // Unable to convert to DNS style addressing. Fall back to continue using path style.
            }
        }

        // Remove accelerate from operations that don't allow it
        if (advancedConfiguration != null && advancedConfiguration.accelerateModeEnabled()
            && ACCELERATE_DISABLED_OPERATIONS.contains(request.getOriginalRequest().getClass())) {
            removeAccelerate(request);
        }
    }

    private URI removeAccelerate(Request<?> request) {
        return URI.create(request.getEndpoint().toASCIIString().replaceFirst("s3-accelerate", "s3"));
    }

    private void changeToDnsEndpoint(Request<?> request, String bucketName) {
        // Replace /bucketName from resourcePath with nothing
        String resourcePath = request.getResourcePath().replaceFirst("/" + bucketName, "");

        // Prepend bucket to endpoint
        URI endpoint = invokeSafely(() -> new URI(
                request.getEndpoint().getScheme(), // Existing scheme
                request.getEndpoint().getHost().replaceFirst("s3", bucketName + "." + "s3"), // replace "s3" with "bucket.s3"
                null,
                null));

        request.setEndpoint(endpoint);

        request.setResourcePath(resourcePath);
    }
}
