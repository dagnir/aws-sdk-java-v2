package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToStringCopier {
    static Map<String, String> copyMapOfStringToString(Map<String, String> mapOfStringToStringParam) {
        if (mapOfStringToStringParam == null) {
            return null;
        }
        Map<String, String> mapOfStringToStringParamCopy = new HashMap<>(mapOfStringToStringParam.size());
        for (Map.Entry<String, String> e : mapOfStringToStringParam.entrySet()) {
            mapOfStringToStringParamCopy.put(StringCopier.copyString(e.getKey()), StringCopier.copyString(e.getValue()));
        }
        return mapOfStringToStringParamCopy;
    }
}

