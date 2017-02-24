package hacking.xspec.solution;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import hacking.xspec.HackerIntegrationTestBase;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.ItemCollection;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.ScanExpressionSpec;

public class Solution_ProjectionIntegrationTest extends HackerIntegrationTestBase {
    /**
     * Scans for hackers that have an id that begins with "067e6162-0", and with a start date that is between year 2010 and 2015.
     * Only returns the 4 attributes: "hacker_id", "start_yyyymmdd", "photo.image_file_name", "is_active".
     */
    @Test
    public void testProjectionExpression() {
        Table table = dynamo.getTable(HACKER_TABLE_NAME);
        ScanExpressionSpec xspec = new ExpressionSpecBuilder()
            .withCondition(
                S(HASH_KEY_HACKER_ID).beginsWith("067e6162-0")
                    .and(N(RANGE_KEY_START_DATE).between(20100000, 20150000))
        ).addProjections("hacker_id", "start_yyyymmdd", "photo.image_file_name", "is_active")
         .buildForScan();

        // begins_with(#0, :0) AND #1 BETWEEN :1 AND :2
        System.out.println(xspec.getFilterExpression());

        ItemCollection<?> col = table.scan(xspec);
        for (Item it: col) {
            System.out.println(it.toJSONPretty());
        }
    }
}
