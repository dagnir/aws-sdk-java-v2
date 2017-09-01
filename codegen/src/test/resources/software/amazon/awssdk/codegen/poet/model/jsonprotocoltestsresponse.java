package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.core.AwsResponse;
import software.amazon.awssdk.core.AwsResponseMetadata;

@Generated("software.amazon.awssdk:codegen")
public abstract class JsonProtocolTestsResponse extends AwsResponse {
    protected JsonProtocolTestsResponse(Builder builder) {
        super(builder);
    }

    public interface Builder extends AwsResponse.Builder {
        @Override
        JsonProtocolTestsResponse build();

        @Override
        Builder responseMetadata(AwsResponseMetadata awsResponseMetadata);
    }

    protected abstract static class BuilderImpl extends AwsResponse.BuilderImpl implements Builder {
        protected BuilderImpl() {
        }

        protected BuilderImpl(JsonProtocolTestsResponse request) {
            super(request);
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata awsResponseMetadata) {
            super.responseMetadata(awsResponseMetadata);
            return this;
        }
    }
}

