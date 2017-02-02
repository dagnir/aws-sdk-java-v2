package software.amazon.awssdk.services.sts;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;

import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;

/**
 * Base class for all STS integration tests that also need IAM
 */
public abstract class IntegrationTestBaseWithIAM extends IntegrationTestBase {

    /** The shared IAM client for all tests to use */
    protected static AmazonIdentityManagementClient iam;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        IntegrationTestBase.setUp();
        iam = new AmazonIdentityManagementClient(credentials);
    }
}
