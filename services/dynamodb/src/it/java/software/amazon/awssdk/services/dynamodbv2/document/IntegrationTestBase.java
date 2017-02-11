package software.amazon.awssdk.services.dynamodbv2.document;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;
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
 * hashkey only            gsi hashkey only
 * gsi (hashkey + range key)
 *
 * hashkey + rangekey      lsi (same hash key + range key)
 * gsi hashkey only
 * gsi (hashkey + range key)
 */
public class IntegrationTestBase {
    protected static DynamoDB dynamo;
    protected static DynamoDB dynamoOld;
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

    //    private static final boolean IS_SERVICE_BUILDER_USED = false;
    @BeforeClass
    public static void setup() throws InterruptedException {
        dynamoOld = new DynamoDB(new AmazonDynamoDBClient(awsTestCredentials()));
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsTestCredentials());
        dynamo = new DynamoDB(client);
        createTable_hashKeyOnly();
        createTable_rangeKey();
    }

    @AfterClass
    public static void tearDown() {
        dynamo.shutdown();
        dynamoOld.shutdown();
    }

    // Creates a hashkey only table with 2 GSI,
    // one hashkey only, the other hashkey and rangekey
    private static void createTable_hashKeyOnly() throws InterruptedException {
        DynamoDB[] ddbs = {dynamo, dynamoOld};
        for (DynamoDB ddb : ddbs) {
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
        DynamoDB[] ddbs = {dynamo, dynamoOld};
        for (DynamoDB ddb : ddbs) {
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

    protected static AWSCredentials awsTestCredentials() {
        try {
            return new PropertiesCredentials(new File(
                    System.getProperty("user.home")
                    + "/.aws/awsTestAccount.properties"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertTrue(int expected, int actual) {
        Assert.assertTrue("expected: " + expected + " but actual: " + actual,
                          expected == actual);
    }

    public static void assertGT(int greater, int less) {
        Assert.assertTrue("Expect " + greater + " > " + less, greater > less);
    }

    protected void putDataToRangeTable(DynamoDB dynamo, String hashKeyValue,
                                       int rangeKeyValueFrom, int rangeKeyValueTo) {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withPrimaryKey(HASH_KEY_NAME, hashKeyValue, RANGE_KEY_NAME, 0)
                .withBinary("binary", new byte[]{1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[]{5, 6}, new byte[]{7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = rangeKeyValueFrom; i <= rangeKeyValueTo; i++) {
            item.withKeyComponent(RANGE_KEY_NAME, i);
            table.putItem(item);
        }
    }

    protected String[] getRangeTableAttributes() {
        String[] attrNames = {HASH_KEY_NAME, RANGE_KEY_NAME, "binary", "binarySet", "intAttr", "numberAttr", "stringAttr",
                              "stringSetAttr"};
        return attrNames;
    }

    protected String getRangeTableProjectionExpression() {
        return HASH_KEY_NAME + ", " + RANGE_KEY_NAME + ", #binary, binarySet, intAttr, numberAttr, stringAttr, stringSetAttr";
    }
}
