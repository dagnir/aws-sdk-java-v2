package demo.xspec;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.parenthesize;

import org.junit.Assert;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.ItemCollection;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.ScanExpressionSpec;

public class ABC_ConditionIntegrationTest extends DemoIntegrationTestBase {


    // True or True And False => True
    @Test
    public void a_or_b_and_c() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
            .withString(HASH_KEY_NAME, "a_or_b_and_c")
            .withNumber(RANGE_KEY_NAME, 0)
            .withNumber("a", 1)
            .withNumber("b", 2)
            .withNumber("c", 3)
            ;
        table.putItem(item);
        ScanExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").ne(1).or(N("b").eq(2)).and(N("c").eq(3))
        ).buildForScan();
        ItemCollection<?> col = table.scan(xspec);
        int resultCount = 0;
        for (Item it: col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(1, resultCount);
    }

    // True or (True And False) => True
    @Test
    public void a_or_$b_and_c$() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
            .withString(HASH_KEY_NAME, "a_or_b_and_c")
            .withNumber(RANGE_KEY_NAME, 0)
            .withNumber("a", 1)
            .withNumber("b", 2)
            .withNumber("c", 3)
            ;
        table.putItem(item);
        ScanExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            N("a").eq(1).or(parenthesize(N("b").eq(2).and(N("c").ne(3))))
        ).buildForScan();
        ItemCollection<?> col = table.scan(xspec);
        int resultCount = 0;
        for (Item it: col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(1, resultCount);
    }

    // (True or True) And False => False
    @Test
    public void $a_or_b$_and_c() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
            .withString(HASH_KEY_NAME, "a_or_b_and_c")
            .withNumber(RANGE_KEY_NAME, 0)
            .withNumber("a", 1)
            .withNumber("b", 2)
            .withNumber("c", 3)
            ;
        table.putItem(item);
        ScanExpressionSpec xspec = new ExpressionSpecBuilder().withCondition(
            parenthesize(N("a").eq(1).or(N("b").eq(2))).and(N("c").ne(3))
        ).buildForScan();
        ItemCollection<?> col = table.scan(xspec);
        int resultCount = 0;
        for (Item it: col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(0, resultCount);
    }
}
