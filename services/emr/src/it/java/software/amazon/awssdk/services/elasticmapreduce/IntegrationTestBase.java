package software.amazon.awssdk.services.elasticmapreduce;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for EMR integration tests. Provides convenience methods for
 * creating test data, and automatically loads AWS credentials from a properties
 * file on disk and instantiates clients for the individual tests to use.
 */
public class IntegrationTestBase extends AWSTestBase {

	/** The EMR client for all tests to use */
    protected static AmazonElasticMapReduce emr;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * EMR client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        emr = new AmazonElasticMapReduceClient(credentials);
    }
}
