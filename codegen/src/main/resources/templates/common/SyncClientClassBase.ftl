${fileHeader}
<#assign serviceAbbreviation = (metadata.serviceAbbreviation)!metadata.serviceFullName/>
package ${metadata.packageName};

import org.w3c.dom.*;

<#if metadata.hasApiWithStreamInput>
import java.io.*;
</#if>
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import javax.annotation.Generated;

import org.apache.commons.logging.*;

import software.amazon.awssdk.*;
import software.amazon.awssdk.auth.*;
import software.amazon.awssdk.auth.presign.PresignerParams;
import software.amazon.awssdk.handlers.*;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.metrics.*;
import software.amazon.awssdk.metrics.spi.*;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics.Field;
import software.amazon.awssdk.regions.*;
import software.amazon.awssdk.runtime.transform.*;
import software.amazon.awssdk.util.*;
import software.amazon.awssdk.protocol.json.*;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.client.AwsSyncClientParams;
import ${metadata.packageName}.${metadata.syncClientBuilderClassName};
<#if hasWaiters>
import ${metadata.packageName}.waiters.${metadata.syncInterface}Waiters;
</#if>

<#if customizationConfig.serviceClientHoldInputStream>
import software.amazon.awssdk.util.ServiceClientHolderInputStream;
</#if>
import ${serviceBaseExceptionFqcn};


import ${metadata.packageName}.model.*;
import ${transformPackage}.*;

<#assign documentation = (metadata.documentation)!""/>

/**
 * Client for accessing ${serviceAbbreviation}.  All service calls made
 * using this client are blocking, and will not return until the service call
 * completes.
 * <p>
 * ${documentation}
 */
