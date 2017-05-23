package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.BaseTypeMarshaller;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class BaseType implements ToCopyableBuilder<BaseType.Builder, BaseType>, StructuredPojo {
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

    public interface Builder extends CopyableBuilder<Builder, BaseType> {
        /**
         *
         * @param baseMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder baseMember(String baseMember);
    }

    private static final class BeanStyleBuilder implements Builder {
        private String baseMember;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(BaseType model) {
            setBaseMember(model.baseMember);
        }

        @Override
        public final Builder baseMember(String baseMember) {
            this.baseMember = baseMember;
            return this;
        }

        public final void setBaseMember(String baseMember) {
            this.baseMember = baseMember;
        }

        @Override
        public BaseType build() {
            return new BaseType(this);
        }
    }
}

