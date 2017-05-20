package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Date;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class TimestampCopier {
    static Date copyDate(Date dateParam) {
        if (dateParam == null) {
            return null;
        }
        return new Date(dateParam.getTime());
    }
}

