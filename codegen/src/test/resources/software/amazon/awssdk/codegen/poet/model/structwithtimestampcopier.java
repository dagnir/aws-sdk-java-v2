package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class StructWithTimestampCopier {
    static StructWithTimestamp copyStructWithTimestamp(StructWithTimestamp structWithTimestampParam) {
        if (structWithTimestampParam == null) {
            return null;
        }
        return structWithTimestampParam.toBuilder().build();
    }
}

