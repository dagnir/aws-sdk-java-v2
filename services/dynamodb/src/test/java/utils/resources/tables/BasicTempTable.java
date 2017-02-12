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

public class BasicTempTable extends DynamoDBTableResource {

    public static final String TEMP_TABLE_NAME = "java-sdk-" + System.currentTimeMillis();
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
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(TEMP_TABLE_NAME)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(HASH_KEY_NAME)
                                .withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(
                                HASH_KEY_NAME).withAttributeType(
                                ScalarAttributeType.S));
        request.setProvisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT);
        return request;
    }

}
