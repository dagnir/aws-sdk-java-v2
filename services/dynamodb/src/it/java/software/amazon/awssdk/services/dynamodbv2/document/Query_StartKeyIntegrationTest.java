package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.spec.QuerySpec;

public class Query_StartKeyIntegrationTest extends IntegrationTestBase {

    @Test
    public void testExclusiveStartKey() {
        Table table = dynamoOld.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "allDataTypes")
                .withNumber(RANGE_KEY_NAME, 1)
                .withBinary("binary", new byte[]{1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        PutItemOutcome out = table.putItem(item);
        System.out.println(out);
        for (int i = 1; i < 11; i++) {
            out = table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }
        QuerySpec spec = new QuerySpec()
                .withHashKey(HASH_KEY_NAME, "allDataTypes")
                .withRangeKeyCondition(new RangeKeyCondition(RANGE_KEY_NAME).between(1, 10))
                .withMaxPageSize(3)
                .withExclusiveStartKey(
                        "hashkeyAttr", "allDataTypes",
                        "rangekeyAttr", 1);
        int count = 0;
        ItemCollection<?> col = table.query(spec);
        for (Item it : col) {
            count++;
            System.out.println(it);
        }
        assertTrue(9, count);
    }
}
