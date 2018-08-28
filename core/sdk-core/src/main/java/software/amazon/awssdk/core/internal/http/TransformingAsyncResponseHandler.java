package software.amazon.awssdk.core.internal.http;

import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

import java.util.concurrent.CompletableFuture;

/**
 * A response handler that returns a transformed response.
 *
 * @param <ResultT> The type of the result.
 */
public interface TransformingAsyncResponseHandler<ResultT> extends SdkAsyncHttpResponseHandler {
    /**
     * @return The future holding the transformed response.
     */
    CompletableFuture<ResultT> transformResult();
}
