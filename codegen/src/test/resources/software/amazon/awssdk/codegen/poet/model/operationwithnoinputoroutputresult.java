package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public class OperationWithNoInputOrOutputResult extends AmazonWebServiceResult<ResponseMetadata> implements
        ToCopyableBuilder<OperationWithNoInputOrOutputResult.Builder, OperationWithNoInputOrOutputResult> {
    private OperationWithNoInputOrOutputResult(BeanStyleBuilder builder) {
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
        if (!(obj instanceof OperationWithNoInputOrOutputResult)) {
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

    public interface Builder extends CopyableBuilder<Builder, OperationWithNoInputOrOutputResult> {
    }

    private static final class BeanStyleBuilder implements Builder {
        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(OperationWithNoInputOrOutputResult model) {
        }

        @Override
        public OperationWithNoInputOrOutputResult build() {
            return new OperationWithNoInputOrOutputResult(this);
        }
    }
}

