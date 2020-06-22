package com.dongieagnir;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.utils.Logger;

public final class PutRecordTask implements Runnable {
    private static final Logger LOG = Logger.loggerFor(PutRecordTask.class);
//    private static final String STREAM_ARN = "arn:aws:kinesis:us-west-2:539653697957:stream/sdk-v2-metrics-test";
    private static final String STREAM_NAME = "sdk-v2-metrics-test";

    private final KinesisAsyncClient kinesisClient;
    private final MetricPublisher metricPublisher;
    private final int iterations;
    private final CompletableFuture<Void> future;

    public PutRecordTask(KinesisAsyncClient kinesisClient, MetricPublisher metricPublisher, int iterations, CompletableFuture<Void> future) {
        this.kinesisClient = kinesisClient;
        this.metricPublisher = metricPublisher;
        this.iterations = iterations;
        this.future = future;
    }

    @Override
    public void run() {
        try {
            doIterations();
        } finally {
            future.complete(null);
        }
    }

    private void doIterations() {
        for (int i = 0; i < iterations; ++i) {
            try {
                PutRecordRequest request = PutRecordRequest.builder()
                        .streamName(STREAM_NAME)
                        .partitionKey("Partition")
                        .data(SdkBytes.fromUtf8String("Hello world"))
                        // Metric publishers can be overridden on a per-request basis
                        .overrideConfiguration(o -> o.metricPublisher(metricPublisher))
                        .build();

                kinesisClient.putRecord(request).join();
            } catch (Throwable t) {
                LOG.warn(() -> "Error calling PutRecord");
            }
        }
    }
}
