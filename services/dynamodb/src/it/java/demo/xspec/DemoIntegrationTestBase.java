package demo.xspec;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodbv2.document.DynamoDB;
import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.document.utils.NameMap;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;
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

public class DemoIntegrationTestBase {
    protected static DynamoDB dynamo;

    // Table names
    protected static final String HASH_ONLY_TABLE_NAME = "DynamoDBTest_PrimaryHashKeyOnly";
    protected static final String RANGE_TABLE_NAME = "DynamoDBTest_PrimaryHashAndRangeKey";
    // Index names
    protected static final String HASH_ONLY_GSI_NAME = "HashOnlyGSI";
    protected static final String RANGE_GSI_NAME = "RangeGSI";
    protected static final String LSI_NAME = "LSI"; // LSI must involve a range key
    // attribute names for the primary keys
    protected static final String HASH_KEY_NAME = "hashkeyAttr";
    protected static final String RANGE_KEY_NAME = "rangekeyAttr";
    // attribute names for the GSI keys
    protected static final String GSI_HASH_KEY_NAME = "gsiHashkeyAttr";
    protected static final String GSI_RANGE_KEY_NAME = "gsiRangekeyAttr";
    // attribute names for the LSI keys (Note an LSI must share the same hash key as the table.)
    protected static final String LSI_RANGE_KEY_NAME = "lsiRangekeyAttr";

    private static final ProvisionedThroughput THRUPUT = new ProvisionedThroughput(1L, 2L);
    private static final Projection PROJECTION = new Projection().withProjectionType(ProjectionType.ALL);

    @BeforeClass
    public static void setup() throws InterruptedException {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsTestCredentials());
        dynamo = new DynamoDB(client);
        createTable_hashKeyOnly();
        createTable_rangeKey();
        setUpExamples();
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

    // Creates a hashkey only table with 2 GSI,
    // one hashkey only, the other hashkey and rangekey
    private static void createTable_hashKeyOnly() throws InterruptedException {
        DynamoDB[] ddbs = {dynamo};
        for (DynamoDB ddb: ddbs) {
            Table table = ddb.getTable(HASH_ONLY_TABLE_NAME);
            TableDescription desc = table.waitForActiveOrDelete();
            if (desc == null) {
                // table doesn't exist; let's create it
                ddb.createTable(new CreateTableRequest()
                .withTableName(HASH_ONLY_TABLE_NAME)
                .withAttributeDefinitions(
                    new AttributeDefinition(HASH_KEY_NAME, ScalarAttributeType.S),
                    new AttributeDefinition(GSI_HASH_KEY_NAME, ScalarAttributeType.S),
                    new AttributeDefinition(GSI_RANGE_KEY_NAME, ScalarAttributeType.N)
                 )
                .withKeySchema(new KeySchemaElement(HASH_KEY_NAME, KeyType.HASH))
                .withGlobalSecondaryIndexes(
                    new GlobalSecondaryIndex().withIndexName(HASH_ONLY_GSI_NAME)
                        .withKeySchema(new KeySchemaElement(GSI_HASH_KEY_NAME, KeyType.HASH))
                        .withProjection(PROJECTION)
                        .withProvisionedThroughput(THRUPUT),
                    new GlobalSecondaryIndex().withIndexName(RANGE_GSI_NAME)
                        .withKeySchema(
                            new KeySchemaElement(GSI_HASH_KEY_NAME, KeyType.HASH),
                            new KeySchemaElement(GSI_RANGE_KEY_NAME, KeyType.RANGE))
                        .withProjection(PROJECTION)
                        .withProvisionedThroughput(THRUPUT)
                ).withProvisionedThroughput(THRUPUT));
                // waits until table becomes active
                table.waitForActive();
            }
        }
    }

    // Creates a (hashkey + rangekey) table with 2 GSI,
    // (one hashkey only, the other hashkey and rangekey)
    // and an LSI (which must have a rangekey and share the hashkey with the table)
    private static void createTable_rangeKey() throws InterruptedException {
        DynamoDB[] ddbs = {dynamo};
        for (DynamoDB ddb: ddbs) {
            Table table = ddb.getTable(RANGE_TABLE_NAME);
            TableDescription desc = table.waitForActiveOrDelete();
            if (desc == null) {
                // table doesn't exist; let's create it
                CreateTableRequest req = new CreateTableRequest()
                .withTableName(RANGE_TABLE_NAME)
                .withAttributeDefinitions(
                    new AttributeDefinition(HASH_KEY_NAME, ScalarAttributeType.S),
                    new AttributeDefinition(RANGE_KEY_NAME, ScalarAttributeType.N),
                    new AttributeDefinition(LSI_RANGE_KEY_NAME, ScalarAttributeType.N),
                    new AttributeDefinition(GSI_HASH_KEY_NAME, ScalarAttributeType.S),
                    new AttributeDefinition(GSI_RANGE_KEY_NAME, ScalarAttributeType.N)
                 )
                .withKeySchema(
                    new KeySchemaElement(HASH_KEY_NAME, KeyType.HASH),
                    new KeySchemaElement(RANGE_KEY_NAME, KeyType.RANGE))
                .withProvisionedThroughput(THRUPUT)
                .withGlobalSecondaryIndexes(
                    new GlobalSecondaryIndex()
                        .withIndexName(HASH_ONLY_GSI_NAME)
                        .withKeySchema(new KeySchemaElement(GSI_HASH_KEY_NAME, KeyType.HASH))
                        .withProjection(PROJECTION)
                        .withProvisionedThroughput(THRUPUT),
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
                        .withProjection(PROJECTION)
                    );
                ddb.createTable(req);
                // waits until table becomes active
                table.waitForActive();
            }
        }
    }

    private static void setUpExamples() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        table.putItem(new Item()
            .withPrimaryKey(HASH_KEY_NAME, "hashKeyValue", RANGE_KEY_NAME, 0)
            .withInt("num1", 100)
            .withInt("num2", 50)
        );
        Item item = table.getItem(HASH_KEY_NAME, "hashKeyValue", RANGE_KEY_NAME, 0);
        System.out.println("Before update: " + String.valueOf(item));
        table.updateItem(HASH_KEY_NAME, "hashKeyValue", RANGE_KEY_NAME, 0,
                "SET #0 = #0 + :0, #1 = :1",
                "#2 BETWEEN :2 AND :3", 
                new NameMap()
                    .with("#0", "num1")
                    .with("#1", "string-attr")
                    .with("#2", "num2"), 
                new ValueMap()
                    .with(":0", 20)
                    .with(":1", "string-value")
                    .with(":2", 0)
                    .with(":3", 100)
                );
        item = table.getItem(HASH_KEY_NAME, "hashKeyValue", RANGE_KEY_NAME, 0);
        System.out.println("After update: " + String.valueOf(item));
    }
}
