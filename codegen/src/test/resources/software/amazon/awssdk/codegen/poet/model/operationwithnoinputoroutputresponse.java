package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public class OperationWithNoInputOrOutputResponse extends AmazonWebServiceResult<ResponseMetadata> implements
        ToCopyableBuilder<OperationWithNoInputOrOutputResponse.Builder, OperationWithNoInputOrOutputResponse> {
    private OperationWithNoInputOrOutputResponse(BeanStyleBuilder builder) {
    }

    @Override
    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder() {
        return new BeanStyleBuilder();
    }

    public static Class<? extends Builder> beanStyleBuilderClass() {
        return BeanStyleBuilder.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OperationWithNoInputOrOutputResponse)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("}");
        return sb.toString();
    }

    public interface Builder extends CopyableBuilder<Builder, OperationWithNoInputOrOutputResponse> {
    }

    private static final class BeanStyleBuilder implements Builder {
        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(OperationWithNoInputOrOutputResponse model) {
        }

        @Override
        public OperationWithNoInputOrOutputResponse build() {
            return new OperationWithNoInputOrOutputResponse(this);
        }
    }
}

