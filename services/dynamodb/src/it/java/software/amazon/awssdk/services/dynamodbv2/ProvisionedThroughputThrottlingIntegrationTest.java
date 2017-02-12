package software.amazon.awssdk.services.dynamodbv2;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;
import software.amazon.awssdk.services.dynamodbv2.model.PutItemRequest;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.BasicTempTableWithLowThroughput;
import utils.test.util.DynamoDBTestBase;

/**
 * DynamoDB integration tests around ProvisionedThroughput/throttling errors.
 */
@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
        @RequiredResource(resource = BasicTempTableWithLowThroughput.class,
                creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS)
})
public class ProvisionedThroughputThrottlingIntegrationTest extends DynamoDBTestBase {

    private static final String tableName = BasicTempTableWithLowThroughput.TEMP_TABLE_NAME;
    private static final String HASH_KEY_NAME = BasicTempTableWithLowThroughput.HASH_KEY_NAME;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBTestBase.setUpTestBase();
    }

    /**
     * Tests that throttling errors and delayed retries are automatically
     * handled for users.
     *
     * We trigger ProvisionedThroughputExceededExceptions here because of the
     * low throughput on our test table, but users shouldn't see any problems
     * because of the backoff and retry strategy.
     */
    @Test
    public void testProvisionedThroughputExceededRetryHandling() throws Exception {
        for (int i = 0; i < 20; i++) {
            Map<String, AttributeValue> item = Collections
                    .singletonMap(HASH_KEY_NAME, new AttributeValue(UUID.randomUUID().toString()));
            dynamo.putItem(new PutItemRequest(tableName, item));
        }
    }

}
