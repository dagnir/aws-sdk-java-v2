package com.dongieagnir;

import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.async.PublisherToInputStreamAdapter;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

public class AsyncToSyncCallAdapter {
    private final PublisherToInputStreamAdapter publisherAdapter = new PublisherToInputStreamAdapter();
    private final ResponseTransformer<GetObjectResponse, Void> responseTransformer;
    private final BlockingQueue<SerializingTransformer.Event> eventQueue;
    private final CompletableFuture<?> completableFuture;

    private GetObjectResponse response;

    public AsyncToSyncCallAdapter(ResponseTransformer<GetObjectResponse, Void> responseTransformer, BlockingQueue<SerializingTransformer.Event> eventQueue, CompletableFuture<?> completableFuture) {
        this.responseTransformer = responseTransformer;
        this.eventQueue = eventQueue;
        this.completableFuture = completableFuture;
    }

    public void adaptAsyncResponse() throws ExecutionException, InterruptedException {
        SerializingTransformer.Event ev;
        while (!completableFuture.isDone()) {
            ev = nextEvent();
            if (ev == null) {
                continue;
            }

            if (ev instanceof SerializingTransformer.ResponseReceived) {
                response = ((SerializingTransformer.ResponseReceived) ev).getResponse();
            } else if (ev instanceof SerializingTransformer.NewStream) {
                Publisher<ByteBuffer> p = ((SerializingTransformer.NewStream) ev).getStream();
                AbortableInputStream is = adaptPublisher(p);

                try {
                    responseTransformer.apply(response, is);
                } catch (Exception e) {
                    System.out.println("Transformer threw");
                }
            } else if (ev instanceof SerializingTransformer.ExceptionOcurred) {
                System.out.println("Exception ocurred");
            }
        }

        completableFuture.get();
    }

    private SerializingTransformer.Event nextEvent() {
        try {
            return eventQueue.poll(2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            completableFuture.cancel(true);
            throw SdkClientException.builder().cause(ie).build();
        }
    }

    private AbortableInputStream adaptPublisher(Publisher<ByteBuffer> publisher) {
        InputStream is = publisherAdapter.adapt(publisher);
        AbortableInputStream abortableIs = new AbortableInputStream(is, () -> invokeSafely(is::close));
        return abortableIs;
    }
}
