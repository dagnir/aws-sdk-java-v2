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

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;

/**
 * Configuration that allows manipulating the way in which the SDK converts request objects to messages to be sent to AWS.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
public final class ClientMarshallerConfiguration
        implements ToCopyableBuilder<ClientMarshallerConfiguration.Builder, ClientMarshallerConfiguration> {

    private final Map<String, List<String>> additionalHeaders;

    @ReviewBeforeRelease("Should this be included in the HTTP configuration object?")
    private final Boolean gzipEnabled;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientMarshallerConfiguration(DefaultClientMarshallerConfigurationBuilder builder) {
        this.additionalHeaders = makeHeaderMapUnmodifiable(copyHeaderMap(builder.additionalHeaders));
        this.gzipEnabled = builder.gzipEnabled;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientMarshallerConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientMarshallerConfigurationBuilder();
    }

    @Override
    public ClientMarshallerConfiguration.Builder toBuilder() {
        return builder().gzipEnabled(gzipEnabled);
    }

    /**
     * An unmodifiable representation of the set of HTTP headers that should be sent with every request. If not set, this will
     * return an empty map.
     *
     * @see Builder#additionalHeaders(Map)
     */
    public Map<String, List<String>> additionalHeaders() {
        return additionalHeaders;
    }

    /**
     * Whether GZIP should be used when communication with AWS.
     *
     * @see Builder#gzipEnabled(Boolean)
     */
    public Optional<Boolean> gzipEnabled() {
        return Optional.ofNullable(gzipEnabled);
    }

    /**
     * Perform a deep copy of the provided header map.
     */
    private static Map<String, List<String>> copyHeaderMap(Map<String, List<String>> headers) {
        return headers.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }

    /**
     * Perform a shallow copy of the provided header map, making the lists and map unmodifiable.
     */
    private static Map<String, List<String>> makeHeaderMapUnmodifiable(Map<String, List<String>> headers) {
        return unmodifiableMap(headers.entrySet().stream()
                                       .collect(toMap(Map.Entry::getKey, e -> unmodifiableList(e.getValue()))));
    }

    /**
     * A builder for {@link ClientMarshallerConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientMarshallerConfiguration> {

        /**
         * @see ClientMarshallerConfiguration#additionalHeaders().
         */
        Map<String, List<String>> additionalHeaders();

        /**
         * Define a set of headers that should be added to every HTTP request sent to AWS. This will override any headers
         * previously added to the builder.
         *
         * @see ClientMarshallerConfiguration#additionalHeaders()
         */
        Builder additionalHeaders(Map<String, List<String>> additionalHeaders);

        /**
         * Add a header that should be sent with every HTTP request to AWS. This will always add a new header to the request,
         * even
         * if that particular header had already been defined.
         *
         * <p>For example, the following code will result in two different "X-Header" values sent to AWS.</p>
         * <pre>
         * httpConfiguration.addAdditionalHeader("X-Header", "Value1");
         * httpConfiguration.addAdditionalHeader("X-Header", "Value2");
         * </pre>
         *
         * @see ClientMarshallerConfiguration#additionalHeaders()
         */
        Builder addAdditionalHeader(String header, String... values);

        /**
         * @see ClientMarshallerConfiguration#gzipEnabled().
         */
        Optional<Boolean> gzipEnabled();

        /**
         * Configure whether GZIP should be used when communicating with AWS. Enabling GZIP increases CPU utilization and memory
         * usage, while decreasing the amount of data sent over the network.
         *
         * @see ClientMarshallerConfiguration#gzipEnabled()
         */
        Builder gzipEnabled(Boolean gzipEnabled);
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientMarshallerConfigurationBuilder implements Builder {

        private Map<String, List<String>> additionalHeaders = new HashMap<>();
        private Boolean gzipEnabled;

        @Override
        public Map<String, List<String>> additionalHeaders() {
            return makeHeaderMapUnmodifiable(additionalHeaders);
        }

        @Override
        public Builder additionalHeaders(Map<String, List<String>> additionalHeaders) {
            this.additionalHeaders = copyHeaderMap(additionalHeaders);
            return this;
        }

        @Override
        public Builder addAdditionalHeader(String header, String... values) {
            List<String> currentHeaderValues = this.additionalHeaders.computeIfAbsent(header, k -> new ArrayList<>());
            Collections.addAll(currentHeaderValues, values);
            return this;
        }

        public Map<String, List<String>> getAdditionalHeaders() {
            return additionalHeaders();
        }

        public void setAdditionalHeaders(Map<String, List<String>> additionalHeaders) {
            additionalHeaders(additionalHeaders);
        }

        @Override
        public Optional<Boolean> gzipEnabled() {
            return Optional.ofNullable(gzipEnabled);
        }

        @Override
        public Builder gzipEnabled(Boolean gzipEnabled) {
            this.gzipEnabled = gzipEnabled;
            return this;
        }

        public Boolean getGzipEnabled() {
            return gzipEnabled;
        }

        public void setGzipEnabled(Boolean gzipEnabled) {
            gzipEnabled(gzipEnabled);
        }

        @Override
        public ClientMarshallerConfiguration build() {
            return new ClientMarshallerConfiguration(this);
        }
    }
}
