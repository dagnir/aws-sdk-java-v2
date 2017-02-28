/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Index;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodbv2.model.CreateGlobalSecondaryIndexAction;
import software.amazon.awssdk.services.dynamodbv2.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodbv2.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodbv2.model.KeyType;
import software.amazon.awssdk.services.dynamodbv2.model.Projection;
import software.amazon.awssdk.services.dynamodbv2.model.ProjectionType;
import software.amazon.awssdk.services.dynamodbv2.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodbv2.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

/**
 * Sample code to create a DynamoDB table.
 */
public class A_UpdateTableTest extends AbstractQuickStart {
    private static final ProvisionedThroughput THRUPUT = new ProvisionedThroughput(1L, 2L);
    private static final ProvisionedThroughput THRUPUT2 = new ProvisionedThroughput(2L, 2L);
    private static final Projection PROJECTION = new Projection().withProjectionType(ProjectionType.ALL);
    /**
     * First create a table, then add an index.
     */
    @Test
    public void howToUpdateTable() throws InterruptedException {
        String TABLE_NAME = "howToUpdateTable";
        Table table = dynamo.getTable(TABLE_NAME);
        // check if table already exists, and if so wait for it to become active
        TableDescription desc = table.waitForActiveOrDelete();
        if (desc != null) {
            System.out.println("Skip creating table which already exists and ready for use: "
                    + desc);
            table.delete();
            desc = table.waitForActiveOrDelete();
            if (desc != null)
                throw new IllegalStateException(String.valueOf(desc));
        }
        // Table doesn't exist.  Let's create it.
        table = dynamo.createTable(newCreateTableRequest(TABLE_NAME));
        // Wait for the table to become active 
        desc = table.waitForActive();
        System.out.println("Table is ready for use! " + desc);

        // Creates a GSI for the table.
        Index index = table.createGSI(new CreateGlobalSecondaryIndexAction()
            .withIndexName(RANGE_GSI_NAME)
            .withKeySchema(
                new KeySchemaElement(GSI_HASH_KEY_NAME, KeyType.HASH),
                new KeySchemaElement(GSI_RANGE_KEY_NAME, KeyType.RANGE))
                .withProjection(PROJECTION)
                .withProvisionedThroughput(THRUPUT),
            new AttributeDefinition(GSI_HASH_KEY_NAME, ScalarAttributeType.S),
            new AttributeDefinition(GSI_RANGE_KEY_NAME, ScalarAttributeType.N));
        index.waitForActive();
        System.out.println("Updated table description: " + desc);

        // Updates the GSI for the table.
        desc = index.updateGSI(THRUPUT2);
        desc = index.waitForActive();

        System.out.println("Updated index description: " + desc);

        // Deletes the GSI for the table.
        desc = index.deleteGSI();
        index.waitForDelete();

        table.delete();
    }

    private CreateTableRequest newCreateTableRequest(String tableName) {
        CreateTableRequest req = new CreateTableRequest()
            .withTableName(tableName)
            .withAttributeDefinitions(
                new AttributeDefinition(HASH_KEY_NAME, ScalarAttributeType.S),
                new AttributeDefinition(RANGE_KEY_NAME, ScalarAttributeType.N))
            .withKeySchema(
                new KeySchemaElement(HASH_KEY_NAME, KeyType.HASH),
                new KeySchemaElement(RANGE_KEY_NAME, KeyType.RANGE))
            .withProvisionedThroughput(THRUPUT);
        return req;
    }
}
