package software.amazon.awssdk.codegen.poet.common.model;

import static software.amazon.awssdk.util.StringUtils.isNullOrEmpty;

import java.util.stream.Stream;
import javax.annotation.Generated;

/**
 * Some comment on the class itself
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public enum TestEnumClass {

    Available("available"),
    PermanentFailure("permanent-failure");

    private final String value;

    private TestEnumClass(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Use this in place of valueOf.
     *
     * @param value real value
     * @return TestEnumClass corresponding to the value
     */
    public static TestEnumClass fromValue(String value) {
        if (isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }
        return Stream.of(TestEnumClass.values())
                .filter(e -> e.toString().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot create enum from " + value + " value!"));
    }
}