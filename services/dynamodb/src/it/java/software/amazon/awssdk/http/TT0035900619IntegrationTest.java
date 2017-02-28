package software.amazon.awssdk.http;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.DescribeTableResult;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

public class TT0035900619IntegrationTest {
    private static AmazonDynamoDBClient client;
    private static final long SLEEP_TIME_MILLIS = 5000;
    protected static String TABLE_NAME = "TT0035900619IntegrationTest-" + UUID.randomUUID();

    @BeforeClass
    public static void setup() throws InterruptedException {
        client = new AmazonDynamoDBClient(awsTestCredentials());
        List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(new AttributeDefinition("hashKey", ScalarAttributeType.S));
        List<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(new KeySchemaElement("hashKey", KeyType.HASH));

        client.createTable(attributeDefinitions, 
            TABLE_NAME,
            keySchema, new ProvisionedThroughput(1L,
            1L));
        waitForActiveTable(TABLE_NAME);
    }

    public static TableDescription waitForActiveTable(String tableName)
            throws InterruptedException {
        DescribeTableResult result = client.describeTable(tableName);
        TableDescription desc = result.getTable();
        String status = desc.getTableStatus();
        for (;; status = desc.getTableStatus()) {
            if ("ACTIVE".equals(status)) {
                return desc;
            } else if ("CREATING".equals(status) || "UPDATING".equals(status)) {
                Thread.sleep(SLEEP_TIME_MILLIS);
                result = client.describeTable(tableName);
                desc = result.getTable();
            } else {
                throw new IllegalArgumentException("Table " + tableName
                        + " is not being created (with status=" + status + ")");
            }
        }
    }
    
    @AfterClass
    public static void bye() {
        // Disable error injection or else the deletion would fail!
        AmazonHttpClient.configUnreliableTestConditions(null);
        client.deleteTable(TABLE_NAME);
        client.shutdown();
    }

    @Test
    public void testFakeRuntimeException_Once() {
        try {
            AmazonHttpClient.configUnreliableTestConditions(
                    new UnreliableTestConfig()
                    .withMaxNumErrors(1)
                    .withBytesReadBeforeException(10)
                    .withFakeIOException(false)
                    .withResetIntervalBeforeException(2)
            );
            System.out.println(client .describeTable(TABLE_NAME));
            Assert.fail();
        } catch (RuntimeException expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void testFakeIOException_Once() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(1)
                .withBytesReadBeforeException(10)
                .withFakeIOException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(TABLE_NAME));
    }

    @Test
    public void testFakeIOException_MaxRetries() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(PredefinedRetryPolicies.DYNAMODB_DEFAULT_MAX_ERROR_RETRY)
                .withBytesReadBeforeException(10)
                .withFakeIOException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(TABLE_NAME));
    }

    @Test
    public void testFakeIOException_OneTooMany() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(PredefinedRetryPolicies.DYNAMODB_DEFAULT_MAX_ERROR_RETRY+1)
                .withBytesReadBeforeException(10)
                .withFakeIOException(true)
                .withResetIntervalBeforeException(2)
        );
        try {
            System.out.println(client.describeTable(TABLE_NAME));
            Assert.fail();
        } catch(AmazonClientException expected) {
            expected.printStackTrace();
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
}
