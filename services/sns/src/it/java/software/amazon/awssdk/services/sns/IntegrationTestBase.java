package software.amazon.awssdk.services.sns;

import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.services.sqs.AmazonSQSClient;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

/**
 * Base class for SNS integration tests; responsible for loading AWS account info for running the
 * tests, and instantiating clients, etc.
 *
 * @author fulghum@amazon.com
 */
public abstract class IntegrationTestBase extends AWSIntegrationTestBase {

    protected static AmazonSNSClient sns;
    protected static AmazonSQSClient sqs;

    /**
     * Loads the AWS account info for the integration tests and creates SNS and SQS clients for
     * tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        sns = new AmazonSNSClient(getCredentials());
        sns.configureRegion(Regions.US_WEST_2);
        sqs = new AmazonSQSClient(getCredentials());
        sqs.configureRegion(Regions.US_WEST_2);
    }

    /**
     * Asserts that the list of topics contains one with the specified ARN, otherwise fails the
     * current test.
     */
    protected void assertTopicIsPresent(List<Topic> topics, String topicArn) {
        for (Topic topic : topics) {
            if (topic.getTopicArn().equals(topicArn)) {
                return;
            }
        }

        fail("Topic '" + topicArn + "' was not present in specified list of topics.");
    }

    /**
     * Asserts that the list of subscriptions contains one with the specified ARN, otherwise fails
     * the current test.
     */
    protected void assertSubscriptionIsPresent(List<Subscription> subscriptions, String subscriptionArn) {
        for (Subscription subscription : subscriptions) {
            if (subscription.getSubscriptionArn().equals(subscriptionArn)) {
                return;
            }
        }

        fail("Subscription '" + subscriptionArn + "' was not present in specified list of subscriptions.");
    }

    /**
     * Turns a one level deep JSON string into a Map.
     */
    protected Map<String, String> parseJSON(String jsonmessage) {
        Map<String, String> parsed = new HashMap<String, String>();
        JsonFactory jf = new JsonFactory();
        try {
            JsonParser parser = jf.createJsonParser(jsonmessage);
            parser.nextToken(); // shift past the START_OBJECT that begins the JSON
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = parser.getCurrentName();
                parser.nextToken(); // move to value, or START_OBJECT/START_ARRAY
                String value = parser.getText();
                parsed.put(fieldname, value);
            }
        } catch (JsonParseException e) {
            // JSON could not be parsed
            e.printStackTrace();
        } catch (IOException e) {
            // Rare exception
        }
        return parsed;
    }

}
