package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructAwareList;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveListTypeCopier {
    static List<RecursiveStructType> copy(Collection<RecursiveStructType> recursiveListTypeParam) {
        if (recursiveListTypeParam == null) {
            return new DefaultSdkAutoConstructAwareList<>();
        }
        List<RecursiveStructType> recursiveListTypeParamCopy = new DefaultSdkAutoConstructAwareList<>(recursiveListTypeParam);
        return Collections.unmodifiableList(recursiveListTypeParamCopy);
    }

    static List<RecursiveStructType> copyFromBuilder(Collection<? extends RecursiveStructType.Builder> recursiveListTypeParam) {
        if (recursiveListTypeParam == null) {
            return null;
        }
        return copy(recursiveListTypeParam.stream().map(RecursiveStructType.Builder::build).collect(toList()));
    }
}
