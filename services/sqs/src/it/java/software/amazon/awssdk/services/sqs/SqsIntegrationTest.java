package software.amazon.awssdk.services.sqs;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for the SQS Java client.
 * 
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class SqsIntegrationTest extends IntegrationTestBase {

    /**
     * Test using a client that points in one region, to work with a queue that's in a different
     * region.
     */
    @Test
    public void testCrossRegionQueueCalls() throws Exception {
        final AmazonSQSAsyncClient sqsClient = createSqsAyncClient();
        sqsClient.setEndpoint("sqs.eu-west-1.amazonaws.com");

        // Create a queue in eu-west-1
        String euQueueURL = createQueue(sqsClient);
        assertNotNull(getQueueCreationDate(sqsClient, euQueueURL));

        // Change the client's endpoint and verify that we can still access the eu-west-1 queue
        sqsClient.setEndpoint("sqs.ap-southeast-2.amazonaws.com");
        assertNotNull(getQueueCreationDate(sqsClient, euQueueURL));
        sqsClient.deleteQueue(euQueueURL);
        sqsClient.shutdown();
    }

    private String getQueueCreationDate(AmazonSQS sqs, String queueURL) {
        GetQueueAttributesRequest request = new GetQueueAttributesRequest(queueURL)
                .withAttributeNames(QueueAttributeName.CreatedTimestamp.toString());
        Map<String, String> attributes = sqs.getQueueAttributes(request).getAttributes();
        return attributes.get(QueueAttributeName.CreatedTimestamp.toString());
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void clockSkewFailure_CorrectsGlobalTimeOffset() {
        final int originalOffset = SDKGlobalConfiguration.getGlobalTimeOffset();
        final int skew = 3600;

        SDKGlobalConfiguration.setGlobalTimeOffset(skew);
        assertEquals(skew, SDKGlobalConfiguration.getGlobalTimeOffset());
        AmazonSQSAsyncClient sqsClient = createSqsAyncClient();
        sqsClient.listQueues();
        assertThat("Clockskew is fixed!", SDKGlobalConfiguration.getGlobalTimeOffset(), lessThan(skew));
        // subsequent changes to the global time offset won't affect existing client
        SDKGlobalConfiguration.setGlobalTimeOffset(skew);
        sqsClient.listQueues();
        assertEquals(skew, SDKGlobalConfiguration.getGlobalTimeOffset());
        sqsClient.shutdown();

        SDKGlobalConfiguration.setGlobalTimeOffset(originalOffset);
    }
}
