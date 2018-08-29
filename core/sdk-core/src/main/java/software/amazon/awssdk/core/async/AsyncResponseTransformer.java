/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * Callback interface to handle a streaming asynchronous response.
 *
 * @param <ResponseT> POJO response type.
 * @param <ResultT>   Type this response handler produces. I.E. the type you are transforming the response into.
 */
@SdkPublicApi
public interface AsyncResponseTransformer<ResponseT, ResultT> {
    /**
     * Return the future holding the transformed response.
     * <p>
     * Note that this will be called for each request attempt, up to the number
     * of retries allowed by the configured {@link
     * software.amazon.awssdk.core.retry.RetryPolicy}.
     * <p>
     * This method is guaranteed to be called before the request is executed,
     * and before {@link #onResponse(Object)} is signaled.
     *
     * @return The future holding the transformed response.
     */
    CompletableFuture<ResultT> newTransformResult();

    /**
     * Called when the unmarshalled response object is ready.
     *
     * @param response The unmarshalled response.
     */
    void onResponse(ResponseT response);

    /**
     * Called when the response stream is ready.
     *
     * @param publisher The publisher.
     */
    void onStream(SdkPublisher<ByteBuffer> publisher);

    /**
     * Called when an exception occurs while establishing the connection or streaming the response. Implementations
     * should free up any resources in this method. This method may be called multiple times during the lifecycle
     * of a request if automatic retries are enabled.
     *
     * @param error Error that occurred.
     */
    void onError(Throwable error);

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all the content to the given file. In the event of an error,
     * the SDK will attempt to delete the file (whatever has been written to it so far). If the file already exists, an
     * exception will be thrown.
     *
     * @param path        Path to file to write to.
     * @param <ResponseT> Pojo Response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(Path path) {
        return new FileAsyncResponseTransformer<>(path);
    }

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all the content to the given file. In the event of an error,
     * the SDK will attempt to delete the file (whatever has been written to it so far). If the file already exists, an
     * exception will be thrown.
     *
     * @param file        File to write to.
     * @param <ResponseT> Pojo Response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(File file) {
        return toFile(file.toPath());
    }

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all content to a byte array.
     *
     * @param <ResponseT> Pojo response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> toBytes() {
        return new ByteArrayAsyncResponseTransformer<>();
    }
}
