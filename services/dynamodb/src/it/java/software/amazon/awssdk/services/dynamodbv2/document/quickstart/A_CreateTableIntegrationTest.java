package software.amazon.awssdk.services.dynamodbv2.document.quickstart;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodbv2.model.Projection;
import software.amazon.awssdk.services.dynamodbv2.model.ProjectionType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

/**
 * Sample code to create a DynamoDB table.
 */
public class A_CreateTableIntegrationTest extends QuickStartIntegrationTestBase {
    private final ProvisionedThroughput THRUPUT = new ProvisionedThroughput(1L, 2L);
    private final Projection PROJECTION = new Projection().withProjectionType(ProjectionType.ALL);

    /**
     * Sample request to create a DynamoDB table with an LSI and GSI that
     * can be accessed via a combination of hash keys and range keys.
     */
    @Test
    public void howToCreateTable() throws InterruptedException {
        String TABLE_NAME = "myTableForMidLevelApi";
        Table table = dynamo.getTable(TABLE_NAME);
        // check if table already exists, and if so wait for it to become active
        TableDescription desc = table.waitForActiveOrDelete();
        if (desc != null) {
            System.out.println("Skip creating table which already exists and ready for use: "
                               + desc);
            return;
        }
        // Table doesn't exist.  Let's create it.
        table = dynamo.createTable(newCreateTableRequest(TABLE_NAME));
        // Wait for the table to become active 
        desc = table.waitForActive();
        System.out.println("Table is ready for use! " + desc);
    }

    private CreateTableRequest newCreateTableRequest(String tableName) {
        CreateTableRequest req = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(
                        new AttributeDefinition(HASH_KEY_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(RANGE_KEY_NAME, ScalarAttributeType.N),
                        new AttributeDefinition(LSI_RANGE_KEY_NAME, ScalarAttributeType.N),
                        new AttributeDefinition(GSI_HASH_KEY_NAME, ScalarAttributeType.S),
                        new AttributeDefinition(GSI_RANGE_KEY_NAME, ScalarAttributeType.N))
                .withKeySchema(
                        new KeySchemaElement(HASH_KEY_NAME, KeyType.HASH),
                        new KeySchemaElement(RANGE_KEY_NAME, KeyType.RANGE))
                .withProvisionedThroughput(THRUPUT)
                .withGlobalSecondaryIndexes(
                        new GlobalSecondaryIndex()
                                .withIndexName(RANGE_GSI_NAME)
                                .withKeySchema(
                                        new KeySchemaElement(GSI_HASH_KEY_NAME, KeyType.HASH),
                                        new KeySchemaElement(GSI_RANGE_KEY_NAME, KeyType.RANGE))
                                .withProjection(PROJECTION)
                                .withProvisionedThroughput(THRUPUT))
                .withLocalSecondaryIndexes(
                        new LocalSecondaryIndex()
                                .withIndexName(LSI_NAME)
                                .withKeySchema(
                                        new KeySchemaElement(HASH_KEY_NAME, KeyType.HASH),
                                        new KeySchemaElement(LSI_RANGE_KEY_NAME, KeyType.RANGE))
                                .withProjection(PROJECTION));
        return req;
    }
}
