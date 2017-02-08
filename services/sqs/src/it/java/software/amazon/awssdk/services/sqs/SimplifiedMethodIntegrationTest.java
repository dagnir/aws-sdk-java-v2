package software.amazon.awssdk.services.sqs;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResult;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResult;
import software.amazon.awssdk.services.sqs.model.ListQueuesResult;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResult;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResult;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;

/**
 * These tests are more or less identical to SqsOperationsIntegrationTest. The only difference is
 * these use the simplified variants of the operation methods rather then the typical methods that
 * take some form of a request object.
 */
public class SimplifiedMethodIntegrationTest extends IntegrationTestBase {

    private static final String ATTRIBUTE_VALUE = "42";
    private static final String ATTRIBUTE_NAME = "VisibilityTimeout";
    private static final String MESSAGE_BODY = "foobarbazbar";

    private final AmazonSQS sqsClient = getSharedSqsAsyncClient();
    private final String queueName = getUniqueQueueName();
    private String queueUrl;

    /** Releases all resources used by these tests */
    @After
    public void tearDown() throws Exception {
        sqsClient.deleteQueue(queueUrl);
    }

    @Test
    public void testSimplifiedMethods() throws InterruptedException {
        runCreateQueueTest();
        runGetQueueTest();
        runListQueuesTest();
        runSetAttributesTest();
        runAddPermissionTest();
        runRemovePermissionTest();
        runSendMessageTest();
        ReceiveMessageResult receiveMessageResult = runReceiveMessageTest();
        runSendMessageBatchTest();
        String receiptHandle = runChangeMessageVisibilityTest(receiveMessageResult);
        runDeleteMessageTest(receiptHandle);
    }

    private void runCreateQueueTest() {
        queueUrl = sqsClient.createQueue(queueName).getQueueUrl();
        assertNotEmpty(queueUrl);
    }

    private void runGetQueueTest() {
        GetQueueUrlResult getQueueUrlResult = sqsClient.getQueueUrl(queueName);
        assertEquals(queueUrl, getQueueUrlResult.getQueueUrl());
    }

    private void runListQueuesTest() {
        ListQueuesResult listQueuesResult = sqsClient.listQueues(queueName);
        assertEquals(1, listQueuesResult.getQueueUrls().size());
        assertEquals(queueUrl, listQueuesResult.getQueueUrls().get(0));
    }

    private void runSetAttributesTest() throws InterruptedException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        sqsClient.setQueueAttributes(queueUrl, attributes);

        Thread.sleep(1000 * 10);
        GetQueueAttributesResult queueAttributesResult = sqsClient.getQueueAttributes(queueUrl,
                Arrays.asList(ATTRIBUTE_NAME));
        assertEquals(1, queueAttributesResult.getAttributes().size());
        Map<String, String> attributes2 = queueAttributesResult.getAttributes();
        assertEquals(1, attributes2.size());
        assertNotNull(attributes2.get(ATTRIBUTE_NAME));
    }

    private void runAddPermissionTest() {
        sqsClient.addPermission(queueUrl, "foo-label", Arrays.asList(getAccountId()),
                Arrays.asList("SendMessage", "DeleteMessage"));
    }

    private void runRemovePermissionTest() throws InterruptedException {
        Thread.sleep(1000 * 2);
        sqsClient.removePermission(queueUrl, "foo-label");
    }

    private void runSendMessageTest() {
        for (int i = 0; i < 10; i++) {
            SendMessageResult sendMessageResult = sqsClient.sendMessage(queueUrl, MESSAGE_BODY);
            assertNotEmpty(sendMessageResult.getMessageId());
            assertNotEmpty(sendMessageResult.getMD5OfMessageBody());
        }
    }

    private ReceiveMessageResult runReceiveMessageTest() {
        ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(queueUrl);
        assertThat(receiveMessageResult.getMessages(), not(empty()));
        Message message = receiveMessageResult.getMessages().get(0);
        assertEquals(MESSAGE_BODY, message.getBody());
        assertNotEmpty(message.getMD5OfBody());
        assertNotEmpty(message.getMessageId());
        assertNotEmpty(message.getReceiptHandle());

        for (Iterator<Entry<String, String>> iterator = message.getAttributes().entrySet().iterator(); iterator
                .hasNext();) {
            Entry<String, String> entry = iterator.next();
            assertNotEmpty((entry.getKey()));
            assertNotEmpty((entry.getValue()));
        }
        return receiveMessageResult;
    }

    private void runSendMessageBatchTest() {
        SendMessageBatchResult sendMessageBatchResult = sqsClient.sendMessageBatch(queueUrl, Arrays.asList(
                new SendMessageBatchRequestEntry().withId("1").withMessageBody("1"), new SendMessageBatchRequestEntry()
                        .withId("2").withMessageBody("2"), new SendMessageBatchRequestEntry().withId("3")
                        .withMessageBody("3"), new SendMessageBatchRequestEntry().withId("4").withMessageBody("4"),
                new SendMessageBatchRequestEntry().withId("5").withMessageBody("5")));

        assertNotNull(sendMessageBatchResult.getFailed());
        assertThat(sendMessageBatchResult.getSuccessful().size(), greaterThan(0));
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getId());
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getMD5OfMessageBody());
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getMessageId());
    }

    private String runChangeMessageVisibilityTest(ReceiveMessageResult receiveMessageResult) {
        String receiptHandle = (receiveMessageResult.getMessages().get(0)).getReceiptHandle();
        sqsClient.changeMessageVisibility(queueUrl, receiptHandle, 123);
        return receiptHandle;
    }

    private void runDeleteMessageTest(String receiptHandle) {
        sqsClient.deleteMessage(queueUrl, receiptHandle);
    }
}
