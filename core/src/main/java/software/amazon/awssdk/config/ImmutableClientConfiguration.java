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
import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link ClientConfiguration} that is guaranteed to be immutable and thread-safe.
 */
@SdkInternalApi
public abstract class ImmutableClientConfiguration implements ClientConfiguration {
    private final ClientTimeoutConfiguration timeoutConfiguration;
    private final ClientMarshallerConfiguration marshallerConfiguration;
    private final ClientMetricsConfiguration metricsConfiguration;
    private final ClientSecurityConfiguration securityConfiguration;
    private final ClientRetryConfiguration retryConfiguration;
    private final ClientListenerConfiguration listenerConfiguration;
    private final AwsCredentialsProvider credentialsProvider;
    private final URI endpoint;
    private final LegacyClientConfiguration legacyConfiguration;

    /**
     * Copy the provided client configuration into an immutable version.
     */
    public ImmutableClientConfiguration(ClientConfiguration configuration) {
        this.timeoutConfiguration = configuration.timeoutConfiguration();
        this.marshallerConfiguration = configuration.marshallerConfiguration();
        this.metricsConfiguration = configuration.metricsConfiguration();
        this.securityConfiguration = configuration.securityConfiguration();
        this.retryConfiguration = configuration.retryConfiguration();
        this.listenerConfiguration = configuration.listenerConfiguration();
        this.credentialsProvider = configuration.credentialsProvider();
        this.endpoint = configuration.endpoint();

        validate();

        this.legacyConfiguration = initializeLegacyConfiguration();
    }

    /**
     * Validate that the provided optional is present, raising an exception if it is not.
     */
    protected final <T> T requireField(String field, Optional<T> requiredConfiguration) {
        return requiredConfiguration.orElseThrow(() ->
                                                         new IllegalStateException(String.format(
                                                                 "The '%s' must be configured in the client builder.", field)));
    }

    /**
     * Validate that the provided optional is present, raising an exception if it is not.
     */
    protected final <T> T requireField(String field, T requiredConfiguration) {
        return Validate.notNull(requiredConfiguration, "The '%s' must be configured in the client builder.", field);
    }

    /**
     * Validate the contents of this configuration to ensure it includes all of the required fields.
     */
    private void validate() {
        // Ensure they have configured something that allows us to derive the endpoint
        Validate.validState(endpoint() != null, "The endpoint could not be determined.");

        requireField("securityConfiguration.signerProvider", securityConfiguration().signerProvider());
        requireField("credentialsProvider", credentialsProvider());
        requireField("marshallerConfiguration.gzipEnabled", marshallerConfiguration().gzipEnabled());
        requireField("metricsConfiguration.requestMetricCollector", metricsConfiguration().requestMetricCollector());
        requireField("metricsConfiguration.userAgentPrefix", metricsConfiguration().userAgentPrefix());
        requireField("metricsConfiguration.userAgentSuffix", metricsConfiguration().userAgentSuffix());
        requireField("retryConfiguration.retryPolicy", retryConfiguration().retryPolicy());
    }

    @Override
    public ClientTimeoutConfiguration timeoutConfiguration() {
        return timeoutConfiguration;
    }

    @Override
    public ClientMarshallerConfiguration marshallerConfiguration() {
        return marshallerConfiguration;
    }

    @Override
    public ClientMetricsConfiguration metricsConfiguration() {
        return metricsConfiguration;
    }

    @Override
    public ClientSecurityConfiguration securityConfiguration() {
        return securityConfiguration;
    }

    @Override
    public ClientRetryConfiguration retryConfiguration() {
        return retryConfiguration;
    }

    @Override
    public ClientListenerConfiguration listenerConfiguration() {
        return listenerConfiguration;
    }

    @Override
    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    @Override
    public URI endpoint() {
        return endpoint;
    }

    /**
     * Convert this client configuration into a legacy-style configuration object.
     */
    @Deprecated
    @ReviewBeforeRelease("This should be removed once we remove our reliance on the legacy client configuration object.")
    public LegacyClientConfiguration asLegacyConfiguration() {
        return this.legacyConfiguration;
    }

    /**
     * Convert this client configuration to a {@link LegacyClientConfiguration}.
     */
    private LegacyClientConfiguration initializeLegacyConfiguration() {
        LegacyClientConfiguration configuration = new LegacyClientConfiguration();

        copyTimeoutConfiguration(configuration, timeoutConfiguration());
        copyMarshallerConfiguration(configuration, marshallerConfiguration());
        copyMetricsConfiguration(configuration, metricsConfiguration());
        copyRetryConfiguration(configuration, retryConfiguration());

        configuration.setProtocol(schemeToProtocol(endpoint().getScheme()).orElse(Protocol.HTTPS));

        return configuration;
    }

    private void copyTimeoutConfiguration(LegacyClientConfiguration configuration,
                                          ClientTimeoutConfiguration timeoutConfiguration) {
        timeoutConfiguration.totalExecutionTimeout().ifPresent(d ->
                                                                       configuration.setClientExecutionTimeout(
                                                                               Math.toIntExact(d.toMillis())));
    }

    private void copyMarshallerConfiguration(LegacyClientConfiguration configuration,
                                             ClientMarshallerConfiguration compressionConfiguration) {
        compressionConfiguration.gzipEnabled().ifPresent(configuration::setUseGzip);
        compressionConfiguration.additionalHeaders()
                .forEach((k, v) -> {
                    configuration.addHeader(k, v.get(0));
                });
    }

    private void copyMetricsConfiguration(LegacyClientConfiguration configuration,
                                          ClientMetricsConfiguration metricsConfiguration) {
        metricsConfiguration.userAgentPrefix().ifPresent(configuration::setUserAgentPrefix);
        metricsConfiguration.userAgentSuffix().ifPresent(configuration::setUserAgentSuffix);
    }

    private void copyRetryConfiguration(LegacyClientConfiguration configuration,
                                        ClientRetryConfiguration retryConfiguration) {
        retryConfiguration.retryPolicy().ifPresent(configuration::setRetryPolicy);
    }

    private Optional<Protocol> schemeToProtocol(String scheme) {
        return Arrays.stream(Protocol.values()).filter(p -> scheme.equals(p.toString())).findFirst();
    }
}
