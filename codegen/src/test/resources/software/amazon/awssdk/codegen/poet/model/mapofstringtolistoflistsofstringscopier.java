package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToListOfListsOfStringsCopier {
    static Map<String, List<List<String>>> copy(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListsOfStringsParam) {
        if (mapOfStringToListOfListsOfStringsParam == null) {
            return null;
        }
        Map<String, List<List<String>>> mapOfStringToListOfListsOfStringsParamCopy = new HashMap<>(
                mapOfStringToListOfListsOfStringsParam.size());
        for (Map.Entry<String, ? extends Collection<? extends Collection<String>>> e : mapOfStringToListOfListsOfStringsParam
                .entrySet()) {
            mapOfStringToListOfListsOfStringsParamCopy.put(StandardMemberCopier.copy(e.getKey()),
                    ListOfListsOfStringsCopier.copy(e.getValue()));
        }
        return mapOfStringToListOfListsOfStringsParamCopy;
    }
}

