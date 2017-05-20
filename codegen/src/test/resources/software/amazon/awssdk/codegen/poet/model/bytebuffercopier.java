package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ByteBufferCopier {
    static ByteBuffer copyByteBuffer(ByteBuffer byteBufferParam) {
        if (byteBufferParam == null) {
            return null;
        }
        return byteBufferParam.duplicate();
    }
}

