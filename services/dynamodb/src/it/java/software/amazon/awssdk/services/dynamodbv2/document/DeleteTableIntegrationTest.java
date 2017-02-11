package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Test;

public class DeleteTableIntegrationTest extends IntegrationTestBase {
    @Test
    public void placeholder() {
    }

    //    @Test
    public void testDeleteHashTable() {
        DynamoDB[] ddbs = {dynamo, dynamoOld};
        for (DynamoDB ddb : ddbs) {
            Table table = ddb.getTable(HASH_ONLY_TABLE_NAME);
            System.out.println(table.delete());
        }
    }

    //    @Test
    public void testDeleteRangeTable() {
        DynamoDB[] ddbs = {dynamo, dynamoOld};
        for (DynamoDB ddb : ddbs) {
            Table table = ddb.getTable(RANGE_TABLE_NAME);
            System.out.println(table.delete());
        }
    }
}
