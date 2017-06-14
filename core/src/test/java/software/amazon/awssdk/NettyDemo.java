/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import software.amazon.awssdk.http.DefaultSdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkRequestChannel;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.util.ImmutableMapParameter;

public class NettyDemo {

    public static final String OUTPUT_FILE_NAME = "/var/tmp/1gb.out";

    private static AsynchronousFileChannel INPUT_CHANNEL = openInputChannel();

    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    private static AsynchronousFileChannel openInputChannel() {
        try {
            final Path path = Paths.get(OUTPUT_FILE_NAME);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            return AsynchronousFileChannel.open(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void closeFile(AsynchronousFileChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        streamingPut();
        //        streamingGet();
    }

    private static void streamingPut() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final SdkAsyncHttpClient client = NettySdkHttpClientFactory.builder().build().createHttpClient();
        final AbortableRunnable abortableRunnable = client.prepareRequest(
                new SdkHttpRequestProvider() {

                    private long position = 0;
                    private boolean complete = false;

                    @Override
                    public SdkHttpRequest request() {
                        return DefaultSdkHttpFullRequest
                                .builder()
                                .endpoint(URI.create("https://s3-us-west-2.amazonaws.com"))
                                .httpMethod(SdkHttpMethod.PUT)
                                .headers(ImmutableMapParameter.of("Content-Length", Collections.singletonList("1000000000")))
                                .resourcePath("/shorea-new-us-west-2/async-test.out")
                                .build();
                    }

                    @Override
                    public SdkRequestContext context() {
                        return SdkRequestContext.builder().metrics(new AwsRequestMetrics()).build();
                    }

                    @Override
                    public void readyForData(SdkRequestChannel channel) {
                        //                        try {
                        //                            channel.writeObject(new ChunkedNioFile(new File(OUTPUT_FILE_NAME)));
                        //                        } catch (IOException e) {
                        //                            e.printStackTrace();
                        //                        }
                        executor.submit(() -> {
                            try {
                                while (!complete) {
                                    if (channel.isWriteable()) {
                                        ByteBuffer buffer = ByteBuffer.allocate(1024);

                                        Future<Integer> operation = INPUT_CHANNEL.read(buffer, position);
                                        int result = operation.get();
                                        if (result != -1) {
                                            position += result;
                                            buffer.flip();
                                            channel.write(buffer);
                                            buffer.clear();

                                        } else {
                                            channel.complete();
                                            complete = true;
                                            closeFile(INPUT_CHANNEL);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }

                    @Override
                    public void exceptionOccurred(Throwable exception) {
                        exception.printStackTrace();
                    }

                }

                , new SdkHttpResponseHandler() {

                    @Override
                    public void headersReceived(
                            SdkHttpResponse response) {
                        System.out.println(
                                response.getStatusCode());
                        System.out.println(
                                response.getStatusText());
                        response.getHeaders().forEach(
                                (k, v) -> System.out
                                        .printf("%s: %s\n", k,
                                                v));
                    }

                    @Override
                    public void bodyPartReceived(
                            ByteBuffer part) {
                        byte[] bytes;
                        if (part.hasArray()) {
                            bytes = part.array();
                        } else {
                            bytes = new byte[part.remaining()];
                            part.get(bytes);
                        }
                        System.out.println(new String(bytes,
                                                      StandardCharsets.UTF_8));
                    }

                    @Override
                    public void exceptionOccurred(
                            Throwable throwable) {
                        throwable.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void complete() {
                        System.out.println("REQUEST COMPLETE");
                        latch.countDown();
                    }

                }

        );

        abortableRunnable.run();
        latch.await();
        System.out.println("Past run command");
        client.close();
    }

    private static void streamingGet() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final SdkAsyncHttpClient client = NettySdkHttpClientFactory.builder().build().createHttpClient();
        final AbortableRunnable abortableRunnable = client.prepareRequest(new SdkHttpRequestProvider() {
            @Override
            public SdkHttpRequest request() {
                return DefaultSdkHttpFullRequest.builder()
                                                .endpoint(URI.create("https://s3-us-west-2.amazonaws.com"))
                                                .httpMethod(SdkHttpMethod.GET)
                                                .resourcePath("/shorea-new-us-west-2/1gb.out")
                                                .build();
            }

            @Override
            public SdkRequestContext context() {
                return SdkRequestContext.builder().metrics(new AwsRequestMetrics()).build();
            }

            @Override
            public void readyForData(SdkRequestChannel channel) {
                channel.complete();
            }

            @Override
            public void exceptionOccurred(Throwable exception) {
                exception.printStackTrace();
            }
        }, new SdkHttpResponseHandler() {

            private AsynchronousFileChannel fileChannel = openFileChannel();
            private long position = 0;

            private AsynchronousFileChannel openFileChannel() throws IOException {
                final Path path = Paths.get(OUTPUT_FILE_NAME);
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
                return AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
            }

            @Override
            public void headersReceived(SdkHttpResponse response) {
                System.out.println(response.getStatusCode());
                System.out.println(response.getStatusText());
                response.getHeaders().forEach((k, v) -> System.out.printf("%s: %s\n", k, v));
            }

            @Override
            public void bodyPartReceived(ByteBuffer part) {
                fileChannel.write(part, position);
                position += part.limit();
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
                closeFile(fileChannel);
                throwable.printStackTrace();
                latch.countDown();
            }

            @Override
            public void complete() {
                closeFile(fileChannel);
                System.out.println("REQUEST COMPLETE");
                latch.countDown();
            }

        });

        abortableRunnable.run();
        latch.await();
        System.out.println("Past run command");
        client.close();
    }
}
