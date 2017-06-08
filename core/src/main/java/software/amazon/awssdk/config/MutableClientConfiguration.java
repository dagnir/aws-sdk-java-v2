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

import java.net.URI;
import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * An implementation of {@link ClientConfiguration}, {@link SyncClientConfiguration} and {@link AsyncClientConfiguration} that
 * provides fluent write and read methods for all configuration properties.
 *
 * <p>This class is mutable and not thread safe.</p>
 */
@SdkInternalApi
public final class MutableClientConfiguration
        implements ClientConfiguration, SyncClientConfiguration, AsyncClientConfiguration, Cloneable {

    // ClientConfiguration
    private ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder().build();
    private AwsCredentialsProvider credentialsProvider;
    private URI endpoint;
    private SdkHttpClient sdkHttpClient;

    // AsyncClientConfiguration
    private ExecutorService asyncExecutorService;

    @Override
    public final ClientOverrideConfiguration overrideConfiguration() {
        return overrideConfiguration;
    }

    public final MutableClientConfiguration overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
        this.overrideConfiguration = overrideConfiguration;
        return this;
    }

    @Override
    public final AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public final MutableClientConfiguration credentialsProvider(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    @Override
    public final URI endpoint() {
        return endpoint;
    }

    public final MutableClientConfiguration endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    @Override
    public final ExecutorService asyncExecutorService() {
        return asyncExecutorService;
    }

    public final MutableClientConfiguration asyncExecutorService(ExecutorService executorService) {
        this.asyncExecutorService = executorService;
        return this;
    }

    @Override
    public final SdkHttpClient httpClient() {
        return sdkHttpClient;
    }

    public final MutableClientConfiguration httpClient(SdkHttpClient sdkHttpClient) {
        this.sdkHttpClient = sdkHttpClient;
        return this;
    }

    @Override
    public final MutableClientConfiguration clone() {
        try {
            return (MutableClientConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone not supported on cloneable object.", e);
        }
    }
}
