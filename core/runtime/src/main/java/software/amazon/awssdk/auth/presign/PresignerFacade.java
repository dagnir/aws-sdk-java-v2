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
package software.amazon.awssdk.auth.presign;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.auth.Presigner;
import software.amazon.awssdk.internal.auth.SignerProvider;
import software.amazon.awssdk.internal.auth.SignerProviderContext;
import software.amazon.awssdk.util.CredentialUtils;
import software.amazon.awssdk.util.RuntimeHttpUtils;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkProtectedApi;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Really thin facade over {@link Presigner} to deal with some common concerns like credential resolution, adding custom headers
 * and query params to be included in signing, and conversion to a usable URL.
 */
@Immutable
@SdkProtectedApi
public final class PresignerFacade {

    private final AWSCredentialsProvider credentialsProvider;
    private final SignerProvider signerProvider;

    public PresignerFacade(PresignerParams presignerParams) {
        this.credentialsProvider = presignerParams.credentialsProvider();
        this.signerProvider = presignerParams.signerProvider();
    }

    public URL presign(Request<?> request, Date expirationDate) {
        final Presigner presigner = (Presigner) signerProvider.getSigner(SignerProviderContext.builder()
                                                                                 .withIsRedirect(false)
                                                                                 .withRequest(request)
                                                                                 .withUri(request.getEndpoint())
                                                                                 .build());
        if (request.getOriginalRequest() != null) {
            addCustomQueryParams(request);
            addCustomHeaders(request);
        }
        final AWSCredentialsProvider credentialsProvider = resolveCredentials(request);
        presigner.presignRequest(request, credentialsProvider.getCredentials(), expirationDate);
        return RuntimeHttpUtils.convertRequestToUrl(request, true, false);
    }

    private void addCustomQueryParams(Request<?> request) {
        final Map<String, List<String>> queryParameters = request.getOriginalRequest().getCustomQueryParameters();
        if (queryParameters == null || queryParameters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
            request.addParameters(param.getKey(), param.getValue());
        }
    }

    private void addCustomHeaders(Request<?> request) {
        final Map<String, String> headers = request.getOriginalRequest().getCustomRequestHeaders();
        if (headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.addHeader(header.getKey(), header.getValue());
        }
    }

    private AWSCredentialsProvider resolveCredentials(Request<?> request) {
        return CredentialUtils.getCredentialsProvider(request.getOriginalRequest(), this.credentialsProvider);
    }

    /**
     * Empty request object useful for setting request level credentials without having the user facing presiging request extend
     * from {@link AmazonWebServiceRequest}.
     */
    public static class PresigningRequest extends AmazonWebServiceRequest {

        public PresigningRequest withRequestCredentialsProvider(AWSCredentialsProvider credentialsProvider) {
            setRequestCredentialsProvider(credentialsProvider);
            return this;
        }
    }
}
