package software.amazon.awssdk.services.acm;

import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
@SdkInternalApi
public final class DefaultACMClientBuilder extends DefaultACMBaseClientBuilder<ACMClientBuilder, ACMClient> implements
        ACMClientBuilder {
    @Override
    protected final ACMClient buildClient() {
        return new DefaultACMClient(super.syncClientConfiguration().asLegacySyncClientParams());
    }
}
