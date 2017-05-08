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

package software.amazon.awssdk.services.dynamodb;

import java.util.UUID;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicSessionCredentials;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Simple smoke test to make sure the new JSON error unmarshaller works as expected.
 */
public class DynamoDBJavaClientExceptionIntegrationTest extends AwsTestBase {

    private static DynamoDBClient ddb;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        ddb = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @Test
    public void testResourceNotFoundException() {
        try {
            ddb.describeTable(new DescribeTableRequest(UUID.randomUUID().toString()));
            Assert.fail("ResourceNotFoundException is expected.");
        } catch (ResourceNotFoundException e) {
            Assert.assertNotNull(e.getErrorCode());
            Assert.assertNotNull(e.getErrorType());
            Assert.assertNotNull(e.getMessage());
            Assert.assertNotNull(e.getRawResponseContent());
        }
    }

    @Test
    public void testPermissionError() {
        STSClient sts = STSClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        Credentials creds = sts.getFederationToken(new GetFederationTokenRequest()
                .withName("NoAccess")
                .withPolicy(
                        "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Deny\",\"Action\":\"*\",\"Resource\":\"*\"}]}")
                .withDurationSeconds(900)).getCredentials();


        DynamoDBClient client = DynamoDBClient.builder().credentialsProvider(
                new AwsStaticCredentialsProvider(new BasicSessionCredentials(
                creds.getAccessKeyId(),
                creds.getSecretAccessKey(),
                creds.getSessionToken()))).build();

        try {
            client.listTables(new ListTablesRequest());
        } catch (AmazonServiceException e) {
            Assert.assertEquals("AccessDeniedException", e.getErrorCode());
            Assert.assertNotNull(e.getErrorMessage());
            Assert.assertNotNull(e.getMessage());
        }
    }
}
