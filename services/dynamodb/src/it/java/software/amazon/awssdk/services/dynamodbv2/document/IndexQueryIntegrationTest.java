package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Test;

public class IndexQueryIntegrationTest extends IntegrationTestBase {
    @Test
    public void testItemIteration_Old() {
        Table table = dynamoOld.getTable(RANGE_TABLE_NAME);
        final String hashkeyval = "IndexQueryTest";
        Item item = new Item()
                .withString(HASH_KEY_NAME, hashkeyval)
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 1; i <= 10; i++) {
            item.withNumber(RANGE_KEY_NAME, i)
                    .withNumber(LSI_RANGE_KEY_NAME, 100 + i)
            ;
            PutItemOutcome out = table.putItem(item);
            System.out.println(out);
        }
        Index lsi = table.getIndex(LSI_NAME);
        ItemCollection<?> col = lsi.query(
                HASH_KEY_NAME, "IndexQueryTest",
                new RangeKeyCondition(LSI_RANGE_KEY_NAME).between(0, 10));
        int count = 0;
        for (Item it : col) {
            System.out.println(it);
            count++;
        }
        assertTrue(0, count);
        col = lsi.query(
                HASH_KEY_NAME, "IndexQueryTest",
                new RangeKeyCondition(LSI_RANGE_KEY_NAME).between(101, 110));
        for (Item it : col) {
            System.out.println(it);
            count++;
        }
        assertTrue(10, count);
    }
}
