package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.builder.CopyableBuilder;
import software.amazon.awssdk.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class EmptyModeledException extends JsonProtocolTestsException implements
        ToCopyableBuilder<EmptyModeledException.Builder, EmptyModeledException> {
    private EmptyModeledException(BeanStyleBuilder builder) {
        super(builder.message);
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

    public interface Builder extends CopyableBuilder<Builder, EmptyModeledException> {
        Builder message(String message);
    }

    private static final class BeanStyleBuilder implements Builder {
        private String message;

        private BeanStyleBuilder() {
        }

        private BeanStyleBuilder(EmptyModeledException model) {
            this.message = model.getMessage();
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public EmptyModeledException build() {
            return new EmptyModeledException(this);
        }
    }
}

