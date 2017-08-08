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

import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The base class for all SDK responses.
 *
 * @see SdkRequest
 */
public abstract class SdkResponse<B extends SdkResponse.Builder<B, R>, R extends SdkResponse<B, R>> implements ToCopyableBuilder<B, R> {

    protected SdkResponse(BuilderImpl<B, R> builder) {
    }

    public interface Builder<B extends SdkResponse.Builder<B, R>, R extends SdkResponse<B, R>> extends CopyableBuilder<B, R> {
    }

    protected static abstract class BuilderImpl<B extends Builder<B, R>, R extends SdkResponse<B, R>> implements Builder<B, R> {
        private final Class<? extends B> concrete;

        protected BuilderImpl(Class<? extends B> concrete) {
            this.concrete = concrete;
        }

        protected BuilderImpl(Class<? extends B> concrete, SdkResponse<B, R> request) {
            this(concrete);
        }
    }
}