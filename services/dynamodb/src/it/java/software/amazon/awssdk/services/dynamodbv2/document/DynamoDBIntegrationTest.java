package software.amazon.awssdk.services.dynamodbv2.document;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

public class DynamoDBIntegrationTest extends IntegrationTestBase {

    @Test
    public void testGetHashTable() {
        DynamoDB[] ddbs = {dynamo, dynamoOld};
        for (DynamoDB ddb : ddbs) {
            Table table = ddb.getTable(HASH_ONLY_TABLE_NAME);
            assertNull(table.getDescription());
            TableDescription desc = table.describe();
            assertNotNull(desc);
            System.out.println(desc);
        }
    }

    @Test
    public void testGetRangeTable() {
        DynamoDB[] ddbs = {dynamo, dynamoOld};
        for (DynamoDB ddb : ddbs) {
            Table table = ddb.getTable(RANGE_TABLE_NAME);
            assertNull(table.getDescription());
            TableDescription desc = table.describe();
            assertNotNull(desc);
            System.out.println(desc);
        }
    }
}
