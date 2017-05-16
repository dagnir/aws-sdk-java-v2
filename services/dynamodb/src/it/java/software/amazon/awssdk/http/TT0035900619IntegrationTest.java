package software.amazon.awssdk.http;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResult;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class TT0035900619IntegrationTest {
    private static DynamoDBClient client;
    private static final long SLEEP_TIME_MILLIS = 5000;
    protected static String TABLE_NAME = "TT0035900619IntegrationTest-" + UUID.randomUUID();

    @BeforeClass
    public static void setup() throws InterruptedException {
        client = DynamoDBClient.builder().credentialsProvider(new StaticCredentialsProvider(awsTestCredentials())).build();
        List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(new AttributeDefinition("hashKey", ScalarAttributeType.S));
        List<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(new KeySchemaElement("hashKey", KeyType.HASH));

        client.createTable(new CreateTableRequest(attributeDefinitions,
            TABLE_NAME,
            keySchema, new ProvisionedThroughput(1L,
            1L)));
        waitForActiveTable(TABLE_NAME);
    }

    public static TableDescription waitForActiveTable(String tableName)
            throws InterruptedException {
        DescribeTableResult result = client.describeTable(new DescribeTableRequest(tableName));
        TableDescription desc = result.getTable();
        String status = desc.getTableStatus();
        for (;; status = desc.getTableStatus()) {
            if ("ACTIVE".equals(status)) {
                return desc;
            } else if ("CREATING".equals(status) || "UPDATING".equals(status)) {
                Thread.sleep(SLEEP_TIME_MILLIS);
                result = client.describeTable(new DescribeTableRequest(tableName));
                desc = result.getTable();
            } else {
                throw new IllegalArgumentException("Table " + tableName
                        + " is not being created (with status=" + status + ")");
            }
        }
    }
    
    @AfterClass
    public static void bye() throws Exception {
        // Disable error injection or else the deletion would fail!
        AmazonHttpClient.configUnreliableTestConditions(null);
        client.deleteTable(new DeleteTableRequest(TABLE_NAME));
        client.close();
    }

    @Test
    public void testFakeRuntimeException_Once() {
        try {
            AmazonHttpClient.configUnreliableTestConditions(
                    new UnreliableTestConfig()
                    .withMaxNumErrors(1)
                    .withBytesReadBeforeException(10)
                    .withFakeIoException(false)
                    .withResetIntervalBeforeException(2)
            );
            System.out.println(client .describeTable(new DescribeTableRequest(TABLE_NAME)));
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
                .withFakeIoException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(new DescribeTableRequest(TABLE_NAME)));
    }

    @Test
    public void testFakeIOException_MaxRetries() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(PredefinedRetryPolicies.DYNAMODB_DEFAULT_MAX_ERROR_RETRY)
                .withBytesReadBeforeException(10)
                .withFakeIoException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(new DescribeTableRequest(TABLE_NAME)));
    }

    @Test
    public void testFakeIOException_OneTooMany() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(PredefinedRetryPolicies.DYNAMODB_DEFAULT_MAX_ERROR_RETRY+1)
                .withBytesReadBeforeException(10)
                .withFakeIoException(true)
                .withResetIntervalBeforeException(2)
        );
        try {
            System.out.println(client.describeTable(new DescribeTableRequest(TABLE_NAME)));
            Assert.fail();
        } catch(AmazonClientException expected) {
            expected.printStackTrace();
        }
    }

    protected static AwsCredentials awsTestCredentials() {
        try {
            return AwsIntegrationTestBase.CREDENTIALS_PROVIDER_CHAIN.getCredentialsOrThrow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
