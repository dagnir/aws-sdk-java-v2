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

import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Logic for determining whether MD5 checksum validation should be performed or not.
 */
public class SkipMd5CheckStrategy {

    public static final SkipMd5CheckStrategy INSTANCE = new SkipMd5CheckStrategy();

    // Singleton
    private SkipMd5CheckStrategy() {
    }

    /**
     * Returns true if the specified ETag was from a multipart upload.
     *
     * @param eTag
     *            The ETag to test.
     * @return True if the specified ETag was from a multipart upload, otherwise false it if belongs
     *         to an object that was uploaded in a single part.
     */
    private static boolean isMultipartUploadETag(String eTag) {
        return eTag.contains("-");
    }

    /**
     * Helper method to avoid long chains of non null checks
     *
     * @return True if any of the provided items is not null. False if all items are null.
     */
    private static boolean containsNonNull(Object... items) {
        for (Object item : items) {
            if (item != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the client should use the {@link Headers#ETAG} header returned by S3 to
     * validate the integrity of the message client side based on the server response. We skip the
     * client side check if any of the following conditions are true:
     * <ol>
     * <li>The request involves SSE-C or SSE-KMS</li>
     * <li>The Etag header is missing</li>
     * <li>The Etag indicates that the object was created by a MultiPart Upload</li>
     * </ol>
     *
     * @return True if client side validation should be skipped, false otherwise.
     */
    public boolean skipClientSideValidationPerGetResponse(ObjectMetadata metadata) {
        return skipClientSideValidationPerResponse(metadata);
    }

    /**
     * Determines whether the client should use the {@link Headers#ETAG} header returned by S3 to
     * validate the integrity of the message client side based on the server response. We skip the
     * client side check if any of the following conditions are true:
     * <ol>
     * <li>The request involves SSE-C or SSE-KMS</li>
     * <li>The Etag header is missing</li>
     * </ol>
     *
     * @return True if client side validation should be skipped, false otherwise.
     */
    public boolean skipClientSideValidationPerPutResponse(ObjectMetadata metadata) {
        return skipClientSideValidationPerResponse(metadata);
    }

    /**
     * Determines whether the client should use the {@link Headers#ETAG} header returned by S3 to
     * validate the integrity of the message client side based on the server response. We skip the
     * client side check if any of the following conditions are true:
     * <ol>
     * <li>The request involves SSE-C or SSE-KMS</li>
     * <li>The Etag header is missing</li>
     * </ol>
     *
     * @return True if client side validation should be skipped, false otherwise.
     */
    public boolean skipClientSideValidationPerUploadPartResponse(ObjectMetadata metadata) {
        return skipClientSideValidationPerPutResponse(metadata);
    }

    /**
     * Conveience method to determine whether to do client side validation of a GetObject call based
     * on both the request and the response. See
     * {@link #skipClientSideValidationPerRequest(GetObjectRequest)} and
     * {@link #skipClientSideValidationPerGetResponse(ObjectMetadata)} for more details on the
     * criterion.
     *
     * @param request
     *            Original {@link GetObjectRequest}
     * @param returnedMetadata
     *            Metadata returned in {@link S3Object}
     * @return True if client side validation should be skipped, false otherwise.
     */
    public boolean skipClientSideValidation(GetObjectRequest request, ObjectMetadata returnedMetadata) {
        return skipClientSideValidationPerRequest(request) || skipClientSideValidationPerGetResponse(returnedMetadata);
    }

    /**
     * Determines whether the client should use the {@link Headers#ETAG} header returned by S3 to
     * validate the integrity of the message client side. We skip the client side check if any of
     * the following conditions are true:
     * <ol>
     * <li>The request involves SSE-C or SSE-KMS</li>
     * </ol>
     *
     * @return True if client side validation should be skipped, false otherwise.
     */
    public boolean skipClientSideValidationPerRequest(PutObjectRequest request) {
        return putRequestInvolvesSse(request) || metadataInvolvesSse(request.getMetadata());
    }

    /**
     * Determines whether the client should use the {@link Headers#ETAG} header returned by S3 to
     * validate the integrity of the message client side. We skip the client side check if any of
     * the following conditions are true:
     * <ol>
     * <li>The request involves SSE-C or SSE-KMS</li>
     * </ol>
     *
     * @return True if client side validation should be skipped, false otherwise.
     */
    public boolean skipClientSideValidationPerRequest(UploadPartRequest request) {
        return request.getSseCustomerKey() != null;
    }

    /**
     * Determines whether the client should calculate and send the {@link Headers#CONTENT_MD5}
     * header to be validated by S3 per the request.
     * <p>
     * Currently we always try and do server side validation. Whether or not we actually
     * calculate the MD5 header is determined in the client based on the source of the data (i.e. if
     * it's a file we calculate, if not then we don't)
     * </p>
     */
    public boolean skipServerSideValidation(PutObjectRequest request) {
        return false;
    }

    /**
     * Determines whether the client should calculate and send the {@link Headers#CONTENT_MD5}
     * header to be validated by S3 per the request.
     * <p>
     * Currently we always try and do server side validation. Whether or not we actually
     * calculate the MD5 header is determined in the client based on the source of the data (i.e. if
     * it's a file we calculate, if not then we don't)
     * </p>
     */
    public boolean skipServerSideValidation(UploadPartRequest request) {
        return false;
    }

    /**
     * Based on the given {@link GetObjectRequest}, returns whether the specified request should
     * skip MD5 check on the requested object content. Specifically, MD5 check should be skipped if
     * one of the following conditions are true:
     * <ol>
     * <li>The request is a range-get operation</li>
     * <li>The request is a GET object operation that involves SSE-C</li>
     * </ol>
     * Otherwise, MD5 check should not be skipped.
     */
    public boolean skipClientSideValidationPerRequest(GetObjectRequest request) {
        // Skip MD5 check for range get
        return request.getRange() != null || request.getSseCustomerKey() != null;
    }

    private boolean skipClientSideValidationPerResponse(ObjectMetadata metadata) {
        // If Etag is not provided or was computed from a multipart upload then skip the check, the
        // etag won't be the MD5 of the original content
        return metadata == null ||
               metadata.getETag() == null ||
               isMultipartUploadETag(metadata.getETag()) ||
               metadataInvolvesSse(metadata);
    }

    /**
     * If SSE-C or SSE-KMS is involved then the Etag will be the MD5 of the ciphertext not the
     * plaintext so we can't validate it client side. Plain SSE with S3 managed keys will return an
     * Etag that does match the MD5 of the plaintext so it's still eligible for client side
     * validation.
     *
     * @param metadata
     *            Metadata of request or response
     * @return True if the metadata indicates that SSE-C or SSE-KMS is used. False otherwise
     */
    private boolean metadataInvolvesSse(ObjectMetadata metadata) {
        return metadata != null && containsNonNull(metadata.getSseCustomerAlgorithm(), metadata.getSseCustomerKeyMd5(),
                                                   metadata.getSseAwsKmsKeyId());
    }

    /**
     * @return True if {@link PutObjectRequest} has been configured to use SSE-C or SSE-KMS
     */
    private boolean putRequestInvolvesSse(PutObjectRequest request) {
        return containsNonNull(request.getSseCustomerKey(), request.getSseAwsKeyManagementParams());
    }
}
