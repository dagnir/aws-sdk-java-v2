/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.internal.http.client;

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;


/**
 * Factory interface that can be used for creating the underlying http client
 * for request execution.
 */
@SdkInternalApi
public interface HttpClientFactory<T> {

    T create(HttpClientSettings settings);

}