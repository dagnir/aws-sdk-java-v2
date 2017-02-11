package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

public class TableRecycleIntegrationTest extends IntegrationTestBase {
    @Test
    public void placeholder() {
    }

    // This test can be interrupted and then re-run later on and still work
    //    @Test
    public void testCreateDeleteTable() throws InterruptedException {
        final String tableName = "TableRecycleIntegrationTest-testCreateDeleteTable";
        final String hashKeyName = "mykey";

        for (int i = 0; i < 2; i++) {
            Table table = dynamo.getTable(tableName);
            TableDescription desc = table.waitForActiveOrDelete();
            System.err.println(i + ") Started with table: " + desc);
            if (desc == null) {
                // table not exist; let's create it
                table = dynamo.createTable(newCreateTableRequest(tableName, hashKeyName));
                desc = table.waitForActive();
                System.err.println("Created table: " + desc);
            } else {
                System.err.println("Existing table :" + desc);
            }
            // Table must be active at this stage, let's delete it
            table.delete();
            table.waitForDelete();  // blocks till the table is deleted
            Assert.assertNull(table.waitForActiveOrDelete());
        }
    }

    private CreateTableRequest newCreateTableRequest(String tableName, String hashKeyName) {
        return new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(new KeySchemaElement(hashKeyName, KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition(hashKeyName, ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
                ;
    }
}
