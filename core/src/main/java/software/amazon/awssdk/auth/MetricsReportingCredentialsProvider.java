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

package software.amazon.awssdk.auth;

import static software.amazon.awssdk.util.ValidationUtils.assertNotNull;

import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * Decorates a {@link AwsCredentialsProvider} to instrument with metrics. See
 * {@link software.amazon.awssdk.metrics.spi.AwsRequestMetrics.Field#CredentialsRequestTime}.
 */
public class MetricsReportingCredentialsProvider implements AwsCredentialsProvider {

    private final AwsCredentialsProvider delegate;
    private final AwsRequestMetrics awsRequestMetrics;

    public MetricsReportingCredentialsProvider(AwsCredentialsProvider delegate, AwsRequestMetrics awsRequestMetrics) {
        this.delegate = assertNotNull(delegate, "Delegate Credentials Provider");
        this.awsRequestMetrics = awsRequestMetrics;
    }

    @Override
    public AwsCredentials getCredentials() {
        awsRequestMetrics.startEvent(AwsRequestMetrics.Field.CredentialsRequestTime);
        try {
            return delegate.getCredentials();
        } finally {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.CredentialsRequestTime);
        }
    }

    @Override
    public void refresh() {
        delegate.refresh();
    }
}
