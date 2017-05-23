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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResult;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResult;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResult;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResult;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResult;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResult;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResult;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

public class SqsOperationsIntegrationTest extends IntegrationTestBase {

    private static final String ATTRIBUTE_VALUE = "42";
    private static final String ATTRIBUTE_NAME = "VisibilityTimeout";
    private static final String SPECIAL_CHARS = "%20%25~!@#$^&*(){}[]_-+\\<>/?";
    private static final String MESSAGE_BODY = "foobarbazbar" + SPECIAL_CHARS;

    private final String queueName = getUniqueQueueName();
    private final String deadLetterQueueName = "DLQ-" + queueName;

    private String queueUrl;
    private String deadLetterQueueUrl;

    @After
    public void tearDown() {
        sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
        sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(deadLetterQueueUrl).build());
    }

    /**
     * Tests that each SQS operation can be called correctly, and that the result data is correctly
     * unmarshalled.
     */
    @Test
    public void testSqsOperations() throws Exception {
        runCreateQueueTest();
        runGetQueueUrlTest();
        runListQueuesTest();
        runSetQueueAttributesTest();
        runGetQueueAttributesTest();
        runAddPermissionTest();
        runRemovePermissionTest();
        runSendMessageTest();
        ReceiveMessageResult receiveMessageResult = runReceiveMessageTest();
        runSendMessageBatch();
        String receiptHandle = runChangeMessageVisibilityTest(receiveMessageResult);
        runDeleteMessageTest(receiptHandle);
        runDlqTests();
        runDeleteMessageWithInvalidReceiptTest();
    }

    private void runCreateQueueTest() {
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(queueName).build();
        CreateQueueResult createQueueResult = sqs.createQueue(createQueueRequest).join();
        queueUrl = createQueueResult.queueUrl();
        assertNotEmpty(queueUrl);
    }

    private void runGetQueueUrlTest() {
        GetQueueUrlResult queueUrlResult = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).join();
        assertEquals(queueUrl, queueUrlResult.queueUrl());
    }

    private void runListQueuesTest() {
        ResponseMetadata responseMetadata;
        ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().queueNamePrefix(queueName).build();
        ListQueuesResult listQueuesResult = sqs.listQueues(listQueuesRequest).join();
        assertEquals(1, listQueuesResult.queueUrls().size());
        assertEquals(queueUrl, listQueuesResult.queueUrls().get(0));
    }

    private void runSetQueueAttributesTest() {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        SetQueueAttributesRequest setQueueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributes(attributes)
                .build();
        sqs.setQueueAttributes(setQueueAttributesRequest);

    }

    private void runGetQueueAttributesTest() throws InterruptedException {
        Thread.sleep(1000 * 10);
        GetQueueAttributesResult queueAttributesResult = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                                                                                              .queueUrl(queueUrl)
                                                                                              .attributeNames(new String[]{
                                                                                                      ATTRIBUTE_NAME}).build()).join();
        assertEquals(1, queueAttributesResult.attributes().size());
        Map<String, String> attributes2 = queueAttributesResult.attributes();
        assertEquals(1, attributes2.size());
        assertNotNull(attributes2.get(ATTRIBUTE_NAME));
    }

    private void runAddPermissionTest() {
        sqs.addPermission(AddPermissionRequest.builder().actions(new String[]{"SendMessage", "DeleteMessage"})
                                        .awsAccountIds(new String[]{getAccountId()}).label("foo-label")
                                        .queueUrl(queueUrl).build());
    }

    private void runRemovePermissionTest() throws InterruptedException {
        Thread.sleep(1000 * 2);
        sqs.removePermission(RemovePermissionRequest.builder().label("foo-label").queueUrl(queueUrl).build());
    }

    private void runSendMessageTest() {
        for (int i = 0; i < 10; i++) {
            SendMessageResult sendMessageResult = sqs.sendMessage(SendMessageRequest.builder().delaySeconds(1)
                                                                                .messageBody(MESSAGE_BODY)
                                                                                .queueUrl(queueUrl).build()).join();
            assertNotEmpty(sendMessageResult.messageId());
            assertNotEmpty(sendMessageResult.md5OfMessageBody());
        }
    }

    private ReceiveMessageResult runReceiveMessageTest() {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(new Integer(8))
                .attributeNames(new String[]{"SenderId",
                        "SentTimestamp", "All"})
                .build();
        ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest).join();
        assertThat(receiveMessageResult.messages(), not(empty()));
        Message message = receiveMessageResult.messages().get(0);
        assertEquals(MESSAGE_BODY, message.body());
        assertNotEmpty(message.md5OfBody());
        assertNotEmpty(message.messageId());
        assertNotEmpty(message.receiptHandle());
        assertThat(message.attributes().size(), greaterThan(3));

        for (Iterator<Entry<String, String>> iterator = message.attributes().entrySet().iterator(); iterator
                .hasNext(); ) {
            Entry<String, String> entry = iterator.next();
            assertNotEmpty((entry.getKey()));
            assertNotEmpty((entry.getValue()));
        }
        return receiveMessageResult;
    }

    private void runSendMessageBatch() {
        SendMessageBatchResult sendMessageBatchResult = sqs.sendMessageBatch(SendMessageBatchRequest.builder()
                .queueUrl(queueUrl).entries(
                        SendMessageBatchRequestEntry.builder().id("1").messageBody("1" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("2").messageBody("2" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("3").messageBody("3" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("4").messageBody("4" + SPECIAL_CHARS).build(),
                        SendMessageBatchRequestEntry.builder().id("5").messageBody("5" + SPECIAL_CHARS).build())
                .build())
                .join();
        assertNotNull(sendMessageBatchResult.failed());
        assertThat(sendMessageBatchResult.successful().size(), greaterThan(0));
        assertNotNull(sendMessageBatchResult.successful().get(0).id());
        assertNotNull(sendMessageBatchResult.successful().get(0).md5OfMessageBody());
        assertNotNull(sendMessageBatchResult.successful().get(0).messageId());
    }

    private String runChangeMessageVisibilityTest(ReceiveMessageResult receiveMessageResult) {
        String receiptHandle = (receiveMessageResult.messages().get(0)).receiptHandle();
        sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl)
                                                  .receiptHandle(receiptHandle).visibilityTimeout(new Integer(123)).build());
        return receiptHandle;
    }

    private void runDeleteMessageTest(String receiptHandle) {
        sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
    }

    private void runDlqTests() throws InterruptedException {
        CreateQueueResult createDLQResult = sqs.createQueue(CreateQueueRequest.builder()
                                                                          .queueName(deadLetterQueueName).build()).join();
        deadLetterQueueUrl = createDLQResult.queueUrl();
        // We have to get the ARN for the DLQ in order to set it on the redrive policy
        GetQueueAttributesResult deadLetterQueueAttributes = sqs.getQueueAttributes(
                GetQueueAttributesRequest.builder().queueUrl(deadLetterQueueUrl).attributeNames(Arrays.asList(QueueAttributeName.QueueArn.toString())).build())
                .join();
        assertNotNull(deadLetterQueueUrl);
        // Configure the DLQ
        final String deadLetterConfigAttributeName = "RedrivePolicy";
        final String deadLetterConfigAttributeValue = "{\"maxReceiveCount\" : 5, \"deadLetterTargetArn\" : \""
                                                      + deadLetterQueueAttributes.attributes()
                                                              .get(QueueAttributeName.QueueArn.toString()) + "\"}";
        sqs.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(
                Collections.singletonMap(deadLetterConfigAttributeName, deadLetterConfigAttributeValue)).build());
        // List the DLQ
        Thread.sleep(1000 * 10);
        ListDeadLetterSourceQueuesResult listDeadLetterSourceQueuesResult = sqs
                .listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest.builder().queueUrl(deadLetterQueueUrl).build()).join();
        assertThat(listDeadLetterSourceQueuesResult.queueUrls(), contains(queueUrl));
    }

    private void runDeleteMessageWithInvalidReceiptTest() {
        try {
            sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(
                    queueUrl).receiptHandle(
                    "alkdjfadfaldkjfdjkfldjfkjdljdljfljdjfldjfljflsjdf").build());
            fail("Expected an AmazonServiceException, but wasn't thrown");
        } catch (ReceiptHandleIsInvalidException e) {
            assertEquals("ReceiptHandleIsInvalid", e.getErrorCode());
            assertEquals(ErrorType.Client, e.getErrorType());
            assertNotEmpty(e.getMessage());
            assertNotEmpty(e.getRequestId());
            assertEquals("SQSClient", e.getServiceName());
            assertThat(e.getStatusCode(), allOf(greaterThanOrEqualTo(400), lessThan(500)));
        }
    }
}
