package software.amazon.awssdk.services.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.Stream;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

/**
 * Created by dongie on 5/15/17.
 */
public class ReflectHelper {
    public static <T> void setObjectMember(Object o, String memberName, T value) {
        Arrays.stream(o.getClass().getDeclaredFields())
                .filter(f -> f.getName().equals(memberName))
                .findFirst()
                .ifPresent(f -> {
                    f.setAccessible(true);
                    try {
                        f.set(o, value);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Unable to reflectively set member " + memberName);
                    }
                });
    }
}
