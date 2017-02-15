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

package software.amazon.awssdk.services.dynamodbv2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DynamoDBJavaClientServiceNameTest {

    @Test
    public void client() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        String serviceName = client.getServiceName();
        assertEquals("dynamodb", serviceName);
    }

    @Test
    public void asyncClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBAsyncClient();
        String serviceName = client.getServiceName();
        assertEquals("dynamodb", serviceName);
    }

    @Test
    public void subclassing() {
        AmazonDynamoDBClient subclass = new AmazonDynamoDBClient() {
        };
        String serviceName = subclass.getServiceName();
        assertEquals("dynamodb", serviceName);
    }

}
