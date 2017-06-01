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

package software.amazon.awssdk.services.swf;

import java.time.Duration;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpConfigurationOptions;

@SdkInternalApi
class SwfHttpConfigurationOptions {

    static final SdkHttpConfigurationOptions OPTIONS = SdkHttpConfigurationOptions
            .builder()
            .option(SdkHttpConfigurationOption.SOCKET_TIMEOUT, Duration.ofMillis(90_000))
            .option(SdkHttpConfigurationOption.MAX_CONNECTIONS, 1000)
            .build();
}
