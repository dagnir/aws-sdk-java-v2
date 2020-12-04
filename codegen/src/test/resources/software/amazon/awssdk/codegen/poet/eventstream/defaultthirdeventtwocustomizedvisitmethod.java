package software.amazon.awssdk.services.json.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A specialization of {@code software.amazon.awssdk.services.json.model.EventTwo} that represents the
 * {@code EventStream$thirdEventTwoCustomizedVisitMethod} event. Do not use this class directly. Instead, use the static
 * builder methods on {@link software.amazon.awssdk.services.json.model.EventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class EventTwo extends EventTwo {
    private static final long serialVersionUID = 1L;

    EventTwo(BuilderImpl builderImpl) {
        super(builderImpl);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public void accept(EventStreamOperationResponseHandler.Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(EventStreamOperationWithOnlyOutputResponseHandler.Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public EventStream.EventType sdkEventType() {
        return EventStream.EventType.THIRD_EVENT_TWO_CUSTOMIZED_VISIT_METHOD;
    }

    public interface Builder extends Builder {
        @Override
        EventTwo build();
    }

    private static final class BuilderImpl extends BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(EventTwo event) {
            super(event);
        }

        @Override
        public EventTwo build() {
            return new EventTwo(this);
        }
    }
}

