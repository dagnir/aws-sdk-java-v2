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

package utils.metrics;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.util.AwsRequestMetrics;

/**
 * Mock metrics collector that just holds a reference to all metrics recorded. Get captured metrics
 * via {@link MockRequestMetricsCollector#getMetrics()}.
 */
@NotThreadSafe
public class MockRequestMetricsCollector extends RequestMetricCollector {

    private final List<AwsRequestMetrics> metrics = new ArrayList<AwsRequestMetrics>();

    @Override
    public void collectMetrics(Request<?> request, Response<?> response) {
        metrics.add(request.getAwsRequestMetrics());
    }

    /**
     * @return A list of all metrics captured by this metric collector.
     */
    public List<AwsRequestMetrics> getMetrics() {
        return metrics;
    }
}
