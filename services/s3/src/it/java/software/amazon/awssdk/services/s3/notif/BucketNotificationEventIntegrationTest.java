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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.S3Event;
import software.amazon.awssdk.services.s3.model.TopicConfiguration;
import software.amazon.awssdk.services.sns.AmazonSNS;
import software.amazon.awssdk.services.sns.AmazonSNSClient;

@RunWith(Parameterized.class)
public class BucketNotificationEventIntegrationTest extends S3IntegrationTestBase {

    private static final String TOPIC_NAME = "bucket-notification-event-topic";
    private static final String BUCKET_NAME = "bucket-notification-event-integ-" + System.currentTimeMillis();
    private static final String NOTIFICATION_CONFIG_NAME = "bucket-notification-event-config";

    private final S3Event s3Event;
    private String topicArn;
    private AmazonSNS sns;

    public BucketNotificationEventIntegrationTest(S3Event s3Event) {
        this.s3Event = s3Event;
    }

    @Parameters(name = "{0}")
    public static Collection<S3Event[]> data() {
        Collection<S3Event[]> events = new ArrayList<S3Event[]>();
        for (S3Event event : S3Event.values()) {
            events.add(new S3Event[] {event});
        }
        return events;
    }

    @Before
    public void setup() {
        sns = new AmazonSNSClient(credentials);
        s3.createBucket(BUCKET_NAME);
        topicArn = sns.createTopic(TOPIC_NAME).getTopicArn();
        BucketNotificationTestUtils.authorizeS3ToSendToSNS(sns, topicArn, BUCKET_NAME);
    }

    @After
    public void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
        sns.deleteTopic(topicArn);
    }

    @Test
    public void putBucketConfiguration_WithGivenEvent_ReturnsSameEventOnGet() {
        TopicConfiguration topicConfiguration = new TopicConfiguration(topicArn, EnumSet.of(s3Event));
        s3.setBucketNotificationConfiguration(BUCKET_NAME, new BucketNotificationConfiguration(
                NOTIFICATION_CONFIG_NAME, topicConfiguration));

        // Assert that the Event type is set correctly on GET
        BucketNotificationConfiguration config = s3.getBucketNotificationConfiguration(BUCKET_NAME);
        TopicConfiguration topicConfig = (TopicConfiguration) config.getConfigurationByName(NOTIFICATION_CONFIG_NAME);
        assertTrue(topicConfig.getEvents().contains(s3Event.toString()));
    }

}
