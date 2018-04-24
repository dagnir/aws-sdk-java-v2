package software.amazon.awssdk.services.kinesis;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.BasicConfigurator;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.builder.ClientAsyncHttpConfiguration;
import software.amazon.awssdk.core.client.builder.ClientBuilder;
import software.amazon.awssdk.core.flow.FlowPublisher;
import software.amazon.awssdk.core.flow.FlowResponseTransformer;
import software.amazon.awssdk.core.flow.ResponseIterator;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.RecordBatchEvent;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;

public class H2Demo {

    private static final String ALPHA_STREAM_NAME = "foobar";
    private static final String DEVPERF_STREAM_NAME = "prashray-50";

    public static final int COUNT = 500_000;
    public static final int INTERVAL = 10;
    private static final Random random = new Random();

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        BasicConfigurator.configure();
        KinesisAsyncClient client = alpha(
            KinesisAsyncClient
                .builder()
                .asyncHttpConfiguration(ClientAsyncHttpConfiguration
                                            .builder()
                                            .httpClientFactory(NettySdkHttpClientFactory.builder()
                                                                                        .trustAllCertificates(true)
                                                                                        .maxConnectionsPerEndpoint(10)
                                                                                        .build())
                                            .build())
        ).build();

//        ExecutorService recordProducer = startProducer();
        long start = System.nanoTime();
        ResponseIterator<SubscribeToShardResponse, RecordBatchEvent> iterator =
            client.subscribeToShardBlocking(SubscribeToShardRequest.builder()
                                                                   .consumerARN(ALPHA_STREAM_NAME)
                                                                   .shardId("shardId-000000000000")
                                                                   .shardIteratorType(ShardIteratorType.LATEST)
                                                                   .streamName(ALPHA_STREAM_NAME)
                                                                   .build());
        iterator.forEachRemaining(System.out::println);
        //        subscribeToShardResponseHandler(client).join();
        try {
            System.out.println("Total time = " + (System.nanoTime() - start));
        } catch (Exception e) {
            System.out.println("Closing client");
            client.close();
        }
        System.out.println("Closing client");
        client.close();
//        recordProducer.shutdownNow();
    }

    private static ExecutorService startProducer() {
        KinesisAsyncClient client = alpha(
            KinesisAsyncClient
                .builder()
                .asyncHttpConfiguration(ClientAsyncHttpConfiguration
                                            .builder()
                                            .httpClientFactory(NettySdkHttpClientFactory.builder()
                                                                                        .trustAllCertificates(true)
                                                                                        .maxConnectionsPerEndpoint(10)
                                                                                        .build())
                                            .build())
        ).build();
        ExecutorService recordProducer = Executors.newSingleThreadExecutor();
        recordProducer.submit(() -> {
            while (true) {
                client.putRecord(PutRecordRequest.builder()
                                                 .streamName(ALPHA_STREAM_NAME)
                                                 .partitionKey(UUID.randomUUID().toString())
                                                 .data(ByteBuffer.wrap(randomBytes(100)))
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

    private static CompletableFuture<Integer> subscribeToShardResponseHandler(KinesisAsyncClient client) {
        return client.subscribeToShard(SubscribeToShardRequest.builder()
                                                              .consumerARN(ALPHA_STREAM_NAME)
                                                              .shardId("shardId-000000000000")
                                                              .shardIteratorType(ShardIteratorType.LATEST)
                                                              .streamName(ALPHA_STREAM_NAME)
                                                              .build(),
                                       new FlowResponseTransformer<SubscribeToShardResponse, RecordBatchEvent, Integer>() {
                                           AtomicInteger count = new AtomicInteger(0);

                                           @Override
                                           public void responseReceived(SubscribeToShardResponse response) {
                                               System.out.println("Initial Response = " + response);
                                           }

                                           @Override
                                           public void onStream(FlowPublisher<RecordBatchEvent> p) {
                                               p.subscribe(new Subscriber<RecordBatchEvent>() {
                                                   @Override
                                                   public void onSubscribe(Subscription subscription) {
                                                       subscription.request(Long.MAX_VALUE);
                                                   }

                                                   @Override
                                                   public void onNext(RecordBatchEvent recordBatchEvent) {
                                                       count.incrementAndGet();
                                                       System.out.println("RECORDS = " + recordBatchEvent);
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

    private static <T extends ClientBuilder<?, ?>> T prod(T builder) {
        return (T) builder.region(Region.US_EAST_1)
                          .credentialsProvider(ProfileCredentialsProvider.create("personal"));
    }

    private static <T extends ClientBuilder<?, ?>> T alpha(T builder) {
        return (T) builder.endpointOverride(URI.create("https://aws-kinesis-alpha.corp.amazon.com"))
                          .region(Region.US_EAST_1)
                          .credentialsProvider(ProfileCredentialsProvider.create("kinesis-alpha"));
    }

    private static KinesisAsyncClientBuilder devPerf(KinesisAsyncClientBuilder builder) {
        return builder.endpointOverride(URI.create("https://kinesis-devperf2.us-east-1.amazon.com"))
                      .region(Region.US_EAST_1)
                      .credentialsProvider(ProfileCredentialsProvider.create("kinesis-dev-perf"));
    }

}
