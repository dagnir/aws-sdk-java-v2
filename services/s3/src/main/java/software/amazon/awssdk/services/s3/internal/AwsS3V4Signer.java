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

import static software.amazon.awssdk.auth.internal.SignerConstants.X_AMZ_CONTENT_SHA256;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.auth.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.request.S3HandlerContextKeys;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * AWS4 signer implementation for AWS S3.
 */
public class AwsS3V4Signer extends Aws4Signer {
    private static final String CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

    /**
     * Sent to S3 in lieu of a payload hash when unsigned payloads are enabled.
     */
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    /**
     * Don't double-url-encode path elements; S3 expects path elements to be encoded only once in
     * the canonical URI.
     */
    public AwsS3V4Signer() {
        super(false);
    }

    /**
     * Read the content of the request to get the length of the stream. This
     * method will wrap the stream by SdkBufferedInputStream if it is not
     * mark-supported.
     */
    static long getContentLength(SdkHttpFullRequest.Builder request) throws IOException {
        final InputStream content = request.getContent();
        if (!content.markSupported()) {
            throw new IllegalStateException("Bug: request input stream must have been made mark-and-resettable at this point");
        }
        long contentLength = 0;
        byte[] tmp = new byte[4096];
        int read;
        content.mark(getReadLimit(request));
        while ((read = content.read(tmp)) != -1) {
            contentLength += read;
        }
        try {
            content.reset();
        } catch (IOException ex) {
            throw new ResetException("Failed to reset the input stream", ex);
        }
        return contentLength;
    }

    /**
     * If necessary, creates a chunk-encoding wrapper on the request payload.
     */
    @Override
    protected void processRequestPayload(SdkHttpFullRequest.Builder request, byte[] signature,
                                         byte[] signingKey, Aws4SignerRequestParams signerRequestParams) {
        if (useChunkEncoding(request)) {
            AwsChunkedEncodingInputStream chunkEncodededStream = new AwsChunkedEncodingInputStream(
                    request.getContent(), signingKey,
                    signerRequestParams.getFormattedSigningDateTime(),
                    signerRequestParams.getScope(),
                    BinaryUtils.toHex(signature), this);
            request.content(chunkEncodededStream);
        }
    }

    @Override
    protected String calculateContentHashPresign(SdkHttpFullRequest.Builder request) {
        return "UNSIGNED-PAYLOAD";
    }

    /**
     * Returns the pre-defined header value and set other necessary headers if
     * the request needs to be chunk-encoded. Otherwise calls the superclass
     * method which calculates the hash of the whole content for signing.
     */
    @Override
    protected String calculateContentHash(SdkHttpFullRequest.Builder request) {
        // To be consistent with other service clients using sig-v4,
        // we just set the header as "required", and AWS4Signer.sign() will be
        // notified to pick up the header value returned by this method.
        request.header(X_AMZ_CONTENT_SHA256, "required");

        if (isPayloadSigningEnabled(request)) {
            if (useChunkEncoding(request)) {
                final String contentLength = request.getFirstHeaderValue(Headers.CONTENT_LENGTH).orElse(null);
                final long originalContentLength;
                if (contentLength != null) {
                    originalContentLength = Long.parseLong(contentLength);
                } else {
                    /**
                     * "Content-Length" header could be missing if the caller is
                     * uploading a stream without setting Content-Length in
                     * ObjectMetadata. Before using sigv4, we rely on HttpClient to
                     * add this header by using BufferedHttpEntity when creating the
                     * HttpRequest object. But now, we need this information
                     * immediately for the signing process, so we have to cache the
                     * stream here.
                     */
                    try {
                        originalContentLength = getContentLength(request);
                    } catch (IOException e) {
                        throw new SdkClientException(
                                "Cannot get the content-length of the request content.", e);
                    }
                }
                request.header("x-amz-decoded-content-length",
                               Long.toString(originalContentLength));
                // Make sure "Content-Length" header is not empty so that HttpClient
                // won't cache the stream again to recover Content-Length
                request.header(Headers.CONTENT_LENGTH, Long.toString(
                        AwsChunkedEncodingInputStream
                                .calculateStreamContentLength(originalContentLength)));
                return CONTENT_SHA_256;
            } else {
                return super.calculateContentHash(request);
            }
        }

        return UNSIGNED_PAYLOAD;
    }

    /**
     * Determine whether to use aws-chunked for signing.
     */
    private boolean useChunkEncoding(SdkHttpRequest request) {
        Object originalRequest = request.handlerContext(AwsHandlerKeys.REQUEST_CONFIG).getOriginalRequest();
        // If chunked encoding is explicitly disabled through client options return right here.
        // Chunked encoding only makes sense to do when the payload is signed
        if (!isPayloadSigningEnabled(request) || isChunkedEncodingDisabled(request)) {
            return false;
        }
        return originalRequest instanceof PutObjectRequest || originalRequest instanceof UploadPartRequest;
    }

    /**
     * @return True if chunked encoding has been explicitly disabled per the request. False
     * otherwise.
     */
    private boolean isChunkedEncodingDisabled(SdkHttpRequest request) {
        Boolean isChunkedEncodingDisabled = request
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED);
        return isChunkedEncodingDisabled != null && isChunkedEncodingDisabled;
    }

    /**
     * @return True if payload signing is explicitly enabled.
     */
    private boolean isPayloadSigningEnabled(SdkHttpRequest request) {
        /**
         * If we aren't using https we should always sign the payload.
         */
        if (!request.getEndpoint().getScheme().equals("https")) {
            return true;
        }

        Boolean isPayloadSigningEnabled = request
                .handlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED);
        return isPayloadSigningEnabled != null && isPayloadSigningEnabled;
    }
}
