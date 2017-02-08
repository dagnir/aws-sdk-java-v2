/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.global.handlers.TestGlobalRequestHandler;
import software.amazon.awssdk.regions.Regions;

public class GlobalRequestHandlerTest {

    @Before
    public void setup() {
        TestGlobalRequestHandler.reset();
    }

    @Test
    public void clientCreatedWithConstructor_RegistersGlobalHandlers() {
        assertFalse(TestGlobalRequestHandler.wasCalled());
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(new BasicAWSCredentials("akid", "skid"));
        callApi(client);
        assertTrue(TestGlobalRequestHandler.wasCalled());
    }

    @Test
    public void clientCreatedWithBuilder_RegistersGlobalHandlers() {
        assertFalse(TestGlobalRequestHandler.wasCalled());
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("akid", "skid")))
                .withRegion(Regions.US_WEST_2)
                .build();
        callApi(client);
        assertTrue(TestGlobalRequestHandler.wasCalled());
    }

    private void callApi(AmazonDynamoDB client) {
        try {
            client.listTables();
        } catch (SdkBaseException expected) {
        }
    }
}
