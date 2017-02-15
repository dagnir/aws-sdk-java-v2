package software.amazon.awssdk.services.sns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.services.sns.model.AddPermissionRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResult;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResult;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResult;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsResult;
import software.amazon.awssdk.services.sns.model.ListTopicsResult;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResult;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.util.SignatureChecker;
import software.amazon.awssdk.services.sns.util.Topics;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * Integration tests for Cloudcast operations.
 */
public class CloudcastIntegrationTest extends IntegrationTestBase {

    private final SignatureChecker signatureChecker = new SignatureChecker();
    private final String DELIVERY_POLICY = "{ " + "  \"healthyRetryPolicy\":" + "    {"
                                           + "       \"minDelayTarget\": 1," + "       \"maxDelayTarget\": 1," + "       \"numRetries\": 1, "
                                           + "       \"numMaxDelayRetries\": 0, " + "       \"backoffFunction\": \"linear\"" + "     }" + "}";
    /** The ARN of the topic created by these tests */
    private String topicArn;
    /** The URL of the SQS queue created to receive notifications */
    private String queueUrl;
    private String subscriptionArn;

    @Before
    public void setup() {
        topicArn = null;
        queueUrl = null;
        subscriptionArn = null;
    }

    /** Releases all resources used by this test */
    @After
    public void tearDown() throws Exception {
        if (topicArn != null) {
            sns.deleteTopic(new DeleteTopicRequest(topicArn));
        }
        if (queueUrl != null) {
            sqs.deleteQueue(new DeleteQueueRequest(queueUrl));
        }
        if (subscriptionArn != null) {
            sns.unsubscribe(subscriptionArn);
        }
    }

