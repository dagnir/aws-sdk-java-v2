package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class ListOfBlobsTypeCopier {
    static List<ByteBuffer> copy(Collection<ByteBuffer> listOfBlobsTypeParam) {
        if (listOfBlobsTypeParam == null) {
            return null;
        }
        List<ByteBuffer> listOfBlobsTypeParamCopy = new ArrayList<>(listOfBlobsTypeParam.size());
        for (ByteBuffer e : listOfBlobsTypeParam) {
            listOfBlobsTypeParamCopy.add(StandardMemberCopier.copy(e));
        }
        return Collections.unmodifiableList(listOfBlobsTypeParamCopy);
    }
}
