package software.amazon.awssdk.services.kinesis;

import io.reactivex.Flowable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.BasicConfigurator;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StartingPosition;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.awssdk.utils.AttributeMap;

public class H2Demo {

    //    private static final String STREAM_NAME = "foobar";
    private static final String STREAM_NAME = "pfifer-test";
    private static final String ERROR_CONSUMER_ARN = "arn:aws:kinesis:us-east-1:052958737983:stream/foobar/consumer/joker:1527204892";
    //    private static final String CONSUMER_ARN = "arn:aws:kinesis:us-east-1:052958737983:stream/foobar/consumer/shorea-consumer:1525898737";
    private static final String CONSUMER_ARN = "arn:aws:kinesis:us-east-1:052958737983:stream/pfifer-test/consumer/shorea-consumer:1529977611";

    private static final Random random = new Random();

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onResponse(r -> System.out.println("Initial response = " + r))
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .publisherTransformer(p -> p.filter(e -> e instanceof SubscribeToShardEvent).limit(100))
            //            .subscriber(e -> System.out.println("Received event - " + e))
            .build();
        BasicConfigurator.configure();

        KinesisAsyncClient client = alpha(
            KinesisAsyncClient
                .builder()
                .asyncHttpClient(
                    NettyNioAsyncHttpClient.builder()
                                           .buildWithDefaults(AttributeMap.builder()
                                                                          .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                                                                          .put(SdkHttpConfigurationOption.PROTOCOL, Protocol.HTTP2)
                                                                          .build()))
        ).build();

