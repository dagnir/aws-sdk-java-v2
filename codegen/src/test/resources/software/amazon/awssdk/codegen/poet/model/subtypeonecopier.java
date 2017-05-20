package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class SubTypeOneCopier {
    static SubTypeOne copySubTypeOne(SubTypeOne subTypeOneParam) {
        if (subTypeOneParam == null) {
            return null;
        }
        return subTypeOneParam.toBuilder().build();
    }
}

