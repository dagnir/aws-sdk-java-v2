package hacking.xspec.solution;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.BOOL;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import hacking.xspec.HackerIntegrationTestBase;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.ItemCollection;
import software.amazon.awssdk.services.dynamodbv2.document.RangeKeyCondition;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.QueryExpressionSpec;

public class Solution_QueryIntegrationTest extends HackerIntegrationTestBase {

    @Test
    public void testFilterExpression() {
        Table table = dynamo.getTable(HACKER_TABLE_NAME);
        QueryExpressionSpec xspec = new ExpressionSpecBuilder()
            // Filter/Condition: is_active is true AND lines_of_code >= 1000
            .withCondition(
                BOOL("is_active").eq(true)
                    .and(N("lines_of_code").ge(1000)))
            .buildForQuery();

        // #0 = :0 AND #1 >= :1
        System.out.println(xspec.getFilterExpression());

        ItemCollection<?> col = table.query(HASH_KEY_HACKER_ID, hacker_uuid_1,
                new RangeKeyCondition(RANGE_KEY_START_DATE).between(20130000, 20150000), xspec);
        for (Item it: col) {
            System.out.println(it.toJSONPretty());
            // {"personal_blobs":["AAAA","AQEB"],"start_yyyymmdd":20140501,"end_yyyymmdd":null,"is_active":true,"photo":{"image_file":"AQID","image_file_name":"hacker1.png"},"lines_of_code":1000,"favorite_colors":["red","green","blue"],"favoriate_numbers":[2.1828,3.1415,0.5772],"hacker_id":"067e6162-3b6f-4ae2-a171-2470b63dff00","email_addresses":["hacker1@amazon.com","hacker1@lab126.com"]}
        }
    }
}
