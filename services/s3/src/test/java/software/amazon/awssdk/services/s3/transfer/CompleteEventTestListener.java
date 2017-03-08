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

package software.amazon.awssdk.services.s3.transfer;

import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.SyncProgressListener;
import software.amazon.awssdk.services.s3.transfer.internal.AbstractTransfer;

/**
 * A ProgressListener implementation that checks whether the specified
 * transfer is done when COMPLETE event is fired.
 */
final class CompleteEventTestListener extends SyncProgressListener {
    public volatile boolean seenCompleteEvent;
    public volatile boolean transferIsDoneUponCompleteEvent;
    public volatile boolean duplicateCompleteEvent;
    private final AbstractTransfer transfer;

    public CompleteEventTestListener(AbstractTransfer transfer) {
        this.transfer = transfer;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        if (progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
            if (seenCompleteEvent) {
                duplicateCompleteEvent = true;
            }
            seenCompleteEvent = true;
            transferIsDoneUponCompleteEvent = transfer.isDone();
        }
    }
}
