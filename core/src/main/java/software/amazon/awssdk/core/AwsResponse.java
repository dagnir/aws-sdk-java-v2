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

import java.util.Optional;

import software.amazon.awssdk.core.SdkResponse;

/**
 * Base class for all AWS Service responses.
 */
public abstract class AwsResponse implements SdkResponse {
    private final AwsResponseMetadata awsResponseMetadata;

    protected AwsResponse(Builder builder) {
        this.awsResponseMetadata = builder.responseMetadata();
    }

    @Override
    public Optional<AwsResponseMetadata> responseMetadata() {
        return Optional.ofNullable(awsResponseMetadata);
    }

    @Override
    public abstract Builder toBuilder();

    protected interface Builder extends SdkResponse.Builder {
        @Override
        AwsResponse build();

        AwsResponseMetadata responseMetadata();

        Builder responseMetadata(AwsResponseMetadata awsResponseMetadata);
    }

    protected abstract static class BuilderImpl implements Builder {
        private AwsResponseMetadata awsResponseMetadata;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsResponse response) {
            response.responseMetadata().map(this::responseMetadata);
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata awsResponseMetadata) {
            this.awsResponseMetadata = awsResponseMetadata;
            return this;
        }

        @Override
        public AwsResponseMetadata responseMetadata() {
            return awsResponseMetadata;
        }
    }
}
