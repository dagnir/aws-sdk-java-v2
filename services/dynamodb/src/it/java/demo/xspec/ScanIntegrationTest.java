package demo.xspec;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.BS;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.NS;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.SS;

import org.junit.Assert;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.ItemCollection;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.ScanExpressionSpec;

public class ScanIntegrationTest extends DemoIntegrationTestBase {


    @Test
    public void testFilterExpression() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
            .withString(HASH_KEY_NAME, "allDataTypes")
            .withBinary("binary", new byte[]{1,2,3,4})
            .withBinarySet("binarySet", new byte[]{5,6}, new byte[]{7,8})
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
            .withStringSet("stringSetAttr", "da","di","foo","bar","bazz")
            ;
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }
        ScanExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            S(HASH_KEY_NAME).eq("allDataTypes")
                .and(N(RANGE_KEY_NAME).between(1, 10))
        ).buildForScan();
        ItemCollection<?> col = table.scan(xspec);

//        ItemCollection<?> col = table.scan(
//                "(#hk = :hashkeyAttrValue) AND (#rk BETWEEN :lo AND :hi)",
//                new NameMap()
//                    .with("#hk", HASH_KEY_NAME)
//                    .with("#rk", RANGE_KEY_NAME),
//                new ValueMap()
//                    .withString(":hashkeyAttrValue", "allDataTypes")
//                    .withInt(":lo", 1)
//                    .withInt(":hi", 10)
//        );
        int resultCount = 0;
        for (Item it: col) {
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
            .withBinary("binary", new byte[]{1,2,3,4})
            .withBinarySet("binarySet", new byte[]{5,6}, new byte[]{7,8})
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
            .withStringSet("stringSetAttr", "da","di","foo","bar","bazz")
            ;
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }
        ScanExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            S(HASH_KEY_NAME).eq("allDataTypes")
                .and(N(RANGE_KEY_NAME).between(1, 10))
        ).addProjections("binarySet", "listAtr", "mapAttr", "stringSetAttr")
         .buildForScan();
        ItemCollection<?> col = table.scan(xspec);

//        ItemCollection<?> col = table.scan(
//                "(#hk = :hashkeyAttrValue) AND (#rk BETWEEN :lo AND :hi)",
//                // Only return the set/list/map attributes
//                "binarySet, listAtr, mapAttr, stringSetAttr",
//                new NameMap()
//                    .with("#hk", HASH_KEY_NAME)
//                    .with("#rk", RANGE_KEY_NAME),
//                new ValueMap()
//                    .withString(":hashkeyAttrValue", "allDataTypes")
//                    .withInt(":lo", 1)
//                    .withInt(":hi", 10)
//        );
        int resultCount = 0;
        for (Item it: col) {
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

    @Test
    public void testContains() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
            .withString(HASH_KEY_NAME, "allDataTypes")
            .withBinary("binary", new byte[]{1,2,3,4})
            .withBinarySet("binarySet", new byte[]{5,6}, new byte[]{7,8})
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
            .withStringSet("stringSetAttr", "da","di","foo","bar","bazz")
            ;
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }
        ScanExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            S(HASH_KEY_NAME).contains("DataTypes").and(SS("stringSetAttr").contains("foo")).or(NS("NS").contains(123).or(BS("BS").contains(new byte[]{0})))
        ).addProjections(HASH_KEY_NAME, "binarySet", "listAtr", "mapAttr", "stringSetAttr")
         .buildForScan();
        ItemCollection<?> col = table.scan(xspec);

        int resultCount = 0;
        for (Item it: col) {
            resultCount++;
            System.out.println(it);

            Assert.assertNotNull(it.get(HASH_KEY_NAME));

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
        Assert.assertEquals(15, resultCount);
    }
}
