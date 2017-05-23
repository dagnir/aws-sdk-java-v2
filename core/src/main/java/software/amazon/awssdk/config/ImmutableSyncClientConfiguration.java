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
import java.util.List;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.runtime.auth.SignerProvider;

/**
 * An implementation of {@link SyncClientConfiguration} that is guaranteed to be immutable and thread-safe.
 */
@SdkInternalApi
public final class ImmutableSyncClientConfiguration extends ImmutableClientConfiguration implements SyncClientConfiguration {

    public ImmutableSyncClientConfiguration(SyncClientConfiguration configuration) {
        super(configuration);
    }

    /**
     * Convert this synchronous client configuration into a legacy-style client params object.
     */
    @Deprecated
    @ReviewBeforeRelease("We should no longer need the client params object by GA.")
    public AwsSyncClientParams asLegacySyncClientParams() {
        return new AwsSyncClientParams() {
            @Override
            public AwsCredentialsProvider getCredentialsProvider() {
                return credentialsProvider();
            }

            @Override
            public LegacyClientConfiguration getClientConfiguration() {
                return asLegacyConfiguration();
            }

            @Override
            public RequestMetricCollector getRequestMetricCollector() {
                return metricsConfiguration().requestMetricCollector().orElse(null);
            }

            @Override
            public List<RequestHandler2> getRequestHandlers() {
                return listenerConfiguration().requestListeners();
            }

            @Override
            public SignerProvider getSignerProvider() {
                return securityConfiguration().signerProvider().orElse(null);
            }

            @Override
            public SdkHttpClient sdkHttpClient() {
                return ImmutableSyncClientConfiguration.this.httpClient();
            }

            @Override
            public URI getEndpoint() {
                return endpoint();
            }
        };
    }
}
