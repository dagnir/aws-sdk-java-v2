package software.amazon.awssdk.services.directconnect;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AmazonDirectConnect dc;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        dc = new AmazonDirectConnectClient(credentials);
        dc.setRegion(Region.getRegion(Regions.US_WEST_1));
    }
}
