package software.amazon.awssdk.services.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class IntegrationTestBase extends AWSIntegrationTestBase {

    /** The shared DP client for all tests to use */
    protected static AWSSupportClient support;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        support = new AWSSupportClient(getCredentials());
    }
}

