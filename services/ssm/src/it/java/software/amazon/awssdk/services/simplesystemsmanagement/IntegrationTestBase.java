package software.amazon.awssdk.services.simplesystemsmanagement;

import org.junit.BeforeClass;
import software.amazon.awssdk.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import software.amazon.awssdk.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AWSSimpleSystemsManagement ssm;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        ssm = new AWSSimpleSystemsManagementClient(credentials);
    }

}
