package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.SubTypeOneMarshaller;

public class SubTypeOne implements Cloneable, StructuredPojo, Serializable {
    private String subTypeOneMember;

    private SubTypeOne(Builder builder) {
        this.subTypeOneMember = builder.subTypeOneMember;
    }

    /**
     * turn
     */
    public String getSubTypeOneMember() {
        return subTypeOneMember;
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
        hashCode = 31 * hashCode + ((getSubTypeOneMember() == null) ? 0 : getSubTypeOneMember().hashCode());
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
        if (other.getSubTypeOneMember() == null ^ this.getSubTypeOneMember() == null) {
            return false;
        }
        if (other.getSubTypeOneMember() != null && !other.getSubTypeOneMember().equals(this.getSubTypeOneMember())) {
            return false;
        }
        return true;
    }

    @Override
    public SubTypeOne clone() {
        try {
            return (SubTypeOne) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getSubTypeOneMember() != null) {
            sb.append("SubTypeOneMember: ").append(getSubTypeOneMember()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        SubTypeOneMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public static class Builder {
        private String subTypeOneMember;

        private Builder() {
        }

        private Builder(SubTypeOne model) {
            this.subTypeOneMember = model.subTypeOneMember;
        }

        /**
         *
         * @param subTypeOneMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setSubTypeOneMember(String subTypeOneMember) {
            this.subTypeOneMember = subTypeOneMember;
            return this;
        }

        public SubTypeOne build_() {
            return new SubTypeOne(this);
        }
    }
}
