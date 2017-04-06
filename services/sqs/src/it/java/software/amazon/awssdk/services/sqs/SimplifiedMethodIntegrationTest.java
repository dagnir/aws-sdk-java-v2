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
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResult;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResult;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResult;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResult;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResult;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * These tests are more or less identical to SqsOperationsIntegrationTest. The only difference is
 * these use the simplified variants of the operation methods rather then the typical methods that
 * take some form of a request object.
 */
public class SimplifiedMethodIntegrationTest extends IntegrationTestBase {

    private static final String ATTRIBUTE_VALUE = "42";
    private static final String ATTRIBUTE_NAME = "VisibilityTimeout";
    private static final String MESSAGE_BODY = "foobarbazbar";

    private final String queueName = getUniqueQueueName();
    private String queueUrl;

    /**
     * Releases all resources used by these tests.
     */
    @After
    public void tearDown() throws Exception {
        sqs.deleteQueue(new DeleteQueueRequest(queueUrl));
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
        queueUrl = sqs.createQueue(new CreateQueueRequest(queueName)).join().getQueueUrl();
        assertNotEmpty(queueUrl);
    }

    private void runGetQueueTest() {
        GetQueueUrlResult getQueueUrlResult = sqs.getQueueUrl(new GetQueueUrlRequest(queueName)).join();
        assertEquals(queueUrl, getQueueUrlResult.getQueueUrl());
    }

    private void runListQueuesTest() {
        ListQueuesResult listQueuesResult = sqs.listQueues(new ListQueuesRequest(queueName)).join();
        assertEquals(1, listQueuesResult.getQueueUrls().size());
        assertEquals(queueUrl, listQueuesResult.getQueueUrls().get(0));
    }

    private void runSetAttributesTest() throws InterruptedException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        sqs.setQueueAttributes(new SetQueueAttributesRequest(queueUrl, attributes)).join();

        Thread.sleep(1000 * 10);
        GetQueueAttributesResult queueAttributesResult = sqs.getQueueAttributes(new GetQueueAttributesRequest(queueUrl,
                Arrays.asList(ATTRIBUTE_NAME))).join();
        assertEquals(1, queueAttributesResult.getAttributes().size());
        Map<String, String> attributes2 = queueAttributesResult.getAttributes();
        assertEquals(1, attributes2.size());
        assertNotNull(attributes2.get(ATTRIBUTE_NAME));
    }

    private void runAddPermissionTest() {
        sqs.addPermission(new AddPermissionRequest(queueUrl, "foo-label", Arrays.asList(getAccountId()),
                Arrays.asList("SendMessage", "DeleteMessage"))).join();
    }

    private void runRemovePermissionTest() throws InterruptedException {
        Thread.sleep(1000 * 2);
        sqs.removePermission(new RemovePermissionRequest(queueUrl, "foo-label")).join();
    }

    private void runSendMessageTest() {
        for (int i = 0; i < 10; i++) {
            SendMessageResult sendMessageResult = sqs.sendMessage(new SendMessageRequest(queueUrl, MESSAGE_BODY)).join();
            assertNotEmpty(sendMessageResult.getMessageId());
            assertNotEmpty(sendMessageResult.getMD5OfMessageBody());
        }
    }

    private ReceiveMessageResult runReceiveMessageTest() {
        ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(new ReceiveMessageRequest(queueUrl)).join();
        assertThat(receiveMessageResult.getMessages(), not(empty()));
        Message message = receiveMessageResult.getMessages().get(0);
        assertEquals(MESSAGE_BODY, message.getBody());
        assertNotEmpty(message.getMD5OfBody());
        assertNotEmpty(message.getMessageId());
        assertNotEmpty(message.getReceiptHandle());

        for (Iterator<Entry<String, String>> iterator = message.getAttributes().entrySet().iterator(); iterator
                .hasNext(); ) {
            Entry<String, String> entry = iterator.next();
            assertNotEmpty((entry.getKey()));
            assertNotEmpty((entry.getValue()));
        }
        return receiveMessageResult;
    }

    private void runSendMessageBatchTest() {
        SendMessageBatchResult sendMessageBatchResult = sqs.sendMessageBatch(new SendMessageBatchRequest(queueUrl, Arrays.asList(
                new SendMessageBatchRequestEntry().withId("1").withMessageBody("1"), new SendMessageBatchRequestEntry()
                        .withId("2").withMessageBody("2"), new SendMessageBatchRequestEntry().withId("3")
                        .withMessageBody("3"), new SendMessageBatchRequestEntry().withId("4").withMessageBody("4"),
                new SendMessageBatchRequestEntry().withId("5").withMessageBody("5")))).join();

        assertNotNull(sendMessageBatchResult.getFailed());
        assertThat(sendMessageBatchResult.getSuccessful().size(), greaterThan(0));
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getId());
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getMD5OfMessageBody());
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getMessageId());
    }

    private String runChangeMessageVisibilityTest(ReceiveMessageResult receiveMessageResult) {
        String receiptHandle = (receiveMessageResult.getMessages().get(0)).getReceiptHandle();
        sqs.changeMessageVisibility(new ChangeMessageVisibilityRequest(queueUrl, receiptHandle, 123));
        return receiptHandle;
    }

    private void runDeleteMessageTest(String receiptHandle) {
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, receiptHandle));
    }
}
