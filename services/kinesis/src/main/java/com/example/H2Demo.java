package com.example;

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.BasicConfigurator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.AwsSystemSetting;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.builder.ClientAsyncHttpConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.core.retry.RetryPolicyAdapter;
import software.amazon.awssdk.core.util.ImmutableMapParameter;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.http.nio.netty.h2.H2MetricsCollector;
import software.amazon.awssdk.http.nio.netty.h2.NettyH2AsyncHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.model.KinesisException;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.FunctionalUtils;

public class H2Demo {

    public static final int COUNT = 500_000;
    public static final int INTERVAL = 10;

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        BasicConfigurator.configure();

        NettyH2AsyncHttpClient sdkHttpClient = new NettyH2AsyncHttpClient(10);
        KinesisAsyncClient client = alpha(
            KinesisAsyncClient
                .builder()
                .asyncHttpConfiguration(ClientAsyncHttpConfiguration
                                            .builder()
                                            .httpClient(sdkHttpClient)
                                            //.httpClientFactory(NettySdkHttpClientFactory.builder()
                                            //.trustAllCertificates(true)
                                            //.build())
                                            .build())
                .overrideConfiguration(ClientOverrideConfiguration
                                           .builder()
                                           .retryPolicy(new RetryPolicyAdapter(PredefinedRetryPolicies.NO_RETRY_POLICY))
                                           .build())
        ).build();

