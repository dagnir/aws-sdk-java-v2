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

package software.amazon.awssdk.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Base per-request override configuration for all SDK requests.
 */
public abstract class SdkRequestOverrideConfig {

    private final Map<String, String> additionalHeaders;

    private final Map<String, List<String>> additionalQueryParameters;

    private final Duration clientExecutionTimeout;

    private final String appendUserAgent;

    private ReadLimitInfo readLimitInfo;

    protected SdkRequestOverrideConfig(Builder<?> builder) {
        this.additionalHeaders = builder.additionalHeaders();
        this.additionalQueryParameters = builder.additionalQueryParameters();
        this.clientExecutionTimeout = builder.clientExecutionTimeout();
        this.appendUserAgent = builder.appendUserAgent();
        this.readLimitInfo = builder.readLimitInfo();
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

    public Optional<String> appendUserAgent() {
        return Optional.ofNullable(appendUserAgent);
    }

    public Optional<ReadLimitInfo> readLimitInfo() {
        return Optional.ofNullable(readLimitInfo);
    }

    public abstract Builder<? extends Builder> toBuilder();

    public interface Builder<B extends Builder> {
        Map<String, String> additionalHeaders();

        B additionalHeader(String name, String value);

        B additionalHeaders(Map<String, String> customHeaders);

        Map<String, List<String>> additionalQueryParameters();

        B additionalQueryParameter(String name, String value);

        B additionalQueryParameters(Map<String, List<String>> customQueryParameters);

        Duration clientExecutionTimeout();

        B clientExecutionTimeout(Duration clientExecutionTimeout);

        String appendUserAgent();

        B appendUserAgent(String appendUserAgent);

        ReadLimitInfo readLimitInfo();

        B readLimitInfo(ReadLimitInfo readLimitInfo);
    }

    protected abstract static class BuilderImpl<B extends Builder> implements Builder<B> {
        private Map<String, String> additionalHeaders;

        private Map<String, List<String>> additionalQueryParameters;

        private Duration clientExecutionTimeout;

        private String appendUserAgent;

        private ReadLimitInfo readLimitInfo;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkRequestOverrideConfig sdkRequestOverrideConfig) {
            sdkRequestOverrideConfig.additionalHeaders().ifPresent(this::additionalHeaders);
            sdkRequestOverrideConfig.additionalQueryParameters().ifPresent(this::additionalQueryParameters);
            sdkRequestOverrideConfig.clientExecutionTimeout().ifPresent(this::clientExecutionTimeout);
            sdkRequestOverrideConfig.appendUserAgent().ifPresent(this::appendUserAgent);
            sdkRequestOverrideConfig.readLimitInfo().ifPresent(this::readLimitInfo);
        }

        @Override
        public Map<String, String> additionalHeaders() {
            return additionalHeaders;
        }

        @Override
        public B additionalHeader(String name, String value) {
            if (additionalHeaders == null) {
                additionalHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            }
            additionalHeaders.put(name, value);
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B additionalHeaders(Map<String, String> customHeaders) {
            if (customHeaders == null) {
                this.additionalHeaders = null;
            } else {
                this.additionalHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                this.additionalHeaders.putAll(customHeaders);
            }
            return (B) this;
        }

        @Override
        public B additionalQueryParameter(String name, String value) {
            if (additionalQueryParameters == null) {
                additionalQueryParameters = new HashMap<>();
            }
            additionalQueryParameters.computeIfAbsent(name, n -> new ArrayList<>())
                    .add(value);
            return (B) this;
        }

        @Override
        public Map<String, List<String>> additionalQueryParameters() {
            return additionalQueryParameters;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B additionalQueryParameters(Map<String, List<String>> customQueryParameters) {
            if (customQueryParameters == null) {
                this.additionalQueryParameters = null;
            } else {
                this.additionalQueryParameters = customQueryParameters.entrySet().stream()
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

        @Override
        @SuppressWarnings("unchecked")
        public B readLimitInfo(ReadLimitInfo readLimitInfo) {
            this.readLimitInfo = readLimitInfo;
            return (B) this;
        }

        @Override
        public ReadLimitInfo readLimitInfo() {
            return readLimitInfo;
        }

        @Override
        public String appendUserAgent() {
            return appendUserAgent;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B appendUserAgent(String appendUserAgent) {
            this.appendUserAgent = appendUserAgent;
            return (B) this;
        }

        public abstract SdkRequestOverrideConfig build();
    }
}
