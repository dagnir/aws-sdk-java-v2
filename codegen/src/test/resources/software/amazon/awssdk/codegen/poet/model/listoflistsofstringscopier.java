package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListsOfStringsCopier {
    static List<List<String>> copyListOfListsOfStrings(Collection<? extends Collection<String>> listOfListsOfStringsParam) {
        if (listOfListsOfStringsParam == null) {
            return null;
        }
        List<List<String>> listOfListsOfStringsParamCopy = new ArrayList<>(listOfListsOfStringsParam.size());
        for (Collection<String> e : listOfListsOfStringsParam) {
            listOfListsOfStringsParamCopy.add(ListOfStringsCopier.copyListOfStrings(e));
        }
        return listOfListsOfStringsParamCopy;
    }
}

