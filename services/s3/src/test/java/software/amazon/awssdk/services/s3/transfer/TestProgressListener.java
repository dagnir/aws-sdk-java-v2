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
            case TRANSFER_STARTED_EVENT :
                seenStarted = true;
                break;
            case TRANSFER_COMPLETED_EVENT :
                seenCompleted = true;
                break;
            case TRANSFER_FAILED_EVENT :
                seenFailed = true;
                break;
            case TRANSFER_CANCELED_EVENT :
                seenCanceled = true;
                break;
            default :
                // ignore other event types
                break;
        }
    }
}