package software.amazon.awssdk.services.dynamodbv2.document;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.document.spec.GetItemSpec;

public class ItemEqualityIntegrationTest extends IntegrationTestBase {

    @Test
    public void test_equalMapAndJSON() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "user123")
                .withJSON(
                        "Details",
                        "{ \"UserID1\": 0}");
        table.putItem(item);
        Item itemGet = table.getItem(new GetItemSpec().withPrimaryKey(new
                                                                              KeyAttribute
                                                                              (HASH_KEY_NAME, "user123"))
                                             .withConsistentRead(true));
        assertEquals(item.asMap(), itemGet.asMap());
        assertEquals(Item.fromJSON(item.toJSON()), Item.fromJSON(itemGet.toJSON()));
    }

    @Test
    public void test_equalMap() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "user123")
                .withString("DateTime", "1357306017")
                .withJSON(
                        "Details",
                        "{ \"UserID1\": 0, \"UserID2\": 0, \"Message\": \"my message\", \"DateTime\": 0}");
        table.putItem(item);
        Item itemGet = table.getItem(new GetItemSpec().withPrimaryKey(new
                                                                              KeyAttribute
                                                                              (HASH_KEY_NAME, "user123"))
                                             .withConsistentRead(true));
        assertEquals(item.asMap(), itemGet.asMap());
    }
}
