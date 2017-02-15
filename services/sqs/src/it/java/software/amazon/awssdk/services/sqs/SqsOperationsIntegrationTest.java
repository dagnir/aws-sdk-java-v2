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

    private final AmazonSQSAsync sqsClient = getSharedSqsAsyncClient();
    private final String queueName = getUniqueQueueName();
    private final String deadLetterQueueName = "DLQ-" + queueName;

    private String queueUrl;
    private String deadLetterQueueUrl;

    @After
    public void tearDown() {
        sqsClient.deleteQueue(new DeleteQueueRequest().withQueueUrl(queueUrl));
        sqsClient.deleteQueue(new DeleteQueueRequest().withQueueUrl(deadLetterQueueUrl));
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
        CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(queueName);
        CreateQueueResult createQueueResult = sqsClient.createQueue(createQueueRequest);
        queueUrl = createQueueResult.getQueueUrl();
        assertNotEmpty(queueUrl);
        ResponseMetadata responseMetadata = sqsClient.getCachedResponseMetadata(createQueueRequest);
        assertNotNull(responseMetadata.getRequestId());
    }

    private void runGetQueueUrlTest() {
        GetQueueUrlResult getQueueUrlResult = sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName(queueName));
        assertEquals(queueUrl, getQueueUrlResult.getQueueUrl());
    }

    private void runListQueuesTest() {
        ResponseMetadata responseMetadata;
        ListQueuesRequest listQueuesRequest = new ListQueuesRequest().withQueueNamePrefix(queueName);
        ListQueuesResult listQueuesResult = sqsClient.listQueues(listQueuesRequest);
        assertEquals(1, listQueuesResult.getQueueUrls().size());
        assertEquals(queueUrl, listQueuesResult.getQueueUrls().get(0));
        responseMetadata = sqsClient.getCachedResponseMetadata(listQueuesRequest);
        assertNotNull(responseMetadata.getRequestId());
    }

    private void runSetQueueAttributesTest() {
        ResponseMetadata responseMetadata;
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        SetQueueAttributesRequest setQueueAttributesRequest = new SetQueueAttributesRequest();
        sqsClient.setQueueAttributes(setQueueAttributesRequest.withQueueUrl(queueUrl).withAttributes(attributes));
        responseMetadata = sqsClient.getCachedResponseMetadata(setQueueAttributesRequest);
        assertNotNull(responseMetadata.getRequestId());
    }

    private void runGetQueueAttributesTest() throws InterruptedException {
        Thread.sleep(1000 * 10);
        GetQueueAttributesResult queueAttributesResult = sqsClient.getQueueAttributes(new GetQueueAttributesRequest()
                                                                                              .withQueueUrl(queueUrl).withAttributeNames(new String[] {ATTRIBUTE_NAME}));
        assertEquals(1, queueAttributesResult.getAttributes().size());
        Map<String, String> attributes2 = queueAttributesResult.getAttributes();
        assertEquals(1, attributes2.size());
        assertNotNull(attributes2.get(ATTRIBUTE_NAME));
    }

    private void runAddPermissionTest() {
        sqsClient.addPermission(new AddPermissionRequest().withActions(new String[] {"SendMessage", "DeleteMessage"})
                                                          .withAWSAccountIds(new String[] {getAccountId()}).withLabel("foo-label").withQueueUrl(queueUrl));
    }

    private void runRemovePermissionTest() throws InterruptedException {
        Thread.sleep(1000 * 2);
        sqsClient.removePermission(new RemovePermissionRequest().withLabel("foo-label").withQueueUrl(queueUrl));
    }

    private void runSendMessageTest() {
        for (int i = 0; i < 10; i++) {
            SendMessageResult sendMessageResult = sqsClient.sendMessage(new SendMessageRequest().withDelaySeconds(1)
                                                                                                .withMessageBody(MESSAGE_BODY).withQueueUrl(queueUrl));
            assertNotEmpty(sendMessageResult.getMessageId());
            assertNotEmpty(sendMessageResult.getMD5OfMessageBody());
        }
    }

    private ReceiveMessageResult runReceiveMessageTest() {
        ResponseMetadata responseMetadata;
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
        ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest
                                                                                     .withQueueUrl(queueUrl).withWaitTimeSeconds(5).withMaxNumberOfMessages(new Integer(8))
                                                                                     .withAttributeNames(new String[] {"SenderId", "SentTimestamp", "All"}));
        assertThat(receiveMessageResult.getMessages(), not(empty()));
        responseMetadata = sqsClient.getCachedResponseMetadata(receiveMessageRequest);
        assertNotNull(responseMetadata.getRequestId());
        Message message = receiveMessageResult.getMessages().get(0);
        assertEquals(MESSAGE_BODY, message.getBody());
        assertNotEmpty(message.getMD5OfBody());
        assertNotEmpty(message.getMessageId());
        assertNotEmpty(message.getReceiptHandle());
        assertThat(message.getAttributes().size(), greaterThan(3));

        for (Iterator<Entry<String, String>> iterator = message.getAttributes().entrySet().iterator(); iterator
                .hasNext(); ) {
            Entry<String, String> entry = iterator.next();
            assertNotEmpty((entry.getKey()));
            assertNotEmpty((entry.getValue()));
        }
        return receiveMessageResult;
    }

    private void runSendMessageBatch() {
        SendMessageBatchResult sendMessageBatchResult = sqsClient.sendMessageBatch(new SendMessageBatchRequest()
                                                                                           .withQueueUrl(queueUrl).withEntries(
                        new SendMessageBatchRequestEntry().withId("1").withMessageBody("1" + SPECIAL_CHARS),
                        new SendMessageBatchRequestEntry().withId("2").withMessageBody("2" + SPECIAL_CHARS),
                        new SendMessageBatchRequestEntry().withId("3").withMessageBody("3" + SPECIAL_CHARS),
                        new SendMessageBatchRequestEntry().withId("4").withMessageBody("4" + SPECIAL_CHARS),
                        new SendMessageBatchRequestEntry().withId("5").withMessageBody("5" + SPECIAL_CHARS)));
        assertNotNull(sendMessageBatchResult.getFailed());
        assertThat(sendMessageBatchResult.getSuccessful().size(), greaterThan(0));
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getId());
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getMD5OfMessageBody());
        assertNotNull(sendMessageBatchResult.getSuccessful().get(0).getMessageId());
    }

    private String runChangeMessageVisibilityTest(ReceiveMessageResult receiveMessageResult) {
        String receiptHandle = (receiveMessageResult.getMessages().get(0)).getReceiptHandle();
        sqsClient.changeMessageVisibility(new ChangeMessageVisibilityRequest().withQueueUrl(queueUrl)
                                                                              .withReceiptHandle(receiptHandle).withVisibilityTimeout(new Integer(123)));
        return receiptHandle;
    }

    private void runDeleteMessageTest(String receiptHandle) {
        sqsClient.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(receiptHandle));
    }

    private void runDlqTests() throws InterruptedException {
        CreateQueueResult createDLQResult = sqsClient.createQueue(new CreateQueueRequest()
                                                                          .withQueueName(deadLetterQueueName));
        deadLetterQueueUrl = createDLQResult.getQueueUrl();
        // We have to get the ARN for the DLQ in order to set it on the redrive policy
        GetQueueAttributesResult deadLetterQueueAttributes = sqsClient.getQueueAttributes(deadLetterQueueUrl,
                                                                                          Arrays.asList(QueueAttributeName.QueueArn.toString()));
        assertNotNull(deadLetterQueueUrl);
        // Configure the DLQ
        final String deadLetterConfigAttributeName = "RedrivePolicy";
        final String deadLetterConfigAttributeValue = "{\"maxReceiveCount\" : 5, \"deadLetterTargetArn\" : \""
                                                      + deadLetterQueueAttributes.getAttributes().get(QueueAttributeName.QueueArn.toString()) + "\"}";
        sqsClient.setQueueAttributes(new SetQueueAttributesRequest().withQueueUrl(queueUrl).withAttributes(
                Collections.singletonMap(deadLetterConfigAttributeName, deadLetterConfigAttributeValue)));
        // List the DLQ
        Thread.sleep(1000 * 10);
        ListDeadLetterSourceQueuesResult listDeadLetterSourceQueuesResult = sqsClient
                .listDeadLetterSourceQueues(new ListDeadLetterSourceQueuesRequest().withQueueUrl(deadLetterQueueUrl));
        assertThat(listDeadLetterSourceQueuesResult.getQueueUrls(), contains(queueUrl));
    }

    private void runDeleteMessageWithInvalidReceiptTest() {
        try {
            ;
            sqsClient
                    .deleteMessage(new DeleteMessageRequest(
                            queueUrl,
                            "alkdjfadfaldkjfdjkfldjfkjdljdljfljdjfldjfljflsjdf"));
            fail("Expected an AmazonServiceException, but wasn't thrown");
        } catch (ReceiptHandleIsInvalidException e) {
            assertEquals("ReceiptHandleIsInvalid", e.getErrorCode());
            assertEquals(ErrorType.Client, e.getErrorType());
            assertNotEmpty(e.getMessage());
            assertNotEmpty(e.getRequestId());
            assertEquals("AmazonSQS", e.getServiceName());
            assertThat(e.getStatusCode(), allOf(greaterThanOrEqualTo(400), lessThan(500)));
        }
    }
}
