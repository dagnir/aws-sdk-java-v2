/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.handlers.AsyncHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.AmazonSQSAsync;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResult;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResult;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResult;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResult;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
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
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
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
public class AmazonSQSBufferedAsyncClient implements AmazonSQSAsync {

    public static final String USER_AGENT = AmazonSQSBufferedAsyncClient.class.getSimpleName() + "/"
            + VersionInfoUtils.getVersion();

    private final CachingMap buffers = new CachingMap(16, (float) 0.75, true);
    private final AmazonSQSAsync realSQS;
    private final QueueBufferConfig bufferConfigExemplar;

    public AmazonSQSBufferedAsyncClient(AmazonSQSAsync paramRealSQS) {
        this(paramRealSQS, new QueueBufferConfig());
    }

    // route all future constructors to the most general one, because validation
    // happens here
    public AmazonSQSBufferedAsyncClient(AmazonSQSAsync paramRealSQS, QueueBufferConfig config) {
        config.validate();
        realSQS = paramRealSQS;
        bufferConfigExemplar = config;
    }

    /*
     * (non-Javadoc)
     * @see software.amazon.awssdk.services.sqs.AmazonSQS#setRegion(software.amazon.awssdk.regions.Region)
     */
    @Override
    public void setRegion(Region region) throws IllegalArgumentException {
        realSQS.setRegion(region);
    }

