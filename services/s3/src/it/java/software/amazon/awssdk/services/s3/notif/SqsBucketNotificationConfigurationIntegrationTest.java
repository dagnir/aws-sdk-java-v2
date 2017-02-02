package software.amazon.awssdk.services.s3.notif;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.auth.policy.Policy;
import software.amazon.awssdk.auth.policy.actions.SQSActions;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.s3.model.S3Event;
import software.amazon.awssdk.services.sqs.AmazonSQS;
import software.amazon.awssdk.services.sqs.AmazonSQSClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResult;
import software.amazon.awssdk.util.ImmutableMapParameter;

public class SqsBucketNotificationConfigurationIntegrationTest extends S3IntegrationTestBase {

    private static final String NOTIFICATION_NAME = "myQueueConfig";

    private static final String BUCKET_NAME = "bucket-notification-integ-test-" + System.currentTimeMillis();

    private static final String QUEUE_NAME = "bucket-notification-queue-" + System.currentTimeMillis();

    protected static AmazonSQS sqs;

    private static String queueArn = null;

    private static String queueUrl = null;

    /**
     * creates the s3 bucket, sns topic and sqs queue. also authorizes s3 to publish messages to the
     * sns and sqs.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        sqs = new AmazonSQSClient(credentials);
        s3.createBucket(BUCKET_NAME);
        queueUrl = sqs.createQueue(QUEUE_NAME).getQueueUrl();

        authorizeS3ToSendToSQS();
    }

    /**
     * Deletes the buckets, queue and the topic
     */
    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
        if (queueUrl != null) {
            sqs.deleteQueue(queueUrl);
        }
    }

    @Test
    public void putSqsBucketNotification_ReturnsSameConfigurationOnGet() throws Exception {
        setBucketNotification();
        BucketNotificationConfiguration notificationConfig = s3.getBucketNotificationConfiguration(BUCKET_NAME);
        assertEquals(1, notificationConfig.getConfigurations().size());
        assertContainsCorrectQueueConfiguration(notificationConfig.getConfigurations());
    }

    public static Policy getSqsPolicy() {
        GetQueueAttributesResult getQueueAtrtibutesResult = sqs.getQueueAttributes(queueUrl,
                Arrays.asList("QueueArn", "Policy"));
        queueArn = getQueueAtrtibutesResult.getAttributes().get("QueueArn");
        String policyString = getQueueAtrtibutesResult.getAttributes().get("Policy");
        return policyString == null ? new Policy() : Policy.fromJson(policyString);
    }

    /**
     * updates sqs policy so S3 can send messages to the queue.
     */
    private static void authorizeS3ToSendToSQS() {
        Policy policy = getSqsPolicy();

        policy.getStatements().add(
                BucketNotificationTestUtils.createAllowS3AccessToResourcePolicyStatement(BUCKET_NAME, queueArn,
                        SQSActions.SendMessage));

        sqs.setQueueAttributes(queueUrl, ImmutableMapParameter.of("Policy", policy.toJson()));
    }

    public void setBucketNotification() {
        BucketNotificationConfiguration bucketNotificationConfiguration = new BucketNotificationConfiguration();
        bucketNotificationConfiguration.addConfiguration(NOTIFICATION_NAME,
                new QueueConfiguration(queueArn, EnumSet.of(S3Event.ObjectCreatedByPut)));
        s3.setBucketNotificationConfiguration(BUCKET_NAME, bucketNotificationConfiguration);
    }

    private void assertContainsCorrectQueueConfiguration(Map<String, NotificationConfiguration> notificationConfigs) {
        assertThat(notificationConfigs.get(NOTIFICATION_NAME), instanceOf(QueueConfiguration.class));
        QueueConfiguration queueConfiguration = (QueueConfiguration) notificationConfigs.get(NOTIFICATION_NAME);
        assertEquals(queueArn, queueConfiguration.getQueueARN());
    }

}
