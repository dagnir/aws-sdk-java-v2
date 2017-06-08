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

package software.amazon.awssdk.http.loader;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOptions;
import software.amazon.awssdk.http.SdkHttpService;

/**
 * Utility to load the default HTTP client factory and create an instance of {@link SdkHttpClient}.
 */
public class DefaultSdkHttpClientFactory implements SdkHttpClientFactory {

    private static final SdkHttpServiceProvider DEFAULT_CHAIN = new CachingSdkHttpServiceProvider(
            new SdkHttpServiceProviderChain(new SystemPropertyHttpServiceProvider(), new ClasspathSdkHttpServiceProvider()));

    @Override
    public SdkHttpClient createHttpClientWithDefaults(SdkHttpConfigurationOptions serviceDefaults) {
        // TODO We create and SdkHttpClientFactory every time. Do we want to cache it instead of the service binding?
        return DEFAULT_CHAIN
                .loadService()
                .map(SdkHttpService::createHttpClientFactory)
                .map(f -> f.createHttpClientWithDefaults(serviceDefaults))
                .orElseThrow(
                    () -> new SdkClientException("Unable to load an HTTP implementation from any provider in the chain. " +
                                                 "You must declare a dependency on an appropriate HTTP implementation or " +
                                                 " pass in an SdkHttpClient explicitly to the client builder."));
    }

}
