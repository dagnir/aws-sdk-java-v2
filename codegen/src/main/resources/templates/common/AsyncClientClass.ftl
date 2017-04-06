${fileHeader}
package ${metadata.packageName};

import static java.util.concurrent.Executors.newFixedThreadPool;

import javax.annotation.Generated;

import ${metadata.packageName}.model.*;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.AmazonWebServiceClient;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.auth.DefaultAwsCredentialsProviderChain;

/**
 * Client for accessing ${metadata.serviceName} asynchronously. Each
 * asynchronous method will return a Java Future object representing the
 * asynchronous operation; overloads which accept an {@code AsyncHandler} can
 * be used to receive notification when an asynchronous operation completes.
<#if metadata.documentation??>
 * <p>
 * ${metadata.documentation}
</#if>
 */
@ThreadSafe
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${metadata.asyncClient} implements ${metadata.asyncInterface} {

    private final ${metadata.syncInterface} syncClient;

    /**
     * Constructs a new asynchronous client to invoke service methods on
     * ${metadata.serviceName} using the specified parameters.
     *
     * @param asyncClientParams Object providing client parameters.
     */
    ${metadata.asyncClient}(AwsAsyncClientParams asyncClientParams) {
        this.syncClient = ${metadata.syncInterface}Builder.standard().build((AwsSyncClientParams)asyncClientParams);
    }

  <#list operations?values as operationModel>
    <@AsyncClientMethodForOperation.content operationModel />
  </#list>

  <#if AdditionalClientMethodsMacro?has_content>
    <@AdditionalClientMethodsMacro.content .data_model />
  </#if>

    /**
     * Shuts down the client, releasing all managed resources. This includes
     * forcibly terminating all pending asynchronous service calls.
     */
    public void close() throws Exception{
        syncClient.close();
    }
}
