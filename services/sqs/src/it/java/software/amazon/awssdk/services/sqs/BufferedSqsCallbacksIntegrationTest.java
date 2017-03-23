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
import software.amazon.awssdk.services.sqs.buffered.SqsBufferedAsyncClient;
import software.amazon.awssdk.services.sqs.buffered.QueueBufferConfig;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResult;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;

public class BufferedSqsCallbacksIntegrationTest extends IntegrationTestBase {

    private static final int CALLBACK_TIMEOUT_IN_MILLIS = 10 * 1000;

    private SqsBufferedAsyncClient buffSqs;
    private SQSAsyncClient realSqs;
    private String queueUrl;

    @Before
    public void setup() {
        realSqs = createSqsAyncClient();
        buffSqs = new SqsBufferedAsyncClient(realSqs, new QueueBufferConfig());
        queueUrl = createQueue(buffSqs);
    }

    @After
    public void tearDown() throws Exception{
        buffSqs.deleteQueue(new DeleteQueueRequest(queueUrl));
        buffSqs.close();
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
        Future<SendMessageResult> sendMessageResultFuture = buffSqs.sendMessage(request);

        SendMessageResult sendMessageResult = sendMessageResultFuture.get();
        verify(sendCallback, timeout(CALLBACK_TIMEOUT_IN_MILLIS)).onSuccess(Mockito.any(SendMessageRequest.class),
                                                                            Mockito.any(SendMessageResult.class));
        assertNotNull(sendMessageResult.getMD5OfMessageAttributes());
    }

    public void testReceiveSuccessCallback() throws InterruptedException, ExecutionException {
        AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> receiveCallback = mock(ReceiveAsyncCallback.class);

        ReceiveMessageRequest receiveRq = new ReceiveMessageRequest().withMaxNumberOfMessages(1).withQueueUrl(queueUrl);
        buffSqs.receiveMessage(receiveRq).get();

        verify(receiveCallback, timeout(CALLBACK_TIMEOUT_IN_MILLIS)).onSuccess(
                Mockito.any(ReceiveMessageRequest.class), Mockito.any(ReceiveMessageResult.class));
    }

    /**
     * See https://github.com/aws/aws-sdk-java/pull/483 for additional information.
     */
    @Test
    public void receiveMessageWithNonStandardRequest_CallsOnSuccessHandlerAfterFetchingFromSqs() throws Exception {
        realSqs.sendMessage(new SendMessageRequest(queueUrl, "test"));
        AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> receiveCallback = mock(ReceiveAsyncCallback.class);

        // A custom visibility timeout and requesting message attributes will force the buffered
        // client to go back to SQS rather then fulfill from the buffer. Regardless the onSuccess
        // callback should be invoked
        ReceiveMessageRequest receiveRq = new ReceiveMessageRequest().withMaxNumberOfMessages(1).withQueueUrl(queueUrl)
                                                                     .withMessageAttributeNames("All").withVisibilityTimeout(20);
        buffSqs.receiveMessage(receiveRq).get();

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
