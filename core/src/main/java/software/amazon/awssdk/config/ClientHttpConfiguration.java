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

/**
 * Configures the behavior of the AWS SDK HTTP client.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
@ReviewBeforeRelease("Configuration descriptions here are relatively short because it is expected that this will be "
                     + "heavily refactored for the pluggable HTTP layer. If that ends up not happening, these descriptions "
                     + "should be enhanced.")
public final class ClientHttpConfiguration {
    private final Map<String, List<String>> additionalHeaders;
    private final Boolean expectContinueEnabled;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientHttpConfiguration(DefaultClientHttpConfigurationBuilder builder) {
        this.additionalHeaders = makeHeaderMapUnmodifiable(copyHeaderMap(builder.additionalHeaders));
        this.expectContinueEnabled = builder.expectContinueEnabled;
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientHttpConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientHttpConfigurationBuilder();
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
     * Whether the client should send an HTTP expect-continue handshake before a request.
     *
     * @see Builder#expectContinueEnabled(Boolean)
     */
    public Optional<Boolean> expectContinueEnabled() {
        return Optional.ofNullable(expectContinueEnabled);
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
     * A builder for {@link ClientHttpConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    interface Builder {
        /**
         * @see ClientHttpConfiguration#additionalHeaders().
         */
        Map<String, List<String>> additionalHeaders();

        /**
         * Define a set of headers that should be added to every HTTP request sent to AWS. This will override any headers
         * previously added to the builder.
         *
         * @see ClientHttpConfiguration#additionalHeaders()
         */
        Builder additionalHeaders(Map<String, List<String>> additionalHeaders);

        /**
         * Add a header that should be sent with every HTTP request to AWS. This will always add a new header to the request, even
         * if that particular header had already been defined.
         *
         * <p>For example, the following code will result in two different "X-Header" values sent to AWS.</p>
         * <pre>
         * httpConfiguration.addAdditionalHeader("X-Header", "Value1");
         * httpConfiguration.addAdditionalHeader("X-Header", "Value2");
         * </pre>
         *
         * @see ClientHttpConfiguration#additionalHeaders()
         */
        Builder addAdditionalHeader(String header, String... values);

        /**
         * @see ClientHttpConfiguration#expectContinueEnabled().
         */
        Optional<Boolean> expectContinueEnabled();

        /**
         * Configure whether the client should send an HTTP expect-continue handshake before each request.
         *
         * @see ClientHttpConfiguration#expectContinueEnabled()
         */
        Builder expectContinueEnabled(Boolean expectContinueEnabled);

        /**
         * Build a {@link ClientHttpConfiguration} from the values currently configured in this builder.
         */
        ClientHttpConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link ClientIpConfiguration.Builder}.
     */
    private static final class DefaultClientHttpConfigurationBuilder implements Builder {
        private Map<String, List<String>> additionalHeaders = new HashMap<>();
        private Boolean expectContinueEnabled;

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
        public Optional<Boolean> expectContinueEnabled() {
            return Optional.ofNullable(expectContinueEnabled);
        }

        @Override
        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public Boolean getExpectContinueEnabled() {
            return expectContinueEnabled;
        }

        public void setExpectContinueEnabled(Boolean expectContinueEnabled) {
            expectContinueEnabled(expectContinueEnabled);
        }

        @Override
        public ClientHttpConfiguration build() {
            return new ClientHttpConfiguration(this);
        }
    }
}
