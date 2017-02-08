package software.amazon.awssdk.services.workspaces;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AmazonWorkspaces client;

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        client = new AmazonWorkspacesClient(credentials);
    }

    @AfterClass
    public static void tearDown() {
        client.shutdown();
    }
}
