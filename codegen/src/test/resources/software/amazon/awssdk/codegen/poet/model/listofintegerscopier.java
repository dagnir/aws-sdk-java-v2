package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfIntegersCopier {
    static List<Integer> copyListOfIntegers(Collection<Integer> listOfIntegersParam) {
        if (listOfIntegersParam == null) {
            return null;
        }
        List<Integer> listOfIntegersParamCopy = new ArrayList<>(listOfIntegersParam.size());
        for (Integer e : listOfIntegersParam) {
            listOfIntegersParamCopy.add(IntegerCopier.copyInteger(e));
        }
        return listOfIntegersParamCopy;
    }
}

