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
 * Represents the queue configuration for an Amazon S3 bucket.
 */
public class QueueConfiguration extends NotificationConfiguration implements Serializable {

    /**
     * The Amazon SQS queue ARN for this configuration.
     */
    private String queueArn;

    public QueueConfiguration() {
        super();
    }

    /**
     * Creates a new queue configuration with the given queue arn and set of events.
     *
     * @param queueArn
     *            the Amazon SQS queue arn to which the notifications are to be sent.
     * @param events
     *            the events for which the notifications are to be sent
     */
    public QueueConfiguration(String queueArn, EnumSet<S3Event> events) {
        super(events);
        this.queueArn = queueArn;
    }

    /**
     * Creates a new queue configuration with the given queue arn and set of events.
     *
     * @param queueArn
     *            the Amazon SQS queue arn to which the notifications are to be sent.
     * @param events
     *            the events for which the notifications are to be sent
     */
    public QueueConfiguration(String queueArn, String... events) {
        super(events);
        this.queueArn = queueArn;
    }

    /**
     * Returns the queue arn for this notification configuration.
     */
    public String getQueueArn() {
        return queueArn;
    }

    /**
     * Sets the queue ARN for this configuration
     *
     * @param queueArn
     *            ARN for the SQS queue
     */
    public void setQueueArn(String queueArn) {
        this.queueArn = queueArn;
    }

    /**
     * Fluent method to set the queue ARN for this configuration
     *
     * @param queueARN
     *            ARN for the SQS queue
     * @return This object for method chaining
     */
    public QueueConfiguration withQueueArn(String queueArn) {
        setQueueArn(queueArn);
        return this;
    }
}
