package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.ProgressListener;

final class ProgressTestListenerWithBlockCallback implements ProgressListener {

    public boolean startEventCallbackTriggered;
    public boolean startEventCallbackReturned;
    public boolean completeEventCallbackTriggered;
    public boolean completeEventCallbackReturned;

    private final Upload upload;

    ProgressTestListenerWithBlockCallback(Upload upload) {
        this.upload = upload;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        switch (progressEvent.getEventType()) {
            case TRANSFER_STARTED_EVENT :
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

            default : break;
        }
    }

    public void waitForAllCallbacksDone() throws InterruptedException {
        while( !completeEventCallbackReturned ) {
            System.out.println("Waiting for callbacks to return...");
            Thread.sleep(1000);
        }
    }
}