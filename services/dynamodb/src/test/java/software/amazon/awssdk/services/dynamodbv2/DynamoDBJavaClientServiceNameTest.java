package software.amazon.awssdk.services.dynamodbv2;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;

public class DynamoDBJavaClientServiceNameTest {

    @Test
    public void client() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        String serviceName = client.getServiceName();
        assertEquals("dynamodb", serviceName);
    }

    @Test
    public void asyncClient() {
        AmazonDynamoDBClient  client = new AmazonDynamoDBAsyncClient();
        String serviceName = client.getServiceName();
        assertEquals("dynamodb", serviceName);
    }

    @Test
    public void subclassing() {
        AmazonDynamoDBClient subclass = new AmazonDynamoDBClient() {};
        String serviceName = subclass.getServiceName();
        assertEquals("dynamodb", serviceName);
    }

}
