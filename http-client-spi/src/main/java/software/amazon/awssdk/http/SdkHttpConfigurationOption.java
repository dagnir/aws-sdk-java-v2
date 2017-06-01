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
import software.amazon.awssdk.annotation.SdkProtectedApi;

/**
 * Type safe key for an HTTP related configuration option. These options are used for service specific configuration
 * and are treated as hints for the underlying HTTP implementation for better defaults. If an implementation does not support
 * a particular option, they are free to ignore it.
 *
 * @param <T> Type of option
 * @see SdkHttpConfigurationOptions
 */
@SdkProtectedApi
public final class SdkHttpConfigurationOption<T> {

    /**
     * Timeout for each read to the underlying socket.
     */
    public static final SdkHttpConfigurationOption<Duration> SOCKET_TIMEOUT =
            new SdkHttpConfigurationOption<>("SocketTimeout");

    /**
     * Timeout for establishing a connection to a remote service.
     */
    public static final SdkHttpConfigurationOption<Duration> CONNECTION_TIMEOUT =
            new SdkHttpConfigurationOption<>("ConnectionTimeout");

    /**
     * Maximum number of connections allowed in a connection pool.
     */
    public static final SdkHttpConfigurationOption<Integer> MAX_CONNECTIONS =
            new SdkHttpConfigurationOption<>("MaxConnections");

    private final String name;

    private SdkHttpConfigurationOption(String name) {
        this.name = name;
    }

    /**
     * Note that the name is mainly used for debugging purposes. Two option key objects with the same name do not represent
     * the same option. Option keys are compared by reference when obtaining a value from {@link SdkHttpConfigurationOptions}.
     *
     * @return Name of this option key.
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

