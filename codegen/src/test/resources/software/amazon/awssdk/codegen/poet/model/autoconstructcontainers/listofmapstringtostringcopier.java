package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructAwareList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfMapStringToStringCopier {
    static List<Map<String, String>> copy(Collection<Map<String, String>> listOfMapStringToStringParam) {
        if (listOfMapStringToStringParam == null) {
            return new DefaultSdkAutoConstructAwareList<>();
        }
        List<Map<String, String>> listOfMapStringToStringParamCopy = new DefaultSdkAutoConstructAwareList<>(
                listOfMapStringToStringParam.size());
        for (Map<String, String> e : listOfMapStringToStringParam) {
            listOfMapStringToStringParamCopy.add(MapOfStringToStringCopier.copy(e));
        }
        return Collections.unmodifiableList(listOfMapStringToStringParamCopy);
    }
}
