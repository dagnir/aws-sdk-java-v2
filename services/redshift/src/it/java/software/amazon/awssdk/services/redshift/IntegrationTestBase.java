package software.amazon.awssdk.services.redshift;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;

import software.amazon.awssdk.test.AWSIntegrationTestBase;


public class IntegrationTestBase extends AWSIntegrationTestBase {
    protected static AmazonRedshiftClient redshift;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        redshift = new AmazonRedshiftClient(getCredentials());
    }


}
