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

package software.amazon.awssdk.services.sqs.buffered;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.sqs.SQSAsyncClient;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResult;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResult;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResult;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResult;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResult;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResult;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResult;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResult;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResult;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResult;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResult;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResult;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResult;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.RemovePermissionResult;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResult;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResult;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResult;
import software.amazon.awssdk.util.VersionInfoUtils;

/**
 * AmazonSQSBufferedAsyncClient provides client-side batching of outgoing sendMessage, deleteMessage
 * and changeMessageVisibility calls. <br>
 * After receiving a call, rather than executing it right away, this client waits for a configurable
 * period of time ( default=200ms) for other calls of the same type to come in; if such calls do
 * come in, they are also not executed immediately, but instead are added to the batch. When the
 * batch becomes full or the timeout period expires, the entire batch is executed at once and the
 * results are returned to the callers. This method of operation leads to reduced operating costs
 * (since SQS charges per call and fewer calls are made) and increased overall throughput (since
 * more work is performed per call, and all fixed costs of making a call are amortized over a
 * greater amount of work). The cost of this method is increased latency for individual calls, since
 * calls spend some time waiting on the client side for the potential batch-mates to appear before
 * they are actually executed. <br>
 * This client also performs pre-fetching of messages from SQS. After the first receiveMessage call
 * is made, the client attempts not only to satisfy that call, but also pre-fetch extra messages to
 * store in a temporary buffer. Future receiveMessage calls will be satisfied from the buffer, and
 * only if the buffer is empty will the calling thread have to wait for the messages to be fetched.
 * The size of the buffer and the maximum number of threads used for prefetching are configurable. <br>
 * AmazonSQSBufferedAsyncClient is thread-safe.<br>
 */
public class SQSBufferedAsyncClient implements SQSAsyncClient {

    public static final String USER_AGENT = SQSBufferedAsyncClient.class.getSimpleName() + "/"
                                            + VersionInfoUtils.getVersion();

    private final CachingMap buffers = new CachingMap(16, (float) 0.75, true);
    private final SQSAsyncClient realSQS;
    private final QueueBufferConfig bufferConfigExemplar;

    public SQSBufferedAsyncClient(SQSAsyncClient paramRealSQS) {
        this(paramRealSQS, new QueueBufferConfig());
    }

    // route all future constructors to the most general one, because validation
    // happens here
    public SQSBufferedAsyncClient(SQSAsyncClient paramRealSQS, QueueBufferConfig config) {
        config.validate();
        realSQS = paramRealSQS;
        bufferConfigExemplar = config;
    }

