package software.amazon.awssdk.services.sqs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueResult;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.test.AWSTestBase;
import software.amazon.awssdk.util.StringUtils;

/**
 * Base class for SQS integration tests. Provides convenience methods for creating test data, and
 * automatically loads AWS credentials from a properties file on disk and instantiates clients for
 * the individual tests to use.
 * 
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class IntegrationTestBase extends AWSTestBase {

    /**
     * Creating new clients is expensive so we share one across tests when possible. Tests that need
     * to do something special can create their own clients specifically for their test case. We
     * subclass SQS client to prevent a test from accidently shutting down the client as that would
     * cause subsequent tests to fail.
     */
    private static class SharedSqsClient extends AmazonSQSAsyncClient {
        public SharedSqsClient(AWSCredentials credentials) {
            super(credentials);
        }

        @Override
        public void shutdown() {
            throw new IllegalAccessError("Cannot shut down the shared client. "
                    + "If a test requires a client to be shutdown please create a new one specifically for that test");
        }
    }

    /** The SQS client for all tests to use */
    private static AmazonSQSAsyncClient sqs;

    /** Random number used for naming message attributes. */
    private static final Random random = new Random(System.currentTimeMillis());

    /**
     * Account ID of the AWS Account identified by the credentials provider setup in AWSTestBase.
     * Cached for performance
     **/
    private static String accountId;

    /**
     * Loads the AWS account info for the integration tests and creates an SQS client for tests to
     * use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        sqs = new SharedSqsClient(credentials).withRegion(Region.getRegion(Regions.US_EAST_1));
    }

    protected static AmazonSQSAsync getSharedSqsAsyncClient() {
        return sqs;
    }

    public static AmazonSQSAsyncClient createSqsAyncClient() {
        return new AmazonSQSAsyncClient(credentials).withRegion(Region.getRegion(Regions.US_EAST_1));
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
    protected String createQueue(AmazonSQS sqsClient) {
        CreateQueueResult res = sqsClient.createQueue(getUniqueQueueName());
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
            AmazonIdentityManagementClient iamClient = new AmazonIdentityManagementClient(credentials);
            accountId = parseAccountIdFromArn(iamClient.getUser().getUser().getArn());
        }
        return accountId;
    }

    /**
     * Parse the account ID out of the IAM user arn
     * 
     * @param arn
     *            IAM user ARN
     * @return Account ID if it can be extracted
     * @throws IllegalArgumentException
     *             If ARN is not in a valid format
     */
    private String parseAccountIdFromArn(String arn) throws IllegalArgumentException {
        String[] arnComponents = arn.split(":");
        if(arnComponents.length < 5 || StringUtils.isNullOrEmpty(arnComponents[4])) {
            throw new IllegalArgumentException(String.format("%s is not a valid ARN", arn));
        }
        return arnComponents[4];
    }

}