    /**
     * Tests that we can correctly handle exceptions from SNS.
     */
    @Test
    public void testCloudcastExceptionHandling() {
        try {
            sns.createTopic(new CreateTopicRequest().withName(""));
        } catch (AmazonServiceException ase) {
            assertEquals("InvalidParameter", ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertTrue(ase.getMessage().length() > 5);
            assertTrue(ase.getRequestId().length() > 5);
            assertTrue(ase.getServiceName().length() > 5);
            assertEquals(400, ase.getStatusCode());
        }
    }

    /** Tests the functionality in the Topics utility class. */
    @Test
    public void testTopics_subscribeQueue() throws Exception {
        topicArn = sns.createTopic(new CreateTopicRequest("subscribeTopicTest-" + System.currentTimeMillis()))
                      .getTopicArn();
        queueUrl = sqs.createQueue(new CreateQueueRequest("subscribeTopicTest-" + System.currentTimeMillis()))
                      .getQueueUrl();

        subscriptionArn = Topics.subscribeQueue(sns, sqs, topicArn, queueUrl);
        assertNotNull(subscriptionArn);

        // Verify that the queue is receiving messages
        sns.publish(new PublishRequest(topicArn, "Hello SNS World").withSubject("Subject"));
        String message = receiveMessage();
        Map<String, String> messageDetails = parseJSON(message);
        assertEquals("Hello SNS World", messageDetails.get("Message"));
        assertEquals("Subject", messageDetails.get("Subject"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));
    }

    @Test
    public void testSendUnicodeMessages() throws InterruptedException {
        String unicodeMessage = "你好";
        String unicodeSubject = "主题";
        topicArn = sns.createTopic(new CreateTopicRequest("unicodeMessageTest-" + System.currentTimeMillis()))
                      .getTopicArn();
        queueUrl = sqs.createQueue(new CreateQueueRequest("unicodeMessageTest-" + System.currentTimeMillis()))
                      .getQueueUrl();

        subscriptionArn = Topics.subscribeQueue(sns, sqs, topicArn, queueUrl);
        assertNotNull(subscriptionArn);

        // Verify that the queue is receiving unicode messages
        sns.publish(new PublishRequest(topicArn, unicodeMessage).withSubject(unicodeSubject));
        String message = receiveMessage();
        Map<String, String> messageDetails = parseJSON(message);
        assertEquals(unicodeMessage, messageDetails.get("Message"));
        assertEquals(unicodeSubject, messageDetails.get("Subject"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        sns.deleteTopic(topicArn);
        topicArn = null;
        sqs.deleteQueue(queueUrl);
        queueUrl = null;
    }

    /**
     * Tests that we can invoke operations on Cloudcast and correctly interpret the responses.
     */
    @Test
    public void testCloudcastOperations() throws Exception {

        // Create Topic
        CreateTopicResult createTopicResult = sns
                .createTopic(new CreateTopicRequest("test-topic-" + System.currentTimeMillis()));
        topicArn = createTopicResult.getTopicArn();
        assertTrue(topicArn.length() > 1);

        // List Topics
        Thread.sleep(1000 * 5);
        ListTopicsResult listTopicsResult = sns.listTopics();
        assertNotNull(listTopicsResult.getTopics());
        assertTopicIsPresent(listTopicsResult.getTopics(), topicArn);

        // Set Topic Attributes
        sns.setTopicAttributes(new SetTopicAttributesRequest().withTopicArn(topicArn).withAttributeName("DisplayName")
                                                              .withAttributeValue("MyTopicName"));

        // Get Topic Attributes
        GetTopicAttributesResult getTopicAttributesResult = sns
                .getTopicAttributes(new GetTopicAttributesRequest().withTopicArn(topicArn));
        assertEquals("MyTopicName", getTopicAttributesResult.getAttributes().get("DisplayName"));

        // Subscribe an SQS queue for notifications
        String queueArn = initializeReceivingQueue();
        SubscribeResult subscribeResult = sns
                .subscribe(new SubscribeRequest().withEndpoint(queueArn).withProtocol("sqs").withTopicArn(topicArn));
        subscriptionArn = subscribeResult.getSubscriptionArn();
        assertTrue(subscriptionArn.length() > 1);

        // List Subscriptions by Topic
        Thread.sleep(1000 * 5);
        ListSubscriptionsByTopicResult listSubscriptionsByTopicResult = sns
                .listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn));
        assertSubscriptionIsPresent(listSubscriptionsByTopicResult.getSubscriptions(), subscriptionArn);

        // List Subscriptions
        List<Subscription> subscriptions = getAllSubscriptions(sns);
        assertSubscriptionIsPresent(subscriptions, subscriptionArn);

        // Get Subscription Attributes
        Map<String, String> attributes = sns
                .getSubscriptionAttributes(new GetSubscriptionAttributesRequest(subscriptionArn)).getAttributes();
        assertTrue(attributes.size() > 0);
        Entry<String, String> entry = attributes.entrySet().iterator().next();
        assertNotNull(entry.getKey());
        assertNotNull(entry.getValue());

        // Set Subscription Attributes
        sns.setSubscriptionAttributes(
                new SetSubscriptionAttributesRequest(subscriptionArn, "DeliveryPolicy", DELIVERY_POLICY));

        // Publish
        sns.publish(new PublishRequest(topicArn, "Hello SNS World").withSubject("Subject"));

        // Receive Published Message
        String message = receiveMessage();
        Map<String, String> messageDetails = parseJSON(message);
        assertEquals("Hello SNS World", messageDetails.get("Message"));
        assertEquals("Subject", messageDetails.get("Subject"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        // Verify Message Signature
        Certificate certificate = CertificateFactory.getInstance("X509")
                                                    .generateCertificate(getClass().getResourceAsStream(SnsTestResources.PUBLIC_CERT));
        assertTrue(signatureChecker.verifyMessageSignature(message, certificate.getPublicKey()));

        // Add/Remove Permissions
        sns.addPermission(new AddPermissionRequest(topicArn, "foo", null, null)
                                  .withActionNames(new String[] {"Publish"}).withAWSAccountIds(new String[] {"750203240092"}));
        Thread.sleep(1000 * 5);
        sns.removePermission(new RemovePermissionRequest(topicArn, "foo"));
    }

    /**
     * Get all subscriptions as a list of {@link Subscription} objects
     *
     * @param sns
     *            Client
     * @return List of all subscriptions
     */
    private List<Subscription> getAllSubscriptions(AmazonSNS sns) {
        ListSubscriptionsResult result = sns.listSubscriptions();
        List<Subscription> subscriptions = new ArrayList<Subscription>(result
                                                                               .getSubscriptions());
        while (result.getNextToken() != null) {
            result = sns.listSubscriptions(result.getNextToken());
            subscriptions.addAll(result.getSubscriptions());
        }
        return subscriptions;
    }

    @Test
    public void testSimplifiedMethods() throws InterruptedException {
        // Create Topic
        CreateTopicResult createTopicResult = sns.createTopic("test-topic-" + System.currentTimeMillis());
        topicArn = createTopicResult.getTopicArn();
        assertTrue(topicArn.length() > 1);

        // List Topics
        Thread.sleep(1000 * 5);
        ListTopicsResult listTopicsResult = sns.listTopics();
        assertNotNull(listTopicsResult.getTopics());
        assertTopicIsPresent(listTopicsResult.getTopics(), topicArn);

        // Set Topic Attributes
        sns.setTopicAttributes(topicArn, "DisplayName", "MyTopicName");

        // Get Topic Attributes
        GetTopicAttributesResult getTopicAttributesResult = sns.getTopicAttributes(topicArn);
        assertEquals("MyTopicName", getTopicAttributesResult.getAttributes().get("DisplayName"));

        // Subscribe an SQS queue for notifications
        queueUrl = sqs.createQueue(new CreateQueueRequest("subscribeTopicTest-" + System.currentTimeMillis()))
                      .getQueueUrl();
        String queueArn = initializeReceivingQueue();
        SubscribeResult subscribeResult = sns.subscribe(topicArn, "sqs", queueArn);
        String subscriptionArn = subscribeResult.getSubscriptionArn();
        assertTrue(subscriptionArn.length() > 1);

        // List Subscriptions by Topic
        Thread.sleep(1000 * 5);
        ListSubscriptionsByTopicResult listSubscriptionsByTopicResult = sns.listSubscriptionsByTopic(topicArn);
        assertSubscriptionIsPresent(listSubscriptionsByTopicResult.getSubscriptions(), subscriptionArn);

        // Get Subscription Attributes
        Map<String, String> attributes = sns.getSubscriptionAttributes(subscriptionArn).getAttributes();
        assertTrue(attributes.size() > 0);
        Entry<String, String> entry = attributes.entrySet().iterator().next();
        assertNotNull(entry.getKey());
        assertNotNull(entry.getValue());

        // Set Subscription Attributes
        sns.setSubscriptionAttributes(subscriptionArn, "DeliveryPolicy", DELIVERY_POLICY);

        // Publish With Subject
        sns.publish(topicArn, "Hello SNS World", "Subject");

        // Receive Published Message
        String message = receiveMessage();
        Map<String, String> messageDetails = parseJSON(message);
        assertEquals("Hello SNS World", messageDetails.get("Message"));
        assertEquals("Subject", messageDetails.get("Subject"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        // Publish Without Subject
        sns.publish(topicArn, "Hello SNS World");

        // Receive Published Message
        message = receiveMessage();
        messageDetails = parseJSON(message);
        assertEquals("Hello SNS World", messageDetails.get("Message"));
        assertNotNull(messageDetails.get("MessageId"));
        assertNotNull(messageDetails.get("Signature"));

        // Add/Remove Permissions
        sns.addPermission(topicArn, "foo", Arrays.asList("750203240092"), Arrays.asList("Publish"));
        Thread.sleep(1000 * 5);
        sns.removePermission(topicArn, "foo");

        // Unsubscribe
        sns.unsubscribe(subscriptionArn);

        // Delete Topic
        sns.deleteTopic(topicArn);
        topicArn = null;
    }

    /*
     * Private Interface
     */

    /**
     * Polls the SQS queue created earlier in the test until we find our SNS notification message
     * and returns the base64 decoded message body.
     */
    private String receiveMessage() throws InterruptedException {
        int maxRetries = 15;
        while (maxRetries-- > 0) {
            Thread.sleep(1000 * 10);
            List<Message> messages = sqs.receiveMessage(new ReceiveMessageRequest(queueUrl)).getMessages();
            if (messages.size() > 0) {
                return new String(messages.get(0).getBody());
            }
        }

        fail("No SQS messages received after retrying " + maxRetries + "times");
        return null;
    }

    /**
     * Creates an SQS queue for this test to use when receiving SNS notifications. We need to use an
     * SQS queue because otherwise HTTP or email notifications require a confirmation token that is
     * sent via HTTP or email. Plus an SQS queue lets us test that our notification was delivered.
     */
    private String initializeReceivingQueue() throws InterruptedException {
        String queueName = "sns-integ-test-" + System.currentTimeMillis();
        this.queueUrl = sqs.createQueue(new CreateQueueRequest(queueName)).getQueueUrl();

        Thread.sleep(1000 * 4);
        String queueArn = sqs
                .getQueueAttributes(
                        new GetQueueAttributesRequest(queueUrl).withAttributeNames(new String[] {"QueueArn"}))
                .getAttributes().get("QueueArn");
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("Policy", generateSqsPolicyForTopic(queueArn, topicArn));
        sqs.setQueueAttributes(new SetQueueAttributesRequest(queueUrl, attributes));
        int policyPropagationDelayInSeconds = 120;
        System.out.println("Sleeping " + policyPropagationDelayInSeconds + " seconds to let SQS policy propagate");
        Thread.sleep(1000 * policyPropagationDelayInSeconds);
        return queueArn;
    }

    /**
     * Creates a policy to apply to our SQS queue that allows our SNS topic to deliver notifications
     * to it. Note that this policy is for the SQS queue, *not* for SNS.
     */
    private String generateSqsPolicyForTopic(String queueArn, String topicArn) {
        String policy = "{ " + "  \"Version\":\"2008-10-17\"," + "  \"Id\":\"" + queueArn + "/policyId\","
                        + "  \"Statement\": [" + "    {" + "        \"Sid\":\"" + queueArn + "/statementId\","
                        + "        \"Effect\":\"Allow\"," + "        \"Principal\":{\"AWS\":\"*\"},"
                        + "        \"Action\":\"SQS:SendMessage\"," + "        \"Resource\": \"" + queueArn + "\","
                        + "        \"Condition\":{" + "            \"StringEquals\":{\"aws:SourceArn\":\"" + topicArn + "\"}"
                        + "        }" + "    }" + "  ]" + "}";

        return policy;
    }

}
