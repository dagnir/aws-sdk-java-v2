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

package software.amazon.awssdk.client;

import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;

/**
 * Provides access to all params needed in a asynchronous AWS service client constructor. Abstract
 * to allow additions to the params while maintaining backwards compatibility.
 */
@SdkProtectedApi
public abstract class AwsAsyncClientParams extends AwsSyncClientParams {

    private final SdkAsyncHttpClient asyncHttpClient = NettySdkHttpClientFactory.builder().build().createHttpClient();

    public abstract ExecutorService getExecutor();

    public SdkAsyncHttpClient getAsyncHttpClient() {
        return asyncHttpClient;
    }

}
