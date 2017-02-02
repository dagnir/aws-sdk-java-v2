package software.amazon.awssdk.services.swf;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;

import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.simpleworkflow.AmazonSimpleWorkflowClient;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class IntegrationTestBase extends AWSIntegrationTestBase {

    protected static AmazonSimpleWorkflowClient swf;

    /**
     * Loads the AWS account info for the integration tests and creates a client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        swf = new AmazonSimpleWorkflowClient(getCredentials());
        swf.configureRegion(Regions.US_EAST_1);
    }

}