@ThreadSafe
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${metadata.syncClient} extends AmazonWebServiceClient implements ${metadata.syncInterface} {
<#if customizationConfig.requestMetrics?has_content>
    // register the service specific set of predefined metrics
    static {
        AwsSdkMetrics.addAll(Arrays.asList(${customizationConfig.requestMetrics}.values()));
    }
</#if>
    /** Provider for AWS credentials. */
    private final AwsCredentialsProvider awsCredentialsProvider;

    private static final Log log = LogFactory.getLog(${metadata.syncInterface}.class);

    /** Default signing name for the service. */
    private static final String DEFAULT_SIGNING_NAME = "${metadata.signingName}";

<#if hasWaiters>
    private volatile ${metadata.syncInterface}Waiters waiters;
</#if>

    /** Client configuration factory providing ClientConfigurations tailored to this client */
    protected static final ${clientConfigFactory} configFactory = new ${clientConfigFactory}();

    <@AdditionalSyncClientFieldsMacro.content .data_model />

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation}.  A credentials provider chain will be used
     * that searches for credentials in this order:
     * <ul>
     *  <li> Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY </li>
     *  <li> Java System Properties - aws.accessKeyId and aws.secretKey </li>
     *  <li> Instance profile credentials delivered through the Amazon EC2 metadata service </li>
     * </ul>
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @see DefaultAwsCredentialsProviderChain
     * @deprecated use {@link ${metadata.syncClientBuilderClassName}#defaultClient()}
     */
    @Deprecated
    public ${metadata.syncClient}() {
        this(DefaultAwsCredentialsProviderChain.getInstance(), configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation}.  A credentials provider chain will be used
     * that searches for credentials in this order:
     * <ul>
     *  <li> Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY </li>
     *  <li> Java System Properties - aws.accessKeyId and aws.secretKey </li>
     *  <li> Instance profile credentials delivered through the Amazon EC2 metadata service </li>
     * </ul>
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param clientConfiguration The client configuration options controlling how this
     *                       client connects to ${serviceAbbreviation}
     *                       (ex: proxy settings, retry counts, etc.).
     *
     * @see DefaultAwsCredentialsProviderChain
     * @deprecated use {@link ${metadata.syncClientBuilderClassName}#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public ${metadata.syncClient}(ClientConfiguration clientConfiguration) {
        this(DefaultAwsCredentialsProviderChain.getInstance(), clientConfiguration);
    }

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation} using the specified AWS account credentials.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param awsCredentials The AWS credentials (access key ID and secret key) to use
     *                       when authenticating with AWS services.
     * @deprecated use {@link ${metadata.syncClientBuilderClassName}#withCredentials(AwsCredentialsProvider)} for example:
     * {@code ${metadata.syncClientBuilderClassName}.standard().withCredentials(new AwsStaticCredentialsProvider(awsCredentials)).build();}
     */
    @Deprecated
    public ${metadata.syncClient}(AwsCredentials awsCredentials) {
        this(awsCredentials, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation} using the specified AWS account credentials
     * and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param awsCredentials The AWS credentials (access key ID and secret key) to use
     *                       when authenticating with AWS services.
     * @param clientConfiguration The client configuration options controlling how this
     *                       client connects to ${serviceAbbreviation}
     *                       (ex: proxy settings, retry counts, etc.).
     * @deprecated use {@link ${metadata.syncClientBuilderClassName}#withCredentials(AwsCredentialsProvider)} and
     *             {@link ${metadata.syncClientBuilderClassName}#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public ${metadata.syncClient}(AwsCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(clientConfiguration);
        this.awsCredentialsProvider = new AwsStaticCredentialsProvider(awsCredentials);
        init();
    }

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation} using the specified AWS account credentials provider.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param awsCredentialsProvider
     *            The AWS credentials provider which will provide credentials
     *            to authenticate requests with AWS services.
     * @deprecated use {@link ${metadata.syncClientBuilderClassName}#withCredentials(AwsCredentialsProvider)}
     */
    @Deprecated
    public ${metadata.syncClient}(AwsCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation} using the specified AWS account credentials
     * provider and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param awsCredentialsProvider
     *            The AWS credentials provider which will provide credentials
     *            to authenticate requests with AWS services.
     * @param clientConfiguration The client configuration options controlling how this
     *                       client connects to ${serviceAbbreviation}
     *                       (ex: proxy settings, retry counts, etc.).
     * @deprecated use {@link ${metadata.syncClientBuilderClassName}#withCredentials(AwsCredentialsProvider)} and
     *             {@link ${metadata.syncClientBuilderClassName}#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public ${metadata.syncClient}(AwsCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, null);
    }

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation} using the specified AWS account credentials
     * provider, client configuration options, and request metric collector.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param awsCredentialsProvider
     *            The AWS credentials provider which will provide credentials
     *            to authenticate requests with AWS services.
     * @param clientConfiguration The client configuration options controlling how this
     *                       client connects to ${serviceAbbreviation}
     *                       (ex: proxy settings, retry counts, etc.).
     * @param requestMetricCollector optional request metric collector
     * @deprecated use {@link ${metadata.syncClientBuilderClassName}#withCredentials(AwsCredentialsProvider)} and
     *             {@link ${metadata.syncClientBuilderClassName}#withClientConfiguration(ClientConfiguration)} and
     *             {@link ${metadata.syncClientBuilderClassName}#withMetricsCollector(RequestMetricCollector)}
     */
    @Deprecated
    public ${metadata.syncClient}(AwsCredentialsProvider awsCredentialsProvider,
            ClientConfiguration clientConfiguration,
            RequestMetricCollector requestMetricCollector) {
        super(clientConfiguration, requestMetricCollector);
        this.awsCredentialsProvider = awsCredentialsProvider;
        init();
    }

    /**
     * Constructs a new client to invoke service methods on
     * ${serviceAbbreviation} using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not
     * return until the service call completes.
     *
     * @param clientParams Object providing client parameters.
     */
    ${metadata.syncClient}(AwsSyncClientParams clientParams) {
        super(clientParams);
        this.awsCredentialsProvider = clientParams.getCredentialsProvider();
        init();
    }

<@ClientInitMethodMacro.content .data_model />

<#list operations?values as operationModel>
    <@ClientMethodForOperation.content metadata operationModel/>
    <@ClientMethodForOperationWithSimpleForm.content operationModel />
</#list>

<#if AdditionalClientMethodsMacro?has_content>
    <@AdditionalClientMethodsMacro.content .data_model />
</#if>

<#assign responseMetadataClassName=customizationConfig.customResponseMetadataClassName!"ResponseMetadata" />
    /**
     * Returns additional metadata for a previously executed successful, request, typically used for
     * debugging issues where a service isn't acting as expected.  This data isn't considered part
     * of the result data returned by an operation, so it's available through this separate,
     * diagnostic interface.
     * <p>
     * Response metadata is only cached for a limited period of time, so if you need to access
     * this extra diagnostic information for an executed request, you should use this method
     * to retrieve it as soon as possible after executing the request.
     *
     * @param request
     *            The originally executed request
     *
     * @return The response metadata for the specified request, or null if none
     *         is available.
     */
    public ${responseMetadataClassName} getCachedResponseMetadata(AmazonWebServiceRequest request) {
    <#if customizationConfig.customResponseMetadataClassName?has_content>
        ResponseMetadata metadata = client.getResponseMetadataForRequest(request);
        if (metadata != null)
            return new ${responseMetadataClassName}(metadata);
        else
            return null;
    <#else>
        return client.getResponseMetadataForRequest(request);
    </#if>
    }

    <#if customizationConfig.customSignerProvider?has_content>
    @Override
    protected final software.amazon.awssdk.runtime.auth.SignerProvider createSignerProvider(Signer signer) {
        return new ${customizationConfig.customSignerProvider}(this, signer);
    }
    </#if>

    <#if customizationConfig.calculateCrc32FromCompressedData>
    @Override
    protected final boolean calculateCrc32FromCompressedData() {
        return true;
    }
    </#if>

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the
     * request level.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> invoke(Request<Y> request,
            HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
            ExecutionContext executionContext) {

        executionContext.setCredentialsProvider(CredentialUtils.getCredentialsProvider
            (request.getOriginalRequest(), awsCredentialsProvider));

        return doInvoke(request, responseHandler, executionContext);
    }

    /**
     * Invoke with no authentication. Credentials are not required and any credentials set on the
     * client or request will be ignored for this operation.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> anonymousInvoke(Request<Y> request,
            HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
            ExecutionContext executionContext) {

        return doInvoke(request, responseHandler, executionContext);
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the ExecutionContext beforehand.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> doInvoke(Request<Y> request,
            HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
            ExecutionContext executionContext) {
        request.setEndpoint(endpoint);
        request.setTimeOffset(timeOffset);

        <@ClientInvokeMethodErrorResponseHandlerCreation.content metadata customizationConfig />

        return client.execute(request, responseHandler,
                errorResponseHandler, executionContext);
    }

    <#if hasWaiters>
    @Override
    public ${metadata.syncInterface}Waiters waiters(){
        if(waiters == null) {
               synchronized (this) {
                    if(waiters == null) {
                            waiters = new ${metadata.syncInterface}Waiters(this);
                    }
               }
        }
        return waiters;
    }
    </#if>

    <#if customizationConfig.presignersFqcn??>
    @Override
    public ${customizationConfig.presignersFqcn} presigners() {
        return new ${customizationConfig.presignersFqcn}(PresignerParams.builder()
            .endpoint(endpoint)
            .credentialsProvider(awsCredentialsProvider)
            .signerProvider(getSignerProvider())
            .build());
    }
    </#if>

}
