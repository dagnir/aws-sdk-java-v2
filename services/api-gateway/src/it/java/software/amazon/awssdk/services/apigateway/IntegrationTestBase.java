package software.amazon.awssdk.services.apigateway;

import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AmazonApiGateway apiGateway;

    @BeforeClass
    public static void setUp() throws IOException {
        setUpCredentials();
        apiGateway = new AmazonApiGatewayClient(credentials);
    }

}
