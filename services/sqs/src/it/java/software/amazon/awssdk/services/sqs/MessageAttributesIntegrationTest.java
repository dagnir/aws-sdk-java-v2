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

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResult;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResult;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;
import software.amazon.awssdk.util.ImmutableMapParameter;

/**
 * Integration tests for the SQS message attributes.
 */
public class MessageAttributesIntegrationTest extends IntegrationTestBase {

    private static final String MESSAGE_BODY = "message-body-" + System.currentTimeMillis();

    private String queueUrl;

    @Before
    public void setup() {
        queueUrl = createQueue(sqsAsync);
    }

    @After
    public void tearDown() throws Exception {
        sqsAsync.deleteQueue(new DeleteQueueRequest(queueUrl));
    }

    @Test
    public void sendMessage_WithMessageAttributes_ResultHasMd5OfMessageAttributes() {
        SendMessageResult sendMessageResult = sendTestMessage();
        assertNotEmpty(sendMessageResult.getMD5OfMessageBody());
        assertNotEmpty(sendMessageResult.getMD5OfMessageAttributes());
    }

    /**
     * Makes sure we don't modify the state of ByteBuffer backed attributes in anyway internally
     * before returning the result to the customer. See https://github.com/aws/aws-sdk-java/pull/459
     * for reference
     */
    @Test
    public void receiveMessage_WithBinaryAttributeValue_DoesNotChangeStateOfByteBuffer() {
        byte[] bytes = new byte[]{1, 1, 1, 0, 0, 0};
        String byteBufferAttrName = "byte-buffer-attr";
        Map<String, MessageAttributeValue> attrs = ImmutableMapParameter.of(byteBufferAttrName,
                new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(bytes)));

        sqsAsync.sendMessage(new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody("test")
                .withMessageAttributes(attrs));
        // Long poll to make sure we get the message back
        List<Message> messages = sqsAsync.receiveMessage(
                new ReceiveMessageRequest(queueUrl).withMessageAttributeNames("All").withWaitTimeSeconds(20)).join()
                .getMessages();

        ByteBuffer actualByteBuffer = messages.get(0).getMessageAttributes().get(byteBufferAttrName).getBinaryValue();
        assertEquals(bytes.length, actualByteBuffer.remaining());
        assertArrayEquals(bytes, actualByteBuffer.array());
    }

    @Test
    public void receiveMessage_WithAllAttributesRequested_ReturnsAttributes() throws Exception {
        sendTestMessage();

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl).withWaitTimeSeconds(5)
                .withVisibilityTimeout(0).withMessageAttributeNames("All");
        ReceiveMessageResult receiveMessageResult = sqsAsync.receiveMessage(receiveMessageRequest).join();

        assertFalse(receiveMessageResult.getMessages().isEmpty());
        Message message = receiveMessageResult.getMessages().get(0);
        assertEquals(MESSAGE_BODY, message.getBody());
        assertNotEmpty(message.getMD5OfBody());
        assertNotEmpty(message.getMD5OfMessageAttributes());
    }

    /**
     * Tests SQS operations that involve message attributes checksum.
     */
    @Test
    public void receiveMessage_WithNoAttributesRequested_DoesNotReturnAttributes() throws Exception {
        sendTestMessage();

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl).withWaitTimeSeconds(5)
                .withVisibilityTimeout(0);
        ReceiveMessageResult receiveMessageResult = sqsAsync.receiveMessage(receiveMessageRequest).join();

        assertFalse(receiveMessageResult.getMessages().isEmpty());
        Message message = receiveMessageResult.getMessages().get(0);
        assertEquals(MESSAGE_BODY, message.getBody());
        assertNotEmpty(message.getMD5OfBody());
        assertNull(message.getMD5OfMessageAttributes());
    }

    @Test
    public void sendMessageBatch_WithMessageAttributes_ResultHasMd5OfMessageAttributes() {
        SendMessageBatchResult sendMessageBatchResult = sqsAsync.sendMessageBatch(new SendMessageBatchRequest()
                .withQueueUrl(queueUrl).withEntries(
                        new SendMessageBatchRequestEntry("1", MESSAGE_BODY)
                                .withMessageAttributes(createRandomAttributeValues(1)),
                        new SendMessageBatchRequestEntry("2", MESSAGE_BODY)
                                .withMessageAttributes(createRandomAttributeValues(2)),
                        new SendMessageBatchRequestEntry("3", MESSAGE_BODY)
                                .withMessageAttributes(createRandomAttributeValues(3)),
                        new SendMessageBatchRequestEntry("4", MESSAGE_BODY)
                                .withMessageAttributes(createRandomAttributeValues(4)),
                        new SendMessageBatchRequestEntry("5", MESSAGE_BODY)
                                .withMessageAttributes(createRandomAttributeValues(5)))).join();

        assertThat(sendMessageBatchResult.getSuccessful().size(), greaterThan(0));
        assertNotEmpty(sendMessageBatchResult.getSuccessful().get(0).getId());
        assertNotEmpty(sendMessageBatchResult.getSuccessful().get(0).getMD5OfMessageBody());
        assertNotEmpty(sendMessageBatchResult.getSuccessful().get(0).getMD5OfMessageAttributes());
    }

    private SendMessageResult sendTestMessage() {
        SendMessageResult sendMessageResult = sqsAsync.sendMessage(new SendMessageRequest(queueUrl, MESSAGE_BODY)
                .withMessageAttributes(createRandomAttributeValues(10))).join();
        return sendMessageResult;
    }
}
