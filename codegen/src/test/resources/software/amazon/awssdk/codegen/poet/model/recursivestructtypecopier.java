package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveStructTypeCopier {
    static RecursiveStructType copyRecursiveStructType(RecursiveStructType recursiveStructTypeParam) {
        if (recursiveStructTypeParam == null) {
            return null;
        }
        return recursiveStructTypeParam.toBuilder().build();
    }
}

