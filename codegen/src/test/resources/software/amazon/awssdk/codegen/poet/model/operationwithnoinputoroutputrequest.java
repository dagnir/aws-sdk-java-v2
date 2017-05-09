package software.amazon.awssdk.services.jsonprotocoltests.model;

import software.amazon.awssdk.AmazonWebServiceRequest;

public class OperationWithNoInputOrOutputRequest extends AmazonWebServiceRequest implements Cloneable {
    private OperationWithNoInputOrOutputRequest(BeanStyleBuilder builder) {
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
        if (!(obj instanceof OperationWithNoInputOrOutputRequest)) {
            return false;
        }
        return true;
    }

    @Override
    public OperationWithNoInputOrOutputRequest clone() {
        try {
            return (OperationWithNoInputOrOutputRequest) super.clone();
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
        OperationWithNoInputOrOutputRequest build_();
    }

    private static final class BeanStyleBuilder implements Builder {
        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(OperationWithNoInputOrOutputRequest model) {
        }

        @Override
        public OperationWithNoInputOrOutputRequest build_() {
            return new OperationWithNoInputOrOutputRequest(this);
        }
    }
}
