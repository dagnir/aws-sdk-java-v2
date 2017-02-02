package software.amazon.awssdk.services.dynamodbv2;

import java.util.HashMap;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.ScanRequest;
import software.amazon.awssdk.services.dynamodbv2.util.TableUtils;
import software.amazon.awssdk.services.dynamodbv2.util.TableUtils.TableNeverTransitionedToStateException;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class GzipConfigurationIntegrationTest extends AWSIntegrationTestBase {

    private static AmazonDynamoDB dynamo;
    private static final String TABLE_NAME = "test-gzip-" + System.currentTimeMillis();

    private static final String KEY_NAME = "key";
    private static final String VALUE_NAME = "value";
    private static final int ITEMS_COUNT = 500;

    @BeforeClass
    public static void setup() throws TableNeverTransitionedToStateException,
            InterruptedException {
        dynamo = new AmazonDynamoDBClient(getCredentials(),
                new ClientConfiguration().withGzip(true));
        createTable();
        // For this integration test, if the payload is not big enough, the service will not compress the data.
        putItems(ITEMS_COUNT);
    }

    @AfterClass
    public static void tearDown() {
        dynamo.deleteTable(new DeleteTableRequest().withTableName(TABLE_NAME));
    }

    @Test
    public void gzipConfigurationIntegrationTest() throws TableNeverTransitionedToStateException,
            InterruptedException {
        int count = dynamo.scan(new ScanRequest().withTableName(TABLE_NAME))
                .getCount();
        Assert.assertEquals(ITEMS_COUNT, count);
    }

    private static void createTable() throws TableNeverTransitionedToStateException, InterruptedException {
        dynamo.createTable(new CreateTableRequest()
                .withTableName(TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(KEY_NAME)
                                .withAttributeType(ScalarAttributeType.S))
                .withKeySchema(
                        new KeySchemaElement().withKeyType(KeyType.HASH)
                                .withAttributeName(KEY_NAME))
                .withProvisionedThroughput(
                        new ProvisionedThroughput(100L, 100L)));

        TableUtils.waitUntilActive(dynamo, TABLE_NAME);
    }

    @SuppressWarnings("serial")
    private static void putItems(int count) {
        for (int i = 0; i < count; ++i) {
            dynamo.putItem(new PutItemRequest().withTableName(TABLE_NAME)
                    .withItem(new HashMap<String, AttributeValue>() {
                        {
                            put(KEY_NAME, new AttributeValue().withS(UUID
                                    .randomUUID().toString()));
                            put(VALUE_NAME, new AttributeValue().withS(UUID
                                    .randomUUID().toString()));
                        }
                    }));
        }
    }

}
