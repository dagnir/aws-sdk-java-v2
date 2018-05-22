package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructAwareList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfStringsCopier {
    static List<List<String>> copy(Collection<? extends Collection<String>> listOfListOfStringsParam) {
        if (listOfListOfStringsParam == null) {
            return new DefaultSdkAutoConstructAwareList<>();
        }
        List<List<String>> listOfListOfStringsParamCopy = new DefaultSdkAutoConstructAwareList<>(listOfListOfStringsParam.size());
        for (Collection<String> e : listOfListOfStringsParam) {
            listOfListOfStringsParamCopy.add(ListOfStringsCopier.copy(e));
        }
        return Collections.unmodifiableList(listOfListOfStringsParamCopy);
    }
}
