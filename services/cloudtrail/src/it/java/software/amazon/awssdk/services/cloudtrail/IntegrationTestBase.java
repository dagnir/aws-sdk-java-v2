package software.amazon.awssdk.services.cloudtrail;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AWSCloudTrailClient cloudTrail;
    protected static AmazonS3Client s3;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
        cloudTrail = new AWSCloudTrailClient(credentials);
        s3 = new AmazonS3Client(credentials);
    }
}
