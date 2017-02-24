package hacking.xspec;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodbv2.document.DynamoDB;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

public class HackerIntegrationTestBase {
    protected static DynamoDB dynamo;

    // Table names
    protected static final String GAME_TABLE_NAME = "Game";
    protected static final String HACKER_TABLE_NAME = "Hacker";
    // attribute names for the primary keys
    protected static final String GAME_ID = "GameId";
    protected static final String HASH_KEY_HACKER_ID = "hacker_id";
    protected static final String RANGE_KEY_START_DATE = "start_yyyymmdd";

    public static final String hacker_uuid_1 = "067e6162-3b6f-4ae2-a171-2470b63dff00";
    public static final String hacker_uuid_2 = "067e6162-0e9e-4471-a2f9-9af509fb5889";

    private static final ProvisionedThroughput THRUPUT = new ProvisionedThroughput(1L, 2L);

    @BeforeClass
    public static void setup() throws InterruptedException {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsTestCredentials());
        dynamo = new DynamoDB(client);

// Uncomment the following 3 lines to delete and then re-create the hacker table.
//        Table table = dynamo.getTable(HACKER_TABLE_NAME);
//        table.delete();
//        table.waitForDelete();

        createTable_rangeKey();
        createGameTable();
    }

    @AfterClass
    public static void tearDown() {
        dynamo.shutdown();
    }

    protected static AwsCredentials awsTestCredentials() {
        try {
            return new PropertiesCredentials(new File(
                    System.getProperty("user.home")
                            + "/.aws/awsTestAccount.properties"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createTable_rangeKey() throws InterruptedException {
        Table table = dynamo.getTable(HACKER_TABLE_NAME);
        TableDescription desc = table.waitForActiveOrDelete();
        if (desc == null) {
            // table doesn't exist; let's create it
            CreateTableRequest req = new CreateTableRequest()
            .withTableName(HACKER_TABLE_NAME)
            .withAttributeDefinitions(
                new AttributeDefinition(HASH_KEY_HACKER_ID, ScalarAttributeType.S),
                new AttributeDefinition(RANGE_KEY_START_DATE, ScalarAttributeType.N)
             )
            .withKeySchema(
                new KeySchemaElement(HASH_KEY_HACKER_ID, KeyType.HASH),
                new KeySchemaElement(RANGE_KEY_START_DATE, KeyType.RANGE))
            .withProvisionedThroughput(THRUPUT)
            ;
            dynamo.createTable(req);
            // waits until table becomes active
            table.waitForActive();
        }
    }

    private static void createGameTable() throws InterruptedException {
        Table table = dynamo.getTable(GAME_TABLE_NAME);
        TableDescription desc = table.waitForActiveOrDelete();
        if (desc == null) {
            // table doesn't exist; let's create it
            CreateTableRequest req = new CreateTableRequest()
            .withTableName(GAME_TABLE_NAME)
            .withAttributeDefinitions(
                new AttributeDefinition(GAME_ID, ScalarAttributeType.S)
             )
            .withKeySchema(
                new KeySchemaElement(GAME_ID, KeyType.HASH))
            .withProvisionedThroughput(THRUPUT)
            ;
            dynamo.createTable(req);
            // waits until table becomes active
            table.waitForActive();
        }
    }
}
