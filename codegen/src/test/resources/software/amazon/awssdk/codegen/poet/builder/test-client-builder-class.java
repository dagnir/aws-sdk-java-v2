package software.amazon.awssdk.services.acm;

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

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
@SdkInternalApi
public abstract class DefaultACMBaseClientBuilder<B extends ACMBaseClientBuilder<B, C>, C> extends DefaultClientBuilder<B, C>
        implements ClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "acm";
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
                signer.setServiceName("acm");
                signer.setRegionName(signingRegion());
                return new StaticSignerProvider(signer);
            }

            @Override
            protected void applyListenerDefaults(ClientListenerConfiguration.Builder builder) {
                HandlerChainFactory chainFactory = new HandlerChainFactory();
                chainFactory.newRequestHandlerChain("/software/amazon/awssdk/services/acm/request.handlers").forEach(
                        builder::addRequestListener);
                chainFactory.newRequestHandler2Chain("/software/amazon/awssdk/services/acm/request.handler2s").forEach(
                        builder::addRequestListener);
            }
        };
    }
}
