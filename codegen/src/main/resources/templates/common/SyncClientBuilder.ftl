${fileHeader}
package ${metadata.packageName};

import javax.annotation.Generated;

import software.amazon.awssdk.LegacyClientConfigurationFactory;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.client.AwsSyncClientParams;

/**
 * Fluent builder for {@link ${metadata.packageName + "." + metadata.syncInterface}}. Use of the
 * builder is preferred over using constructors of the client class.
**/
@NotThreadSafe
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public final class ${metadata.syncClientBuilderClassName}
    extends AwsSyncClientBuilder<${metadata.syncClientBuilderClassName}, ${metadata.syncInterface}> {

    private static final LegacyClientConfigurationFactory CLIENT_CONFIG_FACTORY = new ${clientConfigFactory}();

    /**
    * @return Create new instance of builder with all defaults set.
    */
    public static ${metadata.syncClientBuilderClassName} standard() {
        return new ${metadata.syncClientBuilderClassName}();
    }

    /**
     * @return Default client using the {@link software.amazon.awssdk.auth.DefaultAwsCredentialsProviderChain}
     * and {@link software.amazon.awssdk.regions.DefaultAwsRegionProviderChain} chain
     */
    public static ${metadata.syncInterface} defaultClient() {
        return standard().build();
    }

    private ${metadata.syncClientBuilderClassName}() {
        super(CLIENT_CONFIG_FACTORY);
    }

    /**
     * Construct a synchronous implementation of ${metadata.syncInterface} using the current builder configuration.
     *
     * @param params Current builder configuration represented as a parameter object.
     * @return Fully configured implementation of ${metadata.syncInterface}.
     */
    @Override
    protected ${metadata.syncInterface} build(AwsSyncClientParams params) {
        return new ${metadata.syncClient}(params);
    }

}
