package software.amazon.awssdk.services.kinesis;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.BasicConfigurator;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardBaseEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseTransformer;
import software.amazon.awssdk.utils.AttributeMap;

public class H2Demo {

    private static final String STREAM_NAME = "foobar";
    private static final String CONSUMER_ARN = "arn:aws:kinesis:us-east-1:052958737983:stream/foobar/consumer/consumer1:123456";

    public static final int COUNT = 500_000;
    public static final int INTERVAL = 10;
    private static final Random random = new Random();

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        BasicConfigurator.configure();
        //        KinesisAsyncClient client = alpha(
        //            KinesisAsyncClient
        //                .builder()
        //                .asyncHttpConfiguration(ClientAsyncHttpConfiguration
        //                                            .builder()
        //                                            .httpClientFactory(NettySdkHttpClientFactory.builder()
        //                                                                                        .trustAllCertificates(true)
        //                                                                                        .build())
        //                                            .build())
        //        ).build();

        int numSubscribers = 1;
        CountDownLatch latch = new CountDownLatch(numSubscribers);
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

        String consumerArn = "arn:aws:kinesis:us-east-1:052958737983:stream/foobar/consumer/shorea-consumer:1525898737";
        if (true) {

            subscribeToShardResponseHandler(client, "Stream-", consumerArn).join();
            System.exit(0);
        }

        client.close();
    }

    private static ExecutorService startProducer(KinesisAsyncClient client) {
        ExecutorService recordProducer = Executors.newSingleThreadExecutor();
        recordProducer.submit(() -> {
            while (true) {
                System.out.println("Putting record");
                client.putRecord(PutRecordRequest.builder()
                                                 .streamName(STREAM_NAME)
                                                 .partitionKey(UUID.randomUUID().toString())
                                                 .data(ByteBuffer.wrap(randomBytes(1000 * 100)))
                                                 .build())
                      .join();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return recordProducer;
    }

    private static CompletableFuture<Integer> subscribeToShardResponseHandler(KinesisAsyncClient client, String prefix, String consumerArn) {
        return client.subscribeToShard(SubscribeToShardRequest.builder()
                                                              .consumerARN(CONSUMER_ARN)
                                                              .shardId("shardId-000000000000")
                                                              .startingPosition(ShardIteratorType.LATEST)
                                                              .consumerARN(consumerArn)
                                                              .build(),
                                       new SubscribeToShardResponseTransformer<Integer>() {
                                           AtomicInteger count = new AtomicInteger(0);

                                           @Override
                                           public void responseReceived(SubscribeToShardResponse response) {
                                               System.out.println(prefix + ": Initial Response = " + response);
                                           }

                                           @Override
                                           public void onStream(SubscribeToShardResponseTransformer.Publisher p) {
                                               p.subscribe(new Subscriber<SubscribeToShardBaseEvent>() {
                                                   public Subscription subscription;

                                                   @Override
                                                   public void onSubscribe(Subscription subscription) {
                                                       this.subscription = subscription;
                                                       subscription.request(Long.MAX_VALUE);
                                                       subscription.cancel();
                                                   }

                                                   @Override
                                                   public void onNext(SubscribeToShardBaseEvent subscribeToShardEvent) {
                                                       subscribeToShardEvent.visit(new MyVisitor());
                                                       count.incrementAndGet();
                                                   }

                                                   @Override
                                                   public void onError(Throwable throwable) {

                                                   }

                                                   @Override
                                                   public void onComplete() {
                                                       try {
                                                           Thread.sleep(5000);
                                                       } catch (InterruptedException e) {
                                                           e.printStackTrace();
                                                       }
                                                   }
                                               });

                                           }

                                           @Override
                                           public void exceptionOccurred(Throwable throwable) {

                                           }

                                           @Override
                                           public Integer complete() {
                                               return count.get();
                                           }
                                       });
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

    private static final class MyVisitor extends SubscribeToShardBaseEvent.Visitor {

        @Override
        public void visit(SubscribeToShardEvent event) {
            System.out.println("Received records " + event.records());
        }
    }

}
