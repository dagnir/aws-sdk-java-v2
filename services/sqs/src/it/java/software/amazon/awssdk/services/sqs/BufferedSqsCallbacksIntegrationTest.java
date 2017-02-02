package software.amazon.awssdk.services.sqs;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import software.amazon.awssdk.handlers.AsyncHandler;
import software.amazon.awssdk.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import software.amazon.awssdk.services.sqs.buffered.QueueBufferConfig;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResult;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;

public class BufferedSqsCallbacksIntegrationTest extends IntegrationTestBase {

    private static final int CALLBACK_TIMEOUT_IN_MILLIS = 10 * 1000;

    private AmazonSQSBufferedAsyncClient buffSqs;
    private AmazonSQSAsync realSqs;
    private String queueUrl;

    @Before
    public void setup() {
        realSqs = createSqsAyncClient();
        buffSqs = new AmazonSQSBufferedAsyncClient(realSqs, new QueueBufferConfig());
        queueUrl = createQueue(buffSqs);
    }

    @After
    public void tearDown() {
        buffSqs.deleteQueue(queueUrl);
        buffSqs.shutdown();
    }

    @Test
    public void testCallbacks() throws Exception {
        testSendSuccessCallback();
        testReceiveSuccessCallback();
    }

    public void testSendSuccessCallback() throws InterruptedException, ExecutionException {
        String body = "test message_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
        SendMessageRequest request = new SendMessageRequest().withMessageBody(body).withQueueUrl(queueUrl)
                .withMessageAttributes(createRandomAttributeValues(10));

        AsyncHandler<SendMessageRequest, SendMessageResult> sendCallback = mock(SendAsyncCallback.class);
        Future<SendMessageResult> sendMessageResultFuture = buffSqs.sendMessageAsync(request, sendCallback);

        SendMessageResult sendMessageResult = sendMessageResultFuture.get();
        verify(sendCallback, timeout(CALLBACK_TIMEOUT_IN_MILLIS)).onSuccess(Mockito.any(SendMessageRequest.class),
                Mockito.any(SendMessageResult.class));
        assertNotNull(sendMessageResult.getMD5OfMessageAttributes());
    }

    public void testReceiveSuccessCallback() throws InterruptedException, ExecutionException {
        AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> receiveCallback = mock(ReceiveAsyncCallback.class);

        ReceiveMessageRequest receiveRq = new ReceiveMessageRequest().withMaxNumberOfMessages(1).withQueueUrl(queueUrl);
        buffSqs.receiveMessageAsync(receiveRq, receiveCallback).get();

        verify(receiveCallback, timeout(CALLBACK_TIMEOUT_IN_MILLIS)).onSuccess(
                Mockito.any(ReceiveMessageRequest.class), Mockito.any(ReceiveMessageResult.class));
    }

    /**
     * See https://github.com/aws/aws-sdk-java/pull/483 for additional information.
     */
    @Test
    public void receiveMessageWithNonStandardRequest_CallsOnSuccessHandlerAfterFetchingFromSqs() throws Exception {
        realSqs.sendMessage(queueUrl, "test");
        AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> receiveCallback = mock(ReceiveAsyncCallback.class);

        // A custom visibility timeout and requesting message attributes will force the buffered
        // client to go back to SQS rather then fulfill from the buffer. Regardless the onSuccess
        // callback should be invoked
        ReceiveMessageRequest receiveRq = new ReceiveMessageRequest().withMaxNumberOfMessages(1).withQueueUrl(queueUrl)
                .withMessageAttributeNames("All").withVisibilityTimeout(20);
        buffSqs.receiveMessageAsync(receiveRq, receiveCallback).get();

        verify(receiveCallback, timeout(CALLBACK_TIMEOUT_IN_MILLIS)).onSuccess(
                Mockito.any(ReceiveMessageRequest.class), Mockito.any(ReceiveMessageResult.class));

    }

    /**
     * To avoid generic arguments warnings when mocking
     */
    private interface SendAsyncCallback extends AsyncHandler<SendMessageRequest, SendMessageResult> {
    }

    /**
     * To avoid generic arguments warnings when mocking
     */
    private interface ReceiveAsyncCallback extends AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> {
    }

}
