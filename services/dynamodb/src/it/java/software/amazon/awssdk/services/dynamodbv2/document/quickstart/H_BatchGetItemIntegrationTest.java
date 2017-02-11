package software.amazon.awssdk.services.dynamodbv2.document.quickstart;

import static software.amazon.awssdk.services.dynamodbv2.document.quickstart.F_UpdateItemIntegrationTest.ADDRESS_TYPE_HOME;
import static software.amazon.awssdk.services.dynamodbv2.document.quickstart.F_UpdateItemIntegrationTest.ADDRESS_TYPE_WORK;
import static software.amazon.awssdk.services.dynamodbv2.document.quickstart.F_UpdateItemIntegrationTest.FIRST_CUSTOMER_ID;
import static software.amazon.awssdk.services.dynamodbv2.document.quickstart.F_UpdateItemIntegrationTest.HASH_KEY;
import static software.amazon.awssdk.services.dynamodbv2.document.quickstart.F_UpdateItemIntegrationTest.RANGE_KEY;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodbv2.document.BatchGetItemOutcome;
import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.TableKeysAndAttributes;
import software.amazon.awssdk.services.dynamodbv2.document.utils.NameMap;
import software.amazon.awssdk.services.dynamodbv2.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodbv2.model.ReturnConsumedCapacity;

/**
 * Sample code to perform batch get items from DynamoDB.
 */
public class H_BatchGetItemIntegrationTest extends QuickStartIntegrationTestBase {

    @Before
    public void before() throws InterruptedException {
        new B_PutItemIntegrationTest().howToPutItems();
        F_UpdateItemIntegrationTest.setupData(dynamo);
    }

    @Test
    public void howToBatchGet_FromOneTable() {
        TableKeysAndAttributes tableKeysAndAttributes =
                new TableKeysAndAttributes(TABLE_NAME)
                        .withAttributeNames("binary", "booleanTrue", "intAttr",
                                            "mapAttr", "stringSetAttr")
                        // you can add a bunch of keys in one go
                        .addHashAndRangePrimaryKeys(
                                HASH_KEY_NAME, RANGE_KEY_NAME,
                                "foo", 1,
                                "foo", 2,
                                "foo", 3
                                // etc.
                        )
                        // or you can take it slow and add one at a time
                        .addHashAndRangePrimaryKey(HASH_KEY_NAME, "foo", RANGE_KEY_NAME, 4)
                        .addHashAndRangePrimaryKey(HASH_KEY_NAME, "foo", RANGE_KEY_NAME, 5);
        BatchGetItemOutcome outcome = dynamo.batchGetItem(
                ReturnConsumedCapacity.TOTAL, tableKeysAndAttributes);
        Map<String, List<Item>> tableItems = outcome.getTableItems();
        Assert.assertTrue(tableItems.size() == 1);
        for (Map.Entry<String, List<Item>> e : tableItems.entrySet()) {
            System.out.println("tableName: " + e.getKey());
            for (Item item : e.getValue()) {
                System.out.println("item: " + item);
            }
            Assert.assertTrue(e.getValue().size() == 5);
        }
    }

    @Test
    public void howToUse_ProjectionExpression() {
        TableKeysAndAttributes tableKeysAndAttributes =
                new TableKeysAndAttributes(TABLE_NAME)
                        // use projection expression instead of attribute names
                        .withProjectionExpression(
                                HASH_KEY_NAME + ", " + RANGE_KEY_NAME + ", "
                                + "#binary, booleanTrue, intAttr, mapAttr, stringSetAttr")
                        .withNameMap(new NameMap().with("#binary", "binary"))
                        // you can add a bunch of keys in one go
                        .addHashAndRangePrimaryKeys(
                                HASH_KEY_NAME, RANGE_KEY_NAME,
                                "foo", 2,
                                "foo", 3,
                                "foo", 4,
                                "foo", 5
                                // etc.
                        );
        BatchGetItemOutcome outcome = dynamo.batchGetItem(
                ReturnConsumedCapacity.TOTAL, tableKeysAndAttributes);
        Map<String, List<Item>> tableItems = outcome.getTableItems();
        Assert.assertTrue(tableItems.size() == 1);
        for (Map.Entry<String, List<Item>> e : tableItems.entrySet()) {
            System.out.println("tableName: " + e.getKey());
            for (Item item : e.getValue()) {
                System.out.println("item: " + item);
            }
            Assert.assertTrue(e.getValue().size() == 4);
        }
    }

