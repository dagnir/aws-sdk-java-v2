package software.amazon.awssdk.services.glacier;

import org.junit.AfterClass;
import software.amazon.awssdk.services.glacier.model.DeleteVaultRequest;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Integration tests for AWS Glacier.
 */
public class GlacierIntegrationTestBase extends AWSTestBase {

    /** Size of data to upload to Glacier */
    static final long CONTENT_LENGTH = 1024 * 1024 * 5 + 123;

    protected static AmazonGlacierClient glacier;

    protected static String accountId = "599169622985";
    protected static String vaultName = "java-sdk-1332366353936";
    //    protected static String accountId = "-";
    //    protected static String vaultName = "java-sdk-140703";

    /** Release any resources created by the tests */
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (vaultName != null) {
                glacier.deleteVault(new DeleteVaultRequest().withAccountId(accountId).withVaultName(vaultName));
            }
        } catch (Exception e) {

        }
    }

    protected void initializeClient() throws Exception {
        setUpCredentials();
        glacier = new AmazonGlacierClient(credentials);
    }
}

