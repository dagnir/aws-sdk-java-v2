package com.dongieagnir;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

public final class GetObjectTask implements Runnable {
    private static final Logger LOG = Logger.loggerFor(GetObjectTask.class);

    private final S3Client s3;
    private final String bucket;
    private final String key;
    private final int iterations;
    private final CompletableFuture<Void> future;

    public GetObjectTask(S3Client s3, String bucket, String key, int iterations, CompletableFuture<Void> future) {
        this.s3 = s3;
        this.bucket = bucket;
        this.key = key;
        this.iterations = iterations;
        this.future = future;
    }

    @Override
    public void run() {
        try {
            doDownloads();
        } finally {
            future.complete(null);
        }
    }

    private void doDownloads() {
        for (int i = 0; i < iterations; ++i) {
            try {
                s3.getObject(r -> r.bucket(bucket).key(key), (resp, is) -> {
                    IoUtils.drainInputStream(is);
                    return null;
                });
            } catch (Throwable t) {
                LOG.warn(() -> "GET Object error", t);
                throw t;
            }
        }
    }
}
