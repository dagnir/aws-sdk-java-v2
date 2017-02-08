/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License is
 * located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.waiters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResult;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.waiters.FixedDelayStrategy;
import software.amazon.awssdk.waiters.HttpFailureStatusAcceptor;
import software.amazon.awssdk.waiters.HttpSuccessStatusAcceptor;
import software.amazon.awssdk.waiters.MaxAttemptsRetryStrategy;
import software.amazon.awssdk.waiters.PollingStrategy;
import software.amazon.awssdk.waiters.Waiter;
import software.amazon.awssdk.waiters.WaiterBuilder;
import software.amazon.awssdk.waiters.WaiterState;

public class AmazonS3Waiters {

    /**
     * Represents the service client
     */
    private final AmazonS3 client;

    private final ExecutorService executorService = Executors
            .newFixedThreadPool(50);

    /**
     * Constructs a new AmazonS3Waiters with the given client
     * 
     * @param client
     *        Service client
     */
    @SdkInternalApi
    public AmazonS3Waiters(AmazonS3 client) {
        this.client = client;
    }

    /**
     * Builds a BucketNotExists waiter by using custom parameters
     * waiterParameters and other parameters defined in the waiters
     * specification, and then polls until it determines whether the resource
     * entered the desired state or not, where polling criteria is bound by
     * either default polling strategy or custom polling strategy.
     */
    public Waiter bucketNotExists() {

        return new WaiterBuilder<HeadBucketRequest, HeadBucketResult>()
                .withSdkFunction(new HeadBucketFunction(client))
                .withAcceptors(
                        new HttpFailureStatusAcceptor(404, WaiterState.SUCCESS))
                .withDefaultPollingStrategy(
                        new PollingStrategy(new MaxAttemptsRetryStrategy(20),
                                new FixedDelayStrategy(5)))
                .withExecutorService(executorService).build();
    }

    /**
     * Builds a BucketExists waiter by using custom parameters waiterParameters
     * and other parameters defined in the waiters specification, and then polls
     * until it determines whether the resource entered the desired state or
     * not, where polling criteria is bound by either default polling strategy
     * or custom polling strategy.
     */
    public Waiter bucketExists() {

        return new WaiterBuilder<HeadBucketRequest, HeadBucketResult>()
                .withSdkFunction(new HeadBucketFunction(client))
                .withAcceptors(
                        new HttpSuccessStatusAcceptor(WaiterState.SUCCESS),
                        new HttpFailureStatusAcceptor(301, WaiterState.SUCCESS),
                        new HttpFailureStatusAcceptor(403, WaiterState.SUCCESS),
                        new HttpFailureStatusAcceptor(404, WaiterState.RETRY))
                .withDefaultPollingStrategy(
                        new PollingStrategy(new MaxAttemptsRetryStrategy(20),
                                new FixedDelayStrategy(5)))
                .withExecutorService(executorService).build();
    }

    /**
     * Builds a ObjectExists waiter by using custom parameters waiterParameters
     * and other parameters defined in the waiters specification, and then polls
     * until it determines whether the resource entered the desired state or
     * not, where polling criteria is bound by either default polling strategy
     * or custom polling strategy.
     */
    public Waiter objectExists() {

        return new WaiterBuilder<GetObjectMetadataRequest, ObjectMetadata>()
                .withSdkFunction(new HeadObjectFunction(client))
                .withAcceptors(
                        new HttpSuccessStatusAcceptor(WaiterState.SUCCESS),
                        new HttpFailureStatusAcceptor(404, WaiterState.RETRY))
                .withDefaultPollingStrategy(
                        new PollingStrategy(new MaxAttemptsRetryStrategy(20),
                                new FixedDelayStrategy(5)))
                .withExecutorService(executorService).build();
    }

    /**
     * Builds a ObjectNotExists waiter by using custom parameters
     * waiterParameters and other parameters defined in the waiters
     * specification, and then polls until it determines whether the resource
     * entered the desired state or not, where polling criteria is bound by
     * either default polling strategy or custom polling strategy.
     */
    public Waiter objectNotExists() {

        return new WaiterBuilder<GetObjectMetadataRequest, ObjectMetadata>()
                .withSdkFunction(new HeadObjectFunction(client))
                .withAcceptors(
                        new HttpFailureStatusAcceptor(404, WaiterState.SUCCESS))
                .withDefaultPollingStrategy(
                        new PollingStrategy(new MaxAttemptsRetryStrategy(20),
                                new FixedDelayStrategy(5)))
                .withExecutorService(executorService).build();
    }

}
