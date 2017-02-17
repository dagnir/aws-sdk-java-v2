${fileHeader}
package ${metadata.packageName};

import javax.annotation.Generated;

import software.amazon.awssdk.ClientConfigurationFactory;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.client.AwsAsyncClientParams;

/**
 * Fluent builder for {@link ${metadata.packageName + "." + metadata.asyncInterface}}. Use of the
 * builder is preferred over using constructors of the client class.
**/
@NotThreadSafe
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public final class ${metadata.asyncClientBuilderClassName}
    extends AwsAsyncClientBuilder<${metadata.asyncClientBuilderClassName}, ${metadata.asyncInterface}> {

    private static final ClientConfigurationFactory CLIENT_CONFIG_FACTORY = new ${clientConfigFactory}();;

    /**
    * @return Create new instance of builder with all defaults set.
    */
    public static ${metadata.asyncClientBuilderClassName} standard() {
        return new ${metadata.asyncClientBuilderClassName}();
    }

    /**
     * @return Default async client using the {@link software.amazon.awssdk.auth.DefaultAwsCredentialsProviderChain}
     * and {@link software.amazon.awssdk.regions.DefaultAwsRegionProviderChain} chain
     */
    public static ${metadata.asyncInterface} defaultClient() {
        return standard().build();
    }

    private ${metadata.asyncClientBuilderClassName}() {
        super(CLIENT_CONFIG_FACTORY);
    }

    /**
     * Construct an asynchronous implementation of ${metadata.asyncInterface} using the
     * current builder configuration.
     *
     * @param params Current builder configuration represented as a parameter object.
     * @return Fully configured implementation of ${metadata.asyncInterface}.
     */
    @Override
    protected ${metadata.asyncInterface} build(AwsAsyncClientParams params) {
        return new ${metadata.asyncClient}(params);
    }

}
