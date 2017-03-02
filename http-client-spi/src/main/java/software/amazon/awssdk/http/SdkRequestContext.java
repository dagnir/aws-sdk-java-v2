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

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * Container for extra dependencies needed during execution of a request.
 */
public class SdkRequestContext {

    private final AwsRequestMetrics metrics;

    private SdkRequestContext(Builder builder) {
        this.metrics = builder.metrics;
    }

    /**
     * @return Object used to record request level metrics.
     */
    public AwsRequestMetrics metrics() {
        return metrics;
    }

    /**
     * @return Builder instance to construct a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    public static final class Builder {

        private AwsRequestMetrics metrics;

        private Builder() {
        }

        public Builder metrics(AwsRequestMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * @return An immutable {@link SdkRequestContext} object.
         */
        public SdkRequestContext build() {
            return new SdkRequestContext(this);
        }
    }
}
