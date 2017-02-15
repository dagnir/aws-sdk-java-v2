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

/**
 * Request object to list the analytics configurations of a bucket.
 */
public class ListBucketAnalyticsConfigurationsRequest extends AmazonWebServiceRequest implements Serializable {

    /** The name of the Amazon S3 bucket to list the analytics configurations. */
    private String bucketName;

    /**
     * Optional parameter which allows list to be continued from a specific point.
     * ContinuationToken is provided in truncated list results.
     */
    private String continuationToken;

    /**
     * Gets the name of the Amazon S3 bucket whose
     * analytics configurations are to be listed.
     *
     * @return The name of the Amazon S3 bucket whose
     *         analytics configurations are to be listed.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the Amazon S3 bucket whose analytics configurations are to be listed.
     *
     * @param bucketName
     *            The name of the Amazon S3 bucket whose analytics configurations are to be listed.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the Amazon S3 bucket whose analytics configurations are to be listed.
     * Returns this {@link ListBucketAnalyticsConfigurationsRequest}, enabling additional method
     * calls to be chained together.
     *
     * @param bucketName
     *            The name of the Amazon S3 bucket whose analytics configurations are to be listed.
     *
     * @return This {@link ListBucketAnalyticsConfigurationsRequest}, enabling additional method
     *         calls to be chained together.
     */
    public ListBucketAnalyticsConfigurationsRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Gets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. ContinuationToken is provided in truncated list results.
     *
     * @return The optional continuation token associated with this request.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * Sets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. ContinuationToken is provided in truncated list results.
     *
     * @param continuationToken
     *                     The optional continuation token to associate with this request.
     */
    public void setContinuationToken(String continuationToken) {
        this.continuationToken = continuationToken;
    }

    /**
     * Sets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. ContinuationToken is provided in truncated list results.
     *
     * @param continuationToken
     *                     The optional continuation token to associate with this request.
     *
     * @return This {@link ListBucketAnalyticsConfigurationsRequest}, enabling additional method
     *         calls to be chained together.
     */
    public ListBucketAnalyticsConfigurationsRequest withContinuationToken(String continuationToken) {
        setContinuationToken(continuationToken);
        return this;
    }
}
