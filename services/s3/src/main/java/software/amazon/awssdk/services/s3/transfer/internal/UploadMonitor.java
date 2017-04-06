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

package software.amazon.awssdk.services.s3.transfer.internal;

import static software.amazon.awssdk.event.SdkProgressPublisher.publishProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListenerChain;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.transfer.PauseResult;
import software.amazon.awssdk.services.s3.transfer.PauseStatus;
import software.amazon.awssdk.services.s3.transfer.PersistableUpload;
import software.amazon.awssdk.services.s3.transfer.Transfer.TransferState;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.model.UploadResult;

/**
 * Manages an upload by periodically checking to see if the upload is done, and
 * returning a result if so. Otherwise, schedules a copy of itself to be run in
 * the future and returns null. When waiting on the result of this class via a
 * Future object, clients must call {@link UploadMonitor#isDone()} and
 * {@link UploadMonitor#getFuture()}
 */
public class UploadMonitor implements Callable<UploadResult>, TransferMonitor {


    private final AmazonS3 s3;
    private final PutObjectRequest origReq;
    private final ProgressListenerChain listener;
    private final UploadCallable multipartUploadCallable;
    private final UploadImpl transfer;
    private final ExecutorService threadPool;

    /*
     * Futures of threads that upload the parts.
     */
    private final List<Future<PartETag>> futures = Collections
            .synchronizedList(new ArrayList<Future<PartETag>>());

    /*
     * State for clients wishing to poll for completion
     */
    private boolean isUploadDone = false;
    private Future<UploadResult> future;

    private UploadMonitor(TransferManager manager, UploadImpl transfer, ExecutorService threadPool,
                          UploadCallable multipartUploadCallable, PutObjectRequest putObjectRequest,
                          ProgressListenerChain progressListenerChain) {

        this.s3 = manager.getAmazonS3Client();
        this.multipartUploadCallable = multipartUploadCallable;
        this.origReq = putObjectRequest;
        this.listener = progressListenerChain;
        this.transfer = transfer;
        this.threadPool = threadPool;
    }

    /**
     * Constructs a new upload watcher and then immediately submits it to
     * the thread pool.
     *
     * @param manager
     *            The {@link TransferManager} that owns this upload.
     * @param transfer
     *            The transfer being processed.
     * @param threadPool
     *            The {@link ExecutorService} to which we should submit new
     *            tasks.
     * @param multipartUploadCallable
     *            The callable responsible for processing the upload
     *            asynchronously
     * @param putObjectRequest
     *            The original putObject request
     * @param progressListenerChain
     *            A chain of listeners that wish to be notified of upload
     *            progress
     */
    public static UploadMonitor create(
            TransferManager manager,
            UploadImpl transfer,
            ExecutorService threadPool,
            UploadCallable multipartUploadCallable,
            PutObjectRequest putObjectRequest,
            ProgressListenerChain progressListenerChain) {

        UploadMonitor uploadMonitor = new UploadMonitor(manager, transfer,
                                                        threadPool, multipartUploadCallable, putObjectRequest,
                                                        progressListenerChain);
        uploadMonitor.setFuture(threadPool.submit(uploadMonitor));
        return uploadMonitor;
    }

    public synchronized Future<UploadResult> getFuture() {
        return future;
    }

    private synchronized void setFuture(Future<UploadResult> future) {
        this.future = future;
    }

    private synchronized void cancelFuture() {
        future.cancel(true);
    }

    public synchronized boolean isDone() {
        return isUploadDone;
    }

    private synchronized void markAllDone() {
        isUploadDone = true;
    }

    @Override
    public UploadResult call() throws Exception {
        try {
            UploadResult result = multipartUploadCallable.call();

            /**
             * If the result is null, it is a mutli part parellel upload. So, an
             * new task is submitted for initiating a complete multi part upload
             * request.
             */
            if (result == null) {
                futures.addAll(multipartUploadCallable.getFutures());
                setFuture(threadPool.submit(new CompleteMultipartUpload(
                        multipartUploadCallable.getMultipartUploadId(), s3,
                        origReq, futures, multipartUploadCallable
                                .getETags(), listener, this)));
            } else {
                uploadComplete();
            }
            return result;
        } catch (CancellationException e) {
            transfer.setState(TransferState.Canceled);
            publishProgress(listener, ProgressEventType.TRANSFER_CANCELED_EVENT);
            throw new SdkClientException("Upload canceled");
        } catch (Exception e) {
            transfer.setState(TransferState.Failed);
            throw e;
        }
    }

    void uploadComplete() {
        markAllDone();
        transfer.setState(TransferState.Completed);

        // AmazonS3Client takes care of all the events for single part uploads,
        // so we only need to send a completed event for multipart uploads.
        if (multipartUploadCallable.isMultipartUpload()) {
            publishProgress(listener, ProgressEventType.TRANSFER_COMPLETED_EVENT);
        }
    }

    /**
     * Cancels the futures in the following cases - If the user has requested
     * for forcefully aborting the transfers. - If the upload is a multi part
     * parellel upload. - If the upload operation hasn't started. Cancels all
     * the in flight transfers of the upload if applicable. Returns the
     * multi-part upload Id in case of the parallel multi-part uploads. Returns
     * null otherwise.
     */
    PauseResult<PersistableUpload> pause(boolean forceCancel) {

        PersistableUpload persistableUpload = multipartUploadCallable
                .getPersistableUpload();
        if (persistableUpload == null) {
            PauseStatus pauseStatus = TransferManagerUtils
                    .determinePauseStatus(transfer.getState(), forceCancel);
            if (forceCancel) {
                cancelFutures();
                multipartUploadCallable.performAbortMultipartUpload();
            }
            return new PauseResult<PersistableUpload>(pauseStatus);
        }
        cancelFutures();
        return new PauseResult<PersistableUpload>(PauseStatus.SUCCESS,
                                                  persistableUpload);
    }

    /**
     * Cancels the inflight transfers if they are not completed.
     */
    private void cancelFutures() {
        cancelFuture();
        for (Future<PartETag> f : futures) {
            f.cancel(true);
        }
        multipartUploadCallable.getFutures().clear();
        futures.clear();
    }

    /**
     * Cancels all the futures associated with this upload operation. Also
     * cleans up the parts on Amazon S3 if the upload is performed as a
     * multi-part upload operation.
     */
    void performAbort() {
        cancelFutures();
        multipartUploadCallable.performAbortMultipartUpload();
        publishProgress(listener, ProgressEventType.TRANSFER_CANCELED_EVENT);
    }
}
