package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class BaseTypeCopier {
    static BaseType copyBaseType(BaseType baseTypeParam) {
        if (baseTypeParam == null) {
            return null;
        }
        return baseTypeParam.toBuilder().build();
    }
}

