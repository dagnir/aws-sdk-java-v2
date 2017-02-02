package software.amazon.awssdk.services.cloudhsm;

import org.junit.BeforeClass;

import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AWSCloudHSMClient client;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        client = new AWSCloudHSMClient(credentials);
    }
}
