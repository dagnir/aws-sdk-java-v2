package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class StructWithNestedBlobTypeCopier {
    static StructWithNestedBlobType copyStructWithNestedBlobType(StructWithNestedBlobType structWithNestedBlobTypeParam) {
        if (structWithNestedBlobTypeParam == null) {
            return null;
        }
        return structWithNestedBlobTypeParam.toBuilder().build();
    }
}

