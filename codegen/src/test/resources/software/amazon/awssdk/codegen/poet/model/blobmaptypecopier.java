package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class BlobMapTypeCopier {
    static Map<String, ByteBuffer> copyBlobMapType(Map<String, ByteBuffer> blobMapTypeParam) {
        if (blobMapTypeParam == null) {
            return null;
        }
        Map<String, ByteBuffer> blobMapTypeParamCopy = new HashMap<>(blobMapTypeParam.size());
        for (Map.Entry<String, ByteBuffer> e : blobMapTypeParam.entrySet()) {
            blobMapTypeParamCopy.put(StringCopier.copyString(e.getKey()), ByteBufferCopier.copyByteBuffer(e.getValue()));
        }
        return blobMapTypeParamCopy;
    }
}

