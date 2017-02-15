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

package software.amazon.awssdk.services.dynamodbv2.document;

import org.junit.Test;

public class IndexScanIntegrationTest extends IntegrationTestBase {
    @Test
    public void testLSIScan() {
        Table table = dynamoOld.getTable(RANGE_TABLE_NAME);
        final String hashkeyval = "testLSIScan";
        Item item = new Item()
                .withString(HASH_KEY_NAME, hashkeyval)
                .withBinarySet("binarySet", new byte[] {5, 6}, new byte[] {7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 1; i <= 10; i++) {
            item.withNumber(RANGE_KEY_NAME, i)
                .withNumber(LSI_RANGE_KEY_NAME, 100 + i)
            ;
            PutItemOutcome out = table.putItem(item);
            System.out.println(out);
        }
        Index lsi = table.getIndex(LSI_NAME);
        ItemCollection<?> col = lsi.scan(
                new ScanFilter(HASH_KEY_NAME).eq(hashkeyval),
                new ScanFilter(LSI_RANGE_KEY_NAME).between(0, 10));
        int count = 0;
        for (Item it : col) {
            System.out.println(it);
            count++;
        }
        assertTrue(0, count);
        col = lsi.scan(new ScanFilter(HASH_KEY_NAME).eq(hashkeyval),
                       new ScanFilter(LSI_RANGE_KEY_NAME).between(101, 110));
        for (Item it : col) {
            System.out.println(it);
            count++;
        }
        assertTrue(10, count);
    }

    @Test
    public void testGSIScan() {
        Table table = dynamoOld.getTable(RANGE_TABLE_NAME);
        final String hashkeyval = "testGSIScan";
        Item item = new Item()
                .withString(HASH_KEY_NAME, hashkeyval)
                .withString(GSI_HASH_KEY_NAME, hashkeyval)
                .withBinarySet("binarySet", new byte[] {5, 6}, new byte[] {7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 1; i <= 10; i++) {
            item.withNumber(RANGE_KEY_NAME, i)
                .withString(GSI_HASH_KEY_NAME, hashkeyval)
                .withNumber(GSI_RANGE_KEY_NAME, 100 + i)
            ;
            PutItemOutcome out = table.putItem(item);
            System.out.println(out);
        }
        Index lsi = table.getIndex(RANGE_GSI_NAME);
        ItemCollection<?> col = lsi.scan(
                new ScanFilter(GSI_HASH_KEY_NAME).eq(hashkeyval),
                new ScanFilter(GSI_RANGE_KEY_NAME).between(0, 10));
        int count = 0;
        for (Item it : col) {
            System.out.println(it);
            count++;
        }
        assertTrue(0, count);
        col = lsi.scan(new ScanFilter(GSI_HASH_KEY_NAME).eq(hashkeyval),
                       new ScanFilter(GSI_RANGE_KEY_NAME).between(101, 110));
        for (Item it : col) {
            System.out.println(it);
            count++;
        }
        assertTrue(10, count);
    }
}
