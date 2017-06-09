package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.SimpleStructMarshaller;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class SimpleStruct implements StructuredPojo, ToCopyableBuilder<SimpleStruct.Builder, SimpleStruct> {
    private final String stringMember;

    private SimpleStruct(BeanStyleBuilder builder) {
        this.stringMember = builder.stringMember;
    }

    /**
     *
     * @return
     */
    public String stringMember() {
        return stringMember;
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
        hashCode = 31 * hashCode + ((stringMember() == null) ? 0 : stringMember().hashCode());
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
        if (!(obj instanceof SimpleStruct)) {
            return false;
        }
        SimpleStruct other = (SimpleStruct) obj;
        if (other.stringMember() == null ^ this.stringMember() == null) {
            return false;
        }
        if (other.stringMember() != null && !other.stringMember().equals(this.stringMember())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (stringMember() != null) {
            sb.append("StringMember: ").append(stringMember()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        SimpleStructMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, SimpleStruct> {
        /**
         *
         * @param stringMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);
    }

    private static final class BeanStyleBuilder implements Builder {
        private String stringMember;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(SimpleStruct model) {
            setStringMember(model.stringMember);
        }

        @Override
        public final Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public final void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public SimpleStruct build() {
            return new SimpleStruct(this);
        }
    }
}

