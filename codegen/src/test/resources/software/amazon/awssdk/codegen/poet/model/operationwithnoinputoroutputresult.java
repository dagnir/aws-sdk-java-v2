package software.amazon.awssdk.services.jsonprotocoltests.model;

import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;

public class OperationWithNoInputOrOutputResult extends AmazonWebServiceResult<ResponseMetadata> implements Cloneable {
    private OperationWithNoInputOrOutputResult(BeanStyleBuilder builder) {
    }

    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder_() {
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
    public OperationWithNoInputOrOutputResult clone() {
        try {
            return (OperationWithNoInputOrOutputResult) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("}");
        return sb.toString();
    }

    public interface Builder {
        OperationWithNoInputOrOutputResult build_();
    }

    private static final class BeanStyleBuilder implements Builder {
        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(OperationWithNoInputOrOutputResult model) {
        }

        @Override
        public OperationWithNoInputOrOutputResult build_() {
            return new OperationWithNoInputOrOutputResult(this);
        }
    }
}
