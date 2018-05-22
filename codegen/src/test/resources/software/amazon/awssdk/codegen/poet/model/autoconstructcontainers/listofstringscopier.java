package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructAwareList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfStringsCopier {
    static List<String> copy(Collection<String> listOfStringsParam) {
        if (listOfStringsParam == null) {
            return new DefaultSdkAutoConstructAwareList<>();
        }
        List<String> listOfStringsParamCopy = new DefaultSdkAutoConstructAwareList<>(listOfStringsParam);
        return Collections.unmodifiableList(listOfStringsParamCopy);
    }
}
