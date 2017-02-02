package software.amazon.awssdk.services.sqs;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import software.amazon.awssdk.services.sqs.buffered.QueueBufferConfig;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.util.StringUtils;

public class BufferedSQSIntegrationTest extends IntegrationTestBase {

    private static final int MAX_SIZE_MESSAGE = 260 * 1024 - 1;

    private AmazonSQSAsyncClient sqsClient;
    private QueueBufferConfig config;
    private AmazonSQSBufferedAsyncClient buffSqs;
    private String queueUrl;

    @Before
    public void setup() {
        config = new QueueBufferConfig();
        sqsClient = createSqsAyncClient();
        buffSqs = new AmazonSQSBufferedAsyncClient(sqsClient, config);
        queueUrl = createQueue(sqsClient);
    }

    @After
    public void tearDown() {
        buffSqs.deleteQueue(queueUrl);
        buffSqs.shutdown();
    }

    @Test
    public void receiveMessage_NoMessagesOnQueue_ReturnsEmptyListOfMessages() {
        assertEquals(0, buffSqs.receiveMessage(new ReceiveMessageRequest(queueUrl)).getMessages().size());
    }

    @Test
    public void receiveMessage_WhenAllBufferedBatchesExpire_FetchesNewBatchesFromSqs() throws InterruptedException {
        final int visiblityTimeoutSeconds = 2;
        config.withVisibilityTimeoutSeconds(visiblityTimeoutSeconds);

        List<SendMessageBatchRequestEntry> messages = new ArrayList<SendMessageBatchRequestEntry>();
        final int numOfTestMessages = 10;
        for (int messageNum = 1; messageNum <= numOfTestMessages; messageNum++) {
            messages.add(new SendMessageBatchRequestEntry(String.valueOf(messageNum), "test-" + messageNum));
        }
        // Use the normal client so we don't have to wait for the buffered messages to be sent
        sqsClient.sendMessageBatch(new SendMessageBatchRequest(queueUrl).withEntries(messages));
        assertThat(buffSqs.receiveMessage(queueUrl).getMessages().size(), greaterThan(0));
        // Make sure they are expired by waiting twice the timeout
        Thread.sleep((visiblityTimeoutSeconds * 2) * 1000);
        assertThat(buffSqs.receiveMessage(queueUrl).getMessages().size(), greaterThan(0));
    }

    /**
     * Tests by trying to send a message of size larger than the allowed limit. Also tests to see if
     * an exception is thrown when the user tries to set the max size more than the allowed limit of
     * 256 KiB
     */
    @Test(expected = AmazonClientException.class)
    public void sendMessage_MaxSizeExceeded_ThrowsAmazonClientException() {

        final byte[] bytes = new byte[MAX_SIZE_MESSAGE];
        new Random().nextBytes(bytes);
        final String randomString = new String(bytes,StringUtils.UTF8);

        SendMessageRequest request = new SendMessageRequest()
                                        .withMessageBody(randomString)
                                        .withQueueUrl(queueUrl);
        buffSqs.sendMessage(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMaxBatchSizeOnQueueBuffer_WhenMaxSizeExceeded_ThrowsIllegalArgumentException() {
        QueueBufferConfig config = new QueueBufferConfig();
        config.setMaxBatchSizeBytes(MAX_SIZE_MESSAGE);
    }

    @Test
    public void receiveMessage_WhenMessagesAreOnTheQueueAndLongPollIsEnabled_ReturnsMessage() throws Exception {
        String body = "test message_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
        // Use the normal client so we don't have to wait for the buffered messages to be sent
        sqsClient.sendMessage(new SendMessageRequest().withMessageBody(body).withQueueUrl(queueUrl));
        long start = System.nanoTime();

        ReceiveMessageRequest receiveRq = new ReceiveMessageRequest().withMaxNumberOfMessages(1)
                .withWaitTimeSeconds(60).withQueueUrl(queueUrl);
        List<Message> messages = buffSqs.receiveMessage(receiveRq).getMessages();
        assertThat(messages, hasSize(1));
        assertEquals(body, messages.get(0).getBody());

        long total = System.nanoTime() - start;

        if (TimeUnit.SECONDS.convert(total, TimeUnit.NANOSECONDS) > 60) {
            // we've waited for more than a minute for our message to
            // arrive. that's pretty bad.
            throw new Exception("Timed out waiting for the desired message to arrive");
        }
    }

}
