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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResult;

public class ParallelScanTask {

    /**
     * The list of hard copies of ScanRequest with different segment number.
     */
    private final List<ScanRequest> parallelScanRequests;

    private final int totalSegments;

    /**
     * Cache all the future tasks, so that we can extract the exception when
     * we see failed segment scan.
     */
    private final List<Future<ScanResult>> segmentScanFutureTasks;

    /**
     * Cache all the most recent ScanResult on each segment.
     */
    private final List<ScanResult> segmentScanResults;

    /**
     * The current state of the scan on each segment.
     * Used as the monitor for synchronization.
     */
    private final List<SegmentScanState> segmentScanStates;
    private final DynamoDBClient dynamo;
    private ExecutorService executorService;

    @Deprecated
    public ParallelScanTask(DynamoDBMapper mapper, DynamoDBClient dynamo, List<ScanRequest> parallelScanRequests) {
        this(dynamo, parallelScanRequests);
    }

    ParallelScanTask(DynamoDBClient dynamo, List<ScanRequest> parallelScanRequests) {
        this(dynamo, parallelScanRequests, Executors.newCachedThreadPool());
    }

    @SdkTestInternalApi
    ParallelScanTask(DynamoDBClient dynamo, List<ScanRequest> parallelScanRequests,
                     ExecutorService executorService) {
        this.dynamo = dynamo;
        this.parallelScanRequests = parallelScanRequests;
        this.totalSegments = parallelScanRequests.size();
        this.executorService = executorService;

        // Create synchronized views of the list to guarantee any changes are visible across all threads.
        segmentScanFutureTasks = Collections
                .synchronizedList(new ArrayList<Future<ScanResult>>(totalSegments));
        segmentScanResults = Collections.synchronizedList(new ArrayList<ScanResult>(totalSegments));
        segmentScanStates = Collections
                .synchronizedList(new ArrayList<SegmentScanState>(totalSegments));

        initSegmentScanStates();
    }

    String getTableName() {
        return parallelScanRequests.get(0).getTableName();
    }

    public boolean isAllSegmentScanFinished() {
        synchronized (segmentScanStates) {
            for (int segment = 0; segment < totalSegments; segment++) {
                if (segmentScanStates.get(segment) != SegmentScanState.SegmentScanCompleted) {
                    return false;
                }
            }
            // Shut down if all data have been scanned and loaded.
            executorService.shutdown();
            return true;
        }
    }

    public List<ScanResult> getNextBatchOfScanResults() throws SdkClientException {
        /**
         * Kick-off all the parallel scan tasks.
         */
        startScanNextPages();
        /**
         * Wait till all the tasks have finished.
         */
        synchronized (segmentScanStates) {
            while (segmentScanStates.contains(SegmentScanState.Waiting)
                   || segmentScanStates.contains(SegmentScanState.Scanning)) {
                try {
                    segmentScanStates.wait();
                } catch (InterruptedException ie) {
                    throw new SdkClientException("Parallel scan interrupted by other thread.", ie);
                }
            }
            /**
             *  Keep the lock on segmentScanStates until all the cached results are marshaled and returned.
             */
            return marshalParallelScanResults();
        }

    }

    private void startScanNextPages() {
        for (int segment = 0; segment < totalSegments; segment++) {
            final int currentSegment = segment;
            final SegmentScanState currentSegmentState = segmentScanStates.get(currentSegment);
            /**
             * Assert: Should never see any task in state of "Scanning" when starting a new batch.
             */
            if (currentSegmentState == SegmentScanState.Scanning) {

                throw new SdkClientException("Should never see a 'Scanning' state when starting parallel scans.");

            } else if (currentSegmentState == SegmentScanState.Failed ||
                     currentSegmentState == SegmentScanState.SegmentScanCompleted) {
                /**
                 * Skip any failed or completed segment, and clear the corresponding cached result.
                 */
                segmentScanResults.set(currentSegment, null);
                continue;
            } else {
                /**
                 * Otherwise, submit a new future task and save it in segmentScanFutureTasks.
                 */
                // Update the state to "Scanning" and notify any waiting thread.
                synchronized (segmentScanStates) {
                    segmentScanStates.set(currentSegment, SegmentScanState.Scanning);
                    segmentScanStates.notifyAll();
                }
                Future<ScanResult> futureTask = executorService.submit(() -> {
                    try {
                        if (currentSegmentState == SegmentScanState.HasNextPage) {
                            return scanNextPageOfSegment(currentSegment, true);
                        } else if (currentSegmentState == SegmentScanState.Waiting) {
                            return scanNextPageOfSegment(currentSegment, false);
                        } else {
                            throw new SdkClientException("Should not start a new future task");
                        }
                    } catch (Exception e) {
                        synchronized (segmentScanStates) {
                            segmentScanStates.set(currentSegment, SegmentScanState.Failed);
                            segmentScanStates.notifyAll();
                            executorService.shutdown();
                        }
                        throw e;
                    }
                });
                // Cache the future task (for getting the Exceptions in the working thread).
                segmentScanFutureTasks.set(currentSegment, futureTask);
            }
        }
    }

