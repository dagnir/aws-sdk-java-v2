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

package software.amazon.awssdk.services.rds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.rds.model.AddSourceIdentifierToSubscriptionRequest;
import software.amazon.awssdk.services.rds.model.CreateDBInstanceRequest;
import software.amazon.awssdk.services.rds.model.CreateEventSubscriptionRequest;
import software.amazon.awssdk.services.rds.model.DeleteEventSubscriptionRequest;
import software.amazon.awssdk.services.rds.model.DescribeEventSubscriptionsRequest;
import software.amazon.awssdk.services.rds.model.DescribeEventSubscriptionsResult;
import software.amazon.awssdk.services.rds.model.EventSubscription;
import software.amazon.awssdk.services.rds.model.RemoveSourceIdentifierFromSubscriptionRequest;
import software.amazon.awssdk.services.sns.SNSClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;

public class EventSubscriptionIntegrationTest extends IntegrationTestBase {

    private static final String DB_INSTANCE_CLASS = "db.m1.small";
    private static final int PORT = 1234;
    private static String topicArn;
    private static String SOURCE_TYPE = "db-instance";
    private static final String SUBSCRIPTION_NAME = "java-integ-subscription" + System.currentTimeMillis();
    private String databaseInstanceName1 = "java-integ-test-db1-" + System.currentTimeMillis();
    private String databaseInstanceName2 = "java-integ-test-db2-" + System.currentTimeMillis();

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        IntegrationTestBase.setUp();
        sns = SNSClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        String topicName = "java-sns-policy-integ-test-" + System.currentTimeMillis();
        topicArn = sns.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();
    }

    @AfterClass
    public static void cleanUp() {
        try {
            sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testEventSubscriptionOperations() throws Exception {

        databaseInstancesToRelease.add(databaseInstanceName1);

        // Create a DB
        createDB(databaseInstanceName1);
        createDB(databaseInstanceName2);

        // Create event subscription
        EventSubscription eventSubscription = rds.createEventSubscription(CreateEventSubscriptionRequest.builder()
                                                                                                        .enabled(true).sourceIds(
                        databaseInstanceName1)
                                                                                                        .snsTopicArn(topicArn)
                                                                                                        .subscriptionName(
                                                                                                                SUBSCRIPTION_NAME)
                                                                                                        .sourceType(SOURCE_TYPE)
                                                                                                        .build());
        assertValidEventSubscription(eventSubscription, 1);

        // Describe the event subscription
        DescribeEventSubscriptionsResult describeEventSubscriptionsResult =
                rds.describeEventSubscriptions(DescribeEventSubscriptionsRequest.builder().build());
        assertTrue(describeEventSubscriptionsResult.eventSubscriptionsList().size() > 0);

        describeEventSubscriptionsResult = rds.describeEventSubscriptions(
                DescribeEventSubscriptionsRequest.builder().subscriptionName(SUBSCRIPTION_NAME).build());
        assertEquals(1, describeEventSubscriptionsResult.eventSubscriptionsList().size());
        assertValidEventSubscription(describeEventSubscriptionsResult.eventSubscriptionsList().get(0), 1);

        // Add the resource Id
        eventSubscription = rds.addSourceIdentifierToSubscription(
                AddSourceIdentifierToSubscriptionRequest.builder()
                                                        .sourceIdentifier(databaseInstanceName2)
                                                        .subscriptionName(SUBSCRIPTION_NAME).build());

        describeEventSubscriptionsResult = rds.describeEventSubscriptions(
                DescribeEventSubscriptionsRequest.builder()
                                                 .subscriptionName(SUBSCRIPTION_NAME).build());
        assertEquals(1, describeEventSubscriptionsResult.eventSubscriptionsList().size());
        assertValidEventSubscription(describeEventSubscriptionsResult.eventSubscriptionsList().get(0), 2);

        // Remove the resource Id
        eventSubscription = rds.removeSourceIdentifierFromSubscription(
                RemoveSourceIdentifierFromSubscriptionRequest.builder()
                                                             .sourceIdentifier(databaseInstanceName2)
                                                             .subscriptionName(SUBSCRIPTION_NAME).build());
        assertValidEventSubscription(eventSubscription, 1);

        describeEventSubscriptionsResult = rds.describeEventSubscriptions(
                DescribeEventSubscriptionsRequest.builder()
                                                 .subscriptionName(SUBSCRIPTION_NAME).build());
        assertEquals(1, describeEventSubscriptionsResult.eventSubscriptionsList().size());
        assertValidEventSubscription(describeEventSubscriptionsResult.eventSubscriptionsList().get(0), 1);

        // Delete event subscription
        rds.deleteEventSubscription(DeleteEventSubscriptionRequest.builder().subscriptionName(SUBSCRIPTION_NAME).build());
        waitForEventSubscriptionsToBeDeleted(SUBSCRIPTION_NAME);

    }

    private void assertValidEventSubscription(EventSubscription eventSubscription, int numOfResouces) {
        assertEquals(topicArn, eventSubscription.snsTopicArn());
        assertEquals(true, eventSubscription.enabled());
        assertNotNull(eventSubscription.subscriptionCreationTime());
        assertEquals(numOfResouces, eventSubscription.sourceIdsList().size());
        assertTrue(eventSubscription.sourceIdsList().contains(databaseInstanceName1));
        if (numOfResouces == 2) {
            assertTrue(eventSubscription.sourceIdsList().contains(databaseInstanceName2));
        }
        assertNotNull(eventSubscription.customerAwsId());
    }

    private void waitForEventSubscriptionsToBeDeleted(String subscriptionName) throws InterruptedException {
        DescribeEventSubscriptionsResult describeEventSubscriptionsResult = rds.describeEventSubscriptions(
                DescribeEventSubscriptionsRequest.builder()
                                                 .subscriptionName(SUBSCRIPTION_NAME).build());
        long timeout = 120;
        int count = 0;
        String status = describeEventSubscriptionsResult.eventSubscriptionsList().get(0).status();
        while (status.equals("deleting")) {
            System.out.println("Status: " + status);
            Thread.sleep(1000 * 5);
            try {
                describeEventSubscriptionsResult = rds.describeEventSubscriptions(
                        DescribeEventSubscriptionsRequest.builder()
                                                         .subscriptionName(SUBSCRIPTION_NAME).build());
            } catch (AmazonServiceException e) {
                assertNotNull(e.getMessage());
                assertNotNull(e.getErrorType());
                assertNotNull(e.getErrorType());
                assertNotNull(e.getRequestId());
                return;
            }
            status = describeEventSubscriptionsResult.eventSubscriptionsList().get(0).status();
            if (count++ >= timeout) {
                fail("event subscription never be deleted");
            }
        }
    }

    private void createDB(String instanceIdentifier) {
        rds.createDBInstance(CreateDBInstanceRequest.builder()
                                                    .allocatedStorage(5)
                                                    .dbInstanceClass(DB_INSTANCE_CLASS)
                                                    .dbInstanceIdentifier(instanceIdentifier)
                                                    .engine(ENGINE)
                                                    .dbName("integtestdb")
                                                    .masterUsername("admin")
                                                    .masterUserPassword("passwordmustbelongerthan8char")
                                                    .multiAZ(true).port(PORT)
                                                    .licenseModel("general-public-license").build());
    }

}
