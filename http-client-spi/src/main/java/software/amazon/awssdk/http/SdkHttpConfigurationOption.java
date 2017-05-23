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

package software.amazon.awssdk.http;

import java.time.Duration;

public final class SdkHttpConfigurationOption<T> {

    public static final SdkHttpConfigurationOption<Duration> SOCKET_TIMEOUT =
        new SdkHttpConfigurationOption<>("SocketTimeout");

    public static final SdkHttpConfigurationOption<Duration> CONNECTION_TIMEOUT =
        new SdkHttpConfigurationOption<>("ConnectionTimeout");

    public static final SdkHttpConfigurationOption<Integer> MAX_CONNECTIONS =
        new SdkHttpConfigurationOption<>("MaxConnections");

    private final String name;

    private SdkHttpConfigurationOption(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