        SubscribeToShardRequest request = SubscribeToShardRequest.builder()
                                                                 .shardId("shardId-000000000000")
                                                                 .startingPosition(
                                                                     StartingPosition.builder()
                                                                                     .type(ShardIteratorType.TRIM_HORIZON)
                                                                                     .build())
                                                                 .consumerARN(CONSUMER_ARN)
                                                                 .build();
        responseHandlerBuilder_PublisherTransformer(client, request).join();
        client.close();
    }

    /**
     * Creates a SubscribeToShardResponseHandler using the builder which lets you set each lifecycle callback separately
     * rather than implementing the interface.
     */
    private static CompletableFuture<Void> responseHandlerBuilder(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onResponse(r -> System.out.println("Receieved initial response"))
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .onComplete(() -> System.out.println("All records stream successfully"))
            // Must supplier some type of subscriber
            .subscriber(e -> System.out.println("Recieved event - " + e))
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Creates a SubscribeToShardResponseHandler.Visitor using the builder which lets you register an event handler for
     * all events you're interested in rather than implementing the interface.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_VisitorBuilder(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler.Visitor visitor = SubscribeToShardResponseHandler.Visitor
            .builder()
            .onSubscribeToShardEvent(e -> System.out.println("Received subscribe to shard event " + e))
            .build();
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .subscriber(visitor)
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Subscribes to the stream of events by implementing the SubscribeToShardResponseHandler.Visitor interface.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_Visitor(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler.Visitor visitor = new SubscribeToShardResponseHandler.Visitor() {
            @Override
            public void visit(SubscribeToShardEvent event) {
                System.out.println("Received subscribe to shard event " + event);
            }
        };
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .subscriber(visitor)
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Using the SubscribeToShardResponseHandler.Builder and a traditional subscriber.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_Subscriber(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .subscriber(MySubscriber::new)
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Using the SubscribeToShardResponseHandler.Builder and a simple Consumer of events to subscribe.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_Consumer(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .subscriber(e -> System.out.println("Received event - " + e))
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Uses the publisherTransformer method to customize the publisher before ultimately subscribing to it.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_PublisherTransformer(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onResponse(r -> System.out.println("Initial response = " + r))
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .publisherTransformer(p -> p.filter(e -> e instanceof SubscribeToShardEvent).limit(100))
            .onEventStream(p -> {
                p.subscribe(System.out::println);
                p.subscribe(System.out::println);
            })
            //            .subscriber(e -> System.out.println("Received event - " + e))
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Subscribes to the publisher using the onEventStream lifecycle callback method. This allows for greater control
     * over the publisher and allows for transformation methods on the publisher like map and buffer.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_OnEventStream(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .onEventStream(p -> p.filter(SubscribeToShardEvent.class).subscribe(new MySubscriber()))
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Since a Flowable is also a publisher, the publisherTransformer method integrates nicely with RxJava. Note that
     * you must adapt to an SdkPublisher.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_OnEventStream_RxJava(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .publisherTransformer(p -> SdkPublisher.adapt(Flowable.fromPublisher(p).limit(100)))
            .build();
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Creates a SubscribeToShardResponseHandler the classic way by implementing the interface.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_Classic(KinesisAsyncClient client, SubscribeToShardRequest request) {
        SubscribeToShardResponseHandler responseHandler = new SubscribeToShardResponseHandler() {

            @Override
            public void responseReceived(SubscribeToShardResponse response) {
                System.out.println("Receieved initial response");
            }

            @Override
            public void onEventStream(SdkPublisher<SubscribeToShardEventStream> publisher) {
                publisher
                    // Filter to only SubscribeToShardEvents
                    .filter(SubscribeToShardEvent.class)
                    // Flat map into a publisher of just records
                    .flatMapIterable(SubscribeToShardEvent::records)
                    // Limit to 1000 total records
                    .limit(1000)
                    // Batch records into lists of 25
                    .buffer(25)
                    // Print out each record batch
                    .subscribe(batch -> System.out.println("Record Batch - " + batch));
            }

            @Override
            public void complete() {
                System.out.println("All records stream successfully");
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
                System.err.println("Error during stream - " + throwable.getMessage());
            }
        };
        return client.subscribeToShard(request, responseHandler);
    }

    /**
     * Uses RxJava via the onEventStream lifecycle method. This gives you full access to the publisher which can be used
     * to create an Rx Flowable.
     */
    private static CompletableFuture<Void> responseHandlerBuilder_RxJava(KinesisAsyncClient client, SubscribeToShardRequest request) {

        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
            .builder()
            .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
            .onEventStream(p -> Flowable.fromPublisher(p)
                                        .ofType(SubscribeToShardEvent.class)
                                        .flatMapIterable(SubscribeToShardEvent::records)
                                        .limit(1000)
                                        .buffer(25)
                                        .subscribe(e -> System.out.println("Record batch = " + e)))
            .build();
        return client.subscribeToShard(request, responseHandler);

    }

    /**
     * Simple subscriber implementation that prints events and cancels the subscription after 100 events.
     */
    private static class MySubscriber implements Subscriber<SubscribeToShardEventStream> {

        private Subscription subscription;
        private AtomicInteger eventCount = new AtomicInteger(0);

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            this.subscription.request(1);
        }

        @Override
        public void onNext(SubscribeToShardEventStream shardSubscriptionEventStream) {
            System.out.println("Received event " + shardSubscriptionEventStream);
            if (eventCount.incrementAndGet() >= 100) {
                // You can cancel the subscription at any time if you wish to stop receiving events.
                subscription.cancel();
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println("Error occurred while stream - " + throwable.getMessage());
        }

        @Override
        public void onComplete() {
            System.out.println("Finished streaming all events");
        }
    }

    private static CompletableFuture<Void> subscribeToShardResponseHandler(KinesisAsyncClient client) {
        System.out.println("Invoking STS");

        SubscribeToShardResponseHandler responseHandler =
            SubscribeToShardResponseHandler.builder()
                                           .onError(e -> {
                                               System.out.println("SHOREA onExceptionOccurred");
                                               e.printStackTrace();
                                           })
                                           .onResponse(System.out::println)
                                           // Supplier for Subscribe
                                           // No collisions with Subscriber names on response handler
                                           .subscriber(e -> System.out.println(e))
                                           .build();

        return client.subscribeToShard(SubscribeToShardRequest.builder()
                                                              .shardId("shardId-000000000000")
                                                              .startingPosition(
                                                                  StartingPosition.builder()
                                                                                  .type(ShardIteratorType.TRIM_HORIZON)
                                                                                  .build())
                                                              .consumerARN(CONSUMER_ARN)
                                                              .build(),
                                       responseHandler);
    }

    private static SubscribeToShardResponseHandler createImplementingTransformer(String prefix) {
        return new SubscribeToShardResponseHandler() {

            @Override
            public void responseReceived(SubscribeToShardResponse response) {
                System.out.println(prefix + ": Initial Response = " + response);
            }

            @Override
            public void onEventStream(SdkPublisher<SubscribeToShardEventStream> p) {
                p.subscribe(new Subscriber<SubscribeToShardEventStream>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {

                    }

                    @Override
                    public void onNext(SubscribeToShardEventStream shardSubscriptionEventStream) {
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
                System.out.println("exception occurred");
                throwable.printStackTrace();
            }

            @Override
            public void complete() {
                System.out.println("complete called");
            }
        };
    }

    private static Subscriber<? super SubscribeToShardEventStream> newSubscriber() {
        return
            new Subscriber<SubscribeToShardEventStream>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(SubscribeToShardEventStream subscribeToShardEvent) {
                    System.out.println(subscribeToShardEvent);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("Subscriber#onError called");
                    throwable.printStackTrace();
                }

                @Override
                public void onComplete() {
                    System.out.println("Subscriber#onComplete called");
                }
            };
    }

    private static byte[] randomBytes(int numBytes) {
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);
        return bytes;
    }

    private static <T extends AwsClientBuilder<?, ?>> T prod(T builder) {
        return (T) builder.region(Region.US_EAST_1)
                          .credentialsProvider(ProfileCredentialsProvider.create("personal"));
    }

    private static <T extends AwsClientBuilder<?, ?>> T alpha(T builder) {
        return (T) builder.endpointOverride(URI.create("https://aws-kinesis-alpha.corp.amazon.com"))
                          .region(Region.US_EAST_1)
                          .credentialsProvider(ProfileCredentialsProvider.create("kinesis-alpha"));
    }

    private static KinesisAsyncClientBuilder devPerf(KinesisAsyncClientBuilder builder) {
        return builder.endpointOverride(URI.create("https://kinesis-devperf2.us-east-1.amazon.com"))
                      .region(Region.US_EAST_1)
                      .credentialsProvider(ProfileCredentialsProvider.create("kinesis-dev-perf"));
    }

    private static KinesisAsyncClientBuilder hailstone(KinesisAsyncClientBuilder builder) {
        return builder.endpointOverride(URI.create("https://kinesis-hailstoneperf.pdx.amazon.com"))
                      .region(Region.US_EAST_1)
                      .credentialsProvider(ProfileCredentialsProvider.create("kinesis-hailstone"));
    }

    private static final class MyVisitor implements SubscribeToShardResponseHandler.Visitor {

        @Override
        public void visit(SubscribeToShardEvent event) {
            System.out.println("Received records " + event.records());
        }
    }

}