        //
        while (true) {
            client.putRecord(PutRecordRequest.builder()
                                             .streamName("prashray-50")
                                             .partitionKey(UUID.randomUUID().toString())
                                             .data(ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}))
                                             .build())
                  .whenComplete((r, e) -> {
                      if (r != null) {
                          System.out.println("SUCCESS: " + r);
                      } else if (!(e.getCause() instanceof KinesisException)) {
                          e.printStackTrace();
                      }
                  }).join();
            break;
        }
        sdkHttpClient.close();

        //        List<Throwable> exceptions = new ArrayList<>();
        //        CountDownLatch latch = new CountDownLatch(COUNT);
        //        AtomicInteger submitCount = new AtomicInteger(0);
        //
        //        int workerThreadCount = 1;
        //        int councurrentConnections = 10;
        //        ExecutorService executorService = Executors.newFixedThreadPool(workerThreadCount);
        //        for (int i = 0; i < workerThreadCount; i++) {
        //            executorService.submit(() -> {
        //                Semaphore permits = new Semaphore(councurrentConnections / workerThreadCount);
        //                while (submitCount.incrementAndGet() <= COUNT) {
        //                    invokeSafely((FunctionalUtils.UnsafeRunnable) permits::acquire);
        //                    client.putRecord(PutRecordRequest.builder()
        //                                                     .streamName("prashray-50")
        //                                                     .partitionKey(UUID.randomUUID().toString())
        //                                                     .data(ByteBuffer.wrap(new byte[] {1, 2, 3}))
        //                                                     .build())
        //                          .whenComplete((r, e) -> {
        //                              System.out.println(r);
        //                              if (submitCount.get() % INTERVAL == 0) {
        //                                  System.out.println("COUNT=" + submitCount.get());
        //                              }
        //                              permits.release();
        //                              if (e != null) {
        //                                  exceptions.add(e);
        //                              }
        //                              latch.countDown();
        //                          });
        //                }
        //            });
        //        }
        //        latch.await();
        //        System.out.println("Exceptions::::::::");
        //        for (Throwable e : exceptions) {
        //            e.printStackTrace();
        //        }
        //        executorService.shutdown();
        //        executorService.awaitTermination(30, TimeUnit.SECONDS);
        //        System.out.println("SHUTTING DOWN CLIENT");
    }

    private static KinesisAsyncClientBuilder alpha(KinesisAsyncClientBuilder builder) {
        return builder.endpointOverride(URI.create("https://aws-kinesis-alpha.corp.amazon.com"))
                      .region(Region.US_EAST_1)
                      .credentialsProvider(() -> new AwsCredentials("AKIAFKNUZVAC6HDWUJRA", "YF/V6JcKVN30trTF5jqgXEVAJNkAOb/N20GXuHsq"));
    }

    private static KinesisAsyncClientBuilder devPerf(KinesisAsyncClientBuilder builder) {
        return builder.endpointOverride(URI.create("https://kinesis-devperf2.us-east-1.amazon.com"))
                      .region(Region.US_EAST_1)
                      .credentialsProvider(() -> new AwsCredentials("AKIAGTRV6ARSGLEGSSKQ", "3NiC+3IVBgVNValHyCiIkh2SamQWrAbtHrc9XS6O"));
    }

    private static void makeRequest(NettyH2AsyncHttpClient sdkHttpClient) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        sdkHttpClient.prepareRequest(SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.POST)
                                                       .headers(ImmutableMapParameter.<String, List<String>>builder()
                                                                    .put("Authorization", singletonList("AWS4-HMAC-SHA256 Credential=AKIAGTRV6ARSGLEGSSKQ/20180115/us-east-1/kinesis/aws4_request, SignedHeaders=content-length;content-type;user-agent;x-amz-date;x-amz-sdk-invocation-id;x-amz-target, Signature=37ebc3e8fbaa578ec821635bcdd8bb1d1a33fc8ee68fca74d7c23c360be1e622"))
                                                                    .put("Content-Length", singletonList("104"))
                                                                    .put("Content-Type", singletonList("application/x-amz-json-1.1"))
                                                                    .put("Host", singletonList("kinesis-devperf2.us-east-1.amazon.com"))
                                                                    .put("User-Agent", singletonList("aws-sdk-java/2.0.0-preview-5-SNAPSHOT Mac_OS_X/10.12.6 Java_HotSpot(TM)_64-Bit_Server_VM/9.0.1+11/9.0.1"))
                                                                    .put("X-Amz-Date", singletonList("20180115T201830Z"))
                                                                    .put("x-amz-sdk-invocation-id", singletonList("x-amz-sdk-invocation-id: 908cee5e-31c9-18e8-8660-7675841faddb"))
                                                                    .put("X-Amz-Target", singletonList("Kinesis_20131202.PutRecord"))
                                                                    .build())
                                                       .host("kinesis-devperf2.us-east-1.amazon.com")
                                                       .encodedPath("/")
                                                       .port(443)
                                                       .protocol("https")
                                                       .build(), SdkRequestContext.builder().build(), new SdkHttpRequestProvider() {
                                         @Override
                                         public long contentLength() {
                                             return 104;
                                         }

                                         @Override
                                         public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                                             subscriber.onSubscribe(new Subscription() {
                                                 @Override
                                                 public void request(long l) {
                                                     subscriber.onNext(ByteBuffer.wrap("{\"StreamName\":\"prashray-50\",\"Data\":\"AQIDBAUGBwgJ\",\"PartitionKey\":\"9aa44c57-fafb-48b0-8759-489b2a49466e\"}".getBytes(StandardCharsets.UTF_8)));
                                                     subscriber.onComplete();
                                                 }

                                                 @Override
                                                 public void cancel() {

                                                 }
                                             });
                                         }
                                     },
                                     new SdkHttpResponseHandler<Void>() {
                                         @Override
                                         public void headersReceived(SdkHttpResponse response) {

                                         }

                                         @Override
                                         public void onStream(Publisher<ByteBuffer> publisher) {
                                             publisher.subscribe(new Subscriber<ByteBuffer>() {
                                                 @Override
                                                 public void onSubscribe(Subscription subscription) {
                                                     subscription.request(Long.MAX_VALUE);
                                                 }

                                                 @Override
                                                 public void onNext(ByteBuffer buffer) {
                                                     System.out.println(new String(BinaryUtils.copyBytesFrom(buffer), StandardCharsets.UTF_8));
                                                 }

                                                 @Override
                                                 public void onError(Throwable throwable) {
                                                     throwable.printStackTrace();
                                                 }

                                                 @Override
                                                 public void onComplete() {
                                                 }
                                             });

                                         }

                                         @Override
                                         public void exceptionOccurred(Throwable throwable) {
                                             future.completeExceptionally(throwable);
                                         }

                                         @Override
                                         public Void complete() {
                                             future.complete(null);
                                             return null;
                                         }
                                     }
        ).run();

        future.join();
    }
}
