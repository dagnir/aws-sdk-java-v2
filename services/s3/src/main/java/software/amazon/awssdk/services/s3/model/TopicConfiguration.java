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
import java.util.EnumSet;

/**
 * Represents the topic configuration for an Amazon S3 bucket.
 */
public class TopicConfiguration extends NotificationConfiguration implements Serializable {

    /**
     * The Amazon SNS topic ARN for this configuration.
     */
    private String topicArn;

    public TopicConfiguration() {
        super();
    }

    /**
     * Creates a new topic configuration with the given topic arn and set of events.
     *
     * @param topicArn
     *            the Amazon SNS topic arn to which the notifications are to be sent.
     * @param events
     *            the events for which the notifications are to be sent
     */
    public TopicConfiguration(String topicArn, EnumSet<S3Event> events) {
        super(events);
        this.topicArn = topicArn;
    }

    /**
     * Creates a new topic configuration with the given topic arn and set of events.
     *
     * @param topicArn
     *            the Amazon SNS topic arn to which the notifications are to be sent.
     * @param events
     *            the events for which the notifications are to be sent
     */
    public TopicConfiguration(String topicArn, String... events) {
        super(events);
        this.topicArn = topicArn;
    }

    /**
     * Returns the topic arn for this notification configuration.
     */
    public String getTopicArn() {
        return topicArn;
    }

    /**
     * Sets the topic ARN for this configuration
     *
     * @param topicArn
     *            ARN for the SNS topic
     */
    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    /**
     * Fluent method to set the topic ARN for this configuration
     *
     * @param topicArn
     *            ARN for the SNS topic
     * @return This object for method chaining
     */
    public TopicConfiguration withTopicArn(String topicArn) {
        setTopicArn(topicArn);
        return this;
    }
}
