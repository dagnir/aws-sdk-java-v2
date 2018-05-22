package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructAwareList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfIntegersCopier {
    static List<Integer> copy(Collection<Integer> listOfIntegersParam) {
        if (listOfIntegersParam == null) {
            return new DefaultSdkAutoConstructAwareList<>();
        }
        List<Integer> listOfIntegersParamCopy = new DefaultSdkAutoConstructAwareList<>(listOfIntegersParam);
        return Collections.unmodifiableList(listOfIntegersParamCopy);
    }
}
