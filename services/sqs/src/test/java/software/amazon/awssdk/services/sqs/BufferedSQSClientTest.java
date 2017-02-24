package software.amazon.awssdk.services.sqs;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.makeThreadSafe;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.services.sqs.buffered.AmazonSqsBufferedAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResult;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;

public class BufferedSQSClientTest {

    private AmazonSQSAsync mock;
    private AmazonSqsBufferedAsyncClient client;

    @Before
    public void setup() {
        mock = createMock(AmazonSQSAsync.class);
        mock.shutdown();
        expectLastCall().asStub();
        makeThreadSafe(mock, false);
        client = new AmazonSqsBufferedAsyncClient(mock);
    }

    @After
    public void tearDown() {
        client.shutdown();
    }

    @Test
    public void concurrentSend_LongRunningSendRequest_DoesNotBlockSubsequentRequests() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Capture<SendMessageBatchRequest> capture = new Capture<SendMessageBatchRequest>();

        Capture<SendMessageBatchRequest> capture2 = new Capture<SendMessageBatchRequest>();

        // Artificially block the first request's response until we've
        // verified the second batch gets sent.
        expect(mock.sendMessageBatch(capture(capture))).andAnswer(new IAnswer<SendMessageBatchResult>() {
            @Override
            public SendMessageBatchResult answer() throws Throwable {
                latch.await();
                return new SendMessageBatchResult().withSuccessful(new SendMessageBatchResultEntry().withId("0")
                        .withMessageId("1"));
            }
        });

        // Second request gets a response right away.
        expect(mock.sendMessageBatch(capture(capture2))).andReturn(
                new SendMessageBatchResult().withSuccessful(new SendMessageBatchResultEntry().withId("0")
                        .withMessageId("2")));

        replay(mock);

        Future<SendMessageResult> future = client.sendMessageAsync(new SendMessageRequest().withQueueUrl(
                "https://example.com/").withMessageBody("hello world"));

        // Wait for the first batch request to time out and get closed.
        Thread.sleep(250);

        Future<SendMessageResult> future2 = client.sendMessageAsync(new SendMessageRequest().withQueueUrl(
                "https://example.com/").withMessageBody("hello world 2"));

        // Make sure the second batch doesn't block waiting for the first batch
        // to complete.
        SendMessageResult result = future2.get();
        verify(mock);

        assertEquals("2", result.getMessageId());
        assertTrue(capture2.hasCaptured());
        assertEquals(1, capture2.getValue().getEntries().size());
        assertEquals("hello world 2", capture2.getValue().getEntries().get(0).getMessageBody());

        // Unblock the first batch and wait for it to complete.
        latch.countDown();
        result = future.get();

        assertEquals("1", result.getMessageId());
        assertTrue(capture.hasCaptured());
        assertEquals(1, capture.getValue().getEntries().size());
        assertEquals("hello world", capture.getValue().getEntries().get(0).getMessageBody());
    }
}
