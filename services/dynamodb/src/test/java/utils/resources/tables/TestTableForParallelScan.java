package utils.resources.tables;

import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDB;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import utils.test.resources.DynamoDBTableResource;
import utils.test.util.DynamoDBTestBase;

/**
 * Test table for {@link ParallelScanIntegrationTest}
 */
public class TestTableForParallelScan extends DynamoDBTableResource {

    public static final String TABLE_NAME = "java-sdk-parallel-scan";
    public static final String HASH_KEY_NAME = "hash";
    public static final Long READ_CAPACITY = 10L;
    public static final Long WRITE_CAPACITY = 5L;
    public static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
            new ProvisionedThroughput().withReadCapacityUnits(READ_CAPACITY).withWriteCapacityUnits(WRITE_CAPACITY);

    @Override
    protected AmazonDynamoDB getClient() {
        return DynamoDBTestBase.getClient();
    }

    @Override
    protected CreateTableRequest getCreateTableRequest() {
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(TABLE_NAME)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(HASH_KEY_NAME)
                                .withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(
                                HASH_KEY_NAME).withAttributeType(
                                ScalarAttributeType.N))
                .withProvisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT);
        return createTableRequest;
    }

}
