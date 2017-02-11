package software.amazon.awssdk.services.dynamodbv2.document.quickstart;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

/**
 * Sample code to delete a DynamoDB table.
 */
public class Z_DeleteTableIntegrationTest extends QuickStartIntegrationTestBase {
    @Test
    public void placholder() {
    }

    @Test
    public void howToDeleteTable() throws InterruptedException {
        String TABLE_NAME = "myTableForMidLevelApi";
        Table table = dynamo.getTable(TABLE_NAME);
        // Wait for the table to become active or deleted
        TableDescription desc = table.waitForActiveOrDelete();
        if (desc == null) {
            System.out.println("Table " + table.getTableName()
                               + " does not exist.");
        } else {
            table.delete();
            // No need to wait, but you could
            table.waitForDelete();
            System.out.println("Table " + table.getTableName()
                               + " has been deleted");
        }
    }

}
