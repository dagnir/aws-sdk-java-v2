package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.StructWithNestedBlobTypeMarshaller;

public class StructWithNestedBlobType implements Cloneable, StructuredPojo {
    private final ByteBuffer nestedBlob;

    private StructWithNestedBlobType(BeanStyleBuilder builder) {
        this.nestedBlob = builder.nestedBlob;
    }

    /**
     *
     * <p>
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}.
     * </p>
     *
     * @return
     */
    public ByteBuffer nestedBlob() {
        return nestedBlob;
    }

    public Builder toBuilder() {
        return new BeanStyleBuilder(this);
    }

    public static Builder builder_() {
        return new BeanStyleBuilder();
    }

    public static Class<? extends Builder> beanStyleBuilderClass() {
        return BeanStyleBuilder.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + ((nestedBlob() == null) ? 0 : nestedBlob().hashCode());
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
        if (other.nestedBlob() == null ^ this.nestedBlob() == null) {
            return false;
        }
        if (other.nestedBlob() != null && !other.nestedBlob().equals(this.nestedBlob())) {
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
        if (nestedBlob() != null) {
            sb.append("NestedBlob: ").append(nestedBlob()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        StructWithNestedBlobTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder {
        /**
         *
         * @param nestedBlob
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedBlob(ByteBuffer nestedBlob);

        StructWithNestedBlobType build_();
    }

    private static final class BeanStyleBuilder implements Builder {
        private ByteBuffer nestedBlob;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(StructWithNestedBlobType model) {
            this.nestedBlob = model.nestedBlob;
        }

        @Override
        public Builder nestedBlob(ByteBuffer nestedBlob) {
            this.nestedBlob = nestedBlob;
            return this;
        }

        /**
         * <p>
         * AWS SDK for Java performs a Base64 encoding on this field before sending this request to AWS service by
         * default. Users of the SDK should not perform Base64 encoding on this field.
         * </p>
         * <p>
         * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer
         * will be seen by all objects that have a reference to this object. It is recommended to call
         * ByteBuffer.duplicate() or ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This
         * behavior will be changed in a future major version of the SDK.
         * </p>
         *
         * @param nestedBlob
         */
        public void setNestedBlob(ByteBuffer nestedBlob) {
            this.nestedBlob = nestedBlob;
        }

        @Override
        public StructWithNestedBlobType build_() {
            return new StructWithNestedBlobType(this);
        }
    }
}
