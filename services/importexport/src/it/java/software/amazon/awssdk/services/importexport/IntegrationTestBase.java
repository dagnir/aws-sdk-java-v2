package software.amazon.awssdk.services.importexport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;

import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;

import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for ImportExport integration tests; responsible for loading AWS
 * account info, instantiating clients, providing common helper methods, etc.
 *
 * @author fulghum@amazon.com
 */
public abstract class IntegrationTestBase extends AWSTestBase  {

    protected static AmazonImportExport ie;
    protected static AmazonS3 s3;

    /**
     * Loads the AWS account info for the integration tests and creates the
     * client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();        
        ie = new AmazonImportExportClient(credentials);
        s3 = new AmazonS3Client(credentials);
    }

}
