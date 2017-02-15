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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.ProgressListener;

final class ProgressTestListenerWithBlockCallback implements ProgressListener {

    private final Upload upload;
    public boolean startEventCallbackTriggered;
    public boolean startEventCallbackReturned;
    public boolean completeEventCallbackTriggered;
    public boolean completeEventCallbackReturned;

    ProgressTestListenerWithBlockCallback(Upload upload) {
        this.upload = upload;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        switch (progressEvent.getEventType()) {
            case TRANSFER_STARTED_EVENT:
                startEventCallbackTriggered = true;
                // do some dumb method calls here
                try {
                    upload.waitForCompletion();
                    assertNotNull(upload.waitForUploadResult());
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                startEventCallbackReturned = true;
                break;

            case TRANSFER_COMPLETED_EVENT:
                /* Things need to check **/
                assertTrue(upload.isDone());                // isDone is already updated().
                assertTrue(startEventCallbackTriggered);    // Callbacks are executed in order.
                assertTrue(startEventCallbackReturned);     // Complete event call back should be triggered
                // after started event callback returns.

                completeEventCallbackTriggered = true;
                // do the dumb thing again
                try {
                    upload.waitForCompletion();
                    assertNotNull(upload.waitForUploadResult());
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                completeEventCallbackReturned = true;
                break;

            default:
                break;
        }
    }

    public void waitForAllCallbacksDone() throws InterruptedException {
        while (!completeEventCallbackReturned) {
            System.out.println("Waiting for callbacks to return...");
            Thread.sleep(1000);
        }
    }
}