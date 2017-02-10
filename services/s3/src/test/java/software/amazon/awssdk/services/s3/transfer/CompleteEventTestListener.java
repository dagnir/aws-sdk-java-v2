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