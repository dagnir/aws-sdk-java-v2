package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.SubTypeOneMarshaller;

@Generated("software.amazon.awssdk:codegen")
public class SubTypeOne implements ToCopyableBuilder<SubTypeOne.Builder, SubTypeOne>, StructuredPojo {
    private final String subTypeOneMember;

    private SubTypeOne(BeanStyleBuilder builder) {
        this.subTypeOneMember = builder.subTypeOneMember;
    }

    /**
     *
     * @return
     */
    public String subTypeOneMember() {
        return subTypeOneMember;
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
        hashCode = 31 * hashCode + ((subTypeOneMember() == null) ? 0 : subTypeOneMember().hashCode());
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
        if (!(obj instanceof SubTypeOne)) {
            return false;
        }
        SubTypeOne other = (SubTypeOne) obj;
        if (other.subTypeOneMember() == null ^ this.subTypeOneMember() == null) {
            return false;
        }
        if (other.subTypeOneMember() != null && !other.subTypeOneMember().equals(this.subTypeOneMember())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (subTypeOneMember() != null) {
            sb.append("SubTypeOneMember: ").append(subTypeOneMember()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        SubTypeOneMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, SubTypeOne> {
        /**
         *
         * @param subTypeOneMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subTypeOneMember(String subTypeOneMember);
    }

    private static final class BeanStyleBuilder implements Builder {
        private String subTypeOneMember;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(SubTypeOne model) {
            setSubTypeOneMember(model.subTypeOneMember);
        }

        @Override
        public Builder subTypeOneMember(String subTypeOneMember) {
            this.subTypeOneMember = StringCopier.copyString(subTypeOneMember);
            return this;
        }

        /**
         *
         * @param subTypeOneMember
         */
        public void setSubTypeOneMember(String subTypeOneMember) {
            this.subTypeOneMember = StringCopier.copyString(subTypeOneMember);
        }

        @Override
        public SubTypeOne build() {
            return new SubTypeOne(this);
        }
    }
}

