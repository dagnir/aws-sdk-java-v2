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

import java.util.Optional;

import software.amazon.awssdk.core.SdkRequest;

/**
 * Base class for all AWS Service requests.
 */
public abstract class AwsRequest implements SdkRequest {
    private final AwsRequestOverrideConfig requestOverrideConfig;

    protected AwsRequest(Builder builder) {
        this.requestOverrideConfig = builder.requestOverrideConfig();
    }

    @Override
    public final Optional<AwsRequestOverrideConfig> requestOverrideConfig() {
        return Optional.ofNullable(requestOverrideConfig);
    }

    @Override
    public abstract Builder toBuilder();

    protected interface Builder extends SdkRequest.Builder {
        @Override
        AwsRequestOverrideConfig requestOverrideConfig();

        Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig);

        @Override
        AwsRequest build();
    }

    protected abstract static class BuilderImpl implements Builder {
        private AwsRequestOverrideConfig awsRequestOverrideConfig;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsRequest request) {
            this.awsRequestOverrideConfig = request.requestOverrideConfig;
        }

        @Override
        public Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig) {
            this.awsRequestOverrideConfig = awsRequestOverrideConfig;
            return this;
        }

        @Override
        public final AwsRequestOverrideConfig requestOverrideConfig() {
            return awsRequestOverrideConfig;
        }
    }
}
