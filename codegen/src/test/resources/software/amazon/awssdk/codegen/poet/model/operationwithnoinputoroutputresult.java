package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import software.amazon.awssdk.AmazonWebServiceResult;

public class OperationWithNoInputOrOutputResult extends AmazonWebServiceResult implements Serializable, Cloneable {
    private OperationWithNoInputOrOutputResult(Builder builder) {
    }

    public static Builder builder_() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
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
        OperationWithNoInputOrOutputResult other = (OperationWithNoInputOrOutputResult) obj;
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

    public static class Builder {
        private Builder() {
        }

        private Builder(OperationWithNoInputOrOutputResult model) {
        }

        public OperationWithNoInputOrOutputResult build_() {
            return new OperationWithNoInputOrOutputResult(this);
        }
    }
}
