package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.spec.ScanSpec;
import software.amazon.awssdk.services.dynamodbv2.document.utils.NameMap;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;

public class ScanIntegrationTest extends IntegrationTestBase {

    @Test
    public void testItemIteration_New() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "allDataTypes")
                .withBinary("binary", new byte[]{1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withBoolean("booleanTrue", true)
                .withBoolean("booleanFalse", false)
                .withInt("intAttr", 1234)
                .withList("listAtr", "abc", "123")
                .withMap("mapAttr",
                         new ValueMap()
                                 .withString("key1", "value1")
                                 .withInt("key2", 999))
                .withNull("nullAttr")
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }

        ItemCollection<?> col = table.scan(
                new ScanFilter(HASH_KEY_NAME).eq("allDataTypes"),
                new ScanFilter(RANGE_KEY_NAME).between(1, 10)
        );
        int resultCount = 0;
        for (Item it : col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(10, resultCount);
    }

    @Test
    public void testItemIteration_Old() {
        Table table = dynamoOld.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "allDataTypes")
                .withBinary("binary", new byte[]{1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }

        ItemCollection<?> col = table.scan(
                new ScanFilter(HASH_KEY_NAME).eq("allDataTypes"),
                new ScanFilter(RANGE_KEY_NAME).between(1, 10));
        int resultCount = 0;
        for (Item it : col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(10, resultCount);
    }

    @Test
    public void testZeroIteration() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "allDataTypes")
                .withBinary("binary", new byte[]{1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 1; i < 11; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }

        ScanSpec spec = new ScanSpec()
                .withScanFilters(
                        new ScanFilter(HASH_KEY_NAME).eq("allDataTypes"),
                        new ScanFilter(RANGE_KEY_NAME).between(1, 10))
                .withMaxPageSize(3);
        int count = 0;
        ItemCollection<?> col = table.scan(
                spec.withMaxResultSize(0));
        for (Item it : col) {
            count++;
            System.out.println(it);
        }
        assertTrue(0, count);
        count = 0;
        int countPage = 0;
        for (Page<Item, ?> page : col.pages()) {
            countPage++;
            for (Item it : page) {
                count++;
                System.out.println(it);
            }
        }
        assertTrue(0, countPage);
        assertTrue(0, count);
    }

    @Test
    public void testFilterExpression() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "allDataTypes")
                .withBinary("binary", new byte[]{1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withBoolean("booleanTrue", true)
                .withBoolean("booleanFalse", false)
                .withInt("intAttr", 1234)
                .withList("listAtr", "abc", "123")
                .withMap("mapAttr",
                         new ValueMap()
                                 .withString("key1", "value1")
                                 .withInt("key2", 999))
                .withNull("nullAttr")
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }

        ItemCollection<?> col = table.scan(
                "(#hk = :hashkeyAttrValue) AND (#rk BETWEEN :lo AND :hi)",
                new NameMap()
                        .with("#hk", HASH_KEY_NAME)
                        .with("#rk", RANGE_KEY_NAME),
                new ValueMap()
                        .withString(":hashkeyAttrValue", "allDataTypes")
                        .withInt(":lo", 1)
                        .withInt(":hi", 10)
        );
        int resultCount = 0;
        for (Item it : col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(10, resultCount);
    }

    @Test
    public void testProjectionExpression() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "allDataTypes")
                .withBinary("binary", new byte[]{1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withBoolean("booleanTrue", true)
                .withBoolean("booleanFalse", false)
                .withInt("intAttr", 1234)
                .withList("listAtr", "abc", "123")
                .withMap("mapAttr",
                         new ValueMap()
                                 .withString("key1", "value1")
                                 .withInt("key2", 999))
                .withNull("nullAttr")
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }

        ItemCollection<?> col = table.scan(
                "(#hk = :hashkeyAttrValue) AND (#rk BETWEEN :lo AND :hi)",
                // Only return the set/list/map attributes
                "binarySet, listAtr, mapAttr, stringSetAttr",
                new NameMap()
                        .with("#hk", HASH_KEY_NAME)
                        .with("#rk", RANGE_KEY_NAME),
                new ValueMap()
                        .withString(":hashkeyAttrValue", "allDataTypes")
                        .withInt(":lo", 1)
                        .withInt(":hi", 10)
        );
        int resultCount = 0;
        for (Item it : col) {
            resultCount++;
            System.out.println(it);

            Assert.assertNull(it.get(HASH_KEY_NAME));
            Assert.assertNull(it.get("binary"));
            Assert.assertNull(it.get("booleanTrue"));
            Assert.assertNull(it.get("booleanFalse"));
            Assert.assertNull(it.get("intAttr"));
            Assert.assertNull(it.get("nullAttr"));
            Assert.assertNull(it.get("numberAttr"));
            Assert.assertNull(it.get("stringAttr"));

            Assert.assertNotNull(it.get("binarySet"));
            Assert.assertNotNull(it.get("listAtr"));
            Assert.assertNotNull(it.get("mapAttr"));
            Assert.assertNotNull(it.get("stringSetAttr"));
        }
        Assert.assertEquals(10, resultCount);
    }
}
