package software.amazon.awssdk.core.internal.http;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

import java.util.concurrent.CompletableFuture;

/**
 * A response handler that returns a transformed response.
 *
 * @param <ResultT> The type of the result.
 */
public interface TransformingAsyncResponseHandler<ResultT> extends SdkAsyncHttpResponseHandler {
    /**
     * Return the future holding the transformed response.
     * <p>
     * This method is guaranteed to be called before the request is executed,
     * and before {@link
     * SdkAsyncHttpResponseHandler#onHeaders(SdkHttpResponse)} is signaled.
     *
     * @return The future holding the transformed response.
     */
    CompletableFuture<ResultT> transformResult();
}
