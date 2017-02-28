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

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.document.spec.GetItemSpec;

public class B_PutItemJsonBooleanTest extends AbstractQuickStart {
    @Test
    public void howToPutItems() {
        Table table = dynamo.getTable(TABLE_NAME);
        Item item = new Item()
            .withPrimaryKey(HASH_KEY_NAME, "B_PutItemJsonBooleanTest", RANGE_KEY_NAME, 1)
            // Store document as a map
            .withJSON("jsonDoc", "{\"jsonTrue\" : true, \"jsonFalse\" : false}");
        table.putItem(item);
        // Retrieve the entire document and the entire document only
        Item documentItem = table.getItem(new GetItemSpec()
            .withPrimaryKey(HASH_KEY_NAME, "B_PutItemJsonBooleanTest", RANGE_KEY_NAME, 1)
            .withAttributesToGet("jsonDoc"));
        System.out.println(documentItem.get("jsonDoc"));
    }
}
