package software.amazon.awssdk.services.glacier.transfer;

import org.junit.Assert;
import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.SyncProgressListener;

class TestGlacierProgressListener extends SyncProgressListener {
        public volatile boolean transferPreparing;
        public volatile boolean transferFailed;
        public volatile boolean transferStarted;
        public volatile boolean transferCompleted;
        public volatile boolean duplicateEventsSeen;

        private long totalRequestBytesTransferred;
        private long totalResponseBytesTransferred;

        public synchronized long getTotalRequestBytesTransferred() {
            return totalRequestBytesTransferred;
        }

        public synchronized long getTotalResponseBytesTransferred() {
            return totalResponseBytesTransferred;
        }

        public synchronized void progressChanged(ProgressEvent progressEvent) {
            switch (progressEvent.getEventType()) {
            case REQUEST_BYTE_TRANSFER_EVENT:
            case HTTP_REQUEST_CONTENT_RESET_EVENT:
                totalRequestBytesTransferred += progressEvent.getBytesTransferred();
                break;
            case RESPONSE_BYTE_DISCARD_EVENT:
            case RESPONSE_BYTE_TRANSFER_EVENT:
            case HTTP_RESPONSE_CONTENT_RESET_EVENT:
                totalResponseBytesTransferred += progressEvent.getBytesTransferred();
                break;

            case TRANSFER_PREPARING_EVENT:
                if (transferPreparing == true) {
                    duplicateEventsSeen = true;
                    Assert.fail("Received duplicate PREPARING_EVENT");
                }
                transferPreparing = true;
                System.out.println("PREPARING_EVENT");
                break;

            case TRANSFER_STARTED_EVENT:
                if (transferStarted == true) {
                    duplicateEventsSeen = true;
                    Assert.fail("Received duplicate STARTED_EVENT");
                }
                transferStarted = true;
                System.out.println("STARTED_EVENT");
                break;

            case TRANSFER_COMPLETED_EVENT:
                if (transferCompleted == true) {
                    duplicateEventsSeen = true;
                    Assert.fail("Received duplicate COMPLETED_EVENT");
                }
                transferCompleted = true;
                System.out.println("COMPLETED_EVENT");
                break;

            case TRANSFER_FAILED_EVENT:
                if (transferFailed == true) {
                    duplicateEventsSeen = true;
                    Assert.fail("Received duplicate FAILED_EVENT");
                }
                transferFailed = true;
                System.out.println("FAILED_EVENT");
                break;
            default:    // ignore other event types
                break;
            }
        }

        public synchronized void reset() {
            transferPreparing = false;
            transferFailed = false;
            transferStarted = false;
            transferCompleted = false;
            duplicateEventsSeen = false;
            totalRequestBytesTransferred = 0;
            totalResponseBytesTransferred = 0;
        }
    }