    public CompletableFuture<SetQueueAttributesResult> setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest)
            throws 
                   AmazonClientException {
        ResultConverter.appendUserAgent(setQueueAttributesRequest, USER_AGENT);
        return realSQS.setQueueAttributes(setQueueAttributesRequest);
    }

    public CompletableFuture<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityBatchRequest, USER_AGENT);
        return realSQS.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    public CompletableFuture<ChangeMessageVisibilityResult> changeMessageVisibility(
            ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(changeMessageVisibilityRequest.getQueueUrl());
        return CompletableFuture.completedFuture(buffer.changeMessageVisibilitySync(changeMessageVisibilityRequest));
    }

    public CompletableFuture<SendMessageBatchResult> sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageBatchRequest, USER_AGENT);
        return realSQS.sendMessageBatch(sendMessageBatchRequest);
    }

    public CompletableFuture<SendMessageResult> sendMessage(SendMessageRequest sendMessageRequest) throws 
                                                                                       AmazonClientException {
        QueueBuffer buffer = getQBuffer(sendMessageRequest.getQueueUrl());
        ResultConverter.appendUserAgent(sendMessageRequest, USER_AGENT);
        return CompletableFuture.completedFuture(buffer.sendMessageSync(sendMessageRequest));
    }

    public CompletableFuture<ReceiveMessageResult> receiveMessage(ReceiveMessageRequest receiveMessageRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(receiveMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(receiveMessageRequest.getQueueUrl());
        return CompletableFuture.completedFuture(buffer.receiveMessageSync(receiveMessageRequest));
    }

    public CompletableFuture<DeleteMessageBatchResult> deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageBatchRequest, USER_AGENT);
        return realSQS.deleteMessageBatch(deleteMessageBatchRequest);
    }

    public CompletableFuture<DeleteMessageResult> deleteMessage(DeleteMessageRequest deleteMessageRequest) throws
            AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(deleteMessageRequest.getQueueUrl());
        return CompletableFuture.completedFuture(buffer.deleteMessageSync(deleteMessageRequest));
    }

    /**
     * Flushes all outstanding outbound requests. Calling this method will wait for
     * the pending outbound tasks in the {@link QueueBuffer} to finish.
     */
    public void flush() {
        for (QueueBuffer buffer : buffers.values()) {
            buffer.flush();
        }
    }

    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityBatchRequest, USER_AGENT);
        return realSQS.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(
            ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(changeMessageVisibilityRequest.getQueueUrl());
        return buffer.changeMessageVisibility(changeMessageVisibilityRequest, null);

    }

    public Future<SendMessageBatchResult> sendMessageBatchAsync(SendMessageBatchRequest sendMessageBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageBatchRequest, USER_AGENT);
        return realSQS.sendMessageBatch(sendMessageBatchRequest);
    }

    public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(sendMessageRequest.getQueueUrl());
        return buffer.sendMessage(sendMessageRequest, null);

    }

    public Future<ReceiveMessageResult> receiveMessageAsync(ReceiveMessageRequest receiveMessageRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(receiveMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(receiveMessageRequest.getQueueUrl());
        return buffer.receiveMessage(receiveMessageRequest, null);
    }

    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(DeleteMessageBatchRequest deleteMessageBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageBatchRequest, USER_AGENT);
        return realSQS.deleteMessageBatch(deleteMessageBatchRequest);
    }

    public Future<SetQueueAttributesResult> setQueueAttributesAsync(SetQueueAttributesRequest setQueueAttributesRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(setQueueAttributesRequest, USER_AGENT);
        return realSQS.setQueueAttributes(setQueueAttributesRequest);
    }

    public Future<GetQueueUrlResult> getQueueUrlAsync(GetQueueUrlRequest getQueueUrlRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(getQueueUrlRequest, USER_AGENT);
        return realSQS.getQueueUrl(getQueueUrlRequest);
    }

    public Future<RemovePermissionResult> removePermissionAsync(RemovePermissionRequest removePermissionRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(removePermissionRequest, USER_AGENT);
        return realSQS.removePermission(removePermissionRequest);
    }

    public CompletableFuture<GetQueueUrlResult> getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) throws 
                                                                                       AmazonClientException {
        ResultConverter.appendUserAgent(getQueueUrlRequest, USER_AGENT);
        return realSQS.getQueueUrl(getQueueUrlRequest);
    }

    public CompletableFuture<RemovePermissionResult> removePermission(RemovePermissionRequest removePermissionRequest) 
            throws AmazonClientException {
        ResultConverter.appendUserAgent(removePermissionRequest, USER_AGENT);
        return realSQS.removePermission(removePermissionRequest);
    }

    public Future<GetQueueAttributesResult> getQueueAttributesAsync(GetQueueAttributesRequest getQueueAttributesRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(getQueueAttributesRequest, USER_AGENT);
        return realSQS.getQueueAttributes(getQueueAttributesRequest);
    }

    public CompletableFuture<GetQueueAttributesResult> getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(getQueueAttributesRequest, USER_AGENT);
        return realSQS.getQueueAttributes(getQueueAttributesRequest);
    }

    public Future<PurgeQueueResult> purgeQueueAsync(PurgeQueueRequest purgeQueueRequest) throws 
                                                                                                AmazonClientException {
        ResultConverter.appendUserAgent(purgeQueueRequest, USER_AGENT);
        return realSQS.purgeQueue(purgeQueueRequest);
    }

    public CompletableFuture<PurgeQueueResult> purgeQueue(PurgeQueueRequest purgeQueueRequest) throws AmazonClientException {
        ResultConverter.appendUserAgent(purgeQueueRequest, USER_AGENT);
        return realSQS.purgeQueue(purgeQueueRequest);
    }

    public Future<DeleteQueueResult> deleteQueueAsync(DeleteQueueRequest deleteQueueRequest) throws 
                                                                                                    AmazonClientException {
        ResultConverter.appendUserAgent(deleteQueueRequest, USER_AGENT);
        return realSQS.deleteQueue(deleteQueueRequest);
    }

    public CompletableFuture<DeleteQueueResult> deleteQueue(DeleteQueueRequest deleteQueueRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(deleteQueueRequest, USER_AGENT);
        return realSQS.deleteQueue(deleteQueueRequest);
    }

    public Future<ListQueuesResult> listQueuesAsync(ListQueuesRequest listQueuesRequest) throws 
                                                                                                AmazonClientException {
        ResultConverter.appendUserAgent(listQueuesRequest, USER_AGENT);
        return realSQS.listQueues(listQueuesRequest);
    }

    public CompletableFuture<ListQueuesResult> listQueues(ListQueuesRequest listQueuesRequest) throws 
                                                                                   AmazonClientException {
        ResultConverter.appendUserAgent(listQueuesRequest, USER_AGENT);
        return realSQS.listQueues(listQueuesRequest);
    }

    public Future<CreateQueueResult> createQueueAsync(CreateQueueRequest createQueueRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(createQueueRequest, USER_AGENT);
        return realSQS.createQueue(createQueueRequest);
    }

    public CompletableFuture<CreateQueueResult> createQueue(CreateQueueRequest createQueueRequest) throws 
                                                                                       AmazonClientException {
        ResultConverter.appendUserAgent(createQueueRequest, USER_AGENT);
        return realSQS.createQueue(createQueueRequest);
    }

    public Future<AddPermissionResult> addPermissionAsync(AddPermissionRequest addPermissionRequest)
            throws 
                   AmazonClientException {
        ResultConverter.appendUserAgent(addPermissionRequest, USER_AGENT);
        return realSQS.addPermission(addPermissionRequest);
    }

    public CompletableFuture<AddPermissionResult> addPermission(AddPermissionRequest addPermissionRequest) 
            throws AmazonClientException {
        ResultConverter.appendUserAgent(addPermissionRequest, USER_AGENT);
        return realSQS.addPermission(addPermissionRequest);
    }

    public CompletableFuture<ListQueuesResult> listQueues() throws  AmazonClientException {
        return realSQS.listQueues(new ListQueuesRequest());
    }

    public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest)
            throws 
                   AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(deleteMessageRequest.getQueueUrl());
        return buffer.deleteMessage(deleteMessageRequest, null);
    }

    /**
     * Returns (creating it if necessary) a queue buffer for a particular queue Since we are only
     * storing a limited number of queue buffers, it is possible that as a result of calling this
     * method the least recently used queue buffer will be removed from our queue buffer cache
     *
     * @return a queue buffer associated with the provided queue URL. Never null
     */
    private synchronized QueueBuffer getQBuffer(String qUrl) {
        QueueBuffer toReturn = buffers.get(qUrl);
        if (null == toReturn) {
            QueueBufferConfig config = new QueueBufferConfig(bufferConfigExemplar);
            toReturn = new QueueBuffer(config, qUrl, realSQS);
            buffers.put(qUrl, toReturn);
        }
        return toReturn;
    }

    @Override
    public CompletableFuture<ListDeadLetterSourceQueuesResult> listDeadLetterSourceQueues(
            ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(listDeadLetterSourceQueuesRequest, USER_AGENT);
        return realSQS.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    @Override
    public void close() throws Exception {
        realSQS.close();
    }

    class CachingMap extends LinkedHashMap<String, QueueBuffer> {
        private static final long serialVersionUID = 1;
        private static final int MAX_ENTRIES = 100;

        public CachingMap(int initial, float loadFactor, boolean accessOrder) {
            super(initial, loadFactor, accessOrder);
        }

        protected boolean removeEldestEntry(java.util.Map.Entry<String, QueueBuffer> eldest) {
            return size() > MAX_ENTRIES;
        }

    }
}
