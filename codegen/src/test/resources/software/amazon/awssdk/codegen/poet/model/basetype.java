package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.BaseTypeMarshaller;

public class BaseType implements Cloneable, Serializable, StructuredPojo {
    private String baseMember;

    private BaseType(Builder builder) {
        this.baseMember = builder.baseMember;
    }

    /**
     * turn
     */
    public String getBaseMember() {
        return baseMember;
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
        hashCode = 31 * hashCode + ((getBaseMember() == null) ? 0 : getBaseMember().hashCode());
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
        if (!(obj instanceof BaseType)) {
            return false;
        }
        BaseType other = (BaseType) obj;
        if (other.getBaseMember() == null ^ this.getBaseMember() == null) {
            return false;
        }
        if (other.getBaseMember() != null && !other.getBaseMember().equals(this.getBaseMember())) {
            return false;
        }
        return true;
    }

    @Override
    public BaseType clone() {
        try {
            return (BaseType) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getBaseMember() != null) {
            sb.append("BaseMember: ").append(getBaseMember()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        BaseTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public static class Builder {
        private String baseMember;

        private Builder() {
        }

        private Builder(BaseType model) {
            this.baseMember = model.baseMember;
        }

        /**
         *
         * @param baseMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setBaseMember(String baseMember) {
            this.baseMember = baseMember;
            return this;
        }

        public BaseType build_() {
            return new BaseType(this);
        }
    }
}
