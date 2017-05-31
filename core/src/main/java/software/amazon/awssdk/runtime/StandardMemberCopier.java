package software.amazon.awssdk.runtime;

import software.amazon.awssdk.annotation.SdkInternalApi;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Used in combination with the generated member copiers to implement deep
 * copies of shape members.
 */
@SdkInternalApi
public final class StandardMemberCopier {
    public static String copy(String s) {
        return s;
    }

    public static Short copy(Short s) {
        return s;
    }

    public static Integer copy(Integer i) {
        return i;
    }

    public static Long copy(Long l) {
        return l;
    }

    public static Float copy(Float f) {
        return f;
    }

    public static Double copy(Double d) {
        return d;
    }

    public static BigDecimal copy(BigDecimal bd) {
        return bd;
    }

    public static Boolean copy(Boolean b) {
        return b;
    }

    public static InputStream copy(InputStream is) {
        return is;
    }

    public static Date copy(Date d) {
        if (d == null) {
            return null;
        }
        return new Date(d.getTime());
    }

    public static ByteBuffer copy(ByteBuffer bb) {
        if (bb == null) {
            return null;
        }
        return bb.duplicate();
    }
}
