package hacking.xspec.solution;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static org.junit.Assert.assertEquals;
import hacking.xspec.HackerIntegrationTestBase;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.ItemCollection;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.ScanExpressionSpec;

public class Solution_ScanIntegrationTest extends HackerIntegrationTestBase {

    // True or True And False => True
    @Test
    public void a_or_b_and_c() {
        Table table = dynamo.getTable(HACKER_TABLE_NAME);
        Item item = new Item()
            .withString(HASH_KEY_HACKER_ID, "a_or_b_and_c")
            .withNumber(RANGE_KEY_START_DATE, 0)
            .withNumber("a", 1)
            .withNumber("b", 2)
            .withNumber("c", 3)
            ;
        table.putItem(item);

        ScanExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").eq(1).or( N("b").eq(2).and(N("c").ne(3)) )
        ).buildForScan();

        // #0 = :0 OR #1 = :1 AND #2 <> :2
        assertEquals("#0 = :0 OR #1 = :1 AND #2 <> :2", xspec.getFilterExpression());

        ItemCollection<?> col = table.scan(xspec);
        int resultCount = 0;
        for (Item it: col) {
            resultCount++;
            System.out.println(it.toJSONPretty());
        }
        assertEquals(1, resultCount);
    }
}
