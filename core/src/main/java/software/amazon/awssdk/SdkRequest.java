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

import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The base class for all SDK requests.
 *
 * @see SdkResponse
 */
public abstract class SdkRequest<B extends SdkRequest.Builder<B, R>, R extends SdkRequest<B, R>> implements ToCopyableBuilder<B, R> {

    private final ProgressListener progressListener;

    protected SdkRequest(BuilderImpl<B, R> builder) {
        progressListener = builder.progressListener;
    }

    final public Optional<ProgressListener> progressListener() {
        return Optional.ofNullable(progressListener);
    }

    public interface Builder<B extends SdkRequest.Builder<B, R>, R extends SdkRequest<B, R>> extends CopyableBuilder<B, R> {
        B progressListener(ProgressListener progressListener);
    }

    protected static abstract class BuilderImpl<B extends Builder<B, R>, R extends SdkRequest<B, R>> implements Builder<B, R> {
        private final Class<? extends B> concrete;

        private ProgressListener progressListener;

        protected BuilderImpl(Class<? extends B> concrete) {
            this.concrete = concrete;
        }

        protected BuilderImpl(Class<? extends B> concrete, SdkRequest<B, R> request) {
            this(concrete);
            this.progressListener = request.progressListener;
        }

        @Override
        public B progressListener(ProgressListener progressListener) {
            this.progressListener = progressListener;
            return concrete.cast(this);
        }
    }
}
