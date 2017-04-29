package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.Date;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.StructWithTimestampMarshaller;

public class StructWithTimestamp implements Serializable, Cloneable, StructuredPojo {
    private final Date nestedTimestamp;

    private StructWithTimestamp(BeanStyleBuilder builder) {
        this.nestedTimestamp = builder.nestedTimestamp;
    }

    /**
     *
     * @return
     */
    public Date nestedTimestamp() {
        return nestedTimestamp;
    }

    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder_() {
        return new BeanStyleBuilder();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + ((nestedTimestamp() == null) ? 0 : nestedTimestamp().hashCode());
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
        if (other.nestedTimestamp() == null ^ this.nestedTimestamp() == null) {
            return false;
        }
        if (other.nestedTimestamp() != null && !other.nestedTimestamp().equals(this.nestedTimestamp())) {
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
        if (nestedTimestamp() != null) {
            sb.append("NestedTimestamp: ").append(nestedTimestamp()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        StructWithTimestampMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder {
        /**
         *
         * @param nestedTimestamp
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedTimestamp(Date nestedTimestamp);

        StructWithTimestamp build_();
    }

    public static class BeanStyleBuilder implements Builder {
        private Date nestedTimestamp;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(StructWithTimestamp model) {
            this.nestedTimestamp = model.nestedTimestamp;
        }

        @Override
        public Builder nestedTimestamp(Date nestedTimestamp) {
            this.nestedTimestamp = nestedTimestamp;
            return this;
        }

        /**
         *
         * @param nestedTimestamp
         */
        public void setNestedTimestamp(Date nestedTimestamp) {
            this.nestedTimestamp = nestedTimestamp;
        }

        @Override
        public StructWithTimestamp build_() {
            return new StructWithTimestamp(this);
        }
    }
}
