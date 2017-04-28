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

package software.amazon.awssdk.services.sqs;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.Before;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.GetUserRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResult;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.test.AwsTestBase;
import software.amazon.awssdk.util.StringUtils;

/**
 * Base class for SQS integration tests. Provides convenience methods for creating test data, and
 * automatically loads AWS credentials from a properties file on disk and instantiates clients for
 * the individual tests to use.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class IntegrationTestBase extends AwsTestBase {

    /**
     * Random number used for naming message attributes.
     */
    private static final Random random = new Random(System.currentTimeMillis());
    /**
     * The SQS client for all tests to use.
     */
    protected SQSAsyncClient sqs;
    /**
     * Account ID of the AWS Account identified by the credentials provider setup in AWSTestBase.
     * Cached for performance
     **/
    private static String accountId;

    /**
     * Loads the AWS account info for the integration tests and creates an SQS client for tests to
     * use.
     */
    @Before
    public void setUp() {
        sqs = createSqsAyncClient();
    }

    public static SQSAsyncClient createSqsAyncClient() {
        return SQSAsyncClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Regions.US_EAST_1.getName())
                .build();
    }

    protected static MessageAttributeValue createRandomStringAttributeValue() {
        return new MessageAttributeValue().withDataType("String").withStringValue(UUID.randomUUID().toString());
    }

    protected static MessageAttributeValue createRandomNumberAttributeValue() {
        return new MessageAttributeValue().withDataType("Number").withStringValue(Integer.toString(random.nextInt()));
    }

    protected static MessageAttributeValue createRandomBinaryAttributeValue() {
        byte[] randomBytes = new byte[10];
        random.nextBytes(randomBytes);
        return new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(randomBytes));
    }

    protected static Map<String, MessageAttributeValue> createRandomAttributeValues(int attrNumber) {
        Map<String, MessageAttributeValue> attrs = new HashMap<String, MessageAttributeValue>();
        for (int i = 0; i < attrNumber; i++) {
            int randomeAttributeType = random.nextInt(3);
            MessageAttributeValue randomAttrValue = null;
            switch (randomeAttributeType) {
                case 0:
                    randomAttrValue = createRandomStringAttributeValue();
                    break;
                case 1:
                    randomAttrValue = createRandomNumberAttributeValue();
                    break;
                case 2:
                    randomAttrValue = createRandomBinaryAttributeValue();
                    break;
                default:
                    break;
            }
            attrs.put("attribute-" + UUID.randomUUID(), randomAttrValue);
        }
        return Collections.unmodifiableMap(attrs);
    }

    /**
     * Helper method to create a SQS queue with a unique name
     *
     * @return The queue url for the created queue
     */
    protected String createQueue(SQSAsyncClient sqsClient) {
        CreateQueueResult res = sqsClient.createQueue(new CreateQueueRequest(getUniqueQueueName())).join();
        return res.getQueueUrl();
    }

    /**
     * Generate a unique queue name to use in tests
     */
    protected String getUniqueQueueName() {
        return String.format("%s-%s", getClass().getSimpleName(), System.currentTimeMillis());
    }

    /**
     * Get the account id of the AWS account used in the tests
     */
    protected String getAccountId() {
        if (accountId == null) {
            IAMClient iamClient = IAMClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
            accountId = parseAccountIdFromArn(iamClient.getUser(new GetUserRequest()).getUser().getArn());
        }
        return accountId;
    }

    /**
     * Parse the account ID out of the IAM user arn
     *
     * @param arn IAM user ARN
     * @return Account ID if it can be extracted
     * @throws IllegalArgumentException If ARN is not in a valid format
     */
    private String parseAccountIdFromArn(String arn) throws IllegalArgumentException {
        String[] arnComponents = arn.split(":");
        if (arnComponents.length < 5 || StringUtils.isNullOrEmpty(arnComponents[4])) {
            throw new IllegalArgumentException(String.format("%s is not a valid ARN", arn));
        }
        return arnComponents[4];
    }
}
