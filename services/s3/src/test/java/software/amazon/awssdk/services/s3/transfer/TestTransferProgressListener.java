package software.amazon.awssdk.services.s3.transfer;

import software.amazon.awssdk.event.DeliveryMode;
import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.services.s3.transfer.internal.S3ProgressListener;

final class TestTransferProgressListener implements S3ProgressListener, DeliveryMode {
    public boolean transferFailed      = false;
    public boolean transferStarted     = false;
    public boolean transferCompleted   = false;
    public boolean bytesTransferred    = false;
    public boolean transferCanceled    = false;
    public boolean duplicateEventsSeen = false;

    public void progressChanged(ProgressEvent progressEvent) {
        if (progressEvent.getBytesTransferred() > 0) bytesTransferred = true;

        switch (progressEvent.getEventType()) {
            case TRANSFER_STARTED_EVENT:
                if (transferStarted == true) {
                    duplicateEventsSeen = true;
                    throw new RuntimeException("Received duplicate STARTED_EVENT");
                }
                transferStarted = true;
                break;
            case TRANSFER_COMPLETED_EVENT:
                if (transferCompleted == true) {
                    duplicateEventsSeen = true;
                    throw new RuntimeException("Received duplicate COMPLETED_EVENT");
                }
                transferCompleted = true;
                break;
            case TRANSFER_FAILED_EVENT:
                if (transferFailed == true) {
                    duplicateEventsSeen = true;
                    throw new RuntimeException("Received duplicate FAILED_EVENT");
                }
                transferFailed = true;
                break;
            case TRANSFER_CANCELED_EVENT:
                if (transferCanceled == true) {
                    duplicateEventsSeen = true;
                    throw new RuntimeException("Received duplicate TRANSFER_CANCELED_EVENT");
                }
                transferCanceled = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void onPersistableTransfer(PersistableTransfer persistableTransfer) { }

    /**
     * Sync progress listeners is important for reliable tests.
     */
    @Override
    public boolean isSyncCallSafe() {
        return true;
    }
}