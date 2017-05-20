package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Date;
import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.StructWithTimestampMarshaller;

@Generated("software.amazon.awssdk:codegen")
public class StructWithTimestamp implements ToCopyableBuilder<StructWithTimestamp.Builder, StructWithTimestamp>, StructuredPojo {
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

    public interface Builder extends CopyableBuilder<Builder, StructWithTimestamp> {
        /**
         *
         * @param nestedTimestamp
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedTimestamp(Date nestedTimestamp);
    }

    private static final class BeanStyleBuilder implements Builder {
        private Date nestedTimestamp;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(StructWithTimestamp model) {
            setNestedTimestamp(model.nestedTimestamp);
        }

        @Override
        public Builder nestedTimestamp(Date nestedTimestamp) {
            this.nestedTimestamp = DateCopier.copyDate(nestedTimestamp);
            return this;
        }

        /**
         *
         * @param nestedTimestamp
         */
        public void setNestedTimestamp(Date nestedTimestamp) {
            this.nestedTimestamp = DateCopier.copyDate(nestedTimestamp);
        }

        @Override
        public StructWithTimestamp build() {
            return new StructWithTimestamp(this);
        }
    }
}

