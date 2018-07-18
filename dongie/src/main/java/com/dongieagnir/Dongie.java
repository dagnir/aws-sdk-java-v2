package com.dongieagnir;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.PublisherToInputStreamAdapter;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Dongie {
    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
        S3AsyncClient s3 = S3AsyncClient.create();
        SerializingTransformer xform = new SerializingTransformer();

        CompletableFuture<?> cf = s3.getObject(r -> r.bucket("dongie-test-bucket").key("async-to-sync-2mb-test-file.dat"),
                xform
        );

        AsyncToSyncCallAdapter callAdapter = new AsyncToSyncCallAdapter((r,i) -> {
            drain(i);
            return null;
        }, xform.eventQueue(), cf);

        callAdapter.adaptAsyncResponse();
    }

    private static void drain(InputStream is) throws IOException {
        byte[] buf = new byte[4096];
        while (is.read(buf) != -1)
            ;
    }
}
