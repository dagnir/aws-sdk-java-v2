package software.amazon.awssdk.services.kinesis;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.apache.log4j.BasicConfigurator;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.ScalingType;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.ShardSubscriptionEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;

public class H2Demo {

    //    private static final String STREAM_NAME = "foobar";
    private static final String STREAM_NAME = "pfifer-test";
    private static final String ERROR_CONSUMER_ARN = "arn:aws:kinesis:us-east-1:052958737983:stream/foobar/consumer/joker:1527204892";
    //    private static final String CONSUMER_ARN = "arn:aws:kinesis:us-east-1:052958737983:stream/foobar/consumer/shorea-consumer:1525898737";
    private static final String CONSUMER_ARN = "arn:aws:kinesis:us-east-1:052958737983:stream/pfifer-test/consumer/shorea-consumer:1529977611";

    private static final Random random = new Random();

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        BasicConfigurator.configure();

        KinesisAsyncClient client = alpha(
            KinesisAsyncClient
                .builder()
                .asyncHttpClientBuilder(
                    NettyNioAsyncHttpClient.builder()
                                           .trustAllCertificates(true))
        ).build();

//        client.listStreams()
//              .join()
//              .streamNames()
//              .forEach(System.out::println);


        client.listStreams().join();

//        subscribeToShardResponseHandler(client).join();
        client.close();
    }

    private static CompletableFuture<Void> subscribeToShardResponseHandler(KinesisAsyncClient client) {
        System.out.println("Invoking STS");

        SubscribeToShardResponseHandler responseHandler =
            SubscribeToShardResponseHandler.builder()
                                           .onResponse(System.out::println)
                                           .publisherTransformer(p -> p.limit(100))
                                           // Supplier for Subscribe
                                           // No collisions with Subscriber names on response handler
                                           .subscriber(e -> System.out.println(e))
                                           .build();

        return client.subscribeToShard(SubscribeToShardRequest.builder()
                                                              .shardId("shardId-000000000000")
                                                              .startingPosition(ShardIteratorType.LATEST)
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
            public void onEventStream(SdkPublisher<ShardSubscriptionEventStream> p) {
                p.subscribe(new Subscriber<ShardSubscriptionEventStream>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {

                    }

                    @Override
                    public void onNext(ShardSubscriptionEventStream shardSubscriptionEventStream) {

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

    private static Subscriber<? super ShardSubscriptionEventStream> newSubscriber() {
        return
            new Subscriber<ShardSubscriptionEventStream>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ShardSubscriptionEventStream subscribeToShardEvent) {
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
