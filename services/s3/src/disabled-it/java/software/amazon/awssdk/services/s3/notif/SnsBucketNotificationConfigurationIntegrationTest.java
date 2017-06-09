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

package software.amazon.awssdk.services.s3.notif;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.S3Event;
import software.amazon.awssdk.services.s3.model.TopicConfiguration;
import software.amazon.awssdk.services.sns.AmazonSNS;
import software.amazon.awssdk.services.sns.AmazonSNSClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;

/**
 * Integration tests for the Amazon S3 bucket notification configuration operations.
 */
public class SnsBucketNotificationConfigurationIntegrationTest extends S3IntegrationTestBase {

    private static final String NOTIFICATION_NAME = "test-notif";
    private static final String TOPIC_NAME = "bucketNotifyTopic";
    private static final String EVENT_NAME = S3Event.ObjectRemovedDelete.toString();

    private final String bucketName = "bucket-notification-integ-test-" + new Date().getTime();

    private AmazonSNS sns;

    private String topicArn = null;

    @Before
    public void setup() throws Exception {
        s3.createBucket(bucketName);
        waitForBucketCreation(bucketName);

        sns = new AmazonSNSClient(credentials);
        topicArn = sns.createTopic(new CreateTopicRequest(TOPIC_NAME)).getTopicArn();
        BucketNotificationTestUtils.authorizeS3ToSendToSns(sns, topicArn, bucketName);
    }

    /**
     * Releases all resources created by this test.
     */
    @After
    public void tearDown() {
        s3.deleteBucket(bucketName);
        sns.deleteTopic(new DeleteTopicRequest(topicArn));
    }

    /**
     * Tests the bucket notification operations.
     */
    @Test
    public void putSnsBucketConfiguration_ReturnsSameConfigurationOnGet() throws Exception {
        // Test the initial notification configuration
        assertBucketNotificationConfigIsEmpty();

        // Configure notification for a bucket
        setBucketNotificationConfig();
        BucketNotificationConfiguration notificationConfig = s3.getBucketNotificationConfiguration(bucketName);
        assertContainsCorrectTopicConfiguration(notificationConfig.getConfigurations());

        // Remove the bucket notification configuration
        removeBucketConfiguration();
        assertBucketNotificationConfigIsEmpty();
    }

    /**
     * Create a bucket notification configuration that publishes to the SNS topic
     *
     * @return The new configuration added
     */
    private void setBucketNotificationConfig() {
        TopicConfiguration topicConfiguration = new TopicConfiguration(topicArn, EVENT_NAME);
        BucketNotificationConfiguration bucketNotificationConfiguration = new BucketNotificationConfiguration();
        bucketNotificationConfiguration.addConfiguration(NOTIFICATION_NAME, topicConfiguration);
        s3.setBucketNotificationConfiguration(bucketName, bucketNotificationConfiguration);
    }

    /**
     * Clear bucket notification configuration
     */
    private void removeBucketConfiguration() {
        s3.setBucketNotificationConfiguration(bucketName, new BucketNotificationConfiguration());
    }

    private void assertContainsCorrectTopicConfiguration(Map<String, NotificationConfiguration> notificationConfigs) {
        assertThat(notificationConfigs.get(NOTIFICATION_NAME), instanceOf(TopicConfiguration.class));
        TopicConfiguration topicConfiguration = (TopicConfiguration) notificationConfigs.get(NOTIFICATION_NAME);
        assertEquals(topicArn, topicConfiguration.getTopicARN());
        Assert.assertThat(topicConfiguration.getEvents(), Matchers.contains(EVENT_NAME));
    }

    private void assertBucketNotificationConfigIsEmpty() {
        BucketNotificationConfiguration bucketNotificationConfiguration = s3
                .getBucketNotificationConfiguration(bucketName);
        assertTrue(bucketNotificationConfiguration.getConfigurations().isEmpty());
    }

}
