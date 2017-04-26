package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.Date;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.StructWithTimestampMarshaller;

public class StructWithTimestamp implements Serializable, Cloneable, StructuredPojo {
    private Date nestedTimestamp;

    private StructWithTimestamp(Builder builder) {
        this.nestedTimestamp = builder.nestedTimestamp;
    }

    /**
     * turn
     */
    public Date getNestedTimestamp() {
        return nestedTimestamp;
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
        hashCode = 31 * hashCode + ((getNestedTimestamp() == null) ? 0 : getNestedTimestamp().hashCode());
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
        if (!(obj instanceof StructWithTimestamp)) {
            return false;
        }
        StructWithTimestamp other = (StructWithTimestamp) obj;
        if (other.getNestedTimestamp() == null ^ this.getNestedTimestamp() == null) {
            return false;
        }
        if (other.getNestedTimestamp() != null && !other.getNestedTimestamp().equals(this.getNestedTimestamp())) {
            return false;
        }
        return true;
    }

    @Override
    public StructWithTimestamp clone() {
        try {
            return (StructWithTimestamp) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getNestedTimestamp() != null) {
            sb.append("NestedTimestamp: ").append(getNestedTimestamp()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        StructWithTimestampMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public static class Builder {
        private Date nestedTimestamp;

        private Builder() {
        }

        private Builder(StructWithTimestamp model) {
            this.nestedTimestamp = model.nestedTimestamp;
        }

        /**
         *
         * @param nestedTimestamp
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setNestedTimestamp(Date nestedTimestamp) {
            this.nestedTimestamp = nestedTimestamp;
            return this;
        }

        public StructWithTimestamp build_() {
            return new StructWithTimestamp(this);
        }
    }
}
