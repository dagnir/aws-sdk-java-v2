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

package software.amazon.awssdk.opensdk.internal.auth;

import software.amazon.awssdk.SignableRequest;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.CanHandleNullCredentials;
import software.amazon.awssdk.auth.RequestSigner;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.internal.auth.SignerProvider;
import software.amazon.awssdk.internal.auth.SignerProviderContext;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerAware;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerNotFoundException;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerProvider;

public final class SignerProviderAdapter extends SignerProvider {

    private final RequestSignerProvider provider;

    public SignerProviderAdapter(RequestSignerProvider provider) {
        this.provider = provider;
    }

    @Override
    public Signer getSigner(SignerProviderContext context) {
        final Object originalRequest = context.getRequestConfig().getOriginalRequest();
        if (originalRequest instanceof RequestSignerAware) {
            Class<? extends RequestSigner> signerType = ((RequestSignerAware) originalRequest)
                    .signerType();
            return provider.getSigner(signerType)
                    .map(AuthorizerAsSigner::new)
                    .orElseThrow(() -> new RequestSignerNotFoundException(signerType));
        }
        return null;
    }

    private static class AuthorizerAsSigner implements Signer, CanHandleNullCredentials {

        private final RequestSigner authorizer;

        private AuthorizerAsSigner(RequestSigner authorizer) {
            this.authorizer = authorizer;
        }

        @Override
        public void sign(SignableRequest<?> request, AWSCredentials credentials) {
            authorizer.sign(request);
        }
    }
}
