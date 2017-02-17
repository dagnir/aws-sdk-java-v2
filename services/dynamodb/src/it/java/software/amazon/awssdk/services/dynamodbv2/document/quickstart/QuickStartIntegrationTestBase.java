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

package software.amazon.awssdk.services.dynamodbv2.document.quickstart;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodbv2.document.DynamoDB;

/**
 * Common base class used to initialize and shutdown the DynamoDB instance.
 */
public class QuickStartIntegrationTestBase {
    protected static DynamoDB dynamo;

    protected static String TABLE_NAME = "myTableForMidLevelApi";
    protected static String HASH_KEY_NAME = "myHashKey";
    protected static String RANGE_KEY_NAME = "myRangeKey";

    // local secondary index
    protected static String LSI_NAME = "myLSI";
    protected static String LSI_RANGE_KEY_NAME = "myLsiRangeKey";

    // global secondary index
    protected static String RANGE_GSI_NAME = "myRangeGSI";
    protected static String GSI_HASH_KEY_NAME = "myGsiHashKey";
    protected static String GSI_RANGE_KEY_NAME = "myGsiRangeKey";

    @BeforeClass
    public static void setup() throws InterruptedException {
        //        System.setProperty("javax.net.debug", "ssl");
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsTestCredentials());
        dynamo = new DynamoDB(client);
        new CreateTableIntegrationTest().howToCreateTable();
    }

    @AfterClass
    public static void tearDown() {
        dynamo.shutdown();
    }

    protected static AwsCredentials awsTestCredentials() {
        try {
            return new PropertiesCredentials(new File(
                    System.getProperty("user.home")
                    + "/.aws/awsTestAccount.properties"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
