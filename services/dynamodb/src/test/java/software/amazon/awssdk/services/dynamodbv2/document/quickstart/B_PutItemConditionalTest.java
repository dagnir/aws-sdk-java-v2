/*
 * Copyright 2014-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.awssdk.services.dynamodbv2.document.quickstart;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.AttributeUpdate;
import software.amazon.awssdk.services.dynamodbv2.document.Expected;
import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.document.UpdateItemOutcome;
import software.amazon.awssdk.services.dynamodbv2.document.spec.UpdateItemSpec;
import software.amazon.awssdk.services.dynamodbv2.document.utils.NameMap;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeAction;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValue;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodbv2.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodbv2.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodbv2.model.ReturnValue;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;
import software.amazon.awssdk.services.dynamodbv2.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodbv2.model.UpdateItemResult;

/**
 * Sample code to put items to a DynamoDB table.
 */
public class B_PutItemConditionalTest extends AbstractQuickStart {
    // A counter example to illustrate why you don't want to use the low-level
    // client which is a lot more verbose.
    @Test
    public void dont_use_the_lowLevelClient() {
        try {
            // First set up the example by inserting a new item
            // To see different results, change either player's
            // starting positions to 20, or set player 1's location to 19.
            Integer player1Position = 15;
            Integer player2Position = 12;
            client.putItem(new PutItemRequest()
                    .withTableName("Game")
                    .addItemEntry("GameId", new AttributeValue("abc"))
                    .addItemEntry("Player1-Position",
                        new AttributeValue().withN(player1Position.toString()))
                    .addItemEntry("Player2-Position",
                        new AttributeValue().withN(player2Position.toString()))
                    .addItemEntry("Status", new AttributeValue("IN_PROGRESS")));
             
            // Now move Player1 for game "abc" by 1,
            // as long as neither player has reached "20".
            UpdateItemResult result = client.updateItem(new UpdateItemRequest()
                .withTableName("Game")
                .withReturnValues(ReturnValue.ALL_NEW)
                .addKeyEntry("GameId", new AttributeValue("abc"))
                .addAttributeUpdatesEntry(
                     "Player1-Position", new AttributeValueUpdate()
                         .withValue(new AttributeValue().withN("1"))
                         .withAction(AttributeAction.ADD))
                .addExpectedEntry(
                     "Player1-Position", new ExpectedAttributeValue()
                         .withValue(new AttributeValue().withN("20"))
                         .withComparisonOperator(ComparisonOperator.LT))
                .addExpectedEntry(
                     "Player2-Position", new ExpectedAttributeValue()
                         .withValue(new AttributeValue().withN("20"))
                         .withComparisonOperator(ComparisonOperator.LT))
                .addExpectedEntry(
                     "Status", new ExpectedAttributeValue()
                         .withValue(new AttributeValue().withS("IN_PROGRESS"))
                         .withComparisonOperator(ComparisonOperator.EQ))
      
            );
            if ("20".equals(result.getAttributes().get("Player1-Position").getN())) {
                System.out.println("Player 1 wins!");
            } else {
                System.out.println("The game is still in progress: "
                    + result.getAttributes());
            }
        } catch (ConditionalCheckFailedException e) {
            System.out.println("Failed to move player 1 because the game is over");
        }
    }

    @Test
    public void howToUse_documentAPIViaExpression() {
        try {
            // First set up the example by inserting a new item
            // To see different results, change either player's
            // starting positions to 20, or set player 1's location to 19.
            Table table = dynamo.getTable("Game");
            table.putItem(new Item()
                .withPrimaryKey("GameId", "abc")
                .withInt("Player1-Position", 15)
                .withInt("Player2-Position", 12)
                .withString("Status", "IN_PROGRESS"));
            
            // Now move Player1 for game "abc" by 1,
            // as long as neither player has reached "20".
            UpdateItemOutcome outcome = table.updateItem(new UpdateItemSpec()
                .withReturnValues(ReturnValue.ALL_NEW)
                .withPrimaryKey("GameId", "abc")
                .withUpdateExpression("ADD #player1 :position")
                .withConditionExpression("#player1 < :hi_1 AND #player2 < :hi_2 AND #status = :status")
                .withNameMap(new NameMap()
                    .with("#player1", "Player1-Position")
                    .with("#player2", "Player2-Position")
                    .with("#status", "Status"))
                .withValueMap(new ValueMap()
                    .withInt(":hi_1", 20)
                    .withInt(":hi_2", 20)
                    .withString(":status", "IN_PROGRESS")
                    .withInt(":position", 1)
                )
            );
            Item item = outcome.getItem();
            if (item.getInt("Player1-Position") == 20) {
                System.out.println("Player 1 wins!");
            } else {
                System.out.println("The game is still in progress: " + item);
            }
        } catch (ConditionalCheckFailedException e) {
            System.out.println("Failed to move player 1 because the game is over");
        }
    }

