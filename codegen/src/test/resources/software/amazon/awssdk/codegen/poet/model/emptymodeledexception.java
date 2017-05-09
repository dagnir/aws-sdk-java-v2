package software.amazon.awssdk.services.jsonprotocoltests.model;

public class EmptyModeledException extends JsonProtocolTestsException {
    private EmptyModeledException(BeanStyleBuilder builder) {
        super(builder.message);
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

    public interface Builder {
        Builder message(String message);

        EmptyModeledException build_();
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
        public EmptyModeledException build_() {
            return new EmptyModeledException(this);
        }
    }
}
