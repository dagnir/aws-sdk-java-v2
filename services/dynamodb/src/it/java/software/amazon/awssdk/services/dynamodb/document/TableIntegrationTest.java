/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

public class TableIntegrationTest extends IntegrationTestBase {
    private final ProvisionedThroughput THRUPUT = new ProvisionedThroughput(1L, 2L);
    private final ProvisionedThroughput THRUPUT2 = new ProvisionedThroughput(2L, 2L);

    //    @Test
    public void testCreate_Wait_Delete() throws InterruptedException {
        Table table = dynamo.createTable(new CreateTableRequest()
                                                 .withTableName("TableTest-" + UUID.randomUUID().toString())
                                                 .withAttributeDefinitions(
                                                         new AttributeDefinition(HASH_KEY_NAME, ScalarAttributeType.S))
                                                 .withKeySchema(new KeySchemaElement(HASH_KEY_NAME, KeyType.HASH))
                                                 .withProvisionedThroughput(THRUPUT));
        TableDescription desc = table.waitForActive();
        System.out.println(desc);
        Assert.assertSame(desc, table.getDescription());
        table.delete();
        try {
            table.waitForActive();
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            // Waiting on a table being deleted doesn't make sense
        }
    }

    // Waiting on an already active table should return instantly.
    @Test
    public void testWaitOnActiveTable() throws InterruptedException {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        long start = System.nanoTime();
        table.waitForActive();
        long end = System.nanoTime();
        Assert.assertTrue(TimeUnit.NANOSECONDS.toSeconds(end - start) < 3);
    }

    @Test
    public void testUpdateTable() throws InterruptedException {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        table.waitForActive();
        try {
            table.updateTable(THRUPUT2);
            table.waitForActive();
        } catch (AmazonServiceException ex) {
            if (ex.getMessage().contains("requested value equals the current value")) {
                table.updateTable(THRUPUT);
                table.waitForActive();
            } else {
                throw ex;
            }
        }
    }
}
