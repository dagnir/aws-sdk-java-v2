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

package software.amazon.awssdk.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotation.SdkProtectedApi;

/**
 * A builder for creating a thread factory. This allows changing the behavior of the created thread factory.
 */
@SdkProtectedApi
public class ThreadFactoryBuilder {
    private static final AtomicInteger DEFAULT_POOL_NUMBER = new AtomicInteger(1);

    private String threadNameFormat;
    private Boolean daemonThreads = true;

    /**
     * The name of the threads that should be created in {@link String#format(String, Object...)} format. The first %s will be
     * provided with a unique (monotonically increasing) integer. It is strongly suggested to provide a useful name that allows
     * the customer to identify the purpose for the threads.
     *
     * By default, this is "aws-java-sdk-thread-POOL-%s".
     */
    public ThreadFactoryBuilder threadNameFormat(String threadNameFormat) {
        this.threadNameFormat = threadNameFormat;
        return this;
    }

    /**
     * Whether the threads created by the factory should be daemon threads. By default this is true - we shouldn't be holding up
     * the customer's JVM shutdown unless we're absolutely sure we want to.
     */
    public ThreadFactoryBuilder daemonThreads(Boolean daemonThreads) {
        this.daemonThreads = daemonThreads;
        return this;
    }

    /**
     * Create the {@link ThreadFactory} with the configuration currently applied to this builder.
     */
    public ThreadFactory build() {
        String threadNameFormat = this.threadNameFormat != null ? this.threadNameFormat
                                                                : createDefaultThreadPoolNameFormat();

        ThreadFactory result = new NamedThreadFactory(Executors.defaultThreadFactory(), threadNameFormat);

        if (daemonThreads) {
            result = new DaemonThreadFactory(result);
        }

        return result;
    }

    private String createDefaultThreadPoolNameFormat() {
        return "aws-java-sdk-thread-" + DEFAULT_POOL_NUMBER.getAndIncrement() + "-%s";
    }
}
