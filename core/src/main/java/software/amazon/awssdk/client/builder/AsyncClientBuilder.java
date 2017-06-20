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

package software.amazon.awssdk.client.builder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This includes required and optional override configuration required by every async client builder. An instance can be acquired
 * by calling the static "builder" method on the type of async client you wish to create.
 *
 * <p>Implementations of this interface are mutable and not thread-safe.</p>
 *
 * @param <B> The type of builder that should be returned by the fluent builder methods in this interface.
 * @param <C> The type of client generated by this builder.
 */
public interface AsyncClientBuilder<B extends AsyncClientBuilder<B, C>, C>
        extends ClientBuilder<B, C> {
    /**
     * Configure the executor service provider that generates {@link ScheduledExecutorService} instances to queue
     * up async tasks in the client. This executor is used for various processes within the async client like queueing up
     * retries, it is not used to make or process the actual HTTP request (that's handled by the Async HTTP implementation
     * and can be configured via {@link #asyncHttpConfiguration(ClientAsyncHttpConfiguration)} as the implementation allows).
     *
     * <p>
     * A new {@link ExecutorService} will be created from this provider each time {@link ClientBuilder#build()} is invoked.
     * </p>
     */
    B asyncExecutorProvider(ExecutorProvider asyncExecutorProvider);

    /**
     * Configures the HTTP client used by the service client. Either a client factory may be provided (in which case
     * the SDK will merge any service specific configuration on top of customer supplied configuration) or provide an already
     * constructed instance of {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient}. Note that if an {@link
     * software.amazon.awssdk.http.async.SdkAsyncHttpClient} is provided then it is up to the caller to close it when they are
     * finished with it, the SDK will only close HTTP clients that it creates.
     */
    B asyncHttpConfiguration(ClientAsyncHttpConfiguration asyncHttpConfiguration);
}
