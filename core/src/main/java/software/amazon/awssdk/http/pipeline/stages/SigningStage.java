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

package software.amazon.awssdk.http.pipeline.stages;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.CanHandleNullCredentials;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.runtime.auth.SignerProviderContext;

/**
 * Sign the marshalled request (if applicable).
 */
public class SigningStage implements RequestToRequestPipeline {

    private final HttpClientDependencies dependencies;

    public SigningStage(HttpClientDependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public Request<?> execute(Request<?> request, RequestExecutionContext context) throws Exception {
        AmazonHttpClient.checkInterrupted();
        signRequest(request, context);
        AmazonHttpClient.checkInterrupted();
        return request;
    }

    /**
     * Sign the request if the signer if provided and credentials are present.
     */
    private void signRequest(Request<?> request, RequestExecutionContext context) {
        final AwsCredentials credentials = context.credentialsProvider().getCredentialsOrThrow();
        Signer signer = newSigner(request, context);
        if (shouldSign(signer, credentials)) {
            context.awsRequestMetrics().startEvent(AwsRequestMetrics.Field.RequestSigningTime);
            try {
                signer.sign(adjustForClockSkew(request), credentials);
            } finally {
                context.awsRequestMetrics().endEvent(AwsRequestMetrics.Field.RequestSigningTime);
            }
        }
    }

    /**
     * Obtain a signer from the {@link software.amazon.awssdk.runtime.auth.SignerProvider}.
     */
    private Signer newSigner(final Request<?> request, RequestExecutionContext context) {
        final SignerProviderContext.Builder signerProviderContext = SignerProviderContext
                .builder()
                .withRequest(request)
                .withRequestConfig(context.requestConfig());
        return context.signerProvider().getSigner(signerProviderContext.withUri(request.getEndpoint()).build());
    }

    /**
     * We sign if a signer is provided and the credentials are non-null (unless the signer implements the marker interface,
     * {@link
     * CanHandleNullCredentials}).
     *
     * @return True if request should be signed, false if not.
     */
    private boolean shouldSign(Signer signer, AwsCredentials credentials) {
        return signer != null && (credentials != null || signer instanceof CanHandleNullCredentials);
    }

    /**
     * Always use the client level timeOffset if it's non-zero. Otherwise, we respect the timeOffset in the request, which could
     * have been externally configured (at least for the 1st non-retry request).
     */
    private Request<?> adjustForClockSkew(Request<?> request) {
        if (dependencies.timeOffset() != 0) {
            request.setTimeOffset(dependencies.timeOffset());
        }
        return request;
    }
}
