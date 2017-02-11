package software.amazon.awssdk.services.dynamodbv2;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.dynamodbv2.model.ListStreamsRequest;
import software.amazon.awssdk.test.AWSTestBase;

public class StreamsIntegrationTest extends AWSTestBase {

    private AmazonDynamoDBStreams streams;

    @Before
    public void setup() throws Exception {
        setUpCredentials();
        streams = new AmazonDynamoDBStreamsClient(credentials);
    }

    @Test
    public void testDefaultEndpoint() {
        streams.listStreams(new ListStreamsRequest().withTableName("foo"));
    }

    @Test
    public void testSetEndpoint() {
        streams.setEndpoint("streams.dynamodb.us-west-2.amazonaws.com");
        streams.listStreams(new ListStreamsRequest().withTableName("foo"));
    }

    @Test
    public void testSetRegion() {
        streams.setRegion(Region.getRegion(Regions.EU_WEST_1));
        streams.listStreams(new ListStreamsRequest().withTableName("foo"));
    }

}
