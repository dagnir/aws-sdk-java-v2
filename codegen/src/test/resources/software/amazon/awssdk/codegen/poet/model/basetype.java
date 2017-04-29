package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.BaseTypeMarshaller;

public class BaseType implements Serializable, Cloneable, StructuredPojo {
    private final String baseMember;

    private BaseType(BeanStyleBuilder builder) {
        this.baseMember = builder.baseMember;
    }

    /**
     *
     * @return
     */
    public String baseMember() {
        return baseMember;
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
        hashCode = 31 * hashCode + ((baseMember() == null) ? 0 : baseMember().hashCode());
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
        if (other.baseMember() == null ^ this.baseMember() == null) {
            return false;
        }
        if (other.baseMember() != null && !other.baseMember().equals(this.baseMember())) {
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
        if (baseMember() != null) {
            sb.append("BaseMember: ").append(baseMember()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        BaseTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder {
        /**
         *
         * @param baseMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder baseMember(String baseMember);

        BaseType build_();
    }

    public static class BeanStyleBuilder implements Builder {
        private String baseMember;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(BaseType model) {
            this.baseMember = model.baseMember;
        }

        @Override
        public Builder baseMember(String baseMember) {
            this.baseMember = baseMember;
            return this;
        }

        /**
         *
         * @param baseMember
         */
        public void setBaseMember(String baseMember) {
            this.baseMember = baseMember;
        }

        @Override
        public BaseType build_() {
            return new BaseType(this);
        }
    }
}
