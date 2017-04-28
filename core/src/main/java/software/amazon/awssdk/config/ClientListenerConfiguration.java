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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;
import software.amazon.awssdk.handlers.RequestHandler2;

/**
 * Configuration that allows hooking into the lifecycle of events within the SDK.
 *
 * <p>All implementations of this interface must be immutable and thread safe.</p>
 */
public final class ClientListenerConfiguration
        implements ToCopyableBuilder<ClientListenerConfiguration.Builder, ClientListenerConfiguration> {
    private final List<RequestHandler2> requestListeners;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientListenerConfiguration(DefaultClientListenerConfigurationBuilder builder) {
        this.requestListeners = Collections.unmodifiableList(new ArrayList<>(builder.requestListeners));
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientListenerConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientListenerConfigurationBuilder();
    }

    @Override
    public ClientListenerConfiguration.Builder toBuilder() {
        return builder().requestListeners(requestListeners);
    }

    /**
     * An immutable collection of request listeners that should be hooked into the execution of each request, in the order that
     * they should be applied.
     *
     * @see Builder#requestListeners(List)
     */
    @ReviewBeforeRelease("We are probably going to update the request handler interface. The description should be updated to "
                         + "detail the functionality of the new interface.")
    public List<RequestHandler2> requestListeners() {
        return requestListeners;
    }

    /**
     * A builder for {@link ClientListenerConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientListenerConfiguration> {
        /**
         * @see ClientListenerConfiguration#requestListeners().
         */
        List<RequestHandler2> requestListeners();

        /**
         * Configure an immutable collection of request listeners that should be hooked into the execution of each request, in
         * the order that they should be applied. These will override any listeners already configured.
         *
         * @see ClientListenerConfiguration#requestListeners()
         */
        Builder requestListeners(List<RequestHandler2> requestListeners);

        /**
         * Add a request listener that will be hooked into the execution of each request after the listeners that have previously
         * been configured have all been executed.
         *
         * @see ClientListenerConfiguration#requestListeners()
         */
        Builder addRequestListener(RequestHandler2 requestListener);
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientListenerConfigurationBuilder implements Builder {
        private List<RequestHandler2> requestListeners = new ArrayList<>();

        @Override
        public List<RequestHandler2> requestListeners() {
            return Collections.unmodifiableList(requestListeners);
        }

        @Override
        public Builder requestListeners(List<RequestHandler2> requestListeners) {
            this.requestListeners.clear();
            this.requestListeners.addAll(requestListeners);
            return this;
        }

        @Override
        public Builder addRequestListener(RequestHandler2 requestListener) {
            this.requestListeners.add(requestListener);
            return this;
        }

        public List<RequestHandler2> getRequestListeners() {
            return requestListeners();
        }

        public void setRequestListeners(List<RequestHandler2> requestListeners) {
            requestListeners(requestListeners);
        }

        @Override
        public ClientListenerConfiguration build() {
            return new ClientListenerConfiguration(this);
        }
    }
}