    @Test
    public void howToBatchGet_FromMultipleTables() {
        BatchGetItemOutcome outcome = dynamo.batchGetItem(
                // First table
                new TableKeysAndAttributes(TABLE_NAME)
                        .withAttributeNames("binary", "booleanTrue", "intAttr",
                                            "mapAttr", "stringSetAttr")
                        // you can add a bunch of keys in one go
                        .addHashAndRangePrimaryKeys(
                                HASH_KEY_NAME, RANGE_KEY_NAME,
                                "foo", 1,
                                "foo", 2,
                                "foo", 3
                                // etc.
                        ),
                // Second table
                new TableKeysAndAttributes(F_UpdateItemIntegrationTest.TABLE_NAME)
                        .withAttributeNames(HASH_KEY, RANGE_KEY, "AddressLine1",
                                            "city", "state", "zipcode", "phone")
                        // you can add a bunch of keys in one go
                        .addHashAndRangePrimaryKeys(
                                HASH_KEY, RANGE_KEY,
                                FIRST_CUSTOMER_ID, ADDRESS_TYPE_HOME,
                                FIRST_CUSTOMER_ID, ADDRESS_TYPE_WORK
                                // etc.
                        )
        );
        Map<String, List<Item>> tableItems = outcome.getTableItems();
        Assert.assertTrue(tableItems.size() == 2);
        for (Map.Entry<String, List<Item>> e : tableItems.entrySet()) {
            String tableName = e.getKey();
            System.out.println("tableName: " + tableName);
            for (Item item : e.getValue()) {
                System.out.println("item: " + item);
            }
            if (tableName.equals(TABLE_NAME)) {
                Assert.assertTrue(e.getValue().size() == 3);
            } else {
                Assert.assertTrue(e.getValue().size() == 2);
            }
        }
    }

    @Test
    public void howToHandle_UnprocessedKeys() throws InterruptedException {
        TableKeysAndAttributes tableKeysAndAttributes =
                new TableKeysAndAttributes(TABLE_NAME)
                        .withAttributeNames("binary", "booleanTrue", "intAttr",
                                            "mapAttr", "stringSetAttr")
                        // you can add a bunch of keys in one go
                        .addHashAndRangePrimaryKeys(
                                HASH_KEY_NAME, RANGE_KEY_NAME,
                                "foo", 1,
                                "foo", 2,
                                "foo", 3,
                                "foo", 4,
                                "foo", 5
                                // etc.
                        );
        // unprocessed items from DynamoDB
        int attempts = 0;
        Map<String, KeysAndAttributes> unprocessed = null;
        do {
            if (attempts > 0) {
                // exponential backoff per DynamoDB recommendation
                Thread.sleep((1 << attempts) * 1000);
            }
            BatchGetItemOutcome outcome;
            if (unprocessed == null || unprocessed.size() > 0) {
                // handle initial request
                outcome = dynamo.batchGetItem(tableKeysAndAttributes);
            } else {
                // handle unprocessed items
                outcome = dynamo.batchGetItemUnprocessed(unprocessed);
            }
            Map<String, List<Item>> tableItems = outcome.getTableItems();
            for (Map.Entry<String, List<Item>> e : tableItems.entrySet()) {
                System.out.println("tableName: " + e.getKey());
                for (Item item : e.getValue()) {
                    System.out.println("item: " + item);
                }
                Assert.assertTrue(e.getValue().size() == 5);
            }
            unprocessed = outcome.getUnprocessedKeys();
            System.out.println("unprocessed: " + unprocessed);
        } while (unprocessed.size() > 0);
    }

    @Test(expected = AmazonServiceException.class)
    public void nullTableKeysAndAttributes() {
        dynamo.batchGetItem((TableKeysAndAttributes[]) null);
    }

    @Test(expected = AmazonServiceException.class)
    public void emptyTableKeysAndAttributes() {
        dynamo.batchGetItem(new TableKeysAndAttributes[0]);
    }
}
