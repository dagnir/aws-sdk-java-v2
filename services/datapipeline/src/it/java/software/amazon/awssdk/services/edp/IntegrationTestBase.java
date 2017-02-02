package software.amazon.awssdk.services.edp;


import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;

import software.amazon.awssdk.services.datapipeline.DataPipelineClient;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for all STS integration tests. Loads AWS credentials from a
 * properties file on disk, provides helper methods for tests, and instantiates
 * the STS client object for all tests to use.
 */
public class IntegrationTestBase extends AWSTestBase {

    /** The shared DP client for all tests to use */
    protected static DataPipelineClient edp;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();

        edp = new DataPipelineClient(credentials);
      //  edp.setEndpoint("road-runner-g.amazonaws.com", "datapipeline", "us-east-1");
    }
}
