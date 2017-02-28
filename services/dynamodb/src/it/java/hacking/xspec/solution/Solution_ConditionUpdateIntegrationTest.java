package hacking.xspec.solution;

import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.NULL;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.attribute_not_exists;
import static software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder.parenthesize;
import hacking.xspec.HackerIntegrationTestBase;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.AttributeUpdate;
import software.amazon.awssdk.services.dynamodbv2.document.Expected;
import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.document.UpdateItemOutcome;
import software.amazon.awssdk.services.dynamodbv2.document.spec.UpdateItemSpec;
import software.amazon.awssdk.services.dynamodbv2.model.ReturnValue;
import software.amazon.awssdk.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import software.amazon.awssdk.services.dynamodbv2.xspec.ScanExpressionSpec;
import software.amazon.awssdk.services.dynamodbv2.xspec.UpdateItemExpressionSpec;

public class Solution_ConditionUpdateIntegrationTest extends HackerIntegrationTestBase {
    @Test
    public void conditionalUpdate() {
        Table table = dynamo.getTable(HACKER_TABLE_NAME);
        Item item = table.getItem(HASH_KEY_HACKER_ID, hacker_uuid_1,
                RANGE_KEY_START_DATE, 20140201);
        System.out.println(item.toJSONPretty());

        UpdateItemExpressionSpec xspec = new ExpressionSpecBuilder()
            // SET lines_of_code = lines_of_code + 1
            .addUpdate(
                N("lines_of_code").set(N("lines_of_code").plus(1)))
            // SET photo.image_file_name = "joe.d.velopar.png"
            .addUpdate(
                S("photo.image_file_name").set("joe.d.velopar.png")
            )
            .addUpdate(NULL("nullAttr").set())
            .withCondition(
                 // add explicit parenthesis
                parenthesize( attribute_not_exists("item_version")
                    .and( attribute_not_exists("config_id") )
                    .and( attribute_not_exists("config_version") )
                ).or( S("photo.image_file_name").eq("xyz.png") )
                 .or( N("lines_of_code").lt(99999))
            ).buildForUpdate();

        // (attribute_not_exists(#3) AND attribute_not_exists(#4) AND attribute_not_exists(#5)) OR #1.#2 = :2 OR #0 < :3
        System.out.println(xspec.getConditionExpression());
        // SET #0 = #0 + :0, #1.#2 = :1
        System.out.println(xspec.getUpdateExpression());

        table.updateItem(HASH_KEY_HACKER_ID, hacker_uuid_1,
                RANGE_KEY_START_DATE, 20140201, xspec);

        item = table.getItem(HASH_KEY_HACKER_ID, hacker_uuid_1,
                RANGE_KEY_START_DATE, 20140201);
        System.out.println(item.toJSONPretty());
    }

    @Test
    public void blogSample() {
        Table table = dynamo.getTable("Game");
        table.putItem(new Item()
            .withPrimaryKey("GameId", "abc")
            .withInt("Player1-Position", 15)
            .withInt("Player2-Position", 12)
            .withString("Status", "IN_PROGRESS"));

        table.updateItem(new UpdateItemSpec()
            .withReturnValues(ReturnValue.ALL_NEW)
            .withPrimaryKey("GameId", "abc")
            .withExpressionSpec(new ExpressionSpecBuilder()
                .addUpdate(N("Player1-Position").add(1))
                .withCondition(
                          N("Player1-Position").lt(20)
                    .and( N("Player2-Position").lt(20) )
                    .and( S("Status").eq("IN_PROGRESS")
                        .or( attribute_not_exists("Status") )))
                .buildForUpdate()));
        Item item = table.getItem("GameId", "abc");
        System.out.println(item.toJSONPretty());
    }

    @Test
    public void blogScan() {
        Table table = dynamo.getTable("Game");
        table.putItem(new Item()
            .withPrimaryKey("GameId", "abc")
            .withInt("Player1-Position", 15)
            .withInt("Player2-Position", 12)
            .withString("Status", "IN_PROGRESS"));

        ScanExpressionSpec xspec = new ExpressionSpecBuilder()
            .withCondition(N("Player1-Position").between(10, 20)
                .and( S("Status").in("IN_PROGRESS", "IDLE")
                      .or(attribute_not_exists("Player2-Position"))))
            .buildForScan();
        for (Item item: table.scan(xspec)) {
            System.out.println(item.toJSONPretty());
        }
    }

    @Test
    public void previousblog() {
        Table table = dynamo.getTable("Game");
        UpdateItemOutcome outcome = table.updateItem(new UpdateItemSpec()
        .withReturnValues(ReturnValue.ALL_NEW)
        .withPrimaryKey("GameId", "abc")
        .withAttributeUpdate(
            new AttributeUpdate("Player1-Position").addNumeric(1))
                .withExpected(
                    new Expected("Player1-Position").lt(20),
                    new Expected("Player2-Position").lt(20),
                    new Expected("Status").eq("IN_PROGRESS")));
        }
}
