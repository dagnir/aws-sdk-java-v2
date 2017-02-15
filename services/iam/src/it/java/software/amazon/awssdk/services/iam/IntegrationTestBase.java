package software.amazon.awssdk.services.iam;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Before;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagement;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for IAM integration tests. Provides convenience methods for
 * creating test data, and automatically loads AWS credentials from a properties
 * file on disk and instantiates clients for the individual tests to use.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class IntegrationTestBase extends AWSTestBase {

    /** The IAM client for all tests to use */
    protected AmazonIdentityManagement iam;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * IAM client for tests to use.
     */
    @Before
    public void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        iam = new AmazonIdentityManagementClient(credentials);
    }

}
