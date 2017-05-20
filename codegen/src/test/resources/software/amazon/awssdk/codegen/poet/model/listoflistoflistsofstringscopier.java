package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfListsOfStringsCopier {
    static List<List<List<String>>> copyListOfListOfListsOfStrings(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListsOfStringsParam) {
        if (listOfListOfListsOfStringsParam == null) {
            return null;
        }
        List<List<List<String>>> listOfListOfListsOfStringsParamCopy = new ArrayList<>(listOfListOfListsOfStringsParam.size());
        for (Collection<? extends Collection<String>> e : listOfListOfListsOfStringsParam) {
            listOfListOfListsOfStringsParamCopy.add(ListOfListsOfStringsCopier.copyListOfListsOfStrings(e));
        }
        return listOfListOfListsOfStringsParamCopy;
    }
}

