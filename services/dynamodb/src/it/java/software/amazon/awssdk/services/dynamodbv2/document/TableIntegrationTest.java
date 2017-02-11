package software.amazon.awssdk.services.dynamodbv2.document;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

public class TableIntegrationTest extends IntegrationTestBase {
    private final ProvisionedThroughput THRUPUT = new ProvisionedThroughput(1L, 2L);
    private final ProvisionedThroughput THRUPUT2 = new ProvisionedThroughput(2L, 2L);

    //    @Test
    public void testCreate_Wait_Delete() throws InterruptedException {
        Table table = dynamo.createTable(new CreateTableRequest()
                                                 .withTableName("TableTest-" + UUID.randomUUID().toString())
                                                 .withAttributeDefinitions(
                                                         new AttributeDefinition(HASH_KEY_NAME, ScalarAttributeType.S))
                                                 .withKeySchema(new KeySchemaElement(HASH_KEY_NAME, KeyType.HASH))
                                                 .withProvisionedThroughput(THRUPUT));
        TableDescription desc = table.waitForActive();
        System.out.println(desc);
        Assert.assertSame(desc, table.getDescription());
        table.delete();
        try {
            table.waitForActive();
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            // Waiting on a table being deleted doesn't make sense
        }
    }

    // Waiting on an already active table should return instantly.
    @Test
    public void testWaitOnActiveTable() throws InterruptedException {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        long start = System.nanoTime();
        table.waitForActive();
        long end = System.nanoTime();
        Assert.assertTrue(TimeUnit.NANOSECONDS.toSeconds(end - start) < 3);
    }

    @Test
    public void testUpdateTable() throws InterruptedException {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        table.waitForActive();
        try {
            table.updateTable(THRUPUT2);
            table.waitForActive();
        } catch (AmazonServiceException ex) {
            if (ex.getMessage().contains("requested value equals the current value")) {
                table.updateTable(THRUPUT);
                table.waitForActive();
            } else {
                throw ex;
            }
        }
    }
}
