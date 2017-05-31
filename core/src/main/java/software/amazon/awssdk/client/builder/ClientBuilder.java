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

package software.amazon.awssdk.client.builder;

import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.builder.SdkBuilder;
import software.amazon.awssdk.config.ClientListenerConfiguration;
import software.amazon.awssdk.config.ClientMarshallerConfiguration;
import software.amazon.awssdk.config.ClientMetricsConfiguration;
import software.amazon.awssdk.config.ClientRetryConfiguration;
import software.amazon.awssdk.config.ClientSecurityConfiguration;
import software.amazon.awssdk.config.ClientTimeoutConfiguration;

/**
 * This includes required and optional override configuration required by every client builder. An instance can be acquired by
 * calling the static "builder" method on the type of client you wish to create.
 *
 * <p>Implementations of this interface are mutable and not thread-safe.</p>
 *
 * @param <B> The type of builder that should be returned by the fluent builder methods in this interface.
 * @param <C> The type of client generated by this builder.
 */
public interface ClientBuilder<B extends ClientBuilder<B, C>, C> extends SdkBuilder<B, C> {

    /**
     * Override configuration specifying request and response timeouts within the SDK. This will never return null.
     */
    ClientTimeoutConfiguration timeoutConfiguration();

    /**
     * Configure overrides specifying request and response timeouts within the SDK.
     */
    B timeoutConfiguration(ClientTimeoutConfiguration timeoutConfiguration);

    /**
     * Override configuration related to converting request objects to data that should be transmitted. This will never return
     * null.
     */
    ClientMarshallerConfiguration marshallerConfiguration();

    /**
     * Configure overrides related to converting request objects to data that should be transmitted.
     */
    B marshallerConfiguration(ClientMarshallerConfiguration marshallerConfiguration);

    /**
     * Override configuration related to metrics gathered by the SDK. This will never return null.
     */
    ClientMetricsConfiguration metricsConfiguration();

    /**
     * Configure overrides related to metrics gathered by the SDK.
     */
    B metricsConfiguration(ClientMetricsConfiguration metricsConfiguration);

    /**
     * Override configuration related to the security of the integration with AWS. This will never return null.
     */
    ClientSecurityConfiguration securityConfiguration();

    /**
     * Configure overrides related to the security of the integration with AWS.
     */
    B securityConfiguration(ClientSecurityConfiguration securityConfiguration);

    /**
     * Override configuration related to the automatic request retry behavior of the SDK. This will never return null.
     */
    ClientRetryConfiguration retryConfiguration();

    /**
     * Configure overrides related to the automatic request retry behavior of the SDK.
     */
    B retryConfiguration(ClientRetryConfiguration retryConfiguration);

    /**
     * Override configuration related to behavioral hooks provided within the SDK. This will never return null.
     */
    ClientListenerConfiguration listenerConfiguration();

    /**
     * Configure overrides related to behavioral hooks provided within the SDK.
     */
    B listenerConfiguration(ClientListenerConfiguration listenerConfiguration);

    /**
     * Configures the HTTP client used by the service client. Either a client factory may be provided (in which case
     * the SDK will merge any service specific configuration on top of customer supplied configuration) or provide an already
     * constructed instance of {@link software.amazon.awssdk.http.SdkHttpClient}. Note that if an {@link
     * software.amazon.awssdk.http.SdkHttpClient} is provided then it is up to the caller to close it when they are finished with
     * it, the SDK will only close HTTP clients that it creates.
     */
    B httpConfiguration(ClientHttpConfiguration httpConfiguration);

    /**
     * HTTP client configuration. This will never return null.
     */
    ClientHttpConfiguration httpConfiguration();

    /**
     * The credentials that should be used to authenticate the service with AWS.
     */
    @ReviewBeforeRelease("This is AWS-specific, so it should probably be broken out.")
    Optional<AwsCredentialsProvider> credentialsProvider();

    /**
     * Configure the credentials that should be used to authenticate with AWS.
     *
     * <p>The default provider will attempt to identify the credentials automatically using the following checks:
     * <ul>
     *   <li>Java System Properties - <code>aws.accessKeyId</code> and <code>aws.secretKey</code></li>
     *   <li>Environment Variables - <code>AWS_ACCESS_KEY_ID</code> and <code>AWS_SECRET_ACCESS_KEY</code></li>
     *   <li>Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI</li>
     *   <li>Credentials delivered through the Amazon EC2 container service if AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" environment
     *   variable is set and security manager has permission to access the variable,</li>
     *   <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
     * </ul>
     *
     * <p>If the credentials are not found in any of the locations above, an exception will be thrown at {@link #build()} time.
     * </p>
     */
    B credentialsProvider(AwsCredentialsProvider credentialsProvider);

    /**
     * Retrieve the value set with {@link #endpointOverride(URI)}.
     */
    Optional<URI> endpointOverride();

    /**
     * Configure the endpoint with which the SDK should communicate. This will take precedent over the endpoint derived from the
     * {@link #region()}. Even when this is used, the {@link #region(String)} must still be specified for the purposes of message
     * signing.
     */
    B endpointOverride(URI endpointOverride);

    /**
     * Retrieve the value set by {@link #defaultRegionDetectionEnabled(Boolean)}.
     */
    Optional<Boolean> defaultRegionDetectionEnabled();

    /**
     * Whether region detection should be enabled. Region detection is used when the {@link #region()} is not specified. This is
     * enabled by default.
     *
     * <p>By default, this will attempt to identify the endpoint automatically using the following logic:
     * <ol>
     *     <li>Check the 'aws.defaultRegion' system property for the region.</li>
     *     <li>Check the 'AWS_DEFAULT_REGION' environment variable for the region.</li>
     *     <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the region.</li>
     *     <li>If running in EC2, check the EC2 metadata service for the region.</li>
     * </ol>
     * </p>
     *
     * <p>If the region is not found in any of the locations above, an exception will be thrown at {@link #build()} time.</p>
     */
    @ReviewBeforeRelease("Should this be moved to an advancedOptions map to hide it from most customers who don't care about it?")
    B defaultRegionDetectionEnabled(Boolean defaultRegionDetectionEnabled);

    /**
     * Retrieve the region that was configured with {@link #region(String)}.
     */
    Optional<String> region();

    /**
     * Configure the region with which the SDK should communicate. If this is not specified when creating a client, the
     * behavior described in {@link #defaultRegionDetectionEnabled(Boolean)} (assuming it is enabled) will be used.
     */
    B region(String region);

}
