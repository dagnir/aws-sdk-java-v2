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

package software.amazon.awssdk.http;

import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.util.CapacityManager;

/**
 * Client scoped dependencies of {@link AmazonHttpClient}. May be injected into constructors of {@link
 * software.amazon.awssdk.http.pipeline.RequestPipeline} implementations by
 * {@link software.amazon.awssdk.http.pipeline.RequestPipelineBuilder}.
 */
public class HttpClientDependencies {

    private final LegacyClientConfiguration config;
    private final RetryPolicy retryPolicy;
    private final HttpClientSettings httpClientSettings;
    private final CapacityManager retryCapacity;
    private final SdkHttpClient sdkHttpClient;
    private final ClientExecutionTimer clientExecutionTimer;

    /**
     * Time offset may be mutated by {@link software.amazon.awssdk.http.pipeline.RequestPipeline} implementations
     * if a clock skew is detected.
     */
    private volatile int timeOffset = SdkGlobalTime.getGlobalTimeOffset();

    private HttpClientDependencies(Builder builder) {
        this.config = builder.config;
        this.retryPolicy = builder.retryPolicy;
        this.httpClientSettings = builder.httpClientSettings;
        this.retryCapacity = builder.retryCapacity;
        this.sdkHttpClient = builder.sdkHttpClient;
        this.clientExecutionTimer = builder.clientExecutionTimer;
    }

    /**
     * Create a {@link Builder}, used to create a {@link RequestExecutionContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return {@link LegacyClientConfiguration} object provided by generated client.
     */
    public LegacyClientConfiguration config() {
        return config;
    }

    /**
     * @return The {@link RetryPolicy} configured for the client.
     */
    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    /**
     * @return CapacityManager object used for retry throttling.
     */
    public CapacityManager retryCapacity() {
        return retryCapacity;
    }

    /**
     * Returns adapted {@link HttpClientSettings} from {@link LegacyClientConfiguration} and
     * static settings from specific services.
     *
     * @return {@link HttpClientSettings}
     */
    public HttpClientSettings httpClientSettings() {
        return httpClientSettings;
    }

    /**
     * @return SdkHttpClient implementation to make an HTTP request.
     */
    public SdkHttpClient sdkHttpClient() {
        return sdkHttpClient;
    }

    /**
     * @return Controller for the ClientExecution timeout feature.
     */
    public ClientExecutionTimer clientExecutionTimer() {
        return clientExecutionTimer;
    }

    /**
     * @return Current time offset. This is mutable and should not be cached.
     */
    public int timeOffset() {
        return timeOffset;
    }

    /**
     * Updates the time offset of the client as well as the global time offset.
     */
    public void updateTimeOffset(int timeOffset) {
        this.timeOffset = timeOffset;
        // TODO think about why we update global. I assume because it's more likely to have the client's clock skewed.
        SdkGlobalTime.setGlobalTimeOffset(timeOffset);
    }


    /**
     * Builder for {@link HttpClientDependencies}.
     */
    public static final class Builder {

        private LegacyClientConfiguration config;
        private RetryPolicy retryPolicy;
        private HttpClientSettings httpClientSettings;
        private CapacityManager retryCapacity;
        private SdkHttpClient sdkHttpClient;
        private ClientExecutionTimer clientExecutionTimer;

        public Builder config(LegacyClientConfiguration config) {
            this.config = config;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder httpClientSettings(HttpClientSettings httpClientSettings) {
            this.httpClientSettings = httpClientSettings;
            return this;
        }

        public Builder retryCapacity(CapacityManager retryCapacity) {
            this.retryCapacity = retryCapacity;
            return this;
        }

        public Builder sdkHttpClient(SdkHttpClient sdkHttpClient) {
            this.sdkHttpClient = sdkHttpClient;
            return this;
        }

        public Builder clientExecutionTimer(ClientExecutionTimer clientExecutionTimer) {
            this.clientExecutionTimer = clientExecutionTimer;
            return this;
        }

        public HttpClientDependencies build() {
            return new HttpClientDependencies(this);
        }
    }
}
