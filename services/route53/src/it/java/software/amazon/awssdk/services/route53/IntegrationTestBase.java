package software.amazon.awssdk.services.route53;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Before;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for Route53 integration tests.
 */
public abstract class IntegrationTestBase extends AWSTestBase {

    /** Shared client for all tests to use */
    protected static AmazonRoute53Client route53;


    /**
     * Loads the AWS account info for the integration tests and creates a client
     * for tests to use.
     */
    @Before
    public void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        route53 = new AmazonRoute53Client(credentials);
        route53.setEndpoint("https://route53.amazonaws.com");
    }

}