    @Test
    public void howToUse_documentAPI() {
        try {
            // First set up the example by inserting a new item
            // To see different results, change either player's
            // starting positions to 20, or set player 1's location to 19.
            Table table = dynamo.getTable("Game");
            table.putItem(new Item()
                .withPrimaryKey("GameId", "abc")
                .withInt("Player1-Position", 15)
                .withInt("Player2-Position", 12)
                .withString("Status", "IN_PROGRESS"));
            
            // Now move Player1 for game "abc" by 1,
            // as long as neither player has reached "20".
            UpdateItemOutcome outcome = table.updateItem(new UpdateItemSpec()
                .withReturnValues(ReturnValue.ALL_NEW)
                .withPrimaryKey("GameId", "abc")
                .withAttributeUpdate(
                    new AttributeUpdate("Player1-Position").addNumeric(1))
                .withExpected(
                    new Expected("Player1-Position").lt(20),
                    new Expected("Player2-Position").lt(20),
                    new Expected("Status").eq("IN_PROGRESS"))
            );
            Item item = outcome.getItem();
            if (item.getInt("Player1-Position") == 20) {
                System.out.println("Player 1 wins!");
            } else {
                System.out.println("The game is still in progress: " + item);
            }
        } catch (ConditionalCheckFailedException e) {
            System.out.println("Failed to move player 1 because the game is over");
        }
    }

    @Test
    public void howToUse_jsonDirectly() {
        Table table = dynamo.getTable("Game");
        
        String json = "{"
                + "\"Status\" : \"IN_PROGRESS\","
                + "\"GameId\" : \"abc\","
                + "\"Player1-Position\" : 15,"
                 + "\"Player2-Position\" : 12"
             + "}"
             ;
//        Item item = new Item()
//            .withPrimaryKey("GameId", "abc")
//            .withInt("Player1-Position", 15)
//            .withInt("Player2-Position", 12)
//            .withString("Status", "IN_PROGRESS");
//        String json = item.toJSONPretty();
        System.err.println(json);
        //        {
        //            "Status" : "IN_PROGRESS",
        //            "GameId" : "abc",
        //            "Player1-Position" : 15,
        //            "Player2-Position" : 12
        //        }
        
        // Create an Item directly from JSON
        Item jsonItem = Item.fromJSON(json);
        // Saves to DynamoDB
        table.putItem(jsonItem);
        // Updates it in DynamoDB
        UpdateItemOutcome outcome = table.updateItem(new UpdateItemSpec()
            .withReturnValues(ReturnValue.ALL_NEW)
            .withPrimaryKey("GameId", "abc")
            .withAttributeUpdate(
                new AttributeUpdate("Player1-Position").put(1), 
                new AttributeUpdate("Status").put("SUSPENDED"))
            .withExpected(
                new Expected("Player1-Position").lt(20),
                new Expected("Player2-Position").lt(20),
                new Expected("Status").eq("IN_PROGRESS"))
        );
        // Get it back and print out the JSON equivalent
        Item itemUpdated = outcome.getItem();
        System.err.println(itemUpdated.toJSONPretty());
        //        Item updated: {
        //            "Status" : "IN_PROGRESS",
        //            "GameId" : "abc",
        //            "Player1-Position" : 1,
        //            "Player2-Position" : 12
        //        }
    }

    @BeforeClass
    public static void setup() throws InterruptedException {
        AbstractQuickStart.setup();
        Table table = dynamo.getTable("Game");
        TableDescription desc = table.waitForActiveOrDelete();
        if (desc == null) {
            // Table not exist; let's create it.
            table = dynamo.createTable(newGameTableRequest());
            table.waitForActive();
        }
    }

    static CreateTableRequest newGameTableRequest() {
        CreateTableRequest req = new CreateTableRequest()
            .withTableName("Game")
             // primary keys
            .withAttributeDefinitions(
                    new AttributeDefinition("GameId", ScalarAttributeType.S))
            .withKeySchema(new KeySchemaElement("GameId", KeyType.HASH))
            .withProvisionedThroughput(new ProvisionedThroughput(1L, 2L))
            ;
        return req;
    }
        
}
