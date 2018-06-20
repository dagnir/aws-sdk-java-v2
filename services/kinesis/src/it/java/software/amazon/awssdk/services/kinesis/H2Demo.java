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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.BasicConfigurator;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardBaseEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseTransformer;

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
                .asyncHttpClientBuilder(
                    NettyNioAsyncHttpClient.builder()
                                           .trustAllCertificates(true))
        ).build();

        //        String streamArn = client.describeStream(r -> r.streamName(STREAM_NAME))
        //                                 .join().streamDescription().streamARN();
        //        String consumerArn = client.describeStreamConsumer(r -> r.streamARN(streamArn)
        //                                                                 .consumerName("shorea-consumer"))
        //                                   .join().consumerDescription().consumerARN();
        String consumerArn = "arn:aws:kinesis:us-east-1:052958737983:stream/foobar/consumer/shorea-consumer:1525898737";
        client.putRecord(PutRecordRequest.builder()
                                         .streamName(STREAM_NAME)
                                         .partitionKey(UUID.randomUUID().toString())
                                         .data(ByteBuffer.wrap(randomBytes(1000 * 100)))
                                         .build())
              .join();
        if (true) {

//            subscribeToShardResponseHandler(client, "Stream-", consumerArn).join();
            System.exit(0);
        }

        //        ExecutorService recordProducer = startProducer(client);
        ExecutorService subscriberExecutor = Executors.newFixedThreadPool(numSubscribers);
        for (int i = 1; i <= numSubscribers; i++) {
            int streamNum = i;
            subscriberExecutor.submit(() -> {
                try {
                    subscribeToShardResponseHandler(client, "Stream-" + streamNum, consumerArn).join();

                    //                    ResponseIterator<SubscribeToShardResponse, RecordBatchEvent> iterator =
                    //                        client.subscribeToShardBlocking(SubscribeToShardRequest.builder()
                    //                                                                               .consumerARN(STREAM_NAME)
                    //                                                                               .shardId("shardId-000000000000")
                    //                                                                               .shardIteratorType(ShardIteratorType.LATEST)
                    //                                                                               .consumerARN(consumerArn)
                    //                                                                               .build());
                    //                    System.out.println("Has iterator");
                    //                    iterator.forEachRemaining(System.out::println);
                    System.out.println("Finished processing for stream " + streamNum);
                    System.out.println("Closing client");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Stream " + streamNum + " failed");
                } finally {
                    latch.countDown();
                }

            });
        }
        latch.await();
        System.out.println("Closing client");
        client.close();
        System.out.println("All subscribers finished");
        subscriberExecutor.shutdown();
        subscriberExecutor.awaitTermination(1000, TimeUnit.SECONDS);

        long start = System.nanoTime();
        //                ResponseIterator<SubscribeToShardResponse, RecordBatchEvent> iterator =
        //            client.subscribeToShardBlocking(SubscribeToShardRequest.builder()
        //                                                                   .consumerARN(ALPHA_STREAM_NAME)
        //                                                                   .shardId("shardId-000000000000")
        //                                                                   .shardIteratorType(ShardIteratorType.LATEST)
        //                                                                   .streamName(ALPHA_STREAM_NAME)
        //                                                                   .build());
        //        iterator.forEachRemaining(System.out::println);
        try {
            System.out.println("Total time = " + (System.nanoTime() - start));
        } catch (Exception e) {
            System.out.println("Closing client");
        }
        //        recordProducer.shutdownNow();
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
                                                   @Override
                                                   public void onSubscribe(Subscription subscription) {
                                                       subscription.request(Long.MAX_VALUE);
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
