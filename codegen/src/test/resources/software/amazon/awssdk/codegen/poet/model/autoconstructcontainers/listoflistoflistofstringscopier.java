package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructAwareList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfListOfStringsCopier {
    static List<List<List<String>>> copy(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStringsParam) {
        if (listOfListOfListOfStringsParam == null) {
            return new DefaultSdkAutoConstructAwareList<>();
        }
        List<List<List<String>>> listOfListOfListOfStringsParamCopy = new DefaultSdkAutoConstructAwareList<>(
                listOfListOfListOfStringsParam.size());
        for (Collection<? extends Collection<String>> e : listOfListOfListOfStringsParam) {
            listOfListOfListOfStringsParamCopy.add(ListOfListOfStringsCopier.copy(e));
        }
        return Collections.unmodifiableList(listOfListOfListOfStringsParamCopy);
    }
}
