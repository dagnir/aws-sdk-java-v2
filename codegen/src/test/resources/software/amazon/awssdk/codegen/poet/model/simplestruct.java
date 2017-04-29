package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.SimpleStructMarshaller;

public class SimpleStruct implements Serializable, Cloneable, StructuredPojo {
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

    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder_() {
        return new BeanStyleBuilder();
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
    public SimpleStruct clone() {
        try {
            return (SimpleStruct) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
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

    public interface Builder {
        /**
         *
         * @param stringMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);

        SimpleStruct build_();
    }

    public static class BeanStyleBuilder implements Builder {
        private String stringMember;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(SimpleStruct model) {
            this.stringMember = model.stringMember;
        }

        @Override
        public Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        /**
         *
         * @param stringMember
         */
        public void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public SimpleStruct build_() {
            return new SimpleStruct(this);
        }
    }
}
