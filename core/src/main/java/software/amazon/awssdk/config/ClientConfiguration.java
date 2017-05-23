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

package software.amazon.awssdk.config;

import java.net.URI;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * An interface that represents all configuration required by an AWS client in order to operate. AWS clients accept
 * implementations of the child interfaces ({@link AsyncClientConfiguration} or {@link SyncClientConfiguration}) when
 * constructed.
 *
 * <p>Implementations of this interface are not necessarily immutable or thread safe. If thread safety is required, consider
 * creating an immutable representation with {@link ImmutableClientConfiguration}.</p>
 */
@SdkInternalApi
@ReviewBeforeRelease("Do we want to have all optional Client*Configuration objects merged under one 'ClientOverrideConfig', to "
                     + "make it easier to find the required configuration, like endpoint? This would also make it clear why "
                     + "the credential configuration is separated from the other security configuration.")
public interface ClientConfiguration {

    /**
     * Override configuration specifying request and response timeouts within the SDK. This will never return null.
     */
    ClientTimeoutConfiguration timeoutConfiguration();

    /**
     * Override configuration related to converting request objects to data that should be transmitted. This will never return
     * null.
     */
    ClientMarshallerConfiguration marshallerConfiguration();

    /**
     * Override configuration related to metrics gathered by the SDK. This will never return null.
     */
    ClientMetricsConfiguration metricsConfiguration();

    /**
     * Override configuration related to the security of the integration with AWS. This will never return null.
     */
    ClientSecurityConfiguration securityConfiguration();

    /**
     * Override configuration related to the automatic request retry behavior of the SDK. This will never return null.
     */
    ClientRetryConfiguration retryConfiguration();

    /**
     * Override configuration related to behavioral hooks provided within the SDK. This will never return null.
     */
    ClientListenerConfiguration listenerConfiguration();

    /**
     * The credentials that should be used to authenticate the service with AWS.
     */
    @ReviewBeforeRelease("This is AWS-specific, so it should probably be broken out.")
    AwsCredentialsProvider credentialsProvider();

    /**
     * The endpoint with which the SDK should communicate.
     */
    URI endpoint();

    /**
     * The HTTP client the SDK will use to make HTTP requests.
     */
    SdkHttpClient httpClient();
}
