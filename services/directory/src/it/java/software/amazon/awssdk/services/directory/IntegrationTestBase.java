package software.amazon.awssdk.services.directory;

import org.junit.BeforeClass;

import software.amazon.awssdk.services.ec2.AmazonEC2Client;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class IntegrationTestBase extends AWSIntegrationTestBase {

    protected static AWSDirectoryServiceClient dsClient;
    protected static AmazonEC2Client ec2Client;

    @BeforeClass
    public static void baseSetupFixture() {
        dsClient = new AWSDirectoryServiceClient(getCredentials());
        ec2Client = new AmazonEC2Client(getCredentials());
    }
}
