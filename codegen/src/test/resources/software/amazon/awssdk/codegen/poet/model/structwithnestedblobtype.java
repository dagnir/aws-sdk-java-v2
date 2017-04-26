package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.transform.StructWithNestedBlobTypeMarshaller;

public class StructWithNestedBlobType implements  Cloneable, Serializable, StructuredPojo {
    private ByteBuffer nestedBlob;

    private StructWithNestedBlobType(Builder builder) {
        this.nestedBlob = builder.nestedBlob;
    }

    /**
     *
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}. </p>
     * 
     * @return
     */
    public ByteBuffer getNestedBlob() {
        return nestedBlob;
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
        hashCode = 31 * hashCode + ((getNestedBlob() == null) ? 0 : getNestedBlob().hashCode());
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
        if (!(obj instanceof StructWithNestedBlobType)) {
            return false;
        }
        StructWithNestedBlobType other = (StructWithNestedBlobType) obj;
        if (other.getNestedBlob() == null ^ this.getNestedBlob() == null) {
            return false;
        }
        if (other.getNestedBlob() != null && !other.getNestedBlob().equals(this.getNestedBlob())) {
            return false;
        }
        return true;
    }

    @Override
    public StructWithNestedBlobType clone() {
        try {
            return (StructWithNestedBlobType) super.clone();
        } catch (Exception e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!",
                    e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getNestedBlob() != null) {
            sb.append("NestedBlob: ").append(getNestedBlob()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        StructWithNestedBlobTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public static class Builder {
        private ByteBuffer nestedBlob;

        private Builder() {
        }

        private Builder(StructWithNestedBlobType model) {
            this.nestedBlob = model.nestedBlob;
        }

        /**
         *
         * @param nestedBlob
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder setNestedBlob(ByteBuffer nestedBlob) {
            this.nestedBlob = nestedBlob;
            return this;
        }

        public StructWithNestedBlobType build_() {
            return new StructWithNestedBlobType(this);
        }
    }
}
