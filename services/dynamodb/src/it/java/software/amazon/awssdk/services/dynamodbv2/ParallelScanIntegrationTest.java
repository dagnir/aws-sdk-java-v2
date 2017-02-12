package software.amazon.awssdk.services.dynamodbv2;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;
import software.amazon.awssdk.services.dynamodbv2.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodbv2.model.Condition;
import software.amazon.awssdk.services.dynamodbv2.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ScanRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ScanResult;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.TestTableForParallelScan;
import utils.test.util.DynamoDBTestBase;

/**
 * DynamoDB integration tests on the low-level parallel scan operation.
 */
@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
        @RequiredResource(resource = TestTableForParallelScan.class,
                creationPolicy = ResourceCreationPolicy.REUSE_EXISTING,
                retentionPolicy = ResourceRetentionPolicy.KEEP)
})
public class ParallelScanIntegrationTest extends DynamoDBTestBase {

    private static final String tableName = TestTableForParallelScan.TABLE_NAME;
    private static final String HASH_KEY_NAME = TestTableForParallelScan.HASH_KEY_NAME;
    private static final String ATTRIBUTE_FOO = "attribute_foo";
    private static final String ATTRIBUTE_BAR = "attribute_bar";
    private static final String ATTRIBUTE_RANDOM = "attribute_random";
    private static final int itemNumber = 200;

    /**
     * Creates a test table with a local secondary index
     */
    @BeforeClass
    public static void setUp() {
        DynamoDBTestBase.setUpTestBase();
    }

    /**
     * Tests making parallel scan on DynamoDB table.
     */
    @Test
    public void testParallelScan() {
        putTestData();

        /**
         * Only one segment.
         */
        ScanRequest scanRequest = new ScanRequest(tableName)
                .withScanFilter(Collections.singletonMap(
                        ATTRIBUTE_RANDOM,
                        new Condition()
                                .withAttributeValueList(
                                        new AttributeValue().withN("" + itemNumber / 2))
                                .withComparisonOperator(
                                        ComparisonOperator.LT.toString())))
                .withTotalSegments(1).withSegment(0);
        ScanResult scanResult = dynamo.scan(scanRequest);
        assertEquals((Object) itemNumber, (Object) scanResult.getScannedCount());
        int filteredItems = scanResult.getCount();

        /**
         * Multiple segments.
         */
        int totalSegments = 10;
        int filteredItemsInSegments = 0;
        for (int segment = 0; segment < totalSegments; segment++) {
            scanRequest = new ScanRequest(tableName)
                    .withScanFilter(
                            Collections.singletonMap(
                                    ATTRIBUTE_RANDOM,
                                    new Condition().withAttributeValueList(
                                            new AttributeValue().withN(""
                                                                       + itemNumber / 2))
                                            .withComparisonOperator(
                                                    ComparisonOperator.LT
                                                            .toString())))
                    .withTotalSegments(totalSegments).withSegment(segment);
            scanResult = dynamo.scan(scanRequest);
            filteredItemsInSegments += scanResult.getCount();
        }
        assertEquals(filteredItems, filteredItemsInSegments);
    }

    private static void putTestData() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        Random random = new Random();
        for (int hashKeyValue = 0; hashKeyValue < itemNumber; hashKeyValue++) {
            item.put(HASH_KEY_NAME, new AttributeValue().withN(Integer.toString(hashKeyValue)));
            item.put(ATTRIBUTE_RANDOM, new AttributeValue().withN(Integer.toString(random.nextInt(itemNumber))));
            if (hashKeyValue < itemNumber / 2) {
                item.put(ATTRIBUTE_FOO, new AttributeValue().withN(Integer.toString(hashKeyValue)));
            } else {
                item.put(ATTRIBUTE_BAR, new AttributeValue().withN(Integer.toString(hashKeyValue)));
            }

            dynamo.putItem(new PutItemRequest(tableName, item));
            item.clear();
        }
    }
}