    private List<ScanResult> marshalParallelScanResults() {
        List<ScanResult> scanResults = new LinkedList<ScanResult>();
        for (int segment = 0; segment < totalSegments; segment++) {
            SegmentScanState currentSegmentState = segmentScanStates.get(segment);
            /**
             * Rethrow the exception from any failed segment scan.
             */
            if (currentSegmentState == SegmentScanState.Failed) {
                try {
                    segmentScanFutureTasks.get(segment).get();
                    throw new SdkClientException("No Exception found in the failed scan task.");
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    if (cause instanceof SdkClientException) {
                        throw (SdkClientException) cause;
                    } else {
                        throw new SdkClientException("Internal error during the scan on segment #" + segment + ".",
                                                     ee.getCause());
                    }
                } catch (Exception e) {
                    throw new SdkClientException("Error during the scan on segment #" + segment + ".", e);
                }
            } else if (currentSegmentState == SegmentScanState.HasNextPage ||
                       currentSegmentState == SegmentScanState.SegmentScanCompleted) {
                /**
                 * Get the ScanResult from cache if the segment scan has finished.
                 */
                ScanResult scanResult = segmentScanResults.get(segment);
                scanResults.add(scanResult);
            } else if (currentSegmentState == SegmentScanState.Waiting
                       || currentSegmentState == SegmentScanState.Scanning) {
                throw new SdkClientException("Should never see a 'Scanning' or 'Waiting' state when marshalling parallel " +
                                             "scan results.");
            }
        }
        return scanResults;
    }

    private ScanResult scanNextPageOfSegment(int currentSegment, boolean checkLastEvaluatedKey) {
        ScanRequest segmentScanRequest = parallelScanRequests.get(currentSegment);
        if (checkLastEvaluatedKey) {
            ScanResult lastScanResult = segmentScanResults.get(currentSegment);
            segmentScanRequest.setExclusiveStartKey(lastScanResult.getLastEvaluatedKey());
        } else {
            segmentScanRequest.setExclusiveStartKey(null);
        }
        ScanResult scanResult = dynamo.scan(DynamoDBMapper.applyUserAgent(segmentScanRequest));

        /**
         * Cache the scan result in segmentScanResults.
         * We should never try to get these scan results by calling get() on the cached future tasks.
         */
        segmentScanResults.set(currentSegment, scanResult);

        /**
         * Update the state and notify any waiting thread.
         */
        synchronized (segmentScanStates) {
            if (null == scanResult.getLastEvaluatedKey()) {
                segmentScanStates.set(currentSegment, SegmentScanState.SegmentScanCompleted);
            } else {
                segmentScanStates.set(currentSegment, SegmentScanState.HasNextPage);
            }
            segmentScanStates.notifyAll();
        }
        return scanResult;
    }

    private void initSegmentScanStates() {
        for (int segment = 0; segment < totalSegments; segment++) {
            segmentScanFutureTasks.add(null);
            segmentScanResults.add(null);
            segmentScanStates.add(SegmentScanState.Waiting);
        }
    }

    /**
     * Enumeration of the possible states of the scan on a segment.
     */
    private static enum SegmentScanState {
        /** The scan on the segment is waiting for resources to execute and has not started yet. */
        Waiting,

        /** The scan is in process, and hasn't finished yet. */
        Scanning,

        /** The scan has already failed. */
        Failed,

        /** The scan on the current page has finished, but there are more pages in the segment to be scanned. */
        HasNextPage,

        /** The scan on the whole segment has completed. */
        SegmentScanCompleted,
    }
}
