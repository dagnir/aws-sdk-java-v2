package com.dongieagnir;

import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SerializingTransformer implements AsyncResponseTransformer<GetObjectResponse, Void> {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();

    @Override
    public void responseReceived(GetObjectResponse response) {
        eventQueue.add(new ResponseReceived(response));
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        eventQueue.add(new NewStream(publisher));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        eventQueue.add(new ExceptionOcurred(throwable));
    }

    @Override
    public Void complete() {
        return null;
    }

    public BlockingQueue<Event> eventQueue() {
        return eventQueue;
    }

    public interface Event {
    }

    public static class ResponseReceived implements Event {
        private final GetObjectResponse response;

        public ResponseReceived(GetObjectResponse response) {
            this.response = response;
        }

        public GetObjectResponse getResponse() {
            return response;
        }
    }

    public static class NewStream implements Event {
        private final Publisher<ByteBuffer> stream;

        public NewStream(Publisher<ByteBuffer> stream) {
            this.stream = stream;
        }

        public Publisher<ByteBuffer> getStream() {
            return stream;
        }
    }

    public static class ExceptionOcurred implements Event {
        private final Throwable exception;

        public ExceptionOcurred(Throwable exception) {
            this.exception = exception;
        }

        public Throwable getException() {
            return exception;
        }
    }
}
