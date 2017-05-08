package software.amazon.awssdk.services.acm;

import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
@SdkInternalApi
public final class DefaultACMAsyncClientBuilder extends DefaultACMBaseClientBuilder<ACMAsyncClientBuilder, ACMAsyncClient>
        implements ACMAsyncClientBuilder {
    @Override
    protected final ACMAsyncClient buildClient() {
        return new DefaultACMAsyncClient(super.asyncClientConfiguration().asLegacyAsyncClientParams());
    }
}
