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

package software.amazon.awssdk.services.s3.model;

import java.io.Serializable;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.services.s3.model.metrics.MetricsConfiguration;

/**
 * Request object to set metrics configuration to a bucket.
 */
public class SetBucketMetricsConfigurationRequest extends AmazonWebServiceRequest implements Serializable {

    private String bucketName;
    private MetricsConfiguration metricsConfiguration;

    public SetBucketMetricsConfigurationRequest() {
    }

    public SetBucketMetricsConfigurationRequest(String bucketName, MetricsConfiguration metricsConfiguration) {
        this.bucketName = bucketName;
        this.metricsConfiguration = metricsConfiguration;
    }

    /**
     * Returns the name of the bucket for which the metrics configuration is set.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket for which the metrics configuration is set.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket for which the metrics configuration is set
     * and returns {@link SetBucketMetricsConfigurationRequest} object for method chaining.
     */
    public SetBucketMetricsConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Returns the metrics configuration that is set on the bucket.
     */
    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }

    /**
     * Sets the metrics configuration.
     */
    public void setMetricsConfiguration(MetricsConfiguration metricsConfiguration) {
        this.metricsConfiguration = metricsConfiguration;
    }

    /**
     * Sets the metrics configuration and returns the
     * {@link SetBucketMetricsConfigurationRequest} object for method chaining.
     */
    public SetBucketMetricsConfigurationRequest withMetricsConfiguration(MetricsConfiguration metricsConfiguration) {
        setMetricsConfiguration(metricsConfiguration);
        return this;
    }
}
