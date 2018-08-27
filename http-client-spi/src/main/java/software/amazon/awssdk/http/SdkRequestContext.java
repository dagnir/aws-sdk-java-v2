package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Container for extra dependencies needed during execution of a request.
 */
@ReviewBeforeRelease("Should we keep this? It was previously used for metrics, which was removed.")
@SdkProtectedApi
public class SdkRequestContext {

    private SdkRequestContext(BuilderImpl builder) {
    }

    /**
     * @return Builder instance to construct a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        SdkRequestContext build();
    }

    /**
     * Builder for a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    private static class BuilderImpl implements Builder {
        @Override
        public SdkRequestContext build() {
            return new SdkRequestContext(this);
        }
    }
}
