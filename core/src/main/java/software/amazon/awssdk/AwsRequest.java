package software.amazon.awssdk;

import java.util.Optional;

/**
 * Base class for all AWS Service requests.
 */
public abstract class AwsRequest implements SdkRequest {
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

        protected BuilderImpl() {}

        protected BuilderImpl(AwsRequest request) {
            this.awsRequestOverrideConfig = request.requestOverrideConfig;
        }

        public Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig) {
            this.awsRequestOverrideConfig = awsRequestOverrideConfig;
            return this;
        }

        @Override
        final public AwsRequestOverrideConfig requestOverrideConfig() {
            return awsRequestOverrideConfig;
        }
    }
}