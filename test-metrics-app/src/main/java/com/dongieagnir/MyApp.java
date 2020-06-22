package com.dongieagnir;

import java.io.Closeable;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.Logger;

public class MyApp implements Closeable {
    private final Logger LOG = Logger.loggerFor(MyApp.class);

    private static final String PROFILE_NAME = "personal-integ";
    private static final Region REGION = Region.US_WEST_2;

    private static final String BUCKET = "dongie-test-bucket";
    private static final String KEY = "8kb_file.dat";
    private static final int GET_OBJECT_ITERATIONS = 16;
    private static final int PUT_RECORD_ITERATIONS = 8;

    private static final Random RNG = new Random();

    private static final int MIN_TASKS = 8;
    private static final int MAX_TASKS = 16;

    private enum TaskType {
        S3_GET_OBJECT,
        KINESIS_PUT_RECORD
    }

    private final ExecutorService exec;

    private AwsCredentialsProvider credentialsProvider;
    private CloudWatchAsyncClient cwClient;
    private MetricPublisher cwPublisher;
    private MyLoggingPublisher myLoggingPublisher;
    private SimplePublisherChain publisherChain;
    private final S3Client s3Client;
    private final KinesisAsyncClient kinesisClient;

    private MyApp() {
        credentialsProvider = ProfileCredentialsProvider.create(PROFILE_NAME);

        exec = Executors.newFixedThreadPool(16);

        initializePublishers();

        s3Client = S3Client.builder()
                // Metric publishers are set through the OverrideConfiguration.
                // This is currently the only entrypoint for metrics in the SDK;
                // design for further flexibility, e.g. similar to dynamic
                // loading of metric publishers is being worked on
                .overrideConfiguration(o -> o.metricPublisher(publisherChain))
                .credentialsProvider(credentialsProvider)
                .region(REGION)
                .build();

        // Note: no publisher set on client, see PutRecordTask
        kinesisClient = KinesisAsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(REGION)
                .build();
    }

    private void initializePublishers() {
        cwClient = CloudWatchAsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(REGION)
                .build();

        cwPublisher = CloudWatchMetricPublisher.builder()
                .cloudWatchClient(cwClient)
                .metricCategories(MetricCategory.DEFAULT, MetricCategory.HTTP_CLIENT)
                .uploadFrequency(Duration.ofSeconds(30))
                .build();

        // Publishers are pluggable, so customers can implement the interface to use publishers that suite their needs
        myLoggingPublisher = new MyLoggingPublisher();

        publisherChain = SimplePublisherChain.create(cwPublisher, myLoggingPublisher);
    }

    public CompletableFuture<Void> runIteration() {
        int nTasks = RNG.nextInt(MAX_TASKS - MIN_TASKS + 1) + MIN_TASKS;
        LOG.debug(() -> String.format("Running iteration with %d tasks", nTasks));
        CompletableFuture[] futures = new CompletableFuture[nTasks];
        for (int i = 0; i < nTasks; ++i) {
            futures[i] = new CompletableFuture();
            exec.submit(randomTask(futures[i]));
        }

        return CompletableFuture.allOf(futures)
                .whenComplete((r, t) -> {
                    LOG.debug(() -> "Iteration complete");
                });
    }

    @Override
    public void close() {
        try {
            exec.awaitTermination(1, TimeUnit.MINUTES);
            kinesisClient.close();
            s3Client.close();
            cwPublisher.close();
            cwClient.close();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private Runnable randomTask(CompletableFuture<Void> future) {
        TaskType taskType = randomTaskType();
        LOG.debug(() -> String.format("Creating a %s task", taskType.name()));
        switch (taskType) {
            case KINESIS_PUT_RECORD:
                return new PutRecordTask(kinesisClient, cwPublisher, PUT_RECORD_ITERATIONS, future);
            default:
            case S3_GET_OBJECT:
                return new GetObjectTask(s3Client, BUCKET, KEY, GET_OBJECT_ITERATIONS, future);
        }
    }

    private static TaskType randomTaskType() {
        TaskType[] values = TaskType.values();
        return values[RNG.nextInt(values.length)];
    }

    public static void main(String[] args) {
        MyApp app = new MyApp();

        try {
            while (true) {
                app.runIteration().join();
            }
        } finally {
            app.close();
        }
    }
}
