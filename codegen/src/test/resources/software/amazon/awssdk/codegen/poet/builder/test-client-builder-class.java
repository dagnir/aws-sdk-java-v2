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

package software.amazon.awssdk.services.json;

import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.StaticSignerProvider;
import software.amazon.awssdk.client.builder.ClientBuilder;
import software.amazon.awssdk.client.builder.DefaultClientBuilder;
import software.amazon.awssdk.config.ClientListenerConfiguration;
import software.amazon.awssdk.config.ClientSecurityConfiguration;
import software.amazon.awssdk.config.defaults.ClientConfigurationDefaults;
import software.amazon.awssdk.handlers.HandlerChainFactory;
import software.amazon.awssdk.runtime.auth.SignerProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public abstract class DefaultJsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends DefaultClientBuilder<B, C>
    implements ClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "json-service";
    }

    @Override
    protected final ClientConfigurationDefaults serviceDefaults() {
        return new ClientConfigurationDefaults() {
            @Override
            protected void applySecurityDefaults(ClientSecurityConfiguration.Builder builder) {
                builder.signerProvider(builder.signerProvider().orElseGet(this::defaultSignerProvider));
            }

            private SignerProvider defaultSignerProvider() {
                Aws4Signer signer = new Aws4Signer();
                signer.setServiceName("json-service");
                signer.setRegionName(signingRegion());
                return new StaticSignerProvider(signer);
            }

            @Override
            protected void applyListenerDefaults(ClientListenerConfiguration.Builder builder) {
                HandlerChainFactory chainFactory = new HandlerChainFactory();
                chainFactory.newRequestHandlerChain("/software/amazon/awssdk/services/json/request.handlers").forEach(
                    builder::addRequestListener);
                chainFactory.newRequestHandler2Chain("/software/amazon/awssdk/services/json/request.handler2s").forEach(
                    builder::addRequestListener);
            }
        };
    }
}
