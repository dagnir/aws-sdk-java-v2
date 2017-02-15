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
import software.amazon.awssdk.services.dynamodbv2.document.quickstart.QuickStartIntegrationTestBase;
import software.amazon.awssdk.services.dynamodbv2.model.TableDescription;

public class DescribeTableIntegrationTest extends QuickStartIntegrationTestBase {
    @Test
    public void test() throws InterruptedException {
        String TABLE_NAME = "myTableForMidLevelApi";
        Table table = dynamo.getTable(TABLE_NAME);
        TableDescription desc = table.describe();
        System.out.println("Table is ready for use! " + desc);
    }
}
