package software.amazon.awssdk.services.kms;

import org.junit.BeforeClass;

import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AWSKMS kms;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        kms = new AWSKMSClient(credentials);
    }
}
