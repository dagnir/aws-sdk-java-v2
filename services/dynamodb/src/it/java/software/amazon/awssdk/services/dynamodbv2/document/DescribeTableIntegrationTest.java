package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.quickstart.QuickStartIntegrationTestBase;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

public class DescribeTableIntegrationTest extends QuickStartIntegrationTestBase {
    @Test
    public void test() throws InterruptedException {
        String TABLE_NAME = "myTableForMidLevelApi";
        Table table = dynamo.getTable(TABLE_NAME);
        TableDescription desc = table.describe();
        System.out.println("Table is ready for use! " + desc);
    }
}
