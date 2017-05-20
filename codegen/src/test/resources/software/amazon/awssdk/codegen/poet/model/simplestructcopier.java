package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class SimpleStructCopier {
    static SimpleStruct copySimpleStruct(SimpleStruct simpleStructParam) {
        if (simpleStructParam == null) {
            return null;
        }
        return simpleStructParam.toBuilder().build();
    }
}

