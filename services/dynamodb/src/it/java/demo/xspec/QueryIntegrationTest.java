package demo.xspec;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.BOOL;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;

import org.junit.Assert;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.ItemCollection;
import software.amazon.awssdk.services.dynamodbv2.document.RangeKeyCondition;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.QueryExpressionSpec;

public class QueryIntegrationTest extends DemoIntegrationTestBase {

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
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
            .addProjections("numberAttr", "stringAttr")
            .withCondition(
                BOOL("booleanTrue").eq(true)
                    .and(S("mapAttr.key1").eq("value1"))
        ).buildForQuery();
        ItemCollection<?> col = table.query(HASH_KEY_NAME, "allDataTypes",
                new RangeKeyCondition(RANGE_KEY_NAME).between(1, 10), xspec);
        int resultCount = 0;
        for (Item it: col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(10, resultCount);
    }
}
