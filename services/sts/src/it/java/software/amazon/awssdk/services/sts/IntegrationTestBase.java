package software.amazon.awssdk.services.sts;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenService;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenServiceClient;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for all STS integration tests. Loads AWS credentials from a
 * properties file on disk, provides helper methods for tests, and instantiates
 * the STS client object for all tests to use.
 */
public abstract class IntegrationTestBase extends AWSTestBase {

    /** The shared STS client for all tests to use */
    protected static AWSSecurityTokenService sts;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        sts = new AWSSecurityTokenServiceClient(credentials);
    }
}
