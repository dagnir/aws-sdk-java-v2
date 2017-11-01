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

package software.amazon.awssdk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import software.amazon.awssdk.core.event.ProgressListener;

/**
 * Base per-request override configuration for all SDK requests.
 */
public abstract class SdkRequestOverrideConfig {

    private final ProgressListener progressListener;

    private final Map<String, String> additionalHeaders;

    private final Map<String, List<String>> additionalQueryParameters;

    private final Duration clientExecutionTimeout;

    protected SdkRequestOverrideConfig(Builder<?> builder) {
        this.progressListener = builder.progressListener();
        this.additionalHeaders = builder.additionalHeaders();
        this.additionalQueryParameters = builder.additionalQueryParameters();
        this.clientExecutionTimeout = builder.clientExecutionTimeout();
    }

    public Optional<ProgressListener> progressListener() {
        return Optional.ofNullable(progressListener);
    }

    public Optional<Map<String, String>> additionalHeaders() {
        return Optional.ofNullable(additionalHeaders);
    }

    public Optional<Map<String, List<String>>> additionalQueryParameters() {
        return Optional.ofNullable(additionalQueryParameters);
    }

    public Optional<Duration> clientExecutionTimeout() {
        return Optional.ofNullable(clientExecutionTimeout);
    }

    public abstract Builder<? extends Builder> toBuilder();

    public interface Builder<B extends Builder> {
        ProgressListener progressListener();

        B progressListener(ProgressListener progressListener);

        Map<String, String> additionalHeaders();

        B additionalHeaders(Map<String, String> customHeaders);

        Map<String, List<String>> additionalQueryParameters();

        B additionalQueryParameters(Map<String, List<String>> customQueryParameters);

        Duration clientExecutionTimeout();

        B clientExecutionTimeout(Duration clientExecutionTimeout);
    }

    protected abstract static class BuilderImpl<B extends Builder> implements Builder<B> {

        private ProgressListener progressListener;

        private Map<String, String> customHeaders;

        private Map<String, List<String>> customQueryParameters;

        private Duration clientExecutionTimeout;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkRequestOverrideConfig sdkRequestOverrideConfig) {
            sdkRequestOverrideConfig.progressListener().map(this::progressListener);
            sdkRequestOverrideConfig.additionalHeaders().map(this::additionalHeaders);
            sdkRequestOverrideConfig.additionalQueryParameters().map(this::additionalQueryParameters);
        }

        @Override
        public ProgressListener progressListener() {
            return progressListener;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B progressListener(ProgressListener progressListener) {
            this.progressListener = progressListener;
            return (B) this;
        }

        @Override
        public Map<String, String> additionalHeaders() {
            return customHeaders;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B additionalHeaders(Map<String, String> customHeaders) {
            if (customHeaders == null) {
                this.customHeaders = null;
            } else {
                this.customHeaders = new HashMap<>(customHeaders);
            }
            return (B) this;
        }

        @Override
        public Map<String, List<String>> additionalQueryParameters() {
            return customQueryParameters;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B additionalQueryParameters(Map<String, List<String>> customQueryParameters) {
            if (customQueryParameters == null) {
                this.customQueryParameters = null;
            } else {
                this.customQueryParameters = customQueryParameters.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
            }
            return (B) this;
        }

        @Override
        public Duration clientExecutionTimeout() {
            return clientExecutionTimeout;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B clientExecutionTimeout(Duration clientExecutionTimeout) {
            this.clientExecutionTimeout = clientExecutionTimeout;
            return (B) this;
        }

        public abstract SdkRequestOverrideConfig build();
    }
}
