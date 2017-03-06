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
import software.amazon.awssdk.event.SyncProgressListener;

final class TestProgressListener extends SyncProgressListener {
    public volatile boolean seenStarted;
    public volatile boolean seenCompleted;
    public volatile boolean seenFailed;
    public volatile boolean seenCanceled;
    public volatile long totalBytesTransferred;

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        // Progress reporting events
        synchronized (this) {
            totalBytesTransferred += progressEvent.getBytesTransferred();
        }

        switch (progressEvent.getEventType()) {
            case TRANSFER_STARTED_EVENT:
                seenStarted = true;
                break;
            case TRANSFER_COMPLETED_EVENT:
                seenCompleted = true;
                break;
            case TRANSFER_FAILED_EVENT:
                seenFailed = true;
                break;
            case TRANSFER_CANCELED_EVENT:
                seenCanceled = true;
                break;
            default:
                // ignore other event types
                break;
        }
    }
}
