package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.SimpleStructMarshaller;

public class SimpleStruct implements Cloneable, Serializable, StructuredPojo {
    private String stringMember;

    private SimpleStruct(Builder builder) {
        this.stringMember = builder.stringMember;
    }

    /**
     * turn
     */
    public String getStringMember() {
        return stringMember;
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
        hashCode = 31 * hashCode + ((getStringMember() == null) ? 0 : getStringMember().hashCode());
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
        if (other.getStringMember() == null ^ this.getStringMember() == null) {
            return false;
        }
        if (other.getStringMember() != null && !other.getStringMember().equals(this.getStringMember())) {
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
        if (getStringMember() != null) {
            sb.append("StringMember: ").append(getStringMember()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        SimpleStructMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public static class Builder {
        private String stringMember;

        private Builder() {
        }

        private Builder(SimpleStruct model) {
            this.stringMember = model.stringMember;
        }

        /**
         *
         * @param stringMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setStringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public SimpleStruct build_() {
            return new SimpleStruct(this);
        }
    }
}
