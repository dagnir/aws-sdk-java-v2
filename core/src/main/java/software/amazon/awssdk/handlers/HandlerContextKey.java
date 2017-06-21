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

package software.amazon.awssdk.handlers;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.auth.AwsCredentials;

/**
 * A type safe key used for setting and retrieving context in a {@link Request} object.
 *
 * <pre class="brush: java">
 *     final HandlerContextKey&lt;String&gt; METRICS_KEY = new HandlerContextKey("METRICS_KEY");
 *
 *      new RequestHandler2(){
 *
 *          {@literal @}Override
 *          public void beforeRequest(Request&lt;?&gt; request) {
 *              request.addHandlerContext(METRICS_KEY, AWSRequestMetrics.Field.HttpRequestTime.name());
 *          }
 *
 *          {@literal @}Override
 *          public void afterResponse(Request&lt;?&gt; request, Response&lt;?&gt; response) {
 *              String metricsKey = request.getHandlerContext(METRICS_KEY);
 *          }
 *
 *          {@literal @}Override
 *          public void afterError(Request&lt;?&gt; request, Response&lt;?&gt; response, Exception e) { }
 *      }
 * </pre>
 */
public class HandlerContextKey<T> {
    /** The key under which the request credentials are set. */
    public static final HandlerContextKey<AwsCredentials> AWS_CREDENTIALS = new HandlerContextKey<>("AWSCredentials");

    public static final HandlerContextKey<ServiceAdvancedConfiguration> SERVICE_ADVANCED_CONFIG =
            new HandlerContextKey<>("ServiceConfig");

    private final String name;

    public HandlerContextKey(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HandlerContextKey<?> key = (HandlerContextKey<?>) o;

        return name.equals(key.getName());

    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
