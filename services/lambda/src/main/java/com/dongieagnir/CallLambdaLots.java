package com.dongieagnir;

import java.lang.reflect.Array;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.regions.ServiceMetadata;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class CallLambdaLots {

    public static final int CONCURRENCY = 1000;
    private final LambdaAsyncClient lambda = createClient();

    @SuppressWarnings("unchecked")
    public void joinAllAtOnce() {
        CompletableFuture.allOf(
                IntStream.range(1, CONCURRENCY)
                        .mapToObj(this::callLambda)
                        .toArray(
                                size ->
                                        (CompletableFuture<InvokeResponse>[])
                                                Array.newInstance(CompletableFuture.class, size)))
                .join();
    }

    private LambdaAsyncClient createClient() {
        URI httpEndpoint = URI.create("https://" + ServiceMetadata.of("lambda").endpointFor(Region.US_WEST_2));
        return LambdaAsyncClient.builder()
                .endpointOverride(httpEndpoint)
                .build();
    }

    private CompletableFuture<InvokeResponse> callLambda(int payload) {
        return lambda.invoke(
                InvokeRequest.builder()
                        .functionName("DongieHelloWorld")
                        .invocationType(InvocationType.EVENT)
                        .payload(ByteBuffer.wrap(Integer.toString(payload).getBytes()))
                        .build())
                .whenComplete((r,e) -> System.out.printf("%d done.%n", payload));
    }

    public static void main(String[] args) throws InterruptedException {
        CallLambdaLots cll = new CallLambdaLots();

        try {
            cll.joinAllAtOnce();
        } finally {
            System.out.println("Completed");
        }
    }
}