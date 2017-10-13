package software.amazon.awssdk;

import java.util.Optional;

/**
 * Base class for all AWS Service requests.
 */
public abstract class AwsRequest implements SdkRequest {
    public static final AwsRequest NOOP = NoopRequest.builder().build();

    private final AwsRequestOverrideConfig requestOverrideConfig;

    protected AwsRequest(Builder builder) {
        this.requestOverrideConfig = builder.requestOverrideConfig();
    }

    @Override
    final public Optional<AwsRequestOverrideConfig> requestOverrideConfig() {
        return Optional.ofNullable(requestOverrideConfig);
    }

    @Override
    public abstract Builder toBuilder();

    protected interface Builder extends SdkRequest.Builder {
        @Override
        AwsRequestOverrideConfig requestOverrideConfig();

        Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig);

        @Override
        AwsRequest build();
    }

    protected static abstract class BuilderImpl implements Builder {
        private AwsRequestOverrideConfig awsRequestOverrideConfig;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsRequest request) {
            this.awsRequestOverrideConfig = request.requestOverrideConfig;
        }

        @Override
        public Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig) {
            this.awsRequestOverrideConfig = awsRequestOverrideConfig;
            return this;
        }

        @Override
        final public AwsRequestOverrideConfig requestOverrideConfig() {
            return awsRequestOverrideConfig;
        }
    }

    // FIXME: This is an artifact from AmazonWebServiceRequest. Remove this once the usage of NOOP is removed.
    private static class NoopRequest extends AwsRequest {
        private NoopRequest(Builder b) {
            super(b);
        }

        @Override
        public Builder toBuilder() {
            return new Builder();
        }

        public static Builder builder() {
            return new Builder();
        }

        private static class Builder extends AwsRequest.BuilderImpl {
            @Override
            public NoopRequest build() {
                return new NoopRequest(this);
            }
        }
    }
}
