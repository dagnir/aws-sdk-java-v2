package software.amazon.awssdk.http.nio.netty;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Dongie {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
//        CompletableFuture<Void> cf = new CompletableFuture<Void>();
//        cf.whenComplete((r, t) -> {
//            if (t != null) {
//                System.out.println("Cancelling");
//            }
//                });
//
//        cf.cancel(false);
//
//        cf.get();
        SdkAsyncHttpClient nettyClient = NettyNioAsyncHttpClient.builder().build();

        SdkHttpRequest req = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .host("s3.us-west-2.amazonaws.com")
                .protocol("https")
                .encodedPath("/")
                .build();

        AsyncExecuteRequest clientRequest = AsyncExecuteRequest.builder()
                .request(req)
                .requestContentPublisher(new EmptyRequestContent())
                .responseHandler(new PrintingHandler())
                .build();

        CompletableFuture<Void> cf = nettyClient.execute(clientRequest);
        cf.cancel(true);
        Thread.sleep(1000);
        cf.join();
    }

    public static class PrintingHandler implements SdkAsyncHttpResponseHandler {
        @Override
        public void onHeaders(SdkHttpResponse headers) {
            System.out.println("headers:\n" + headers.statusCode() + ": " + headers.statusText().orElse("<NONE>"));
            for (Map.Entry<String, List<String>> e : headers.headers().entrySet()) {
                System.out.println(e.getKey() + ": " + e.getValue().stream().collect(Collectors.joining(", ")));
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(1024L);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    System.out.println("onNext: " + byteBuffer.remaining());
                    System.out.print(StandardCharsets.UTF_8.decode(byteBuffer));
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("Error");
                }

                @Override
                public void onComplete() {
                    System.out.println("Complete");
                }
            });
        }
    }

    public static class EmptyRequestContent extends EmptyPublisher<ByteBuffer> implements SdkHttpContentPublisher {
        @Override
        public long contentLength() {
            return 0;
        }
    }

    public static class EmptyPublisher<T> implements Publisher<T> {

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            Subscription s = new EmptySubscription(subscriber);
            subscriber.onSubscribe(s);
        }
    }

    public static class EmptySubscription implements Subscription {
        private final Subscriber<?> subscriber;
        private boolean done = false;

        public EmptySubscription(Subscriber<?> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long l) {
            if (done) return;
            done = true;
            if (l <= 0) {
                subscriber.onError(new IllegalArgumentException("Demand must be positive"));
            } else {
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            done = true;
        }
    }
}
