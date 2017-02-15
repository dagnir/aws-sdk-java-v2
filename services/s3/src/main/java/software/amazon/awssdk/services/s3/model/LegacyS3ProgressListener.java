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

package software.amazon.awssdk.services.s3.model;

import software.amazon.awssdk.event.DeliveryMode;

/**
 * A proxy class that wraps the deprecated S3 progress listener and implements
 * the new {@link software.amazon.awssdk.event.ProgressListener} interface.
 *
 * @see ProgressListener
 * @see software.amazon.awssdk.event.ProgressListener
 */
@Deprecated
public class LegacyS3ProgressListener implements
                                      software.amazon.awssdk.event.ProgressListener, DeliveryMode {
    private final ProgressListener listener;
    private final boolean syncCallSafe;

    public LegacyS3ProgressListener(final ProgressListener listener) {
        this.listener = listener;
        if (listener instanceof DeliveryMode) {
            DeliveryMode mode = (DeliveryMode) listener;
            syncCallSafe = mode.isSyncCallSafe();
        } else {
            syncCallSafe = false;
        }
    }

    public ProgressListener unwrap() {
        return listener;
    }

    public void progressChanged(software.amazon.awssdk.event.ProgressEvent progressEvent) {
        if (listener == null) {
            return;
        }
        listener.progressChanged(adaptToLegacyEvent(progressEvent));
    }

    private ProgressEvent adaptToLegacyEvent(software.amazon.awssdk.event.ProgressEvent event) {
        long bytes = event.getBytesTransferred();
        if (bytes != 0) {
            return new ProgressEvent((int) bytes);
        } else {
            return new ProgressEvent(event.getEventType());
        }
    }

    @Override
    public boolean isSyncCallSafe() {
        return syncCallSafe;
    }
}