    public SetQueueAttributesResult setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(setQueueAttributesRequest, USER_AGENT);
        return realSQS.setQueueAttributes(setQueueAttributesRequest);
    }

    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityBatchRequest, USER_AGENT);
        return realSQS.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    public ChangeMessageVisibilityResult changeMessageVisibility(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(changeMessageVisibilityRequest.getQueueUrl());
        return buffer.changeMessageVisibilitySync(changeMessageVisibilityRequest);
    }

    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageBatchRequest, USER_AGENT);
        return realSQS.sendMessageBatch(sendMessageBatchRequest);
    }

    public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) throws AmazonServiceException,
            AmazonClientException {
        QueueBuffer buffer = getQBuffer(sendMessageRequest.getQueueUrl());
        ResultConverter.appendUserAgent(sendMessageRequest, USER_AGENT);
        return buffer.sendMessageSync(sendMessageRequest);
    }

    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(receiveMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(receiveMessageRequest.getQueueUrl());
        return buffer.receiveMessageSync(receiveMessageRequest);
    }

    public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageBatchRequest, USER_AGENT);
        return realSQS.deleteMessageBatch(deleteMessageBatchRequest);
    }

    public DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(deleteMessageRequest.getQueueUrl());
        return buffer.deleteMessageSync(deleteMessageRequest);
    }

    public void shutdown() {
        for (QueueBuffer buffer : buffers.values()) {
            buffer.shutdown();
        }
        realSQS.shutdown();
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

    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityBatchRequest, USER_AGENT);
        return realSQS.changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest);
    }

    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(changeMessageVisibilityRequest.getQueueUrl());
        return buffer.changeMessageVisibility(changeMessageVisibilityRequest, null);

    }

    public Future<SendMessageBatchResult> sendMessageBatchAsync(SendMessageBatchRequest sendMessageBatchRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageBatchRequest, USER_AGENT);
        return realSQS.sendMessageBatchAsync(sendMessageBatchRequest);
    }

    public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(sendMessageRequest.getQueueUrl());
        return buffer.sendMessage(sendMessageRequest, null);

    }

    public Future<ReceiveMessageResult> receiveMessageAsync(ReceiveMessageRequest receiveMessageRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(receiveMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(receiveMessageRequest.getQueueUrl());
        return buffer.receiveMessage(receiveMessageRequest, null);
    }

    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(DeleteMessageBatchRequest deleteMessageBatchRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageBatchRequest, USER_AGENT);
        return realSQS.deleteMessageBatchAsync(deleteMessageBatchRequest);
    }

    public void setEndpoint(String endpoint) throws IllegalArgumentException {
        realSQS.setEndpoint(endpoint);
    }

    public Future<SetQueueAttributesResult> setQueueAttributesAsync(SetQueueAttributesRequest setQueueAttributesRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(setQueueAttributesRequest, USER_AGENT);
        return realSQS.setQueueAttributesAsync(setQueueAttributesRequest);
    }

    public Future<GetQueueUrlResult> getQueueUrlAsync(GetQueueUrlRequest getQueueUrlRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(getQueueUrlRequest, USER_AGENT);
        return realSQS.getQueueUrlAsync(getQueueUrlRequest);
    }

    public Future<RemovePermissionResult> removePermissionAsync(RemovePermissionRequest removePermissionRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(removePermissionRequest, USER_AGENT);
        return realSQS.removePermissionAsync(removePermissionRequest);
    }

    public GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(getQueueUrlRequest, USER_AGENT);
        return realSQS.getQueueUrl(getQueueUrlRequest);
    }

    public RemovePermissionResult removePermission(RemovePermissionRequest removePermissionRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(removePermissionRequest, USER_AGENT);
        return realSQS.removePermission(removePermissionRequest);
    }

    public Future<GetQueueAttributesResult> getQueueAttributesAsync(GetQueueAttributesRequest getQueueAttributesRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(getQueueAttributesRequest, USER_AGENT);
        return realSQS.getQueueAttributesAsync(getQueueAttributesRequest);
    }

    public GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(getQueueAttributesRequest, USER_AGENT);
        return realSQS.getQueueAttributes(getQueueAttributesRequest);
    }

    public Future<PurgeQueueResult> purgeQueueAsync(PurgeQueueRequest purgeQueueRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(purgeQueueRequest, USER_AGENT);
        return realSQS.purgeQueueAsync(purgeQueueRequest);
    }

    public PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest) throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(purgeQueueRequest, USER_AGENT);
        return realSQS.purgeQueue(purgeQueueRequest);
    }

    public Future<DeleteQueueResult> deleteQueueAsync(DeleteQueueRequest deleteQueueRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(deleteQueueRequest, USER_AGENT);
        return realSQS.deleteQueueAsync(deleteQueueRequest);
    }

    public DeleteQueueResult deleteQueue(DeleteQueueRequest deleteQueueRequest) throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(deleteQueueRequest, USER_AGENT);
        return realSQS.deleteQueue(deleteQueueRequest);
    }

    public Future<ListQueuesResult> listQueuesAsync(ListQueuesRequest listQueuesRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(listQueuesRequest, USER_AGENT);
        return realSQS.listQueuesAsync(listQueuesRequest);
    }

    public ListQueuesResult listQueues(ListQueuesRequest listQueuesRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(listQueuesRequest, USER_AGENT);
        return realSQS.listQueues(listQueuesRequest);
    }

    public Future<CreateQueueResult> createQueueAsync(CreateQueueRequest createQueueRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(createQueueRequest, USER_AGENT);
        return realSQS.createQueueAsync(createQueueRequest);
    }

    public CreateQueueResult createQueue(CreateQueueRequest createQueueRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(createQueueRequest, USER_AGENT);
        return realSQS.createQueue(createQueueRequest);
    }

    public Future<AddPermissionResult> addPermissionAsync(AddPermissionRequest addPermissionRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(addPermissionRequest, USER_AGENT);
        return realSQS.addPermissionAsync(addPermissionRequest);
    }

    @Override
    public Future<AddPermissionResult> addPermissionAsync(String queueUrl, String label,
            List<String> aWSAccountIds, List<String> actions) {
        return addPermissionAsync(new AddPermissionRequest(queueUrl, label,
                aWSAccountIds, actions));
    }

    @Override
    public Future<AddPermissionResult> addPermissionAsync(String queueUrl, String label,
            List<String> aWSAccountIds, List<String> actions,
            AsyncHandler<AddPermissionRequest, AddPermissionResult> asyncHandler) {
        return addPermissionAsync(new AddPermissionRequest(queueUrl, label,
                aWSAccountIds, actions), asyncHandler);
    }


    public AddPermissionResult addPermission(AddPermissionRequest addPermissionRequest) throws AmazonServiceException,
            AmazonClientException {
        ResultConverter.appendUserAgent(addPermissionRequest, USER_AGENT);
        return realSQS.addPermission(addPermissionRequest);
    }

    public ListQueuesResult listQueues() throws AmazonServiceException, AmazonClientException {
        return realSQS.listQueues();
    }

    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        ResultConverter.appendUserAgent(request, USER_AGENT);
        return realSQS.getCachedResponseMetadata(request);
    }

    public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest) throws AmazonServiceException,
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

    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(ChangeMessageVisibilityRequest changeMessageVisibilityRequest,
                                                     AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(changeMessageVisibilityRequest.getQueueUrl());
        return buffer.changeMessageVisibility(changeMessageVisibilityRequest, asyncHandler);
    }

    @Override
    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(String queueUrl,
            String receiptHandle, Integer visibilityTimeout) {
        return changeMessageVisibilityAsync(new ChangeMessageVisibilityRequest(
                queueUrl, receiptHandle, visibilityTimeout));
    }

    @Override
    public Future<ChangeMessageVisibilityResult> changeMessageVisibilityAsync(String queueUrl,
            String receiptHandle, Integer visibilityTimeout,
            AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> asyncHandler) {
        return changeMessageVisibilityAsync(new ChangeMessageVisibilityRequest(
                queueUrl, receiptHandle, visibilityTimeout), asyncHandler);
    }

    public Future<SendMessageResult> sendMessageAsync(SendMessageRequest sendMessageRequest,
                                                      AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(sendMessageRequest.getQueueUrl());
        return buffer.sendMessage(sendMessageRequest, asyncHandler);
    }

    @Override
    public Future<SendMessageResult> sendMessageAsync(String queueUrl,
            String messageBody) {
        return sendMessageAsync(new SendMessageRequest(queueUrl, messageBody));
    }

    @Override
    public Future<SendMessageResult> sendMessageAsync(String queueUrl,
            String messageBody,
            AsyncHandler<SendMessageRequest, SendMessageResult> asyncHandler) {
        return sendMessageAsync(new SendMessageRequest(queueUrl, messageBody),
                asyncHandler);
    }

    public Future<ReceiveMessageResult> receiveMessageAsync(ReceiveMessageRequest receiveMessageRequest,
                                                            AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(receiveMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(receiveMessageRequest.getQueueUrl());
        return buffer.receiveMessage(receiveMessageRequest, asyncHandler);
    }

    @Override
    public Future<ReceiveMessageResult> receiveMessageAsync(String queueUrl) {
        return receiveMessageAsync(new ReceiveMessageRequest(queueUrl));
    }

    @Override
    public Future<ReceiveMessageResult> receiveMessageAsync(
            String queueUrl,
            AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> asyncHandler) {
        return receiveMessageAsync(new ReceiveMessageRequest(queueUrl), asyncHandler);
    }

    public Future<DeleteMessageResult> deleteMessageAsync(DeleteMessageRequest deleteMessageRequest,
                                           AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(deleteMessageRequest.getQueueUrl());
        return buffer.deleteMessage(deleteMessageRequest, asyncHandler);
    }

    @Override
    public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl, String receiptHandle) {
        return deleteMessageAsync(new DeleteMessageRequest(queueUrl,
                receiptHandle));
    }

    @Override
    public Future<DeleteMessageResult> deleteMessageAsync(String queueUrl,
            String receiptHandle,
            AsyncHandler<DeleteMessageRequest, DeleteMessageResult> asyncHandler) {
        return deleteMessageAsync(new DeleteMessageRequest(queueUrl,
                receiptHandle), asyncHandler);
    }

    public Future<SetQueueAttributesResult> setQueueAttributesAsync(SetQueueAttributesRequest setQueueAttributesRequest,
                                                AsyncHandler<SetQueueAttributesRequest, SetQueueAttributesResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.setQueueAttributesAsync(setQueueAttributesRequest, asyncHandler);
    }

    public Future<SetQueueAttributesResult> setQueueAttributesAsync(String queueUrl,
            java.util.Map<String, String> attributes)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.setQueueAttributesAsync(queueUrl, attributes);
    }

    public Future<SetQueueAttributesResult> setQueueAttributesAsync(String queueUrl,
            java.util.Map<String, String> attributes,
            software.amazon.awssdk.handlers.AsyncHandler<SetQueueAttributesRequest, SetQueueAttributesResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.setQueueAttributesAsync(queueUrl, attributes, asyncHandler);
    }

    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest,
                                                                                        AsyncHandler<ChangeMessageVisibilityBatchRequest, ChangeMessageVisibilityBatchResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.changeMessageVisibilityBatchAsync(changeMessageVisibilityBatchRequest, asyncHandler);
    }

    @Override
    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(
            String queueUrl,
            List<ChangeMessageVisibilityBatchRequestEntry> entries) {
        return changeMessageVisibilityBatchAsync(new ChangeMessageVisibilityBatchRequest(
                queueUrl, entries));
    }

    @Override
    public Future<ChangeMessageVisibilityBatchResult> changeMessageVisibilityBatchAsync(
            String queueUrl,
            List<ChangeMessageVisibilityBatchRequestEntry> entries,
            AsyncHandler<ChangeMessageVisibilityBatchRequest, ChangeMessageVisibilityBatchResult> asyncHandler) {
        return changeMessageVisibilityBatchAsync(new ChangeMessageVisibilityBatchRequest(
                queueUrl, entries), asyncHandler);
    }

    public Future<GetQueueUrlResult> getQueueUrlAsync(GetQueueUrlRequest getQueueUrlRequest,
                                                      AsyncHandler<GetQueueUrlRequest, GetQueueUrlResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.getQueueUrlAsync(getQueueUrlRequest, asyncHandler);
    }

    @Override
    public Future<GetQueueUrlResult> getQueueUrlAsync(String queueName) {
        return getQueueUrlAsync(new GetQueueUrlRequest(queueName));
    }

    @Override
    public Future<GetQueueUrlResult> getQueueUrlAsync(String queueName,
            AsyncHandler<GetQueueUrlRequest, GetQueueUrlResult> asyncHandler) {
        return getQueueUrlAsync(new GetQueueUrlRequest(queueName), asyncHandler);
    }

    public Future<RemovePermissionResult> removePermissionAsync(RemovePermissionRequest removePermissionRequest,
                                              AsyncHandler<RemovePermissionRequest, RemovePermissionResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.removePermissionAsync(removePermissionRequest, asyncHandler);
    }

    @Override
    public Future<RemovePermissionResult> removePermissionAsync(String queueUrl, String label) {
        return removePermissionAsync(new RemovePermissionRequest(queueUrl,
                label));
    }

    @Override
    public Future<RemovePermissionResult> removePermissionAsync(String queueUrl, String label,
            AsyncHandler<RemovePermissionRequest, RemovePermissionResult> asyncHandler) {
        return removePermissionAsync(new RemovePermissionRequest(queueUrl,
                label), asyncHandler);
    }

    public Future<GetQueueAttributesResult> getQueueAttributesAsync(GetQueueAttributesRequest getQueueAttributesRequest,
                                                                    AsyncHandler<GetQueueAttributesRequest, GetQueueAttributesResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.getQueueAttributesAsync(getQueueAttributesRequest, asyncHandler);
    }

    @Override
    public Future<GetQueueAttributesResult> getQueueAttributesAsync(
            String queueUrl, List<String> attributeNames) {
        return getQueueAttributesAsync(new GetQueueAttributesRequest(queueUrl,
                attributeNames));
    }

    @Override
    public Future<GetQueueAttributesResult> getQueueAttributesAsync(
            String queueUrl,
            List<String> attributeNames,
            AsyncHandler<GetQueueAttributesRequest, GetQueueAttributesResult> asyncHandler) {
        return getQueueAttributesAsync(new GetQueueAttributesRequest(queueUrl,
                attributeNames), asyncHandler);
    }

    public Future<SendMessageBatchResult> sendMessageBatchAsync(SendMessageBatchRequest sendMessageBatchRequest,
                                                                AsyncHandler<SendMessageBatchRequest, SendMessageBatchResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.sendMessageBatchAsync(sendMessageBatchRequest, asyncHandler);
    }

    @Override
    public Future<SendMessageBatchResult> sendMessageBatchAsync(
            String queueUrl, List<SendMessageBatchRequestEntry> entries) {
        return sendMessageBatchAsync(new SendMessageBatchRequest(queueUrl,
                entries));
    }

    @Override
    public Future<SendMessageBatchResult> sendMessageBatchAsync(
            String queueUrl,
            List<SendMessageBatchRequestEntry> entries,
            AsyncHandler<SendMessageBatchRequest, SendMessageBatchResult> asyncHandler) {
        return sendMessageBatchAsync(new SendMessageBatchRequest(queueUrl,
                entries), asyncHandler);
    }

    public Future<PurgeQueueResult> purgeQueueAsync(PurgeQueueRequest purgeQueueRequest,
                                        AsyncHandler<PurgeQueueRequest, PurgeQueueResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.purgeQueueAsync(purgeQueueRequest, asyncHandler);
    }

    public Future<DeleteQueueResult> deleteQueueAsync(DeleteQueueRequest deleteQueueRequest,
                                         AsyncHandler<DeleteQueueRequest, DeleteQueueResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.deleteQueueAsync(deleteQueueRequest, asyncHandler);
    }

    @Override
    public Future<DeleteQueueResult> deleteQueueAsync(String queueUrl) {
        return deleteQueueAsync(new DeleteQueueRequest(queueUrl));
    }

    @Override
    public Future<DeleteQueueResult> deleteQueueAsync(String queueUrl,
            AsyncHandler<DeleteQueueRequest, DeleteQueueResult> asyncHandler) {
        return deleteQueueAsync(new DeleteQueueRequest(queueUrl), asyncHandler);
    }

    public Future<ListQueuesResult> listQueuesAsync(ListQueuesRequest listQueuesRequest,
                                                    AsyncHandler<ListQueuesRequest, ListQueuesResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.listQueuesAsync(listQueuesRequest, asyncHandler);
    }

    @Override
    public Future<ListQueuesResult> listQueuesAsync() {
        return listQueuesAsync(new ListQueuesRequest());
    }

    @Override
    public Future<ListQueuesResult> listQueuesAsync(
            AsyncHandler<ListQueuesRequest, ListQueuesResult> asyncHandler) {
        return listQueuesAsync(new ListQueuesRequest(), asyncHandler);
    }

    @Override
    public Future<ListQueuesResult> listQueuesAsync(String queueNamePrefix) {
        return listQueuesAsync(new ListQueuesRequest(queueNamePrefix));
    }

    @Override
    public Future<ListQueuesResult> listQueuesAsync(String queueNamePrefix,
            AsyncHandler<ListQueuesRequest, ListQueuesResult> asyncHandler) {
        return listQueuesAsync(new ListQueuesRequest(queueNamePrefix),
                asyncHandler);
    }

    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(DeleteMessageBatchRequest deleteMessageBatchRequest,
                                                                    AsyncHandler<DeleteMessageBatchRequest, DeleteMessageBatchResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.deleteMessageBatchAsync(deleteMessageBatchRequest, asyncHandler);
    }

    @Override
    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(
            String queueUrl, List<DeleteMessageBatchRequestEntry> entries) {
        return deleteMessageBatchAsync(new DeleteMessageBatchRequest(queueUrl,
                entries));
    }

    @Override
    public Future<DeleteMessageBatchResult> deleteMessageBatchAsync(
            String queueUrl,
            List<DeleteMessageBatchRequestEntry> entries,
            AsyncHandler<DeleteMessageBatchRequest, DeleteMessageBatchResult> asyncHandler) {
        return deleteMessageBatchAsync(new DeleteMessageBatchRequest(queueUrl,
                entries), asyncHandler);
    }

    public Future<CreateQueueResult> createQueueAsync(CreateQueueRequest createQueueRequest,
                                                      AsyncHandler<CreateQueueRequest, CreateQueueResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.createQueueAsync(createQueueRequest, asyncHandler);
    }

    @Override
    public Future<CreateQueueResult> createQueueAsync(String queueName) {
        return createQueueAsync(new CreateQueueRequest(queueName));
    }

    @Override
    public Future<CreateQueueResult> createQueueAsync(String queueName,
            AsyncHandler<CreateQueueRequest, CreateQueueResult> asyncHandler) {
        return createQueueAsync(new CreateQueueRequest(queueName), asyncHandler);
    }

    public Future<AddPermissionResult> addPermissionAsync(AddPermissionRequest addPermissionRequest,
                                           AsyncHandler<AddPermissionRequest, AddPermissionResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.addPermissionAsync(addPermissionRequest, asyncHandler);
    }

    @Override
    public ListDeadLetterSourceQueuesResult listDeadLetterSourceQueues(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(listDeadLetterSourceQueuesRequest, USER_AGENT);
        return realSQS.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    @Override
    public Future<ListDeadLetterSourceQueuesResult> listDeadLetterSourceQueuesAsync(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest)
            throws AmazonServiceException, AmazonClientException {
        ResultConverter.appendUserAgent(listDeadLetterSourceQueuesRequest, USER_AGENT);
        return realSQS.listDeadLetterSourceQueuesAsync(listDeadLetterSourceQueuesRequest);
    }

    @Override
    public Future<ListDeadLetterSourceQueuesResult> listDeadLetterSourceQueuesAsync(ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest,
                                                                                    AsyncHandler<ListDeadLetterSourceQueuesRequest, ListDeadLetterSourceQueuesResult> asyncHandler)
            throws AmazonServiceException, AmazonClientException {
        return realSQS.listDeadLetterSourceQueuesAsync(listDeadLetterSourceQueuesRequest, asyncHandler);
    }

    @Override
    public SetQueueAttributesResult setQueueAttributes(String queueUrl, Map<String, String> attributes) throws AmazonServiceException,
            AmazonClientException {
        return setQueueAttributes(new SetQueueAttributesRequest(queueUrl, attributes));
    }

    @Override
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(String queueUrl,
                                                                           List<ChangeMessageVisibilityBatchRequestEntry> entries)
            throws AmazonServiceException, AmazonClientException {
        return changeMessageVisibilityBatch(new ChangeMessageVisibilityBatchRequest(queueUrl, entries));
    }

    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl, String receiptHandle, Integer visibilityTimeout)
            throws AmazonServiceException, AmazonClientException {
        return changeMessageVisibility(new ChangeMessageVisibilityRequest(queueUrl, receiptHandle, visibilityTimeout));
    }

    @Override
    public GetQueueUrlResult getQueueUrl(String queueName) throws AmazonServiceException, AmazonClientException {
        return getQueueUrl(new GetQueueUrlRequest(queueName));
    }

    @Override
    public RemovePermissionResult removePermission(String queueUrl, String label) throws AmazonServiceException, AmazonClientException {
        return removePermission(new RemovePermissionRequest(queueUrl, label));
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(String queueUrl, List<SendMessageBatchRequestEntry> entries)
            throws AmazonServiceException, AmazonClientException {
        return sendMessageBatch(new SendMessageBatchRequest(queueUrl, entries));
    }

    @Override
    public DeleteQueueResult deleteQueue(String queueUrl) throws AmazonServiceException, AmazonClientException {
        return deleteQueue(new DeleteQueueRequest(queueUrl));
    }

    @Override
    public SendMessageResult sendMessage(String queueUrl, String messageBody) throws AmazonServiceException,
            AmazonClientException {
        return sendMessage(new SendMessageRequest(queueUrl, messageBody));
    }

    @Override
    public ReceiveMessageResult receiveMessage(String queueUrl) throws AmazonServiceException, AmazonClientException {
        return receiveMessage(new ReceiveMessageRequest(queueUrl));
    }

    @Override
    public ListQueuesResult listQueues(String queueNamePrefix) throws AmazonServiceException, AmazonClientException {
        return listQueues(new ListQueuesRequest(queueNamePrefix));
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries)
            throws AmazonServiceException, AmazonClientException {
        return deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, entries));
    }

    @Override
    public CreateQueueResult createQueue(String queueName) throws AmazonServiceException, AmazonClientException {
        return createQueue(new CreateQueueRequest(queueName));
    }

    @Override
    public AddPermissionResult addPermission(String queueUrl, String label, List<String> aWSAccountIds, List<String> actions)
            throws AmazonServiceException, AmazonClientException {
        return addPermission(new AddPermissionRequest(queueUrl, label, aWSAccountIds, actions));
    }

    @Override
    public DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle) throws AmazonServiceException,
            AmazonClientException {
        return deleteMessage(new DeleteMessageRequest(queueUrl, receiptHandle));
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(String queueUrl, List<String> attributeNames) {
        return getQueueAttributes(new GetQueueAttributesRequest(queueUrl, attributeNames));
    }
}
