package software.amazon.awssdk.services.dynamodb;

import software.amazon.awssdk.services.dynamodb.document.Attribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Created by dongie on 5/15/17.
 */
public class Test {
    public static void main(String[] args) {
        AttributeValue v = AttributeValue.builder_().build_();

        ReflectHelper.setObjectMember(v, "s", "foobar");

        System.out.println(v.s());
    }
}
